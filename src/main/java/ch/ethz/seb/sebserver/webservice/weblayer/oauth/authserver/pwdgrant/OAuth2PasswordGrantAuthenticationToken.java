/*
 * Copyright (c) 2024 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.oauth.authserver.pwdgrant;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

import java.util.*;

public class OAuth2PasswordGrantAuthenticationToken extends AbstractAuthenticationToken {

    private static final long serialVersionUID = -6067207202119450764L;
    
    private final Authentication clientPrincipal;
    private final Set<String> scopes;
    private final Map<String, Object> additionalParameters;
    

    public OAuth2PasswordGrantAuthenticationToken(
            Authentication clientPrincipal, 
            Set<String> scopes, 
            Map<String, Object> additionalParameters) {
        
        super(Collections.emptyList());
        this.clientPrincipal = clientPrincipal;
        this.scopes = Collections.unmodifiableSet(scopes != null ? new HashSet<>(scopes) : Collections.emptySet());
        this.additionalParameters = Collections.unmodifiableMap(additionalParameters != null ? new HashMap<>(additionalParameters) : Collections.emptyMap());
    }
    
    public AuthorizationGrantType getGrantType() {
        return AuthorizationGrantType.PASSWORD;
    }

    @Override
    public Object getPrincipal() {
        return this.clientPrincipal;
    }

    @Override
    public Object getCredentials() {
        return "";
    }
    
    public Set<String> getScopes() {
        return this.scopes;
    }
    
    public Map<String, Object> getAdditionalParameters() {
        return this.additionalParameters;
    }
    
}
