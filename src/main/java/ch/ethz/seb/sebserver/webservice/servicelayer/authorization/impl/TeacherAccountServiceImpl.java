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
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.user.TokenLoginInfo;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.model.user.UserMod;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Cryptor;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.AdditionalAttributeRecord;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.TeacherAccountService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.AdditionalAttributesDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserDAO;
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
import org.springframework.security.oauth2.provider.endpoint.TokenEndpoint;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Service;

@Lazy
@Service
@WebServiceProfile
public class TeacherAccountServiceImpl implements TeacherAccountService {

    private static final Logger log = LoggerFactory.getLogger(TeacherAccountServiceImpl.class);

    private static final String SUBJECT_CLAIM_NAME = "sub";
    private static final String USER_CLAIM = "usr";
    private static final String EXAM_ID_CLAIM = "exam";

    private static final String EXAM_OTT_SUBJECT_PREFIX = "EXAM_OTT_SUBJECT_";

    private final UserDAO userDAO;
    private final ScreenProctoringService screenProctoringService;
    private final ExamDAO examDAO;
    private final Cryptor cryptor;
    private final AdditionalAttributesDAO additionalAttributesDAO;
    final TokenEndpoint tokenEndpoint;
    private final AdminAPIClientDetails adminAPIClientDetails;

    public TeacherAccountServiceImpl(
            final UserDAO userDAO,
            final ScreenProctoringService screenProctoringService,
            final ExamDAO examDAO,
            final Cryptor cryptor,
            final AdditionalAttributesDAO additionalAttributesDAO,
            final TokenEndpoint tokenEndpoint,
            final AdminAPIClientDetails adminAPIClientDetails) {

        this.userDAO = userDAO;
        this.screenProctoringService = screenProctoringService;
        this.examDAO = examDAO;
        this.cryptor = cryptor;
        this.additionalAttributesDAO = additionalAttributesDAO;
        this.tokenEndpoint = tokenEndpoint;
        this.adminAPIClientDetails = adminAPIClientDetails;
    }


    @Override
    public Result<UserInfo> createNewTeacherAccountForExam(
            final Exam exam,
            final String userId,
            final String username,
            final String timezone) {

        return Result.tryCatch(() -> {

            final String uuid = UUID.randomUUID().toString();
            DateTimeZone dtz = DateTimeZone.UTC;
            if (StringUtils.isNotBlank(timezone)) {
                try {
                    dtz = DateTimeZone.forID(timezone);
                } catch (final Exception e) {
                    log.warn("Failed to set requested time zone for ad-hoc teacher account: {}", timezone);
                }
            }

            final UserMod adHocTeacherUser = new UserMod(
                    uuid,
                    exam.institutionId,
                    userId,
                    getTeacherAccountIdentifier(exam),
                    username,
                    uuid,
                    uuid,
                    null,
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
    public Result<Exam> deleteTeacherAccountsForExam(final Exam exam) {
        return Result.tryCatch(() -> {

            final String externalId = exam.externalId;
            final FilterMap filter = new FilterMap();
            filter.putIfAbsent(Domain.USER.ATTR_SURNAME, getTeacherAccountIdentifier(exam));
            final Collection<UserInfo> accounts = userDAO.allMatching(filter).getOrThrow();

            if (accounts.isEmpty()) {
                return exam;
            }

            if (accounts.size() > 1) {
                log.error("Too many accounts found!?... ad-hoc teacher account mapping: {}", externalId);
                return exam;
            }

            userDAO.delete(Utils.immutableSetOf(new EntityKey(
                            accounts.iterator().next().uuid,
                            EntityType.USER)))
                    .getOrThrow();

            return exam;
        });
    }

    @Override
    public Result<String> getOneTimeTokenForTeacherAccount(
            final Exam exam,
            final String userId,
            final String username,
            final String timezone,
            final boolean createIfNotExists) {

        return this.userDAO
                .byModelId(userId)
                .onErrorDo(error -> handleAccountDoesNotExistYet(createIfNotExists, exam, userId, username, timezone))
                .map(account -> applySupporter(account, exam))
                .map(account -> {
                    this.screenProctoringService.synchronizeSPSUserForExam(exam.id);
                    return account;
                })
                .map(account -> this.createOneTimeToken(account, exam.id));
    }

    @Override
    public Result<TokenLoginInfo> verifyOneTimeTokenForTeacherAccount(final String loginToken) {
        return Result.tryCatch(() -> {

            final Claims claims = checkJWTValid(loginToken);
            final String userId = claims.get(USER_CLAIM, String.class);

            // check if requested user exists
            final UserInfo user = this.userDAO
                    .byModelId(userId)
                    .getOrThrow(error -> new BadCredentialsException("Unknown user claim", error));

            // login the user by getting access token
            final Map<String, String> params = new HashMap<>();
            params.put("grant_type", "password");
            params.put("username", user.username);
            params.put("password", user.uuid);
            //final WebAuthenticationDetails details = new WebAuthenticationDetails("localhost", null);
            final UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                    new UsernamePasswordAuthenticationToken(
                            this.adminAPIClientDetails, // TODO are this the correct details?
                            null,
                            Collections.emptyList());
            final ResponseEntity<OAuth2AccessToken> accessToken =
                    this.tokenEndpoint.postAccessToken(usernamePasswordAuthenticationToken, params);
            final OAuth2AccessToken token = accessToken.getBody();

            return new TokenLoginInfo(user.username, user.uuid, null, token);
        });
    }

    private UserInfo handleAccountDoesNotExistYet(
            final boolean createIfNotExists,
            final Exam exam,
            final String userId,
            final String username,
            final String timezone) {

        if (createIfNotExists) {
            return this
                    .createNewTeacherAccountForExam(exam, userId, username, timezone)
                    .getOrThrow();
        } else {
            throw new RuntimeException("Teacher Account with userId "+ userId + " and username "+username+" does not exist.");
        }
    }

    private UserInfo applySupporter(final UserInfo account, final Exam exam) {
        if (!exam.supporter.contains(account.uuid)) {
            this.examDAO.applySupporter(exam, account.uuid)
                    .onError(error -> log.error(
                            "Failed to apply ad-hoc-teacher account to supporter list of exam: {} user: {}",
                            exam, account, error));
        }
        return account;
    }

    private String createOneTimeToken(final UserInfo account, final Long examId) {

        // create a subject claim for this token only
        final String subjectClaim = UUID.randomUUID().toString();
        this.storeSubjectForExam(examId, account.uuid, subjectClaim);

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
        final String subjectClaim = getSubjectForExam(examPK, userId);
        if (StringUtils.isBlank(subjectClaim)) {
            throw new APIMessage.APIMessageException(APIMessage.ErrorMessage.UNAUTHORIZED.of("Subject not found"));
        }
        final String subject = claims.get(SUBJECT_CLAIM_NAME, String.class);
        if (!subjectClaim.equals(subject)) {
            throw new APIMessage.APIMessageException(APIMessage.ErrorMessage.UNAUTHORIZED.of("Token subject mismatch"));
        }
        return claims;
    }

    private void storeSubjectForExam(final Long examId, final String userId, final String subject) {
        additionalAttributesDAO.saveAdditionalAttribute(
                EntityType.EXAM,
                examId,
                EXAM_OTT_SUBJECT_PREFIX + userId,
                subject)
                .getOrThrow();
    }

    private void deleteSubjectForExam(final Long examId, final String userId) {
        additionalAttributesDAO.delete(EntityType.EXAM, examId, EXAM_OTT_SUBJECT_PREFIX + userId);
    }

    private String getSubjectForExam(final Long examId, final String userId) {
        return additionalAttributesDAO
                .getAdditionalAttribute(EntityType.EXAM, examId, EXAM_OTT_SUBJECT_PREFIX + userId)
                .map(AdditionalAttributeRecord::getValue)
                .onError(error -> log.warn("Failed to get OTT subject from exam: {}", error.getMessage()))
                .getOrElse(null);
    }

    private static String getTeacherAccountIdentifier(final Exam exam) {
        return "AdHoc-Teacher-Account-" + exam.id;
    }
}
