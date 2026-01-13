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
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.TeacherAccountService;
import io.github.bucket4j.local.LocalBucket;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
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
    public TokenLoginInfo verifyJWTToken(@RequestHeader(name = "ONE_TIME_TOKEN_TO_VERIFY") final String loginToken) {

        if (!this.requestRateLimitBucket.tryConsume(1)) {
            throw new TooManyRequests();
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
