/*
 * Copyright (c) 2024 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.oauth.authserver;

import ch.ethz.seb.sebserver.gbl.util.Cryptor;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

@Configuration
public class JWTConfig {

    @Autowired
    private Cryptor cryptor;

    @Bean
    public JwtEncoder jwtEncoder() {
        byte[] secretByte = Utils.toByteArray(cryptor.getInternalSecret256());
        SecretKey secretKey = new SecretKeySpec(secretByte, 0, secretByte.length, MacAlgorithm.HS256.getName());
        ImmutableSecret<SecurityContext> securityContextImmutableSecret = new ImmutableSecret<>(secretKey);
        return new org.springframework.security.oauth2.jwt.NimbusJwtEncoder(securityContextImmutableSecret);
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        byte[] secretByte = Utils.toByteArray(cryptor.getInternalSecret256());
        SecretKey secretKey = new SecretKeySpec(secretByte, 0, secretByte.length, MacAlgorithm.HS256.getName());
        return NimbusJwtDecoder.withSecretKey(secretKey).build();
    }

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer() {
        return (context) -> {
            if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
                context.getJwsHeader().algorithm(MacAlgorithm.HS256);
            }
        };
    }
    
}
