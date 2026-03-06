/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice;

import javax.sql.DataSource;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.apache.catalina.filters.RemoteIpFilter;
import org.cryptonode.jncryptor.AES256JNCryptor;
import org.cryptonode.jncryptor.JNCryptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import ch.ethz.seb.sebserver.gbl.Constants;

@Configuration
public class WebserviceConfig {

    public static final String SWAGGER_AUTH_SEB_API = "SEBOAuth";
    public static final String SWAGGER_AUTH_ADMIN_API = "oauth";

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
                );

    }

}
