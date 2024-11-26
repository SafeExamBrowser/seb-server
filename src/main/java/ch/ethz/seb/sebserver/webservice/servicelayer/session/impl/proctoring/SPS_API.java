/*
 *  Copyright (c) 2019 ETH Zürich, IT Services
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.proctoring;

import java.util.*;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.async.CircuitBreaker;
import ch.ethz.seb.sebserver.gbl.client.ClientCredentials;
import ch.ethz.seb.sebserver.gbl.model.exam.SPSAPIAccessData;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordResourceDetails;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

interface SPS_API {
    
    enum SPSUserRole {
        ADMIN,
        PROCTOR
    }

    String TOKEN_ENDPOINT = "/oauth/token";
    String TEST_ENDPOINT = "/admin-api/v1/proctoring/group";

    String GROUP_COUNT_ENDPOINT = "/admin-api/v1/proctoring/active_counts";

    String USER_ACCOUNT_ENDPOINT = "/admin-api/v1/useraccount/";
    String USERSYNC_SEBSERVER_ENDPOINT = USER_ACCOUNT_ENDPOINT + "usersync/sebserver";
    String EXAM_ENDPOINT = "/admin-api/v1/exam";
    String EXAM_DELETE_REQUEST_ENDPOINT = "/request";
    String GROUP_DELETE_REQUEST_ENDPOINT = "/request";
    String SEB_ACCESS_ENDPOINT = "/admin-api/v1/clientaccess";
    String GROUP_ENDPOINT = "/admin-api/v1/group";
    String GROUP_BY_EXAM_ENDPOINT =  GROUP_ENDPOINT + "/by-exam";
    String SESSION_ENDPOINT = "/admin-api/v1/session";
    String SESSION_ENCRYPTION_KEY_ENDPOINT = SESSION_ENDPOINT + "/encrypt-key";
    String SESSION_ENCRYPTION_KEY_REQUEST_HEADER = "seb_session_encrypt_key";
    String ACTIVE_PATH_SEGMENT = "/active";
    String INACTIVE_PATH_SEGMENT = "/inactive";

    /**
     * The screen proctoring service client-access API attribute names
     */
    interface SEB_ACCESS {
        String ATTR_UUID = "uuid";
        String ATTR_NAME = "name";
        String ATTR_DESCRIPTION = "description";
        String ATTR_CLIENT_NAME = "clientName";
        String ATTR_CLIENT_SECRET = "clientSecret";
    }

    /**
     * The screen proctoring service group API attribute names
     */
    interface EXAM {
        String ATTR_ID = "id";
        String ATTR_UUID = "uuid";
        String ATTR_SEB_SERVER_ID = "sebserverId";
        String ATTR_NAME = "name";
        String ATTR_DESCRIPTION = "description";
        String ATTR_URL = "url";
        String ATTR_TYPE = "type";
        String ATTR_SUPPORTER = "supporter";
        String ATTR_START_TIME = "startTime";
        String ATTR_END_TIME = "endTime";
        String ATTR_DELETION_TIME = "deletionTime";
    }

    /**
     * The screen proctoring service seb-group API attribute names
     */
    interface GROUP {
        String ATTR_UUID = "uuid";
        String ATTR_EXAM_ID = "examId";
        String ATTR_NAME = "name";
        String ATTR_DESCRIPTION = "description";
    }

    /**
     * The screen proctoring service session API attribute names
     */
    interface SESSION {
        String ATTR_UUID = "uuid";
        String ATTR_GROUP_ID = "groupId";
        String ATTR_CLIENT_NAME = "clientName";
        String ATTR_CLIENT_IP = "clientIp";
        String ATTR_CLIENT_MACHINE_NAME = "clientMachineName";
        String ATTR_CLIENT_OS_NAME = "clientOsName";
        String ATTR_CLIENT_VERSION = "clientVersion";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    final class ExamUpdate {
        @JsonProperty(EXAM.ATTR_NAME)
        final String name;
        @JsonProperty(EXAM.ATTR_DESCRIPTION)
        final String description;
        @JsonProperty(EXAM.ATTR_URL)
        final String url;
        @JsonProperty(EXAM.ATTR_TYPE)
        final String type;
        @JsonProperty(EXAM.ATTR_SUPPORTER)
        final Collection<String> supporter;
        @JsonProperty(EXAM.ATTR_START_TIME)
        final Long startTime;
        @JsonProperty(EXAM.ATTR_END_TIME)
        final Long endTime;
        @JsonProperty(EXAM.ATTR_DELETION_TIME)
        final Long deletionTime;

        public ExamUpdate(
                final String name,
                final String description,
                final String url,
                final String type,
                final Long startTime,
                final Long endTime,
                final Long deletionTime,
                final Collection<String> supporter) {

            this.name = name;
            this.description = description;
            this.url = url;
            this.type = type;
            this.startTime = startTime;
            this.endTime = endTime;
            this.deletionTime = deletionTime;
            this.supporter = supporter;
        }
    }

    // TODO make this more compact
    @JsonIgnoreProperties(ignoreUnknown = true)
    final class GroupSessionCount {
        @JsonProperty("uuid")
        public final String groupUUID;
        @JsonProperty("activeCount")
        public final Integer activeCount;
        @JsonProperty("totalCount")
        public final Integer totalCount;

        @JsonCreator
        public GroupSessionCount(
                @JsonProperty("uuid") final String groupUUID,
                @JsonProperty("activeCount") final Integer activeCount,
                @JsonProperty("totalCount") final Integer totalCount) {

            this.groupUUID = groupUUID;
            this.activeCount = activeCount;
            this.totalCount = totalCount;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SPSGroup(
            @JsonProperty("uuid") String uuid,
            @JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("terminationTime") Long terminationTime,
            @JsonProperty("examId") Long exam_id) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    final class SPSData {

        public static final String ATTR_SPS_ACTIVE = "spsExamActive";
        public static final String ATTR_SPS_ACCESS_DATA = "spsAccessData";

        @JsonProperty("spsSEBAccessUUID")
        String spsSEBAccessUUID = null;
        @JsonProperty("spsSEBAccessName")
        String spsSEBAccessName = null;
        @JsonProperty("spsSEBAccessPWD")
        String spsSEBAccessPWD = null;
        @JsonProperty("psExamUUID")
        String spsExamUUID = null;

        SPSData() {}

        @JsonCreator
        SPSData(@JsonProperty("spsSEBAccessUUID") final String spsSEBAccessUUID,
                // NOTE: this is only for compatibility reasons, TODO as soon as possible
                @JsonProperty("spsSEBAccesUUID") final String spsSEBAccesUUID,
                @JsonProperty("spsSEBAccessName") final String spsSEBAccessName,
                @JsonProperty("spsSEBAccessPWD") final String spsSEBAccessPWD,
                @JsonProperty("psExamUUID") final String spsExamUUID) {

            this.spsSEBAccessUUID = StringUtils.isNotBlank(spsSEBAccesUUID) ? spsSEBAccesUUID : spsSEBAccessUUID;
            this.spsSEBAccessName = spsSEBAccessName;
            this.spsSEBAccessPWD = spsSEBAccessPWD;
            this.spsExamUUID = spsExamUUID;
        }
    }

    final class ScreenProctoringServiceOAuthTemplate {

        private static final Logger log = LoggerFactory.getLogger(ScreenProctoringServiceOAuthTemplate.class);


        static final String GRANT_TYPE = "password";
        static final List<String> SCOPES = Collections.unmodifiableList(
                Arrays.asList("read", "write"));

        //final SPSAPIAccessData spsAPIAccessData;
        final String spsServiceURL;
        final CircuitBreaker<ResponseEntity<String>> circuitBreaker;
        final OAuth2RestTemplate restTemplate;

        ScreenProctoringServiceOAuthTemplate(
                final ScreenProctoringAPIBinding apiBinding,
                final SPSAPIAccessData spsAPIAccessData) {

            this.spsServiceURL = spsAPIAccessData.getSpsServiceURL();
            //this.spsAPIAccessData = spsAPIAccessData;
            this.circuitBreaker = apiBinding.asyncService.createCircuitBreaker(
                    2,
                    10 * Constants.SECOND_IN_MILLIS,
                    10 * Constants.SECOND_IN_MILLIS);

            final ClientCredentials clientCredentials = new ClientCredentials(
                    spsAPIAccessData.getSpsAPIKey(),
                    spsAPIAccessData.getSpsAPISecret());

            CharSequence decryptedSecret = apiBinding.cryptor
                    .decrypt(clientCredentials.secret)
                    .getOr(clientCredentials.secret);

            final ResourceOwnerPasswordResourceDetails resource = new ResourceOwnerPasswordResourceDetails();
            resource.setAccessTokenUri(spsAPIAccessData.getSpsServiceURL() + TOKEN_ENDPOINT);
            resource.setClientId(clientCredentials.clientIdAsString());
            resource.setClientSecret(decryptedSecret.toString());
            resource.setGrantType(GRANT_TYPE);
            resource.setScope(SCOPES);
            final ClientCredentials userCredentials = new ClientCredentials(
                    spsAPIAccessData.getSpsAccountId(),
                    spsAPIAccessData.getSpsAccountPassword());

            decryptedSecret = apiBinding.cryptor
                    .decrypt(userCredentials.secret)
                    .getOr(userCredentials.secret);

            resource.setUsername(userCredentials.clientIdAsString());
            resource.setPassword(decryptedSecret.toString());

            this.restTemplate = apiBinding.getOAuth2RestTemplate(resource);
        }

        ResponseEntity<String> testServiceConnection() {

            try {
                this.restTemplate.getAccessToken();
            } catch (final Exception e) {
                final String errors = StringUtils.join(Utils.reduceToErrorMessages(e), "\n  --> ");
                log.error("Failed to get access token for SEB Screen Proctoring Service: {}", errors);
                if (errors.contains("Connection refused")) {
                    return new ResponseEntity<>( errors, HttpStatus.SERVICE_UNAVAILABLE);
                }
                // TODO Test SSL error
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }

            try {

                final String url = UriComponentsBuilder
                        .fromUriString(this.spsServiceURL)
                        .path(TEST_ENDPOINT)
                        .queryParam("pageSize", "1")
                        .queryParam("pageNumber", "1")

                        .build()
                        .toUriString();

                return exchange(url, HttpMethod.GET);

            } catch (final Exception e) {
                log.error("Failed to test SEB Screen Proctoring service connection: ", e);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        boolean isValid() {
            
            try {

                final OAuth2AccessToken accessToken = this.restTemplate.getAccessToken();
                if (accessToken == null) {
                    return false;
                }

                final boolean expired = accessToken.isExpired();
                if (expired) {
                    return false;
                }

                return accessToken.getExpiresIn() >= 60;

            } catch (final Exception e) {
                log.error("Failed to verify SEB Screen Proctoring OAuth2RestTemplate status", e);
                return false;
            }
        }

        ResponseEntity<String> exchange(
                final String url,
                final HttpMethod method) {

            return exchange(url, method, null, getHeaders());
        }

        ResponseEntity<String> exchange(
                final String url,
                final String body) {

            return exchange(url, HttpMethod.POST, body, getHeaders());
        }

        ResponseEntity<String> exchangePUT(
                final String url,
                final String body) {

            final HttpHeaders httpHeaders = getHeaders();
            httpHeaders.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            return exchange(url, HttpMethod.PUT, body, httpHeaders);
        }

        HttpHeaders getHeadersJSONRequest() {
            final HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            httpHeaders.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
            return httpHeaders;
        }

        HttpHeaders getHeaders() {
            final HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);
            httpHeaders.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
            return httpHeaders;
        }

        ResponseEntity<String> exchange(
                final String url,
                final HttpMethod method,
                final Object body,
                final HttpHeaders httpHeaders) {

            final Result<ResponseEntity<String>> protectedRunResult = this.circuitBreaker.protectedRun(() -> {
                final HttpEntity<Object> httpEntity = (body != null)
                        ? new HttpEntity<>(body, httpHeaders)
                        : new HttpEntity<>(httpHeaders);

                try {
                    final ResponseEntity<String> result = this.restTemplate.exchange(
                            url,
                            method,
                            httpEntity,
                            String.class);

                    if (result.getStatusCode().value() >= 400) {
                        log.warn("Error response on SEB Screen Proctoring Service API call to {} response status: {}",
                                url,
                                result.getStatusCode());
                    }

                    return result;
                } catch (final RestClientResponseException rce) {
                    return ResponseEntity
                            .status(rce.getRawStatusCode())
                            .body(rce.getResponseBodyAsString());
                }
            });
            return protectedRunResult.getOrThrow();
        }
    }
}
