/*
 * Copyright (c) 2026 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import java.lang.reflect.Modifier;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
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

@Configuration
public class EntityControllerOpenApiCustomizer {

    private static final String CREATE_METHOD_NAME = "create";
    private static final String FORM_PARAMS_PARAMETER = "formParams";
    private static final String CONTROLLER_SUFFIX = "Controller";

    private final ApplicationContext applicationContext;

    public EntityControllerOpenApiCustomizer(final ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

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

    private static boolean isInheritedEntityCreate(final String methodName, final Class<?> declaringClass) {
        return CREATE_METHOD_NAME.equals(methodName) && EntityController.class.equals(declaringClass);
    }

    private static boolean isConcreteEntityController(final Class<?> beanType) {
        return beanType != null &&
                EntityController.class.isAssignableFrom(ClassUtils.getUserClass(beanType)) &&
                !Modifier.isAbstract(ClassUtils.getUserClass(beanType).getModifiers());
    }

    private static Optional<EntityControllerTypes> resolveEntityControllerTypes(final Class<?> controllerType) {
        final ResolvableType entityControllerType = ResolvableType.forClass(EntityController.class, controllerType);
        final Class<?> responseType = entityControllerType.getGeneric(0).resolve();
        final Class<?> createType = entityControllerType.getGeneric(1).resolve();

        if (responseType == null || createType == null) {
            return Optional.empty();
        }

        return Optional.of(new EntityControllerTypes(responseType, createType));
    }

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

    private static void removeFormParamsParameter(final Operation operation) {
        if (operation.getParameters() == null) {
            return;
        }

        operation.getParameters().removeIf(EntityControllerOpenApiCustomizer::isFormParamsParameter);
    }

    private static boolean isFormParamsParameter(final Parameter parameter) {
        return parameter != null && FORM_PARAMS_PARAMETER.equals(parameter.getName());
    }

    private static RequestBody formRequestBody(final Class<?> createType) {
        return new RequestBody()
                .required(true)
                .content(new Content().addMediaType(
                        org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE,
                        new MediaType().schema(refSchema(createType))));
    }

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

    private static Schema<?> refSchema(final Class<?> modelType) {
        return new Schema<>().$ref("#/components/schemas/" + schemaName(modelType));
    }

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

    private static Components components(final OpenAPI openApi) {
        if (openApi.getComponents() == null) {
            openApi.setComponents(new Components());
        }

        return openApi.getComponents();
    }

    private static void addSchemas(final Components components, final Class<?> modelType) {
        final Map<String, Schema> schemas = ModelConverters.getInstance().readAll(modelType);
        schemas.forEach((name, schema) -> {
            if (components.getSchemas() == null || !components.getSchemas().containsKey(name)) {
                components.addSchemas(name, schema);
            }
        });
    }

    private static String resourceName(final Class<?> controllerType) {
        final String simpleName = controllerType.getSimpleName();
        if (simpleName.endsWith(CONTROLLER_SUFFIX)) {
            return simpleName.substring(0, simpleName.length() - CONTROLLER_SUFFIX.length());
        }

        return simpleName;
    }

    private static String humanize(final String name) {
        return name.replaceAll("(?<!^)([A-Z])", " $1").toLowerCase(Locale.ROOT);
    }

    private static final class EntityControllerTypes {

        private final Class<?> responseType;
        private final Class<?> createType;

        private EntityControllerTypes(final Class<?> responseType, final Class<?> createType) {
            this.responseType = responseType;
            this.createType = createType;
        }
    }
}
