/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.oauth;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.WebSecurityConfig;
import ch.ethz.seb.sebserver.gbl.util.Utils;

@Lazy
@Component
public final class GuiClientDetails implements ClientDetails {

    private static final long serialVersionUID = 4505193832353978832L;

    public static final String RESOURCE_ID = "seb-server-administration-api";

    private static final Set<String> GRANT_TYPES = Utils.immutableSetOf("password", "refresh_token");
    private static final Set<String> RESOURCE_IDS = Utils.immutableSetOf(RESOURCE_ID);
    private static final Set<String> SCOPES = Utils.immutableSetOf("read", "write");

    private final String guiClientId;
    private final String guiClientSecret;
    private final Integer guiClientAccessTokenValiditySeconds;
    private final Integer guiClientRefreshTokenValiditySeconds;

    public GuiClientDetails(
            @Qualifier(WebSecurityConfig.CLIENT_PASSWORD_ENCODER_BEAN_NAME) final PasswordEncoder clientPasswordEncoder,
            @Value("${sebserver.oauth.clients.guiClient.id}") final String guiClientId,
            @Value("${sebserver.oauth.clients.guiClient.secret}") final String guiClientSecret,
            @Value("${sebserver.oauth.clients.guiClient.accessTokenValiditySeconds}") final Integer guiClientAccessTokenValiditySeconds,
            @Value("${sebserver.oauth.clients.guiClient.refreshTokenValiditySeconds}") final Integer guiClientRefreshTokenValiditySeconds) {

        this.guiClientId = guiClientId;
        this.guiClientSecret = clientPasswordEncoder.encode(guiClientSecret);
        this.guiClientAccessTokenValiditySeconds = guiClientAccessTokenValiditySeconds;
        this.guiClientRefreshTokenValiditySeconds = guiClientRefreshTokenValiditySeconds;
    }

    @Autowired
    @Qualifier(WebSecurityConfig.CLIENT_PASSWORD_ENCODER_BEAN_NAME)
    private PasswordEncoder clientPasswordEncoder;

    @Override
    public String getClientId() {
        return this.guiClientId;
    }

    @Override
    public Set<String> getResourceIds() {
        return RESOURCE_IDS;
    }

    @Override
    public boolean isSecretRequired() {
        return true;
    }

    @Override
    public String getClientSecret() {
        return this.guiClientSecret;
    }

    @Override
    public boolean isScoped() {
        return true;
    }

    @Override
    public Set<String> getScope() {
        return SCOPES;
    }

    @Override
    public Set<String> getAuthorizedGrantTypes() {
        return GRANT_TYPES;
    }

    @Override
    public Set<String> getRegisteredRedirectUri() {
        return Collections.emptySet();
    }

    @Override
    public Collection<GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }

    @Override
    public Integer getAccessTokenValiditySeconds() {
        return this.guiClientAccessTokenValiditySeconds;
    }

    @Override
    public Integer getRefreshTokenValiditySeconds() {
        return this.guiClientRefreshTokenValiditySeconds;
    }

    @Override
    public boolean isAutoApprove(final String scope) {
        return true;
    }

    @Override
    public Map<String, Object> getAdditionalInformation() {
        return Collections.emptyMap();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.guiClientId == null) ? 0 : this.guiClientId.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final GuiClientDetails other = (GuiClientDetails) obj;
        if (this.guiClientId == null) {
            if (other.guiClientId != null)
                return false;
        } else if (!this.guiClientId.equals(other.guiClientId))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "GuiClientDetails [guiClientId=" + this.guiClientId + ", guiClientSecret=" + this.guiClientSecret
                + ", guiClientAccessTokenValiditySeconds=" + this.guiClientAccessTokenValiditySeconds
                + ", guiClientRefreshTokenValiditySeconds=" + this.guiClientRefreshTokenValiditySeconds
                + ", clientPasswordEncoder=" + this.clientPasswordEncoder + "]";
    }

}
