/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice;

import ch.ethz.seb.sebserver.gbl.model.Page;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.apache.catalina.filters.RemoteIpFilter;
import org.cryptonode.jncryptor.AES256JNCryptor;
import org.cryptonode.jncryptor.JNCryptor;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.ResolvableType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ch.ethz.seb.sebserver.gbl.Constants;

@Configuration
public class WebserviceConfig {

    public static final String SWAGGER_AUTH_SEB_API = "SEBOAuth";
    public static final String SWAGGER_AUTH_ADMIN_API = "oauth";
    public static final String SWAGGER_AUTH_LMS_API = "LMSOAuth";
    private static final String PAGE_ITEM_SCHEMA_EXTENSION = "x-page-item-schema";
    private static final String API_MESSAGE_SCHEMA = "APIMessage";
    private static final String API_MESSAGE_LIST_SCHEMA = "APIMessageList";
    private static final String API_ERROR_RESPONSE_SCHEMA = "APIErrorResponse";
    private static final List<String> SYNTHETIC_PARAMETER_NAMES = List.of(
            "filterCriteria",
            "allRequestParams",
            "formParams");

    @Lazy
    @Bean
    public JNCryptor jnCryptor() {
        final AES256JNCryptor aes256jnCryptor = new AES256JNCryptor();
        aes256jnCryptor.setPBKDFIterations(Constants.JN_CRYPTOR_ITERATIONS);
        return aes256jnCryptor;
    }

    /** Used to get real remote IP address by using "X-Forwarded-For" and "X-Forwarded-Proto" header.
     * https://tomcat.apache.org/tomcat-7.0-doc/api/org/apache/catalina/filters/RemoteIpFilter.html
     *
     * @return RemoteIpFilter instance */
    @Bean
    public RemoteIpFilter remoteIpFilter() {
        return new RemoteIpFilter();
    }

    @Bean
    public OpenAPI customOpenAPI() {

        return new OpenAPI()
                .info(new Info()
                        .title("SEB Server API")
                        .description("Safe Exam Browser Server REST API — Administration API (OAuth2 password grant) and Exam API (OAuth2 client credentials grant).")
                        .version("2.2"))
                .components(new Components()
                        .addSecuritySchemes(SWAGGER_AUTH_ADMIN_API, new SecurityScheme()
                                .type(SecurityScheme.Type.OAUTH2)
                                .scheme("bearer")
                                .in(SecurityScheme.In.HEADER)
                                .bearerFormat("jwt")
                                .flows(new OAuthFlows().password(new OAuthFlow().tokenUrl("/oauth/token"))))

                        .addSecuritySchemes(SWAGGER_AUTH_SEB_API, new SecurityScheme()
                                .type(SecurityScheme.Type.OAUTH2)
                                .scheme("basic")
                                .in(SecurityScheme.In.HEADER)
                                .flows(new OAuthFlows().clientCredentials(new OAuthFlow()
                                        .tokenUrl("/oauth/token")
                                        .scopes(new Scopes().addString("read", "read").addString("write", "write")))))

                        .addSecuritySchemes(SWAGGER_AUTH_LMS_API, new SecurityScheme()
                                .type(SecurityScheme.Type.OAUTH2)
                                .scheme("basic")
                                .in(SecurityScheme.In.HEADER)
                                .flows(new OAuthFlows().clientCredentials(new OAuthFlow()
                                        .tokenUrl("/oauth/token")
                                        .scopes(new Scopes().addString("lms-api", "lms-api")))))
                );

    }

    /** Customizes operationId values to be unique across controllers by prefixing
     *  with the controller's @Tag name in camelCase. This prevents collisions when
     *  multiple controllers inherit the same base class operations (e.g. getPage, getEntityById).
     *  The same pass also records the concrete Page<T> item schema name for later OpenAPI rewiring. */
    @Bean
    public OperationCustomizer operationIdCustomizer() {
        return (operation, handlerMethod) -> {
            final Tag tag = handlerMethod.getBeanType().getAnnotation(Tag.class);
            if (tag != null && operation.getOperationId() != null) {
                final String prefix = toCamelCase(tag.name());
                if (!operation.getOperationId().startsWith(prefix)) {
                    operation.setOperationId(prefix + "_" + operation.getOperationId());
                }
            }
            final ResolvableType returnType =
                    ResolvableType.forMethodReturnType(handlerMethod.getMethod(), handlerMethod.getBeanType());
            final Class<?> resolvedReturnType = returnType.resolve();
            if (resolvedReturnType != null && Page.class.isAssignableFrom(resolvedReturnType)) {
                final Class<?> itemType = returnType.getGeneric(0).resolve();
                final String schemaName = resolveSchemaName(itemType);
                if (schemaName != null) {
                    operation.addExtension(PAGE_ITEM_SCHEMA_EXTENSION, schemaName);
                }
            }
            return operation;
        };
    }

    /** Fixes Page<T> generic type erasure by creating concrete PageOfXxx schemas
     *  for each paged operation and rewiring the response reference. Without this,
     *  springdoc collapses all Page<T> variants into a single Page schema. */
    @Bean
    public OpenApiCustomizer pageTypeCustomizer() {
        return openApi -> {
            if (openApi.getComponents() == null || openApi.getPaths() == null) {
                return;
            }

            final Map<String, Schema> schemas = openApi.getComponents().getSchemas();
            if (schemas == null) return;
            final Schema<?> basePage = schemas.get("Page");
            if (basePage == null) return;

            openApi.getPaths().values().forEach(pathItem ->
                    pathItem.readOperations().forEach(operation -> {
                        final String itemSchemaName = getStringExtension(operation, PAGE_ITEM_SCHEMA_EXTENSION);
                        if (itemSchemaName == null) {
                            return;
                        }

                        final String pageSchemaName = "PageOf" + itemSchemaName;
                        schemas.computeIfAbsent(
                                pageSchemaName,
                                ignored -> buildConcretePageSchema(basePage, itemSchemaName));

                        if (operation.getResponses() == null) {
                            return;
                        }
                        final io.swagger.v3.oas.models.responses.ApiResponse okResponse =
                                operation.getResponses().get("200");
                        if (okResponse == null) {
                            return;
                        }

                        if (okResponse.getContent() == null) {
                            okResponse.setContent(new Content());
                        }
                        final MediaType jsonMediaType = okResponse.getContent()
                                .computeIfAbsent("application/json", ignored -> new MediaType());
                        jsonMediaType.setSchema(new Schema<>().$ref("#/components/schemas/" + pageSchemaName));
                    }));
        };
    }

    /** Removes synthetic request-body and map-parameter artifacts that springdoc emits
     *  for catch-all MultiValueMap arguments used only to capture query/form parameters. */
    @Bean
    public OpenApiCustomizer requestDocumentationCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }

            openApi.getPaths().values().forEach(pathItem ->
                    pathItem.readOperationsMap().forEach((httpMethod, operation) -> {
                        if (operation.getParameters() != null) {
                            final List<io.swagger.v3.oas.models.parameters.Parameter> filtered =
                                    operation.getParameters().stream()
                                            .filter(parameter -> parameter != null
                                                    && !SYNTHETIC_PARAMETER_NAMES.contains(parameter.getName()))
                                            .toList();
                            operation.setParameters(filtered.isEmpty() ? null : new ArrayList<>(filtered));
                        }

                        if (httpMethod == PathItem.HttpMethod.GET) {
                            operation.setRequestBody(null);
                        }
                    }));
        };
    }

    /** Replaces error responses with a generic error payload contract so 4xx/5xx responses
     *  no longer inherit success schemas from generic controller methods. */
    @Bean
    public OpenApiCustomizer errorResponseCustomizer() {
        return openApi -> {
            if (openApi.getComponents() == null || openApi.getPaths() == null) {
                return;
            }

            ensureErrorSchemas(openApi.getComponents());

            openApi.getPaths().values().forEach(pathItem ->
                    pathItem.readOperations().forEach(operation -> {
                        if (operation.getResponses() == null) {
                            return;
                        }
                        operation.getResponses().forEach((code, response) -> {
                            try {
                                final int status = Integer.parseInt(code);
                                if (status >= 400) {
                                    response.setContent(buildErrorContent());
                                }
                            } catch (final NumberFormatException ignored) {
                                // skip non-numeric response codes like "default"
                            }
                        });
                    }));
        };
    }

    private static Schema<?> buildConcretePageSchema(final Schema<?> basePage, final String itemSchemaName) {
        final Schema<?> concretePageSchema = new Schema<>();
        concretePageSchema.setType(basePage.getType());
        concretePageSchema.setDescription("Page of " + itemSchemaName + " items");
        concretePageSchema.setRequired(basePage.getRequired());
        concretePageSchema.setNullable(basePage.getNullable());
        concretePageSchema.setAdditionalProperties(basePage.getAdditionalProperties());

        if (basePage.getProperties() != null) {
            final Map<String, Schema> properties = new LinkedHashMap<>(basePage.getProperties());
            final ArraySchema contentArray = new ArraySchema();
            contentArray.setDescription("The page content items");
            contentArray.setItems(new Schema<>().$ref("#/components/schemas/" + itemSchemaName));
            properties.put("content", contentArray);
            concretePageSchema.setProperties(properties);
        }

        return concretePageSchema;
    }

    private static void ensureErrorSchemas(final Components components) {
        final Map<String, Schema> schemas = components.getSchemas();
        if (schemas == null || !schemas.containsKey(API_MESSAGE_SCHEMA)) {
            return;
        }

        schemas.computeIfAbsent(API_MESSAGE_LIST_SCHEMA, ignored -> {
            final ArraySchema schema = new ArraySchema();
            schema.setDescription("List of APIMessage error objects");
            schema.setItems(new Schema<>().$ref("#/components/schemas/" + API_MESSAGE_SCHEMA));
            return schema;
        });

        schemas.computeIfAbsent(API_ERROR_RESPONSE_SCHEMA, ignored -> {
            final ComposedSchema schema = new ComposedSchema();
            schema.setDescription("Generic API error response payload");
            schema.addOneOfItem(new Schema<>().$ref("#/components/schemas/" + API_MESSAGE_SCHEMA));
            schema.addOneOfItem(new Schema<>().$ref("#/components/schemas/" + API_MESSAGE_LIST_SCHEMA));
            schema.addOneOfItem(new StringSchema());
            return schema;
        });
    }

    private static Content buildErrorContent() {
        final Content content = new Content();

        final MediaType jsonMediaType = new MediaType();
        jsonMediaType.setSchema(new Schema<>().$ref("#/components/schemas/" + API_ERROR_RESPONSE_SCHEMA));
        content.addMediaType("application/json", jsonMediaType);

        final MediaType textMediaType = new MediaType();
        textMediaType.setSchema(new StringSchema());
        content.addMediaType("text/plain", textMediaType);

        return content;
    }

    private static String getStringExtension(
            final io.swagger.v3.oas.models.Operation operation,
            final String extensionName) {

        if (operation.getExtensions() == null) {
            return null;
        }
        final Object extensionValue = operation.getExtensions().get(extensionName);
        return (extensionValue instanceof String) ? (String) extensionValue : null;
    }

    private static String resolveSchemaName(final Class<?> type) {
        if (type == null) {
            return null;
        }
        final io.swagger.v3.oas.annotations.media.Schema annotation =
                type.getAnnotation(io.swagger.v3.oas.annotations.media.Schema.class);
        if (annotation != null && annotation.name() != null && !annotation.name().isBlank()) {
            return annotation.name();
        }
        return type.getSimpleName();
    }

    private static String toCamelCase(final String text) {
        final String[] parts = text.split("[\\s\\-_]+");
        final StringBuilder sb = new StringBuilder(parts[0].toLowerCase());
        for (int i = 1; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                sb.append(Character.toUpperCase(parts[i].charAt(0)));
                if (parts[i].length() > 1) {
                    sb.append(parts[i].substring(1).toLowerCase());
                }
            }
        }
        return sb.toString();
    }

}
