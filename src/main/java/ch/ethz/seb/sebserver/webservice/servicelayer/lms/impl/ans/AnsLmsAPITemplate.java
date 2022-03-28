/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.ans;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.web.client.RestTemplate;

import ch.ethz.seb.sebserver.ClientHttpRequestFactoryService;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
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
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.ans.AnsLmsData.SEBServerData;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.ans.AnsLmsData.AssignmentData;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.ans.AnsLmsData.UserData;

public class AnsLmsAPITemplate extends AbstractCachedCourseAccess implements LmsAPITemplate {

    private static final Logger log = LoggerFactory.getLogger(AnsLmsAPITemplate.class);

    private final ClientHttpRequestFactoryService clientHttpRequestFactoryService;
    private final ClientCredentialService clientCredentialService;
    private final APITemplateDataSupplier apiTemplateDataSupplier;
    private final Long lmsSetupId;

    private AnsPersonalRestTemplate cachedRestTemplate;

    protected AnsLmsAPITemplate(
            final ClientHttpRequestFactoryService clientHttpRequestFactoryService,
            final ClientCredentialService clientCredentialService,
            final APITemplateDataSupplier apiTemplateDataSupplier,
            final CacheManager cacheManager) {

        super(cacheManager);

        this.clientHttpRequestFactoryService = clientHttpRequestFactoryService;
        this.clientCredentialService = clientCredentialService;
        this.apiTemplateDataSupplier = apiTemplateDataSupplier;
        this.lmsSetupId = apiTemplateDataSupplier.getLmsSetup().id;
    }

    @Override
    public LmsType getType() {
        return LmsType.ANS_DELFT;
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
        } catch (final RuntimeException e) {
            log.error("Failed to access Ans course API: ", e);
            return LmsSetupTestResult.ofQuizAccessAPIError(LmsType.ANS_DELFT, e.getMessage());
        }

        return LmsSetupTestResult.ofOkay(LmsType.ANS_DELFT);
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
            return LmsSetupTestResult.ofMissingAttributes(LmsType.ANS_DELFT, missingAttrs);
        }

        return LmsSetupTestResult.ofOkay(LmsType.ANS_DELFT);
    }

    @Override
    public Result<List<QuizData>> getQuizzes(final FilterMap filterMap) {
        return this
                .allQuizzesRequest(filterMap)
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
                result.addAll(quizzesRequest(leftIds).getOrThrow());
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

        return quizRequest(id);
    }

    private List<QuizData> collectAllQuizzes(final AnsPersonalRestTemplate restTemplate) {
        final LmsSetup lmsSetup = this.apiTemplateDataSupplier.getLmsSetup();
        final List<QuizData> quizDatas = getAssignments(restTemplate)
                .stream()
                .map(a -> quizDataFromAnsData(lmsSetup, a))
                .collect(Collectors.toList());
        quizDatas.forEach(q -> super.putToCache(q));
        return quizDatas;
    }

    private QuizData getQuizByAssignmentId(final RestTemplate restTemplate, final String id) {
        final LmsSetup lmsSetup = this.apiTemplateDataSupplier.getLmsSetup();
        final AssignmentData a = getAssignmentById(restTemplate, id);
        return quizDataFromAnsData(lmsSetup, a);
    }

    private QuizData quizDataFromAnsData(final LmsSetup lmsSetup, final AssignmentData a) {
        // In Ans, one assignment can have multiple timeslots, but the SEB restriciton
        // is done at the assignment level, so timeslots don't really matter.
        // An assignment's start_at and end_at dates indicate when the first timeslot starts
        // and the last timeslot ends. If these are null, there are no timeslots, yet.
        // In that case, we create a placeholder timeslot to display in SEB server.
        if (a.start_at == null) {
            a.start_at = java.time.Instant.now().plus(365, java.time.temporal.ChronoUnit.DAYS).toString();
            a.end_at = java.time.Instant.now().plus(366, java.time.temporal.ChronoUnit.DAYS).toString();
        }
        final DateTime startTime = new DateTime(a.start_at);
        final DateTime endTime = new DateTime(a.end_at);
        final Map<String, String> attrs = new HashMap<>();
        attrs.put("assignment_id", String.valueOf(a.id));
        return new QuizData(
                String.valueOf(a.id),
                lmsSetup.getInstitutionId(),
                lmsSetup.id,
                lmsSetup.getLmsType(),
                String.format("%s", a.name),
                String.format(""),
                startTime,
                endTime,
                a.start_url,
                attrs);
    }

    private List<AssignmentData> getAssignments(final RestTemplate restTemplate) {
        // NOTE: at the moment, seb server cannot be enabled inside the Ans GUI,
        // only via the API, so we need to list all assignments. Maybe in the future,
        // we can only list those for which seb server has been enabled in Ans (like in OLAT):
        //final String url = "/api/v2/search/assignments?query=integrations.safe_exam_browser_server.enabled:true";
        final String url = "/api/v2/search/assignments";
        return this.apiGetList(restTemplate, url, new ParameterizedTypeReference<List<AssignmentData>>() {
        });
    }

    private AssignmentData getAssignmentById(final RestTemplate restTemplate, final String id) {
        final String url = String.format("/api/v2/assignments/%s", id);
        return this.apiGet(restTemplate, url, AssignmentData.class);
    }

    private List<QuizData> getQuizzesByIds(final RestTemplate restTemplate, final Set<String> ids) {
        final LmsSetup lmsSetup = this.apiTemplateDataSupplier.getLmsSetup();
        return ids.stream().map(id -> {
            final String url = String.format("/api/v2/assignments/%s", id);
            return this.apiGet(restTemplate, url, AssignmentData.class);
        }).map(a -> {
            final QuizData quizData = quizDataFromAnsData(lmsSetup, a);
            super.putToCache(quizData);
            return quizData;
        })
                .collect(Collectors.toList());
    }

    protected Result<List<QuizData>> allQuizzesRequest(final FilterMap filterMap) {
        // We cannot filter by from-date or partial names using the Ans search API.
        // Only exact matches are permitted. So we're not implementing filtering
        // on the API level and always retrieve all assignments and let SEB server
        // do the filtering.
        return Result.tryCatch(() -> {
            final List<QuizData> res = getRestTemplate()
                    .map(this::collectAllQuizzes)
                    .getOrThrow();
            super.putToCache(res);
            return res;
        });
    }

    protected Result<Collection<QuizData>> quizzesRequest(final Set<String> ids) {
        return getRestTemplate()
                .map(t -> this.getQuizzesByIds(t, ids));
    }

    protected Result<QuizData> quizRequest(final String id) {
        return getRestTemplate()
                .map(t -> this.getQuizByAssignmentId(t, id));
    }

    private ExamineeAccountDetails getExamineeById(final RestTemplate restTemplate, final String id) {
        final String url = String.format("/api/v2/users/%s", id);
        final UserData u = this.apiGet(restTemplate, url, UserData.class);
        final Map<String, String> attrs = new HashMap<>();
        attrs.put("role", u.role);
        attrs.put("affiliation", u.affiliation);
        attrs.put("active", u.active ? "yes" : "no");
        return new ExamineeAccountDetails(
                String.valueOf(u.id),
                u.last_name + ", " + u.first_name,
                u.external_id,
                u.email_address,
                attrs);
    }

    @Override
    public Result<ExamineeAccountDetails> getExamineeAccountDetails(final String examineeUserId) {
        return getRestTemplate().map(t -> this.getExamineeById(t, examineeUserId));
    }

    @Override
    public String getExamineeName(final String examineeUserId) {
        return getExamineeAccountDetails(examineeUserId)
                .map(ExamineeAccountDetails::getDisplayName)
                .onError(error -> log.warn("Failed to request user-name for ID: {}", error.getMessage(), error))
                .getOr(examineeUserId);
    }

    @Override
    public Result<Chapters> getCourseChapters(final String courseId) {
        return Result.ofError(new UnsupportedOperationException("not available yet"));
    }

    @Override
    public Result<SEBRestriction> getSEBClientRestriction(final Exam exam) {
        return getRestTemplate()
                .map(t -> this.getRestrictionForAssignmentId(t, exam.externalId));
    }

    private SEBRestriction getRestrictionForAssignmentId(final RestTemplate restTemplate, final String id) {
        final String url = String.format("/api/v2/assignments/%s", id);
        final AssignmentData assignment = this.apiGet(restTemplate, url, AssignmentData.class);
        final SEBServerData ts = assignment.integrations.safe_exam_browser_server;
        return new SEBRestriction(Long.valueOf(id), ts.config_keys, null, new HashMap<String, String>());
    }

    private SEBRestriction setRestrictionForAssignmentId(final RestTemplate restTemplate, final String id,
            final SEBRestriction restriction) {
        final String url = String.format("/api/v2/assignments/%s", id);
        final AssignmentData assignment = getAssignmentById(restTemplate, id);
        assignment.integrations.safe_exam_browser_server.config_keys = new ArrayList<>(restriction.configKeys);
        assignment.integrations.safe_exam_browser_server.enabled = true;
        @SuppressWarnings("unused")
        final AssignmentData r =
                this.apiPatch(restTemplate, url, assignment, AssignmentData.class, AssignmentData.class);
        final SEBServerData ts = assignment.integrations.safe_exam_browser_server;
        return new SEBRestriction(Long.valueOf(id), ts.config_keys, null, new HashMap<String, String>());
    }

    private SEBRestriction deleteRestrictionForAssignmentId(final RestTemplate restTemplate, final String id) {
        final String url = String.format("/api/v2/assignments/%s", id);
        final AssignmentData assignment = getAssignmentById(restTemplate, id);
        assignment.integrations.safe_exam_browser_server.config_keys = null;
        assignment.integrations.safe_exam_browser_server.enabled = false;
        @SuppressWarnings("unused")
        final AssignmentData r =
                this.apiPatch(restTemplate, url, assignment, AssignmentData.class, AssignmentData.class);
        final SEBServerData ts = assignment.integrations.safe_exam_browser_server;
        return new SEBRestriction(Long.valueOf(id), ts.config_keys, null, new HashMap<String, String>());
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

    private enum LinkRel {
        FIRST, LAST, PREV, NEXT
    }

    private class PageLink {
        public final String link;
        public final LinkRel rel;

        public PageLink(final String l, final LinkRel r) {
            this.link = l;
            this.rel = r;
        }
    }

    private List<PageLink> parseLinks(final String header) {
        // Extracts the individual links from a header that looks like this:
        // <https://staging.ans.app/api/v2/search/assignments?page=1&items=20>; rel="first",<https://staging.ans.app/api/v2/search/assignments?page=1&items=20>; rel="last"
        final Stream<String> links = Arrays.stream(header.split(","));
        return links
                .map(s -> {
                    final String[] pair = s.split(";");
                    final String link = pair[0].trim().substring(1).replaceFirst(".$", ""); // remove < >
                    final String relName = pair[1].trim().substring(5).replaceFirst(".$", ""); // remove rel=" "
                    return new PageLink(link, LinkRel.valueOf(relName.toUpperCase(Locale.ROOT)));
                })
                .collect(Collectors.toList());
    }

    private Optional<PageLink> getNextLink(final List<PageLink> links) {
        return links.stream().filter(l -> l.rel == LinkRel.NEXT).findFirst();
    }

    private <T> List<T> apiGetList(final RestTemplate restTemplate, final String url,
            final ParameterizedTypeReference<List<T>> type) {
        final LmsSetup lmsSetup = this.apiTemplateDataSupplier.getLmsSetup();
        return apiGetListPages(restTemplate, lmsSetup.lmsApiUrl + url, type);
    }

    private <T> List<T> apiGetListPages(final RestTemplate restTemplate, final String link,
            final ParameterizedTypeReference<List<T>> type) {
        // unlike the other api methods, this one takes an explicit link
        // instead of prepending lmsSetup.lmsApiUrl. This is done because Ans
        // provides absolute links for pagination. This method calls itself
        // recursively to retrieve multiple pages.
        final HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.set("accept", "application/json");
        final ResponseEntity<List<T>> response = restTemplate.exchange(
                link,
                HttpMethod.GET,
                new HttpEntity<>(requestHeaders),
                type);
        final List<T> page = response.getBody();
        final HttpHeaders responseHeaders = response.getHeaders();
        final List<PageLink> links = parseLinks(responseHeaders.getFirst("link"));
        final List<T> nextPage = getNextLink(links).map(l -> {
            return apiGetListPages(restTemplate, l.link, type);
        }).orElse(new ArrayList<T>());
        page.addAll(nextPage);
        return page;
    }

    private <T> T apiGet(final RestTemplate restTemplate, final String url, final Class<T> type) {
        final LmsSetup lmsSetup = this.apiTemplateDataSupplier.getLmsSetup();
        final HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.set("accept", "application/json");
        final ResponseEntity<T> res = restTemplate.exchange(
                lmsSetup.lmsApiUrl + url,
                HttpMethod.GET,
                new HttpEntity<>(requestHeaders),
                type);
        return res.getBody();
    }

    private <P, R> R apiPatch(final RestTemplate restTemplate, final String url, final P patch,
            final Class<P> patchType, final Class<R> responseType) {
        final LmsSetup lmsSetup = this.apiTemplateDataSupplier.getLmsSetup();
        final HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.set("content-type", "application/json");
        final HttpEntity<P> requestEntity = new HttpEntity<>(patch, requestHeaders);
        final ResponseEntity<R> res = restTemplate.exchange(
                lmsSetup.lmsApiUrl + url,
                HttpMethod.PATCH,
                requestEntity,
                responseType);
        return res.getBody();
    }

    private Result<AnsPersonalRestTemplate> getRestTemplate() {
        return Result.tryCatch(() -> {
            if (this.cachedRestTemplate != null) {
                return this.cachedRestTemplate;
            }

            final ClientCredentials credentials = this.apiTemplateDataSupplier.getLmsClientCredentials();
            final ProxyData proxyData = this.apiTemplateDataSupplier.getProxyData();
            final CharSequence plainClientSecret = this.clientCredentialService
                    .getPlainClientSecret(credentials)
                    .getOrThrow();

            final ClientCredentialsResourceDetails details = new ClientCredentialsResourceDetails();
            details.setClientSecret(plainClientSecret.toString());

            final ClientHttpRequestFactory clientHttpRequestFactory = this.clientHttpRequestFactoryService
                    .getClientHttpRequestFactory(proxyData)
                    .getOrThrow();

            final AnsPersonalRestTemplate template = new AnsPersonalRestTemplate(details);
            template.setRequestFactory(clientHttpRequestFactory);

            this.cachedRestTemplate = template;
            return this.cachedRestTemplate;
        });
    }

}
