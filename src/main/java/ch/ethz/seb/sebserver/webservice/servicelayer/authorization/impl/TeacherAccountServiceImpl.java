/*
 *  Copyright (c) 2019 ETH Zürich, IT Services
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.authorization.impl;

import java.nio.file.AccessDeniedException;
import java.util.*;
import java.util.stream.Collectors;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.user.*;
import ch.ethz.seb.sebserver.gbl.util.Cryptor;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.WebserviceInfo;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AdHocAccountData;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.AuthorizationService;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.TeacherAccountService;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ExamDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.UserDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl.ExamDeletionEvent;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ExamFinishedEvent;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.ScreenProctoringService;
import ch.ethz.seb.sebserver.webservice.weblayer.oauth.OAuthRestTemplate;
import ch.ethz.seb.sebserver.webservice.weblayer.oauth.OAuthRestTemplateFactory;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

@Lazy
@Service
public class TeacherAccountServiceImpl implements TeacherAccountService {

    private static final Logger log = LoggerFactory.getLogger(TeacherAccountServiceImpl.class);

    private static final String SUBJECT_CLAIM_NAME = "sub";
    private static final String USER_CLAIM = "usr";
    private static final String EXAM_ID_CLAIM = "exam";

    private final UserDAO userDAO;
    private final ScreenProctoringService screenProctoringService;
    private final OAuthRestTemplateFactory oAuthRestTemplateFactory;
    private final ExamDAO examDAO;
    private final Cryptor cryptor;
    private final WebserviceInfo webserviceInfo;
    protected final AuthorizationService authorizationService;

    private final String clientId;
    private final String clientSecret;

    public TeacherAccountServiceImpl(
            final UserDAO userDAO,
            final ScreenProctoringService screenProctoringService,
            final OAuthRestTemplateFactory oAuthRestTemplateFactory,
            final ExamDAO examDAO,
            final Cryptor cryptor,
            final WebserviceInfo webserviceInfo,
            final AuthorizationService authorizationService,
            @Value("${sebserver.webservice.api.admin.clientId}") final String clientId,
            @Value("${sebserver.webservice.api.admin.clientSecret}") final String clientSecret) {

        this.userDAO = userDAO;
        this.screenProctoringService = screenProctoringService;
        this.oAuthRestTemplateFactory = oAuthRestTemplateFactory;
        this.examDAO = examDAO;
        this.cryptor = cryptor;
        this.webserviceInfo = webserviceInfo;
        this.authorizationService = authorizationService;

        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public String getTeacherAccountIdentifier(
            final String lmsId,
            final String userId) {

        if (lmsId == null || userId == null) {
            log.error("Failed to getTeacherAccountIdentifier, examId and/or userId cannot be null: lmsId: {}, userId: {}", lmsId, userId);
            throw new RuntimeException("examId and/or userId cannot be null");
        }

        return AD_HOC_TEACHER_ID_PREFIX + Constants.UNDERLINE + lmsId + Constants.UNDERLINE + userId;
    }

    @Override
    public Result<UserInfo> createNewTeacherAccountForExam(
            final Exam exam,
            final AdHocAccountData adHocAccountData) {

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
                    .onError(error -> log.error("Failed to create ad hoc user data: {} adHocTeacherUser: {} error: {}",
                            adHocAccountData,
                            adHocTeacherUser,
                            error.getMessage()))
                    .getOrThrow();

        });
    }

    @Override
    public Result<String> getOneTimeTokenForTeacherAccount(
            final Exam exam,
            final AdHocAccountData adHocAccountData,
            final boolean createIfNotExists) {

        if (exam.status == Exam.ExamStatus.FINISHED || exam.status == Exam.ExamStatus.ARCHIVED) {
            return Result.ofError(new IllegalStateException("Exam is not running"));
        }

        log.info("Try to get one time token for AdHocAccountData: {}", adHocAccountData);
        
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
                log.warn("Invalid One Time JWT received. Root exception: ", e);
                throw new AccessDeniedException("Invalid One Time JWT");
            }
            final String userId = claims.get(USER_CLAIM, String.class);

            // check if requested user exists
            final UserInfo user = this.userDAO
                    .byModelId(userId)
                    .getOrThrow(error -> new BadCredentialsException("Unknown user claim", error));

            OAuthRestTemplate.DefaultClientSettingsProvider clientSettings = new OAuthRestTemplate.DefaultClientSettingsProvider(
                    this.clientId,
                    this.clientSecret,
                    user.username,
                    claims.get(SUBJECT_CLAIM_NAME, String.class),
                    null
            );

            OAuthRestTemplate oAuth2RestTemplate = oAuthRestTemplateFactory.getOAuth2RestTemplate(
                    webserviceInfo.getExternalServerURL(),
                    API.OAUTH_TOKEN_ENDPOINT,
                    clientSettings);

            CharSequence accessToken = oAuth2RestTemplate.getAccessToken();

            final String examId = claims.get(EXAM_ID_CLAIM, String.class);
            final EntityKey key = (StringUtils.isNotBlank(examId))
                    ? new EntityKey(examId, EntityType.EXAM)
                    : null;
            final LoginForward loginForward = new LoginForward(
                    key,
                    "MONITOR_EXAM_FROM_LIST");

            return new TokenLoginInfo(user.username, claims.getSubject(), loginForward, accessToken.toString());
        });
    }

    @Override
    public void deleteAllFromLMS(final Long lmsId) {
            userDAO
                    .deleteAdHocAccountsForLMS(AD_HOC_TEACHER_ID_PREFIX, lmsId)
                    .map(this::deleteAccountsOnSPS)
                    .onError(error -> log.error("Failed to delete all teacher accounts for LMS with id: {}", lmsId, error));
    }

    @Override
    public void notifyExamFinished(final ExamFinishedEvent event) {

        if (event.exam.status != Exam.ExamStatus.UP_COMING) {
            final List<String> supporterWithoutTeacherAccounts = event.exam.supporter
                    .stream()
                    .filter(uuid -> uuid != null && !(uuid.contains(AD_HOC_TEACHER_ID_PREFIX) || authorizationService.isTeacherOnly(uuid)))
                    .toList();

            deleteAllTeacherAccounts(event.exam);

            // remove all teacher accounts from Exam supporter list
            examDAO.updateSupporterAccounts(
                    event.exam.id,
                    supporterWithoutTeacherAccounts);
        }
    }

    @Override
    public void notifyExamDeleted(final ExamDeletionEvent event) {
        event.ids.forEach(id -> {
            try {
                deleteAllTeacherAccounts(examDAO.byPK(id).getOrThrow());                
            } catch (final Exception e) {
                log.error("Failed to remove Teacher account for Exam: {} cause: {}", id, e.getMessage());
            }
        });
    }
    
    private void deleteAllTeacherAccounts(final Exam exam) {
        try {

           final Set<EntityKey> keysToDelete = exam.supporter
                    .stream()
                    .filter(uuid -> uuid != null &&
                            (uuid.contains(AD_HOC_TEACHER_ID_PREFIX) || authorizationService.isTeacherOnly(uuid)) && 
                            examDAO.numOfExamsReferencingSupporter(uuid) == 1)
                    .map(uuid -> new EntityKey(uuid, EntityType.USER))
                    .collect(Collectors.toSet());

            if (!keysToDelete.isEmpty()) {
                
                log.info("Deleting teacher accounts: {} for exam: {}", keysToDelete, exam.name);
                
                userDAO.delete(keysToDelete)
                        .map(this::deleteAccountsOnSPS)
                        .onError(error -> log.error(
                                "Failed to delete all teacher accounts for Exam with id: {} cause: {}", 
                                exam.id, 
                                error.getMessage()));
            }
        } catch (final Exception e) {
            log.error("Failed to delete Ad-Hoc Teacher Accounts for exam: {} cause: {}", exam, e.getMessage());
        }
    }

    private Collection<EntityKey> deleteAccountsOnSPS(final Collection<EntityKey> keys) {
        keys.forEach(key -> this.screenProctoringService.deleteSPSUser(key.modelId) );
        return keys;
    }

    private UserInfo handleAccountDoesNotExistYet(
            final boolean createIfNotExists,
            final Exam exam,
            final AdHocAccountData adHocAccountData) {

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
            this.examDAO
                    .applySupporter(exam, account.uuid)
                    .map(e -> screenProctoringService.updateExamOnScreenProctoringService(e.id).getOrThrow())
                    .onError(error -> log.error(
                            "Failed to apply ad-hoc-teacher account to supporter list of exam: {} user: {}",
                            exam, account, error));
        }

        return account;
    }

    private String createOneTimeToken(final UserInfo account, final Long examId) {

        final String subjectClaim = UUID.randomUUID().toString();
        userDAO.changePassword(account.uuid, subjectClaim);
        this.screenProctoringService.synchronizeSPSUserWait(account.uuid);
        //this.screenProctoringService.updateExamOnScreenProctoringService(examId);

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

}
