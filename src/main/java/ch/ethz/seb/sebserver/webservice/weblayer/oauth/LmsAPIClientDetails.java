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
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;
import org.springframework.stereotype.Component;

@Lazy
@Component
public class LmsAPIClientDetails extends BaseClientDetails  {

    public LmsAPIClientDetails(
            @Qualifier(WebSecurityConfig.CLIENT_PASSWORD_ENCODER_BEAN_NAME) final PasswordEncoder clientPasswordEncoder,
            @Value("${sebserver.webservice.lms.api.clientId}") final String clientId,
            @Value("${sebserver.webservice.api.admin.clientSecret}") final String clientSecret,
            @Value("${sebserver.webservice.lms.api.accessTokenValiditySeconds:-1}") final Integer accessTokenValiditySeconds
    ) {
        super(
                clientId,
                WebserviceResourceConfiguration.LMS_API_RESOURCE_ID,
                StringUtils.joinWith(
                        Constants.LIST_SEPARATOR,
                        Constants.OAUTH2_SCOPE_READ,
                        Constants.OAUTH2_SCOPE_WRITE),
                Constants.OAUTH2_GRANT_TYPE_CLIENT_CREDENTIALS,
                null
        );
        super.setClientSecret(clientPasswordEncoder.encode(clientSecret));
        super.setAccessTokenValiditySeconds(accessTokenValiditySeconds);
    }
}
