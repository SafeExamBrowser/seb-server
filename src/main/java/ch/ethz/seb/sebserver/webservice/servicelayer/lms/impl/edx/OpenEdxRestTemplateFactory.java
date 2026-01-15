/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl.edx;

import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import ch.ethz.seb.sebserver.webservice.weblayer.oauth.OAuthRestTemplate;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.client.ClientHttpRequestFactory;

import ch.ethz.seb.sebserver.ClientHttpRequestFactoryService;
import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.APIMessage;
import ch.ethz.seb.sebserver.gbl.client.ClientCredentialService;
import ch.ethz.seb.sebserver.gbl.client.ClientCredentials;
import ch.ethz.seb.sebserver.gbl.client.ProxyData;
import ch.ethz.seb.sebserver.gbl.model.Domain.LMS_SETUP;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.LmsType;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetupTestResult;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.lms.APITemplateDataSupplier;
import org.springframework.web.client.RestTemplate;

final class OpenEdxRestTemplateFactory {

    private static final String OPEN_EDX_DEFAULT_TOKEN_REQUEST_PATH = "/oauth2/access_token";

    final APITemplateDataSupplier apiTemplateDataSupplier;
    final ClientHttpRequestFactoryService clientHttpRequestFactoryService;
    final ClientCredentialService clientCredentialService;
    final Set<String> knownTokenAccessPaths;

    OpenEdxRestTemplateFactory(
            final APITemplateDataSupplier apiTemplateDataSupplier,
            final ClientCredentialService clientCredentialService,
            final ClientHttpRequestFactoryService clientHttpRequestFactoryService,
            final String[] alternativeTokenRequestPaths) {

        this.apiTemplateDataSupplier = apiTemplateDataSupplier;
        this.clientCredentialService = clientCredentialService;
        this.clientHttpRequestFactoryService = clientHttpRequestFactoryService;

        this.knownTokenAccessPaths = new HashSet<>();
        this.knownTokenAccessPaths.add(OPEN_EDX_DEFAULT_TOKEN_REQUEST_PATH);
        if (alternativeTokenRequestPaths != null) {
            this.knownTokenAccessPaths.addAll(Arrays.asList(alternativeTokenRequestPaths));
        }
    }

    APITemplateDataSupplier getApiTemplateDataSupplier() {
        return this.apiTemplateDataSupplier;
    }

    public LmsSetupTestResult test() {

        final LmsSetup lmsSetup = this.apiTemplateDataSupplier.getLmsSetup();
        final ClientCredentials lmsClientCredentials = this.apiTemplateDataSupplier.getLmsClientCredentials();

        final List<APIMessage> missingAttrs = new ArrayList<>();
        if (StringUtils.isBlank(lmsSetup.lmsApiUrl)) {
            missingAttrs.add(APIMessage.fieldValidationError(
                    LMS_SETUP.ATTR_LMS_URL,
                    "lmsSetup:lmsUrl:notNull"));
        } else {
            // try to connect to the url
            if (!Utils.pingHost(lmsSetup.lmsApiUrl)) {
                missingAttrs.add(APIMessage.fieldValidationError(
                        LMS_SETUP.ATTR_LMS_URL,
                        "lmsSetup:lmsUrl:url.invalid"));
            }
        }
        if (!lmsClientCredentials.hasClientId()) {
            missingAttrs.add(APIMessage.fieldValidationError(
                    LMS_SETUP.ATTR_LMS_CLIENTNAME,
                    "lmsSetup:lmsClientname:notNull"));
        }
        if (!lmsClientCredentials.hasSecret()) {
            missingAttrs.add(APIMessage.fieldValidationError(
                    LMS_SETUP.ATTR_LMS_CLIENTSECRET,
                    "lmsSetup:lmsClientsecret:notNull"));
        }

        if (!missingAttrs.isEmpty()) {
            return LmsSetupTestResult.ofMissingAttributes(LmsType.OPEN_EDX, missingAttrs);
        }

        return LmsSetupTestResult.ofOkay(LmsType.OPEN_EDX);
    }

    Result<OAuthRestTemplate> createOAuthRestTemplate() {
        return this.knownTokenAccessPaths
                .stream()
                .map(this::createOAuthRestTemplate)
                .filter(Result::hasValue)
                .findFirst()
                .orElse(Result.ofRuntimeError(
                        "Failed to gain any access on paths: " + this.knownTokenAccessPaths));
    }

    Result<OAuthRestTemplate> createOAuthRestTemplate(final String accessTokenPath) {
        return Result.tryCatch(() -> {
            final OAuthRestTemplate template = createRestTemplate(accessTokenPath);

            CharSequence accessToken = template.getAccessToken();
            if (accessToken == null) {
                throw new RuntimeException("Failed to gain access token on path: " + accessTokenPath);
            }

            return template;
        });
    }

    private OAuthRestTemplate createRestTemplate(final String accessTokenRequestPath) throws URISyntaxException {

        final LmsSetup lmsSetup = this.apiTemplateDataSupplier.getLmsSetup();
        final ClientCredentials credentials = this.apiTemplateDataSupplier.getLmsClientCredentials();
        final ProxyData proxyData = this.apiTemplateDataSupplier.getProxyData();

        final ClientHttpRequestFactory clientHttpRequestFactory = this.clientHttpRequestFactoryService
                .getClientHttpRequestFactory(proxyData)
                .getOrThrow();

        final RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(clientHttpRequestFactory);

        if (credentials.accessToken != null) {
            CharSequence accessToken = this.clientCredentialService
                    .getPlainAccessToken(credentials)
                    .getOrThrow();

            return new OAuthRestTemplate(
                    lmsSetup.lmsApiUrl,
                    accessToken,
                    restTemplate);
        }
        final CharSequence plainClientId = credentials.clientId;
        final CharSequence plainClientSecret = this.clientCredentialService
                .getPlainClientSecret(credentials)
                .getOrThrow();






        return new OAuthRestTemplate(
                lmsSetup.lmsApiUrl,
                accessTokenRequestPath,
                new EdxClientSettingsProvider(plainClientId.toString(), plainClientSecret),
                restTemplate);


//        final ClientCredentialsResourceDetails details = new ClientCredentialsResourceDetails();
//        details.setAccessTokenUri(lmsSetup.lmsApiUrl + accessTokenRequestPath);
//        details.setClientId(plainClientId.toString());
//        details.setClientSecret(plainClientSecret.toString());
//
//        final ClientHttpRequestFactory clientHttpRequestFactory = this.clientHttpRequestFactoryService
//                .getClientHttpRequestFactory(proxyData)
//                .getOrThrow();
//
//        final OAuth2RestTemplate template = new OAuth2RestTemplate(details);
//        template.setRequestFactory(clientHttpRequestFactory);
//        template.setAccessTokenProvider(new EdxClientCredentialsAccessTokenProvider());
//
//        return template;
    }

    private static final class EdxClientSettingsProvider implements OAuthRestTemplate.ClientSettingsProvider {
        private final String clientId;
        private final CharSequence clientSecret;

        public EdxClientSettingsProvider(
                String clientId,
                CharSequence clientSecret) {

            this.clientId = clientId;
            this.clientSecret = clientSecret;
        }

        public String getClientId() {
            return clientId;
        }

        public CharSequence getClientSecret() {
            return clientSecret;
        }

        public String getScopes() {
            return null;
        }

        public CharSequence getPassword() {
            return null;
        }

        public String getUsername() {
            return null;
        }

        @Override
        public String getBasicAuthHeader() {
            final String auth = clientId + Constants.COLON + clientSecret;
            final String authEncoded = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            return "Basic " + authEncoded;
        }

        @Override
        public String getOAuthBody() {
            return "grant_type=client_credentials&client_id=" + clientId + "&client_secret=" + clientSecret;
        }
    }

//    /** A custom ClientCredentialsAccessTokenProvider that adapts the access token request to Open edX
//     * access token request protocol using a form-URL-encoded POST request according to:
//     * https://course-catalog-api-guide.readthedocs.io/en/latest/authentication/index.html#getting-an-access-token */
//    private static final class EdxClientCredentialsAccessTokenProvider extends ClientCredentialsAccessTokenProvider {
//
//        @Override
//        public OAuth2AccessToken obtainAccessToken(
//                final OAuth2ProtectedResourceDetails details,
//                final AccessTokenRequest request)
//                throws UserRedirectRequiredException,
//                AccessDeniedException,
//                OAuth2AccessDeniedException {
//
//            if (details instanceof ClientCredentialsResourceDetails) {
//                final ClientCredentialsResourceDetails resource = (ClientCredentialsResourceDetails) details;
//                final HttpHeaders headers = new HttpHeaders();
//                headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);
//
//                final MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
//                params.add(OAuth2Utils.GRANT_TYPE, Constants.OAUTH2_GRANT_TYPE_CLIENT_CREDENTIALS);
//                params.add(OAuth2Utils.CLIENT_ID, resource.getClientId());
//                params.add(Constants.OAUTH2_CLIENT_SECRET, resource.getClientSecret());
//
//                final OAuth2AccessToken retrieveToken = retrieveToken(request, resource, params, headers);
//                return retrieveToken;
//            } else {
//                return super.obtainAccessToken(details, request);
//            }
//        }
//    }

}
