/*
 * Copyright (c) 2024 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.oauth.resserver;

import ch.ethz.seb.sebserver.gbl.api.API;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.List;

@Configuration
@EnableWebSecurity
public class AdminAPIResourceServerConfig {

    private static final Logger log = LoggerFactory.getLogger(AdminAPIResourceServerConfig.class);
    
    @Autowired
    private JwtDecoder jwtDecoder;
    @Autowired
    private OAuth2AuthorizationService oAuth2AuthorizationService;
    @Value("${sebserver.webservice.api.admin.endpoint}")
    private String adminAPIEndpoint;

    @Bean
    @Order(3)
    SecurityFilterChain adminResourceSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher(adminAPIEndpoint + "/**")
                .authorizeHttpRequests((requests) -> requests
                        .requestMatchers(
                                adminAPIEndpoint + API.INFO_ENDPOINT + API.INFO_INST_PATH_SEGMENT + "/**",
                                adminAPIEndpoint + API.INFO_ENDPOINT + API.INFO_INST_PATH_SEGMENT,
                                adminAPIEndpoint + API.REGISTER_ENDPOINT,
                                adminAPIEndpoint + API.SEB_VERSION_PAGE_ENDPOINT
                        ).permitAll()
                        .anyRequest().authenticated())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .headers(c -> c.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
                .exceptionHandling( c -> c.authenticationEntryPoint(new UnauthorizedRequestHandler("AdminAPIResourceServerConfig")))
        ;

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(new AdminAPIJwtGrantedAuthoritiesConverter(oAuth2AuthorizationService));
        http
                .oauth2ResourceServer(resServer -> resServer
                        .authenticationEntryPoint(new UnauthorizedRequestHandler("AdminAPIResourceServerConfig"))
                        .jwt(jwt -> jwt.decoder(jwtDecoder).jwtAuthenticationConverter(jwtAuthenticationConverter))
                );

        return http.build();
    }
    
    
    private static class AdminAPIJwtGrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

        private final OAuth2AuthorizationService oAuth2AuthorizationService;
        private final JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

        private AdminAPIJwtGrantedAuthoritiesConverter(OAuth2AuthorizationService oAuth2AuthorizationService) {
            this.oAuth2AuthorizationService = oAuth2AuthorizationService;
        }

        @Override
        public Collection<GrantedAuthority> convert(final Jwt source) {
            try {

                final OAuth2Authorization validAuth = oAuth2AuthorizationService.findByToken(source.getTokenValue(), OAuth2TokenType.ACCESS_TOKEN);
                if (validAuth == null) {
                    throw new InvalidBearerTokenException("Invalid Bearer Token");
                }

                final List<String> scopes = source.getClaim("scope");
                if (scopes != null && !scopes.contains(API.WEB_API_SCOPE_NAME)) {
                    throw new InvalidBearerTokenException("Invalid scope");
                }

                return jwtGrantedAuthoritiesConverter.convert(source);
            } catch (Exception e) {
                throw new InvalidBearerTokenException("Failed to verify Token: ", e);
            }
        }
    }
    
}
