/*
 *  Copyright (c) 2019 ETH Zürich, IT Services
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;



import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.TooManyRequests;
import ch.ethz.seb.sebserver.gbl.model.user.TokenLoginInfo;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.TeacherAccountService;
import io.github.bucket4j.local.LocalBucket;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.javassist.tools.web.BadHttpRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

@RestController
@WebServiceProfile
public class AdminJWTAccess {

    private final TeacherAccountService teacherAccountService;
    private final LocalBucket requestRateLimitBucket;

    public AdminJWTAccess(
            final TeacherAccountService teacherAccountService,
            final RateLimitService rateLimitService) {
        this.teacherAccountService = teacherAccountService;
        this.requestRateLimitBucket = rateLimitService.createRequestLimitBucker();
    }

    @RequestMapping(
            path = API.OAUTH_JWT_TOKEN_VERIFY_ENDPOINT,
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public TokenLoginInfo verifyJWTToken(final HttpServletRequest request) {
        if (!this.requestRateLimitBucket.tryConsume(1)) {
            throw new TooManyRequests();
        }

        System.out.println("************* one_time_token_to_verify: " + request.getHeader("one_time_token_to_verify"));
        System.out.println("************* ONE_TIME_TOKEN_TO_VERIFY: " + request.getHeader("ONE_TIME_TOKEN_TO_VERIFY"));

        List<String> headerNames = Collections.list(request.getHeaderNames());
        String loginToken = null;
        if (headerNames.contains("one_time_token_to_verify")) {
            loginToken = request.getHeader("one_time_token_to_verify");
        } else if (headerNames.contains("ONE_TIME_TOKEN_TO_VERIFY")) {
            loginToken = request.getHeader("ONE_TIME_TOKEN_TO_VERIFY");
        }

        if (StringUtils.isBlank(loginToken)) {
            throw new APIMessage.APIMessageException(
                    APIMessage.ErrorMessage.BAD_REQUEST.of("No One Time Token found in Request"));
        }

        final Result<TokenLoginInfo> tokenLoginInfoResult = teacherAccountService
                .verifyOneTimeTokenForTeacherAccount(loginToken);

        if (tokenLoginInfoResult.hasError()) {
            throw new APIMessage.APIMessageException(
                    APIMessage.ErrorMessage.UNAUTHORIZED.of(tokenLoginInfoResult.getError()));
        }

        return tokenLoginInfoResult.get();
    }
}
