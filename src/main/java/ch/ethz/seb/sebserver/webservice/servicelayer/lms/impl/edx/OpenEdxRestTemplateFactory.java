/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.edx;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2AccessDeniedException;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.resource.UserRedirectRequiredException;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.util.OAuth2Utils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import ch.ethz.seb.sebserver.ClientHttpRequestFactoryService;
import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.client.ClientCredentialService;
import ch.ethz.seb.sebserver.gbl.client.ClientCredentials;
import ch.ethz.seb.sebserver.gbl.client.ProxyData;
import ch.ethz.seb.sebserver.gbl.model.Domain.LMS_SETUP;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
import ch.ethz.seb.sebserver.gbl.util.Result;

final class OpenEdxRestTemplateFactory {

    private static final String OPEN_EDX_DEFAULT_TOKEN_REQUEST_PATH = "/oauth2/access_token";

    final LmsSetup lmsSetup;
    final ClientCredentials credentials;
    final ProxyData proxyData;
    final ClientHttpRequestFactoryService clientHttpRequestFactoryService;
    final ClientCredentialService clientCredentialService;
    final Set<String> knownTokenAccessPaths;

    OpenEdxRestTemplateFactory(
            final LmsSetup lmsSetup,
            final ClientCredentials credentials,
            final ProxyData proxyData,
            final ClientCredentialService clientCredentialService,
            final ClientHttpRequestFactoryService clientHttpRequestFactoryService,
            final String[] alternativeTokenRequestPaths) {

        this.lmsSetup = lmsSetup;
        this.clientCredentialService = clientCredentialService;
        this.credentials = credentials;
        this.proxyData = proxyData;
        this.clientHttpRequestFactoryService = clientHttpRequestFactoryService;

        this.knownTokenAccessPaths = new HashSet<>();
        this.knownTokenAccessPaths.add(OPEN_EDX_DEFAULT_TOKEN_REQUEST_PATH);
        if (alternativeTokenRequestPaths != null) {
            this.knownTokenAccessPaths.addAll(Arrays.asList(alternativeTokenRequestPaths));
        }
    }

    public LmsSetupTestResult test() {
        final List<APIMessage> missingAttrs = new ArrayList<>();
        if (StringUtils.isBlank(this.lmsSetup.lmsApiUrl)) {
            missingAttrs.add(APIMessage.fieldValidationError(
                    LMS_SETUP.ATTR_LMS_URL,
                    "lmsSetup:lmsUrl:notNull"));
        }
        if (!this.credentials.hasClientId()) {
            missingAttrs.add(APIMessage.fieldValidationError(
                    LMS_SETUP.ATTR_LMS_CLIENTNAME,
                    "lmsSetup:lmsClientname:notNull"));
        }
        if (!this.credentials.hasSecret()) {
            missingAttrs.add(APIMessage.fieldValidationError(
                    LMS_SETUP.ATTR_LMS_CLIENTSECRET,
                    "lmsSetup:lmsClientsecret:notNull"));
        }

        if (!missingAttrs.isEmpty()) {
            return LmsSetupTestResult.ofMissingAttributes(missingAttrs);
        }

        return LmsSetupTestResult.ofOkay();
    }

    Result<OAuth2RestTemplate> createOAuthRestTemplate() {
        return this.knownTokenAccessPaths
                .stream()
                .map(this::createOAuthRestTemplate)
                .filter(Result::hasValue)
                .findFirst()
                .orElse(Result.ofRuntimeError(
                        "Failed to gain any access on paths: " + this.knownTokenAccessPaths));
    }

    Result<OAuth2RestTemplate> createOAuthRestTemplate(final String accessTokenPath) {
        return Result.tryCatch(() -> {
            final OAuth2RestTemplate template = createRestTemplate(
                    this.lmsSetup,
                    this.credentials,
                    accessTokenPath);

            final OAuth2AccessToken accessToken = template.getAccessToken();
            if (accessToken == null) {
                throw new RuntimeException("Failed to gain access token on path: " + accessTokenPath);
            }

            return template;
        });
    }

    private OAuth2RestTemplate createRestTemplate(
            final LmsSetup lmsSetup,
            final ClientCredentials credentials,
            final String accessTokenRequestPath) throws URISyntaxException {

        final CharSequence plainClientId = credentials.clientId;
        final CharSequence plainClientSecret = this.clientCredentialService.getPlainClientSecret(credentials);

        final ClientCredentialsResourceDetails details = new ClientCredentialsResourceDetails();
        details.setAccessTokenUri(lmsSetup.lmsApiUrl + accessTokenRequestPath);
        details.setClientId(plainClientId.toString());
        details.setClientSecret(plainClientSecret.toString());

        final ClientHttpRequestFactory clientHttpRequestFactory = this.clientHttpRequestFactoryService
                .getClientHttpRequestFactory(this.proxyData)
                .getOrThrow();

        final OAuth2RestTemplate template = new OAuth2RestTemplate(details);
        template.setRequestFactory(clientHttpRequestFactory);
        template.setAccessTokenProvider(new EdxClientCredentialsAccessTokenProvider());

        return template;
    }

    /** A custom ClientCredentialsAccessTokenProvider that adapts the access token request to Open edX
     * access token request protocol using a form-URL-encoded POST request according to:
     * https://course-catalog-api-guide.readthedocs.io/en/latest/authentication/index.html#getting-an-access-token */
    private static final class EdxClientCredentialsAccessTokenProvider extends ClientCredentialsAccessTokenProvider {

        @Override
        public OAuth2AccessToken obtainAccessToken(
                final OAuth2ProtectedResourceDetails details,
                final AccessTokenRequest request)
                throws UserRedirectRequiredException,
                AccessDeniedException,
                OAuth2AccessDeniedException {

            if (details instanceof ClientCredentialsResourceDetails) {
                final ClientCredentialsResourceDetails resource = (ClientCredentialsResourceDetails) details;
                final HttpHeaders headers = new HttpHeaders();
                headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);

                final MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
                params.add(OAuth2Utils.GRANT_TYPE, Constants.OAUTH2_GRANT_TYPE_CLIENT_CREDENTIALS);
                params.add(OAuth2Utils.CLIENT_ID, resource.getClientId());
                params.add(Constants.OAUTH2_CLIENT_SECRET, resource.getClientSecret());

                final OAuth2AccessToken retrieveToken = retrieveToken(request, resource, params, headers);
                return retrieveToken;
            } else {
                return super.obtainAccessToken(details, request);
            }
        }
    }

}
