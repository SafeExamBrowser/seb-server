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
import org.cryptonode.jncryptor.AES256JNCryptor;
import org.cryptonode.jncryptor.JNCryptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.oauth2.provider.token.TokenStore;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.webservice.weblayer.oauth.CachableJdbcTokenStore;

@Configuration
@WebServiceProfile
public class WebserviceConfig {

    public static final String SWAGGER_AUTH_SEB_API = "SEBOAuth";
    public static final String SWAGGER_AUTH_ADMIN_API = "oauth2";

    @Lazy
    @Bean
    public JNCryptor jnCryptor() {
        final AES256JNCryptor aes256jnCryptor = new AES256JNCryptor();
        aes256jnCryptor.setPBKDFIterations(Constants.JN_CRYPTOR_ITERATIONS);
        return aes256jnCryptor;
    }

    @Bean
    public TokenStore tokenStore(final DataSource dataSource) {
        return new CachableJdbcTokenStore(dataSource);
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
                                        .scopes(new Scopes().addString("read", "read").addString("write", "write"))))));

    }

//  @Bean
//  public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
//      return (tomcat) -> tomcat.addConnectorCustomizers((connector) -> {
//          if (connector.getProtocolHandler() instanceof AbstractHttp11Protocol) {
//              System.out.println("*************** tomcatCustomizer");
//              final AbstractHttp11Protocol<?> protocolHandler = (AbstractHttp11Protocol<?>) connector
//                      .getProtocolHandler();
//              protocolHandler.setKeepAliveTimeout(60000);
//              protocolHandler.setMaxKeepAliveRequests(3000);
//              protocolHandler.setUseKeepAliveResponseHeader(true);
//              protocolHandler.setMinSpareThreads(200);
//              protocolHandler.setProcessorCache(-1);
//              protocolHandler.setTcpNoDelay(true);
//              protocolHandler.setThreadPriority(Thread.NORM_PRIORITY + 1);
//              protocolHandler.setMaxConnections(2000);
//              if (protocolHandler instanceof Http11NioProtocol) {
//                  System.out.println("*************** Http11NioProtocol");
//                  ((Http11NioProtocol) protocolHandler).setPollerThreadPriority(Thread.MAX_PRIORITY);
//              }
//
//          }
//      });
//  }

}
