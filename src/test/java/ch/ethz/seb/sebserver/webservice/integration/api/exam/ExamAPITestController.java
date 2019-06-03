/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.integration.api.exam;

import java.util.Set;

import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;

@RestController
@RequestMapping("${sebserver.webservice.api.exam.endpoint.v1}")
@WebServiceProfile
public class ExamAPITestController {

    @RequestMapping(value = "/hello", method = RequestMethod.GET)
    public String helloFromWebService(final OAuth2Authentication principal) {
        final Set<String> scope = principal.getOAuth2Request().getScope();
        System.out.println("OAuth 2 exam client scope is: " + scope);
        return "Hello From Exam-Web-Service";
    }

}
