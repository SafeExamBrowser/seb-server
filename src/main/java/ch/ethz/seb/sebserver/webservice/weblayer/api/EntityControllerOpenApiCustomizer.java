/*
 * Copyright (c) 2026 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Supplier;

import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ResolvableType;
import org.springframework.util.ClassUtils;

/** Fixes OpenAPI docs for generic {@link EntityController} endpoints without overriding every concrete controller.
 * I use this so codegen sees the real create model pair, for example UserMod -> UserInfo, instead of MultiValueMap -> T. */
@Configuration
public class EntityControllerOpenApiCustomizer {

    private static final String CREATE_METHOD_NAME = "create";
    private static final String GET_PAGE_METHOD_NAME = "getPage";
    private static final String FORM_PARAMS_PARAMETER = "formParams";
    private static final String FILTER_CRITERIA_PARAMETER = "filterCriteria";
    private static final String CONTROLLER_SUFFIX = "Controller";
    private static final String QUERY_PARAMETER_LOCATION = "query";
    private static final String SPRINGDOC_COLLISION_SUFFIX_PATTERN = "_\\d+$";

    private static final Map<Class<?>, List<EntityFilterParameter>> ENTITY_FILTER_PARAMETERS = Map.of(
            UserInfo.class, List.of(
                    stringFilter(Entity.FILTER_ATTR_NAME, "Filters user accounts by first or full name."),
                    stringFilter(UserInfo.FILTER_ATTR_SURNAME, "Filters user accounts by surname."),
                    stringFilter(UserInfo.FILTER_ATTR_USER_NAME, "Filters user accounts by login username."),
                    stringFilter(UserInfo.FILTER_ATTR_EMAIL, "Filters user accounts by email address."),
                    stringFilter(UserInfo.FILTER_ATTR_LANGUAGE, "Filters user accounts by language."),
                    stringFilter(UserInfo.FILTER_ATTR_ROLE, "Filters user accounts by role."),
                    booleanFilter(Entity.FILTER_ATTR_ACTIVE, "Filters user accounts by active state.")));

    private final ApplicationContext applicationContext;

    /** Keeps the Spring context so I can find all concrete entity controllers later.
     * That matters because some schemas need to be added globally, not only while one operation is being customized. */
    public EntityControllerOpenApiCustomizer(final ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /** Patches the inherited {@code EntityController#create} operation when Springdoc is building one endpoint.
     * This is where the vague generic create docs get replaced with the concrete create and response models. */
    @Bean
    public OperationCustomizer entityControllerCreateOperationCustomizer() {
        return (operation, handlerMethod) -> {
            if (!isInheritedEntityCreate(handlerMethod.getMethod().getName(), handlerMethod.getMethod().getDeclaringClass())) {
                return operation;
            }

            final Class<?> controllerType = ClassUtils.getUserClass(handlerMethod.getBeanType());
            final Optional<EntityControllerTypes> types = resolveEntityControllerTypes(controllerType);
            if (types.isEmpty()) {
                return operation;
            }

            customizeCreateOperation(operation, controllerType, types.get());
            return operation;
        };
    }

    /** Sets stable {@code operationId}s on the other inherited {@link EntityController} and
     * {@link ActivatableEntityController} endpoints. Without this Springdoc emits collision-suffixed names like
     * {@code getPage_1} or {@code hardDelete_1} which propagate into every generated client identifier. */
    @Bean
    public OperationCustomizer entityControllerInheritedOperationIdCustomizer() {
        return (operation, handlerMethod) -> {
            final Class<?> declaringClass = handlerMethod.getMethod().getDeclaringClass();
            final String methodName = handlerMethod.getMethod().getName();
            if (!needsDomainOperationId(operation, methodName, declaringClass)) {
                return operation;
            }

            // 'create' has its own customizer above that does more than rename it.
            if (isInheritedEntityCreate(methodName, declaringClass)) {
                return operation;
            }

            final Class<?> controllerType = ClassUtils.getUserClass(handlerMethod.getBeanType());
            final String resourceName = resourceName(controllerType);
            final String operationId = inheritedOperationId(methodName, resourceName);
            if (operationId != null) {
                operation.setOperationId(operationId);
            }
            removeGenericFilterParameter(operation);
            addEntityFilterParameters(operation, methodName, controllerType);
            return operation;
        };
    }

    /** Adds explicit flat query parameters for the generic {@code getPage} filter map.
     * The backend still receives a MultiValueMap, but generated clients see typed domain filters. */
    private static void addEntityFilterParameters(
            final Operation operation,
            final String methodName,
            final Class<?> controllerType) {

        if (!GET_PAGE_METHOD_NAME.equals(methodName)) {
            return;
        }

        resolveEntityControllerTypes(controllerType)
                .map(types -> ENTITY_FILTER_PARAMETERS.get(types.responseType))
                .ifPresent(filterParameters -> filterParameters.forEach(parameter ->
                        addQueryParameter(operation, parameter)));
    }

    /** Adds one query parameter if Springdoc has not already put a parameter with the same name on the operation. */
    private static void addQueryParameter(final Operation operation, final EntityFilterParameter filterParameter) {
        if (operation.getParameters() == null) {
            operation.setParameters(new ArrayList<>());
        }

        final boolean alreadyDocumented = operation
                .getParameters()
                .stream()
                .anyMatch(parameter -> parameter != null && filterParameter.name.equals(parameter.getName()));
        if (alreadyDocumented) {
            return;
        }

        operation.addParametersItem(new Parameter()
                .name(filterParameter.name)
                .in(QUERY_PARAMETER_LOCATION)
                .required(false)
                .description(filterParameter.description)
                .schema(filterParameter.schema()));
    }

    private static EntityFilterParameter stringFilter(final String name, final String description) {
        return new EntityFilterParameter(name, StringSchema::new, description);
    }

    private static EntityFilterParameter booleanFilter(final String name, final String description) {
        return new EntityFilterParameter(name, BooleanSchema::new, description);
    }

    /** Drops the generic {@code filterCriteria} {@link org.springframework.util.MultiValueMap} parameter that Springdoc
     * generates for inherited list endpoints. Callers can still pass per-domain filters as flat query parameters at runtime;
     * removing it here keeps the generated client signatures focused on documented params. */
    private static void removeGenericFilterParameter(final Operation operation) {
        if (operation.getParameters() == null) {
            return;
        }

        operation.getParameters().removeIf(
                parameter -> parameter != null && FILTER_CRITERIA_PARAMETER.equals(parameter.getName()));
    }

    /** Whether a method was declared on one of the generic entity base controllers (i.e. not on a concrete subclass).
     * Only inherited methods need an explicit operationId — methods documented on the concrete controller already have one. */
    private static boolean isGenericEntityBase(final Class<?> declaringClass) {
        return EntityController.class.equals(declaringClass)
                || ActivatableEntityController.class.equals(declaringClass)
                || ReadonlyEntityController.class.equals(declaringClass);
    }

    /** Identifies endpoints where Springdoc fell back to method names such as {@code getPage} or {@code getPage_1}.
     * Those names are technically valid OpenAPI, but they generate useless SDK method names. */
    private static boolean needsDomainOperationId(
            final Operation operation,
            final String methodName,
            final Class<?> declaringClass) {

        return isGenericEntityBase(declaringClass)
                || isGenericOperationId(operation.getOperationId(), methodName);
    }

    private static boolean isGenericOperationId(final String operationId, final String methodName) {
        if (operationId == null) {
            return false;
        }

        final String normalizedOperationId = operationId.replaceFirst(SPRINGDOC_COLLISION_SUFFIX_PATTERN, "");
        return methodName.equals(normalizedOperationId) && isMappedOperationMethod(methodName);
    }

    private static boolean isMappedOperationMethod(final String methodName) {
        return switch (methodName) {
            case "getPage",
                 "getBy",
                 "savePut",
                 "hardDelete",
                 "hardDeleteAll",
                 "forceHardDelete",
                 "getDependencies",
                 "getNames",
                 "getForIds",
                 "allActive",
                 "allInactive",
                 "activate",
                 "deactivate",
                 "create" -> true;
            default -> false;
        };
    }

    /** Maps an inherited generic method name to a domain-named operationId.
     * For example, {@code getPage} on a UserAccount controller becomes {@code getUserAccounts}. */
    private static String inheritedOperationId(final String methodName, final String resourceName) {
        return switch (methodName) {
            case "getPage" -> "get" + resourceName + "s";
            case "create" -> "create" + resourceName;
            case "getBy" -> "get" + resourceName + "ById";
            case "savePut" -> "edit" + resourceName;
            case "hardDelete" -> "delete" + resourceName;
            case "hardDeleteAll" -> "deleteAll" + resourceName + "s";
            case "forceHardDelete" -> "forceDelete" + resourceName;
            case "getDependencies" -> "get" + resourceName + "Dependencies";
            case "getNames" -> "get" + resourceName + "Names";
            case "getForIds" -> "get" + resourceName + "sByIds";
            case "allActive" -> "getActive" + resourceName + "s";
            case "allInactive" -> "getInactive" + resourceName + "s";
            case "activate" -> "activate" + resourceName;
            case "deactivate" -> "deactivate" + resourceName;
            default -> null;
        };
    }

    /** Adds the concrete entity schemas to the OpenAPI components section.
     * This makes sure the references created by the operation customizer point to schemas that actually exist. */
    @Bean
    public OpenApiCustomizer entityControllerSchemaOpenApiCustomizer() {
        return openApi -> {
            final Components components = components(openApi);
            for (final String beanName : this.applicationContext.getBeanDefinitionNames()) {
                final Class<?> beanType = this.applicationContext.getType(beanName);
                if (!isConcreteEntityController(beanType)) {
                    continue;
                }

                resolveEntityControllerTypes(ClassUtils.getUserClass(beanType)).ifPresent(types -> {
                    addSchemas(components, types.responseType);
                    addSchemas(components, types.createType);
                });
            }
        };
    }

    /** Exposes the catalogue of stable APIMessage codes as a named {@code ErrorCode} OpenAPI enum.
     * The frontend imports this generated enum to reference known codes instead of hard-coding them, while
     * {@link APIMessage#messageCode} stays a free-form string so unrecognized codes never break client parsing. */
    @Bean
    public OpenApiCustomizer errorCodeSchemaOpenApiCustomizer() {
        return openApi -> {
            final Map<String, String> codeToName = new LinkedHashMap<>();
            final Map<String, String> codeToMessage = new LinkedHashMap<>();
            Arrays.stream(APIMessage.ErrorMessage.values()).forEach(error -> {
                codeToName.putIfAbsent(error.messageCode, error.name());
                codeToMessage.putIfAbsent(error.messageCode, error.systemMessage);
            });
            final List<String> codes = codeToName.keySet().stream().sorted().toList();

            final StringSchema errorCodeSchema = new StringSchema();
            errorCodeSchema.setDescription(
                    "Catalogue of stable SEB Server APIMessage codes (the possible APIMessage.messageCode values).");
            codes.forEach(errorCodeSchema::addEnumItem);
            errorCodeSchema.addExtension("x-enum-varnames", codes.stream().map(codeToName::get).toList());
            errorCodeSchema.addExtension("x-enum-descriptions", codes.stream().map(codeToMessage::get).toList());
            components(openApi).addSchemas("ErrorCode", errorCodeSchema);
        };
    }

    /** Publishes the field-validation message catalogue (the
     * {@code sebserver.form.validation.fieldError.*} entries of {@code messages.properties}) as a
     * root {@code x-field-validation-messages} extension, so the frontend can generate its 1200
     * field-error text from the backend instead of re-maintaining it. */
    @Bean
    public OpenApiCustomizer fieldValidationMessagesOpenApiCustomizer() {
        return openApi -> {
            final Properties messages = new Properties();
            try (final InputStream stream = getClass().getResourceAsStream("/messages.properties")) {
                if (stream == null) {
                    return;
                }
                messages.load(stream);
            } catch (final IOException e) {
                return;
            }

            final String prefix = "sebserver.form.validation.fieldError.";
            final Map<String, String> ruleMessages = new LinkedHashMap<>();
            messages.stringPropertyNames().stream().sorted().forEach(key -> {
                if (key.startsWith(prefix)) {
                    ruleMessages.put(key.substring(prefix.length()), messages.getProperty(key));
                }
            });
            openApi.addExtension("x-field-validation-messages", ruleMessages);
        };
    }

    /** Checks that this is exactly the inherited generic create method.
     * I only want to patch that method, not normal create methods that a controller documents itself. */
    private static boolean isInheritedEntityCreate(final String methodName, final Class<?> declaringClass) {
        return CREATE_METHOD_NAME.equals(methodName)
                && (EntityController.class.equals(declaringClass)
                        || ReadonlyEntityController.class.equals(declaringClass));
    }

    /** Checks whether a Spring bean is a real entity controller that can expose inherited endpoints.
     * Abstract base classes are skipped because they are not actual API resources. */
    private static boolean isConcreteEntityController(final Class<?> beanType) {
        return beanType != null &&
                EntityController.class.isAssignableFrom(ClassUtils.getUserClass(beanType)) &&
                !Modifier.isAbstract(ClassUtils.getUserClass(beanType).getModifiers());
    }

    /** Resolves the {@code T} and {@code M} from {@code EntityController<T, M>} for one concrete controller.
     * For UserAccountController, this is the step that turns the generic types into UserInfo and UserMod. */
    private static Optional<EntityControllerTypes> resolveEntityControllerTypes(final Class<?> controllerType) {
        final ResolvableType entityControllerType = ResolvableType.forClass(EntityController.class, controllerType);
        final Class<?> responseType = entityControllerType.getGeneric(0).resolve();
        final Class<?> createType = entityControllerType.getGeneric(1).resolve();

        if (responseType == null || createType == null) {
            return Optional.empty();
        }

        return Optional.of(new EntityControllerTypes(responseType, createType));
    }

    /** Rewrites the OpenAPI operation for a generic create endpoint.
     * It sets a useful operationId, replaces the request body with the create model, and fixes the 200 response model. */
    private static void customizeCreateOperation(
            final Operation operation,
            final Class<?> controllerType,
            final EntityControllerTypes types) {

        final String resourceName = resourceName(controllerType);
        final String humanName = humanize(resourceName);

        operation.setOperationId("create" + resourceName);
        operation.setSummary("Create " + humanName);
        operation.setDescription("Creates " + humanName + " from application/x-www-form-urlencoded fields matching " +
                types.createType.getSimpleName() + ".");
        removeFormParamsParameter(operation);
        operation.setRequestBody(formRequestBody(types.createType));
        addSuccessResponse(operation, types.responseType, "Created " + humanName + ".");
    }

    /** Removes the old fake {@code formParams} parameter from the generated docs.
     * The real input is now documented as the concrete create model in the request body. */
    private static void removeFormParamsParameter(final Operation operation) {
        if (operation.getParameters() == null) {
            return;
        }

        operation.getParameters().removeIf(EntityControllerOpenApiCustomizer::isFormParamsParameter);
    }

    /** Checks whether a generated OpenAPI parameter is the generic form map placeholder.
     * That placeholder is useful to the backend implementation, but not useful to generated frontend clients. */
    private static boolean isFormParamsParameter(final Parameter parameter) {
        return parameter != null && FORM_PARAMS_PARAMETER.equals(parameter.getName());
    }

    /** Builds the form-urlencoded request body for the concrete create model.
     * The schema is a reference so Orval can generate a named TypeScript type instead of an inline object. */
    private static RequestBody formRequestBody(final Class<?> createType) {
        return new RequestBody()
                .required(true)
                .content(new Content().addMediaType(
                        org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE,
                        new MediaType().schema(refSchema(createType))));
    }

    /** Adds or replaces the 200 response for the create operation.
     * This keeps the response type tied to {@code T}, which is the normal entity info model returned by the backend. */
    private static void addSuccessResponse(
            final Operation operation,
            final Class<?> responseType,
            final String description) {

        ApiResponses responses = operation.getResponses();
        if (responses == null) {
            responses = new ApiResponses();
            operation.setResponses(responses);
        }

        ApiResponse successResponse = responses.get("200");
        if (successResponse == null) {
            successResponse = new ApiResponse();
            responses.addApiResponse("200", successResponse);
        }

        successResponse
                .description(description)
                .content(new Content().addMediaType(
                        org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                        new MediaType().schema(refSchema(responseType))));
    }

    /** Creates a component schema reference for a Java model class.
     * This keeps operation bodies small and points clients to the shared schema definition. */
    private static Schema<?> refSchema(final Class<?> modelType) {
        return new Schema<>().$ref("#/components/schemas/" + schemaName(modelType));
    }

    /** Finds the schema name Swagger uses for a model class.
     * Usually this is just the Java simple name, but the fallback handles cases where Swagger picks another generated name. */
    private static String schemaName(final Class<?> modelType) {
        final Map<String, Schema> schemas = ModelConverters.getInstance().readAll(modelType);
        if (schemas.containsKey(modelType.getSimpleName())) {
            return modelType.getSimpleName();
        }
        if (!schemas.isEmpty()) {
            return schemas.keySet().iterator().next();
        }

        return modelType.getSimpleName();
    }

    /** Gets or creates the OpenAPI components object.
     * The customizer needs this because schemas live under components, not directly on operations. */
    private static Components components(final OpenAPI openApi) {
        if (openApi.getComponents() == null) {
            openApi.setComponents(new Components());
        }

        return openApi.getComponents();
    }

    /** Registers all Swagger schemas needed for a model class if they are missing.
     * This includes nested schemas Swagger discovers while reading the model. */
    private static void addSchemas(final Components components, final Class<?> modelType) {
        final Map<String, Schema> schemas = ModelConverters.getInstance().readAll(modelType);
        schemas.forEach((name, schema) -> {
            if (components.getSchemas() == null || !components.getSchemas().containsKey(name)) {
                components.addSchemas(name, schema);
            }
        });
    }

    /** Turns a controller class name into the resource name used in the operation id.
     * For example, UserAccountController becomes UserAccount, so the generated operation id is createUserAccount. */
    private static String resourceName(final Class<?> controllerType) {
        final String simpleName = controllerType.getSimpleName();
        if (simpleName.endsWith(CONTROLLER_SUFFIX)) {
            return simpleName.substring(0, simpleName.length() - CONTROLLER_SUFFIX.length());
        }

        return simpleName;
    }

    /** Turns a Java-style name into readable OpenAPI text.
     * This is only for summaries/descriptions, not for stable ids used by codegen. */
    private static String humanize(final String name) {
        return name.replaceAll("(?<!^)([A-Z])", " $1").toLowerCase(Locale.ROOT);
    }

    /** Small holder for the two generic model types resolved from EntityController.
     * Keeping them together makes the operation customization code easier to read. */
    private static final class EntityControllerTypes {

        private final Class<?> responseType;
        private final Class<?> createType;

        /** Stores the response model and create model for one concrete controller.
         * For UserAccountController, that means UserInfo and UserMod. */
        private EntityControllerTypes(final Class<?> responseType, final Class<?> createType) {
            this.responseType = responseType;
            this.createType = createType;
        }
    }

    /** OpenAPI-only metadata for one flat entity filter query parameter. */
    private static final class EntityFilterParameter {

        private final String name;
        private final Supplier<Schema<?>> schemaFactory;
        private final String description;

        private EntityFilterParameter(
                final String name,
                final Supplier<Schema<?>> schemaFactory,
                final String description) {

            this.name = name;
            this.schemaFactory = schemaFactory;
            this.description = description;
        }

        private Schema<?> schema() {
            return this.schemaFactory.get();
        }
    }
}
