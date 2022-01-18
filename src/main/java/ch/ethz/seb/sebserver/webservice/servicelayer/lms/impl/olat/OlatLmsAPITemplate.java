/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.olat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.web.client.RestTemplate;

import ch.ethz.seb.sebserver.ClientHttpRequestFactoryService;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.async.AsyncService;
import ch.ethz.seb.sebserver.gbl.client.ClientCredentialService;
import ch.ethz.seb.sebserver.gbl.client.ClientCredentials;
import ch.ethz.seb.sebserver.gbl.client.ProxyData;
import ch.ethz.seb.sebserver.gbl.model.Domain.LMS_SETUP;
import ch.ethz.seb.sebserver.gbl.model.exam.Chapters;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.exam.QuizData;
import ch.ethz.seb.sebserver.gbl.model.exam.SEBRestriction;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.LmsType;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
import ch.ethz.seb.sebserver.gbl.model.user.ExamineeAccountDetails;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.FilterMap;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.APITemplateDataSupplier;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPIService;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.LmsAPITemplate;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.AbstractCachedCourseAccess;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.olat.OlatLmsData.AssessmentData;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.olat.OlatLmsData.RestrictionData;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.olat.OlatLmsData.RestrictionDataPost;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.olat.OlatLmsData.UserData;

public class OlatLmsAPITemplate extends AbstractCachedCourseAccess implements LmsAPITemplate {

    private static final Logger log = LoggerFactory.getLogger(OlatLmsAPITemplate.class);

    private final ClientHttpRequestFactoryService clientHttpRequestFactoryService;
    private final ClientCredentialService clientCredentialService;
    private final APITemplateDataSupplier apiTemplateDataSupplier;
    private final Long lmsSetupId;

    private OlatLmsRestTemplate cachedRestTemplate;

    protected OlatLmsAPITemplate(
            final ClientHttpRequestFactoryService clientHttpRequestFactoryService,
            final ClientCredentialService clientCredentialService,
            final APITemplateDataSupplier apiTemplateDataSupplier,
            final AsyncService asyncService,
            final Environment environment,
            final CacheManager cacheManager) {

        super(asyncService, environment, cacheManager);

        this.clientHttpRequestFactoryService = clientHttpRequestFactoryService;
        this.clientCredentialService = clientCredentialService;
        this.apiTemplateDataSupplier = apiTemplateDataSupplier;
        this.lmsSetupId = apiTemplateDataSupplier.getLmsSetup().id;
    }

    @Override
    public LmsType getType() {
        return LmsType.OPEN_OLAT;
    }

    @Override
    public LmsSetup lmsSetup() {
        return this.apiTemplateDataSupplier.getLmsSetup();
    }

    @Override
    protected Long getLmsSetupId() {
        return this.lmsSetupId;
    }

    @Override
    public LmsSetupTestResult testCourseAccessAPI() {
        final LmsSetupTestResult testLmsSetupSettings = testLmsSetupSettings();
        if (testLmsSetupSettings.hasAnyError()) {
            return testLmsSetupSettings;
        }
        try {
            this.getRestTemplate().get();
        } catch (final Exception e) {
            log.error("Failed to access OLAT course API: ", e);
            return LmsSetupTestResult.ofQuizAccessAPIError(LmsType.OPEN_OLAT, e.getMessage());
        }
        return LmsSetupTestResult.ofOkay(LmsType.OPEN_OLAT);
    }

    @Override
    public LmsSetupTestResult testCourseRestrictionAPI() {
        return testCourseAccessAPI();
    }

    private LmsSetupTestResult testLmsSetupSettings() {

        final LmsSetup lmsSetup = this.apiTemplateDataSupplier.getLmsSetup();
        final ClientCredentials lmsClientCredentials = this.apiTemplateDataSupplier.getLmsClientCredentials();
        final List<APIMessage> missingAttrs = new ArrayList<>();

        // Check given LMS URL
        if (StringUtils.isBlank(lmsSetup.lmsApiUrl)) {
            missingAttrs.add(APIMessage.fieldValidationError(
                    LMS_SETUP.ATTR_LMS_URL,
                    "lmsSetup:lmsUrl:notNull"));
        } else {
            // Try to connect to the URL
            if (!Utils.pingHost(lmsSetup.lmsApiUrl)) {
                missingAttrs.add(APIMessage.fieldValidationError(
                        LMS_SETUP.ATTR_LMS_URL,
                        "lmsSetup:lmsUrl:url.invalid"));
            }
        }

        // Client id is mandatory
        if (!lmsClientCredentials.hasClientId()) {
            missingAttrs.add(APIMessage.fieldValidationError(
                    LMS_SETUP.ATTR_LMS_CLIENTNAME,
                    "lmsSetup:lmsClientname:notNull"));
        }

        // Client secret is mandatory
        if (!lmsClientCredentials.hasSecret()) {
            missingAttrs.add(APIMessage.fieldValidationError(
                    LMS_SETUP.ATTR_LMS_CLIENTSECRET,
                    "lmsSetup:lmsClientsecret:notNull"));
        }

        if (!missingAttrs.isEmpty()) {
            return LmsSetupTestResult.ofMissingAttributes(LmsType.OPEN_OLAT, missingAttrs);
        }

        return LmsSetupTestResult.ofOkay(LmsType.OPEN_OLAT);
    }

    @Override
    public Result<List<QuizData>> getQuizzes(final FilterMap filterMap) {
        return this
                .protectedQuizzesRequest(filterMap)
                .map(quizzes -> quizzes.stream()
                        .filter(LmsAPIService.quizFilterPredicate(filterMap))
                        .collect(Collectors.toList()));
    }

    @Override
    public Result<Collection<QuizData>> getQuizzes(final Set<String> ids) {
        return Result.tryCatch(() -> {
            final HashSet<String> leftIds = new HashSet<>(ids);
            final Collection<QuizData> result = new ArrayList<>();
            ids.stream()
                    .map(id -> super.getFromCache(id))
                    .forEach(q -> {
                        if (q != null) {
                            leftIds.remove(q.id);
                            result.add(q);
                        }
                    });

            if (!leftIds.isEmpty()) {
                result.addAll(super.protectedQuizzesRequest(leftIds).getOrThrow());
            }

            return result;
        });
    }

    @Override
    public Result<QuizData> getQuiz(final String id) {
        final QuizData fromCache = super.getFromCache(id);
        if (fromCache != null) {
            return Result.of(fromCache);
        }

        return super.protectedQuizRequest(id);
    }

    @Override
    protected Supplier<List<QuizData>> allQuizzesSupplier(final FilterMap filterMap) {
        return () -> {
            final List<QuizData> res = getRestTemplate()
                    .map(t -> this.collectAllQuizzes(t, filterMap))
                    .getOrThrow();
            super.putToCache(res);
            return res;
        };
    }

    private String examUrl(final long olatRepositoryId) {
        final LmsSetup lmsSetup = this.apiTemplateDataSupplier.getLmsSetup();
        return lmsSetup.lmsApiUrl + "/auth/RepositoryEntry/" + olatRepositoryId;
    }

    private List<QuizData> collectAllQuizzes(final OlatLmsRestTemplate restTemplate, final FilterMap filterMap) {
        final LmsSetup lmsSetup = this.apiTemplateDataSupplier.getLmsSetup();
        final String quizName = (filterMap != null) ? filterMap.getString(QuizData.FILTER_ATTR_QUIZ_NAME) : null;
        final DateTime quizFromTime = (filterMap != null) ? filterMap.getQuizFromTime() : null;
        final long fromCutTime = (quizFromTime != null) ? Utils.toUnixTimeInSeconds(quizFromTime) : -1;

        String url = "/restapi/assessment_modes/seb?";
        if (fromCutTime != -1) {
            url = String.format("%sdateFrom=%s&", url, fromCutTime);
        }
        if (quizName != null) {
            url = String.format("%sname=%s&", url, quizName);
        }

        final List<AssessmentData> as =
                this.apiGetList(restTemplate, url, new ParameterizedTypeReference<List<AssessmentData>>() {
                });
        return as.stream()
                .map(a -> {
                    return new QuizData(
                            String.format("%d", a.key),
                            lmsSetup.getInstitutionId(),
                            lmsSetup.id,
                            lmsSetup.getLmsType(),
                            a.name,
                            a.description,
                            Utils.toDateTimeUTC(a.dateFrom),
                            Utils.toDateTimeUTC(a.dateTo),
                            examUrl(a.repositoryEntryKey),
                            new HashMap<String, String>());
                })
                .collect(Collectors.toList());
    }

    @Override
    protected Supplier<Collection<QuizData>> quizzesSupplier(final Set<String> ids) {
        return () -> ids.stream().map(id -> quizSupplier(id).get()).collect(Collectors.toList());
    }

    @Override
    protected Supplier<QuizData> quizSupplier(final String id) {
        return () -> getRestTemplate()
                .map(t -> this.quizById(t, id))
                .getOrThrow();
    }

    private QuizData quizById(final OlatLmsRestTemplate restTemplate, final String id) {
        final LmsSetup lmsSetup = this.apiTemplateDataSupplier.getLmsSetup();
        final String url = String.format("/restapi/assessment_modes/%s", id);
        final AssessmentData a = this.apiGet(restTemplate, url, AssessmentData.class);
        return new QuizData(
                String.format("%d", a.key),
                lmsSetup.getInstitutionId(),
                lmsSetup.id,
                lmsSetup.getLmsType(),
                a.name,
                a.description,
                Utils.toDateTimeUTC(a.dateFrom),
                Utils.toDateTimeUTC(a.dateTo),
                examUrl(a.repositoryEntryKey),
                new HashMap<String, String>());
    }

    private ExamineeAccountDetails getExamineeById(final RestTemplate restTemplate, final String id) {
        final String url = String.format("/restapi/users/%s/name_username", id);
        final UserData u = this.apiGet(restTemplate, url, UserData.class);
        final Map<String, String> attrs = new HashMap<>();
        return new ExamineeAccountDetails(
                String.valueOf(u.key),
                u.lastName + ", " + u.firstName,
                u.username,
                "OLAT API does not provide email addresses",
                attrs);
    }

    @Override
    protected Supplier<ExamineeAccountDetails> accountDetailsSupplier(final String id) {
        return () -> getRestTemplate()
                .map(t -> this.getExamineeById(t, id))
                .getOrThrow();
    }

    @Override
    protected Supplier<Chapters> getCourseChaptersSupplier(final String courseId) {
        return () -> {
            throw new UnsupportedOperationException("No Course Chapter available for OpenOLAT LMS");
        };
    }

    private SEBRestriction getRestrictionForAssignmentId(final RestTemplate restTemplate, final String id) {
        final String url = String.format("/restapi/assessment_modes/%s/seb_restriction", id);
        final RestrictionData r = this.apiGet(restTemplate, url, RestrictionData.class);
        return new SEBRestriction(Long.valueOf(id), r.configKeys, r.browserExamKeys, new HashMap<String, String>());
    }

    private SEBRestriction setRestrictionForAssignmentId(
            final RestTemplate restTemplate,
            final String id,
            final SEBRestriction restriction) {

        final String url = String.format("/restapi/assessment_modes/%s/seb_restriction", id);
        final RestrictionDataPost post = new RestrictionDataPost();
        post.browserExamKeys = new ArrayList<>(restriction.browserExamKeys);
        post.configKeys = new ArrayList<>(restriction.configKeys);
        final RestrictionData r =
                this.apiPost(restTemplate, url, post, RestrictionDataPost.class, RestrictionData.class);
        return new SEBRestriction(Long.valueOf(id), r.configKeys, r.browserExamKeys, new HashMap<String, String>());
    }

    private SEBRestriction deleteRestrictionForAssignmentId(final RestTemplate restTemplate, final String id) {
        final String url = String.format("/restapi/assessment_modes/%s/seb_restriction", id);
        final RestrictionData r = this.apiDelete(restTemplate, url, RestrictionData.class);
        // OLAT returns RestrictionData with null values upon deletion.
        // We return it here for consistency, even though SEB server does not need it
        return new SEBRestriction(Long.valueOf(id), r.configKeys, r.browserExamKeys, new HashMap<String, String>());
    }

    @Override
    public Result<SEBRestriction> getSEBClientRestriction(final Exam exam) {
        return getRestTemplate()
                .map(t -> this.getRestrictionForAssignmentId(t, exam.externalId));
    }

    @Override
    public Result<SEBRestriction> applySEBClientRestriction(
            final String externalExamId,
            final SEBRestriction sebRestrictionData) {
        return getRestTemplate()
                .map(t -> this.setRestrictionForAssignmentId(t, externalExamId, sebRestrictionData));
    }

    @Override
    public Result<Exam> releaseSEBClientRestriction(final Exam exam) {
        return getRestTemplate()
                .map(t -> this.deleteRestrictionForAssignmentId(t, exam.externalId))
                .map(x -> exam);
    }

    private <T> T apiGet(final RestTemplate restTemplate, final String url, final Class<T> type) {
        final LmsSetup lmsSetup = this.apiTemplateDataSupplier.getLmsSetup();
        final ResponseEntity<T> res = restTemplate.exchange(
                lmsSetup.lmsApiUrl + url,
                HttpMethod.GET,
                HttpEntity.EMPTY,
                type);
        return res.getBody();
    }

    private <T> List<T> apiGetList(final RestTemplate restTemplate, final String url,
            final ParameterizedTypeReference<List<T>> type) {
        final LmsSetup lmsSetup = this.apiTemplateDataSupplier.getLmsSetup();
        final ResponseEntity<List<T>> res = restTemplate.exchange(
                lmsSetup.lmsApiUrl + url,
                HttpMethod.GET,
                HttpEntity.EMPTY,
                type);
        return res.getBody();
    }

    private <P, R> R apiPost(final RestTemplate restTemplate, final String url, final P post, final Class<P> postType,
            final Class<R> responseType) {
        final LmsSetup lmsSetup = this.apiTemplateDataSupplier.getLmsSetup();
        final HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set("content-type", "application/json");
        final HttpEntity<P> requestEntity = new HttpEntity<>(post, httpHeaders);
        final ResponseEntity<R> res = restTemplate.exchange(
                lmsSetup.lmsApiUrl + url,
                HttpMethod.POST,
                requestEntity,
                responseType);
        return res.getBody();
    }

    private <T> T apiDelete(final RestTemplate restTemplate, final String url, final Class<T> type) {
        final LmsSetup lmsSetup = this.apiTemplateDataSupplier.getLmsSetup();
        final ResponseEntity<T> res = restTemplate.exchange(
                lmsSetup.lmsApiUrl + url,
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                type);
        return res.getBody();
    }

    private Result<OlatLmsRestTemplate> getRestTemplate() {
        return Result.tryCatch(() -> {
            if (this.cachedRestTemplate != null) {
                return this.cachedRestTemplate;
            }

            final LmsSetup lmsSetup = this.apiTemplateDataSupplier.getLmsSetup();
            final ClientCredentials credentials = this.apiTemplateDataSupplier.getLmsClientCredentials();
            final ProxyData proxyData = this.apiTemplateDataSupplier.getProxyData();

            final CharSequence plainClientId = credentials.clientId;
            final CharSequence plainClientSecret = this.clientCredentialService
                    .getPlainClientSecret(credentials)
                    .getOrThrow();

            final ClientCredentialsResourceDetails details = new ClientCredentialsResourceDetails();
            details.setAccessTokenUri(lmsSetup.lmsApiUrl + "/restapi/auth/");
            details.setClientId(plainClientId.toString());
            details.setClientSecret(plainClientSecret.toString());

            final ClientHttpRequestFactory clientHttpRequestFactory = this.clientHttpRequestFactoryService
                    .getClientHttpRequestFactory(proxyData)
                    .getOrThrow();

            final OlatLmsRestTemplate template = new OlatLmsRestTemplate(details);
            template.setRequestFactory(clientHttpRequestFactory);

            this.cachedRestTemplate = template;
            return this.cachedRestTemplate;
        });
    }

}
