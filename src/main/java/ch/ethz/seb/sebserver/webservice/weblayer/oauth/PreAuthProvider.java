/*
 * Copyright (c) 2023 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.oauth;

import javax.annotation.PostConstruct;

import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.webservice.weblayer.WebServiceUserDetails;

@Lazy
@Component
@WebServiceProfile
public class PreAuthProvider extends PreAuthenticatedAuthenticationProvider {

    private final WebServiceUserDetails webServiceUserDetails;

    public PreAuthProvider(final WebServiceUserDetails webServiceUserDetails) {
        this.webServiceUserDetails = webServiceUserDetails;
    }

    @PostConstruct
    public void init() {
        super.setPreAuthenticatedUserDetailsService(this.webServiceUserDetails);
    }
}
