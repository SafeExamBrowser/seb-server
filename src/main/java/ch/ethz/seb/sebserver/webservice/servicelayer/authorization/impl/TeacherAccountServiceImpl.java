/*
 *  Copyright (c) 2019 ETH Zürich, IT Services
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.authorization.impl;

import java.util.*;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.user.*;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Cryptor;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.TeacherAccountService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.FullLmsIntegrationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ScreenProctoringService;
import ch.ethz.seb.sebserver.webservice.weblayer.oauth.AdminAPIClientDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.UnauthorizedUserException;
import org.springframework.security.oauth2.provider.endpoint.TokenEndpoint;
import org.springframework.stereotype.Service;

@Lazy
@Service
@WebServiceProfile
public class TeacherAccountServiceImpl implements TeacherAccountService {

    private static final Logger log = LoggerFactory.getLogger(TeacherAccountServiceImpl.class);

    private static final String SUBJECT_CLAIM_NAME = "sub";
    private static final String USER_CLAIM = "usr";
    private static final String EXAM_ID_CLAIM = "exam";

    private final UserDAO userDAO;
    private final ScreenProctoringService screenProctoringService;
    private final ExamDAO examDAO;
    private final Cryptor cryptor;
    final TokenEndpoint tokenEndpoint;
    private final AdminAPIClientDetails adminAPIClientDetails;

    public TeacherAccountServiceImpl(
            final UserDAO userDAO,
            final ScreenProctoringService screenProctoringService,
            final ExamDAO examDAO,
            final Cryptor cryptor,
            final TokenEndpoint tokenEndpoint,
            final AdminAPIClientDetails adminAPIClientDetails) {

        this.userDAO = userDAO;
        this.screenProctoringService = screenProctoringService;
        this.examDAO = examDAO;
        this.cryptor = cryptor;
        this.tokenEndpoint = tokenEndpoint;
        this.adminAPIClientDetails = adminAPIClientDetails;
    }

    @Override
    public String getTeacherAccountIdentifier(
            final String lmsId,
            final String userId) {

        if (lmsId == null || userId == null) {
            throw new RuntimeException("examId and/or userId cannot be null");
        }

        return "TEACHER_" + Constants.UNDERLINE + lmsId + Constants.UNDERLINE + userId;
    }

    @Override
    public Result<UserInfo> createNewTeacherAccountForExam(
            final Exam exam,
            final FullLmsIntegrationService.AdHocAccountData adHocAccountData) {

        return Result.tryCatch(() -> {

            final String uuid = UUID.randomUUID().toString();
            DateTimeZone dtz = DateTimeZone.UTC;
            if (StringUtils.isNotBlank(adHocAccountData.timezone)) {
                try {
                    dtz = DateTimeZone.forID(adHocAccountData.timezone);
                } catch (final Exception e) {
                    log.warn("Failed to set requested time zone for ad-hoc teacher account: {}", adHocAccountData.timezone);
                }
            }

            final UserMod adHocTeacherUser = new UserMod(
                    getTeacherAccountIdentifier(exam, adHocAccountData),
                    exam.institutionId,
                    adHocAccountData.firstName != null ? adHocAccountData.firstName : adHocAccountData.userId,
                    adHocAccountData.lastName != null ? adHocAccountData.lastName : adHocAccountData.userId,
                    adHocAccountData.username != null ? adHocAccountData.username : adHocAccountData.userId,
                    uuid,
                    uuid,
                    adHocAccountData.userMail,
                    Locale.ENGLISH,
                    dtz,
                    true,
                    false,
                    Utils.immutableSetOf(UserRole.TEACHER.name()));

            return userDAO.createNew(adHocTeacherUser)
                    .flatMap(account -> userDAO.setActive(account, true))
                    .getOrThrow();

        });
    }

    @Override
    public Result<Exam> deactivateTeacherAccountsForExam(final Exam exam) {
        return Result.tryCatch(() -> {

            exam.supporter.stream()
                    .map(userUUID -> userDAO.byModelId(userUUID).getOr(null))
                    .filter(user -> user != null && user.roles.contains(UserRole.TEACHER.name()))
                    .filter( user -> user.roles.size() == 1)
                    .forEach( user -> userDAO.setActive(user, false));

            return exam;
        });
    }

    @Override
    public Result<String> getOneTimeTokenForTeacherAccount(
            final Exam exam,
            final FullLmsIntegrationService.AdHocAccountData adHocAccountData,
            final boolean createIfNotExists) {

        return this.userDAO
                .byModelId(getTeacherAccountIdentifier(exam, adHocAccountData))
                .onErrorDo(error -> handleAccountDoesNotExistYet(createIfNotExists, exam, adHocAccountData))
                .map(account -> applySupporter(account, exam))
                .map(account -> this.createOneTimeToken(account, exam.id));
    }

    @Override
    public Result<TokenLoginInfo> verifyOneTimeTokenForTeacherAccount(final String loginToken) {
        return Result.tryCatch(() -> {

            final Claims claims;
            try {
                claims = checkJWTValid(loginToken);
            } catch (final Exception e) {
                throw new UnauthorizedUserException("Invalid One Time JWT", e);
            }
            final String userId = claims.get(USER_CLAIM, String.class);

            // check if requested user exists
            final UserInfo user = this.userDAO
                    .byModelId(userId)
                    .getOrThrow(error -> new BadCredentialsException("Unknown user claim", error));

            // login the user by getting access token
            final Map<String, String> params = new HashMap<>();
            params.put(Constants.OAUTH2_GRANT_TYPE, Constants.OAUTH2_GRANT_TYPE_PASSWORD);
            params.put(Constants.OAUTH2_USER_NAME, user.username);
            params.put(Constants.OAUTH2_GRANT_TYPE_PASSWORD, claims.get(SUBJECT_CLAIM_NAME, String.class));
            final UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                    new UsernamePasswordAuthenticationToken(
                            this.adminAPIClientDetails.getClientId(),
                            "N/A",
                            Collections.emptyList());
            final ResponseEntity<OAuth2AccessToken> accessToken =
                    this.tokenEndpoint.postAccessToken(usernamePasswordAuthenticationToken, params);
            final OAuth2AccessToken token = accessToken.getBody();

            final String examId = claims.get(EXAM_ID_CLAIM, String.class);
            final EntityKey key = (StringUtils.isNotBlank(examId))
                    ? new EntityKey(examId, EntityType.EXAM)
                    : null;
            final LoginForward loginForward = new LoginForward(
                    key,
                    "MONITOR_EXAM_FROM_LIST");

            return new TokenLoginInfo(user.username, claims.getSubject(), loginForward, token);
        });
    }

    private UserInfo handleAccountDoesNotExistYet(
            final boolean createIfNotExists,
            final Exam exam,
            final FullLmsIntegrationService.AdHocAccountData adHocAccountData) {

        if (createIfNotExists) {
            return this
                    .createNewTeacherAccountForExam(exam, adHocAccountData)
                    .getOrThrow();
        } else {
            throw new RuntimeException("Teacher Account with user "+ adHocAccountData + " does not exist.");
        }
    }

    private UserInfo applySupporter(final UserInfo account, final Exam exam) {
        // activate ad-hoc account if not active
        if (!account.isActive()) {
            userDAO.setActive(account, true)
                    .onError(error -> log.error(
                            "Failed to activate ad-hoc teacher account: {}, exam: {}, error {}",
                            account.uuid, exam.externalId, error.getMessage()));
        }

        if (!exam.supporter.contains(account.uuid)) {
            this.examDAO.applySupporter(exam, account.uuid)
                    .onError(error -> log.error(
                            "Failed to apply ad-hoc-teacher account to supporter list of exam: {} user: {}",
                            exam, account, error));
        }
        return account;
    }

    private String createOneTimeToken(final UserInfo account, final Long examId) {

        final String subjectClaim = UUID.randomUUID().toString();
        userDAO.changePassword(account.uuid, subjectClaim);
        synchronizeSPSUserForExam(account, examId);

        final Map<String, Object> claims = new HashMap<>();
        claims.put(USER_CLAIM, account.uuid);
        claims.put(EXAM_ID_CLAIM, String.valueOf(examId));

        return createToken(claims, subjectClaim);
    }

    // NOTE Token is expired in 30 seconds and is signed with internal secret
    private String createToken(final Map<String, Object> claims, final String subject) {
        final long millisecondsNow = Utils.getMillisecondsNow();
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(millisecondsNow))
                .setExpiration(new Date(millisecondsNow + 30 * Constants.SECOND_IN_MILLIS))
                .signWith(SignatureAlgorithm.HS256, this.cryptor.getInternalPWD().toString())
                .compact();
    }

    private Claims checkJWTValid(final String loginToken) {
        // decode given JWT
        final Claims claims = Jwts.parser()
                .setSigningKey(this.cryptor.getInternalPWD().toString())
                .parseClaimsJws(loginToken)
                .getBody();

        // check expiration date
        final long expirationTime = claims.getExpiration().getTime();
        final long now = Utils.getMillisecondsNow();
        if (expirationTime < now) {
            throw new APIMessage.APIMessageException(APIMessage.ErrorMessage.UNAUTHORIZED.of("Token expired"));
        }

        // check user claim
        final String userId = claims.get(USER_CLAIM, String.class);
        if (StringUtils.isBlank(userId)) {
            throw new APIMessage.APIMessageException(APIMessage.ErrorMessage.UNAUTHORIZED.of("User not found"));
        }

        // get exam id
        final String examId = claims.get(EXAM_ID_CLAIM, String.class);
        if (StringUtils.isBlank(examId)) {
            throw new APIMessage.APIMessageException(APIMessage.ErrorMessage.UNAUTHORIZED.of("Exam id not found"));
        }
        final Long examPK = Long.parseLong(examId);

        // check subject
        final String subject = claims.get(SUBJECT_CLAIM_NAME, String.class);
        if (StringUtils.isBlank(subject)) {
            throw new APIMessage.APIMessageException(APIMessage.ErrorMessage.UNAUTHORIZED.of("Token subject mismatch"));
        }
        return claims;
    }

    private UserInfo synchronizeSPSUserForExam(final UserInfo account, final Long examId) {
        if (this.screenProctoringService.isScreenProctoringEnabled(examId)) {
            this.screenProctoringService.synchronizeSPSUserForExam(examId);
        }
        return account;
    }
}
