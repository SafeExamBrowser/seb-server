/*
 *  Copyright (c) 2019 ETH Zürich, IT Services
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.oauth;

import ch.ethz.seb.sebserver.WebSecurityConfig;
import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.LmsSetupDAO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;
import org.springframework.stereotype.Component;

@Lazy
@Component
public class LmsAPIClientDetails  {

    private final PasswordEncoder clientPasswordEncoder;
    private final String clientId;
    private final String clientSecret;
    private final Integer accessTokenValiditySeconds;
    private final LmsSetupDAO lmsSetupDAO;

    public LmsAPIClientDetails(
            @Qualifier(WebSecurityConfig.CLIENT_PASSWORD_ENCODER_BEAN_NAME) final PasswordEncoder clientPasswordEncoder,
            final LmsSetupDAO lmsSetupDAO,
            @Value("${sebserver.webservice.lms.api.clientId}") final String clientId,
            @Value("${sebserver.webservice.api.admin.clientSecret}") final String clientSecret,
            @Value("${sebserver.webservice.lms.api.accessTokenValiditySeconds:-1}") final Integer accessTokenValiditySeconds
    ) {
        this.clientPasswordEncoder = clientPasswordEncoder;
        this.lmsSetupDAO = lmsSetupDAO;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.accessTokenValiditySeconds = accessTokenValiditySeconds;
    }

    public String getClientId() {
        return clientId;
    }

    // It seems this get called multiple times per token request
    // TODO do we need very short time caching here?

    public ClientDetails getClientDetails() {

        final String joinIds = StringUtils.join(
                lmsSetupDAO.allIdsFullIntegration().getOrThrow(),
                Constants.LIST_SEPARATOR
        );

        final BaseClientDetails clientDetails = new BaseClientDetails(
                clientId,
                WebserviceResourceConfiguration.LMS_API_RESOURCE_ID,
                joinIds,
                Constants.OAUTH2_GRANT_TYPE_CLIENT_CREDENTIALS,
                null
        );
        clientDetails.setClientSecret(clientPasswordEncoder.encode(clientSecret));
        clientDetails.setAccessTokenValiditySeconds(accessTokenValiditySeconds);
        return clientDetails;
    }


}
