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
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.List;

@Configuration
@EnableWebSecurity
public class LmsAPIResourceServerConfig {

    private static final Logger log = LoggerFactory.getLogger(LmsAPIResourceServerConfig.class);

    @Autowired
    private JwtDecoder jwtDecoder;
    @Value("${sebserver.webservice.lms.api.endpoint}")
    private String lmsAPIEndpoint;


    @Bean
    @Order(4)
    SecurityFilterChain lmsResourceSecurityFilterChain(HttpSecurity http) throws Exception {

        http.securityMatcher(lmsAPIEndpoint + "/**")
                .authorizeHttpRequests((requests) -> requests
                        .anyRequest().authenticated())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .headers(c -> c.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
                .exceptionHandling( c -> c.authenticationEntryPoint(new UnauthorizedRequestHandler("LmsAPIResourceServerConfig")))
        ;

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(new LmsAPIResourceServerConfig.LmsAPIJwtGrantedAuthoritiesConverter());
        http
                .oauth2ResourceServer(resServer -> resServer
                        .authenticationEntryPoint(new UnauthorizedRequestHandler("LmsAPIResourceServerConfig"))
                        .jwt(jwt -> jwt.decoder(jwtDecoder).jwtAuthenticationConverter(jwtAuthenticationConverter))
                );

        return http.build();
    }


    private static class LmsAPIJwtGrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

        private final JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

        @Override
        public Collection<GrantedAuthority> convert(final Jwt source) {
            try {
                final List<String> scopes = source.getClaim("scope");
                if (!scopes.contains(API.WEB_API_SCOPE_NAME)) {
                    throw new InvalidBearerTokenException("Invalid scope");
                }

                return jwtGrantedAuthoritiesConverter.convert(source);
            } catch (Exception e) {
                log.warn("Failed to extract scope claim from JWT: {}", source);
                throw new InvalidBearerTokenException("Invalid scope");
            }
        }
    }
}
