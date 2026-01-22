package ch.ethz.seb.sebserver.webservice.weblayer.oauth;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.ArrayUtils.addFirst;

public class OAuthRestTemplate {

    private static final Logger log = LoggerFactory.getLogger(OAuthRestTemplate.class);
    public static final String BEARER_HEADER_PREFIX = "Bearer ";

    private final String apiURL;
    private final String tokenPath;
    private final ClientSettingsProvider clientSettingsProvider;
    private final RestTemplate restTemplate;
    private final JSONMapper jsonMapper;

    private TokenResponse tokenResponse;
    private long expiresAt;

    public OAuthRestTemplate(
            final String apiURL,
            final String tokenPath,
            final ClientSettingsProvider clientSettingsProvider,
            final RestTemplate restTemplate) {

        this.apiURL = apiURL;
        this.tokenPath = tokenPath;
        this.clientSettingsProvider = clientSettingsProvider;
        this.restTemplate = restTemplate;
        this.jsonMapper = new JSONMapper();

        // inject our JSON mapper to the JSON response message converter to support special types
        List<HttpMessageConverter<?>> messageConverters = restTemplate.getMessageConverters();
        for (HttpMessageConverter<?> c : messageConverters) {
            if (c instanceof MappingJackson2HttpMessageConverter) {
                ((MappingJackson2HttpMessageConverter) c).setObjectMapper(jsonMapper);
            }
        }
    }

    public OAuthRestTemplate(
            CharSequence accessToken,
            RestTemplate restTemplate) {

        this.apiURL = null;
        this.tokenPath = null;
        this.clientSettingsProvider = null;
        this.restTemplate = restTemplate;
        this.jsonMapper = new JSONMapper();
        tokenResponse = new TokenResponse(
                accessToken.toString(),
                null,
                null,
                null,
                -1L);
        this.expiresAt = -1;
    }

    public CharSequence getAccessToken() {
        checkAccessToken();
        return tokenResponse.access_token;
    }

    public void clearToken() {
        tokenResponse = null;
    }

    public <T> ResponseEntity<T> getForEntity(String url, Class<T> type) {
        // set auth header with access token
        final HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set(HttpHeaders.AUTHORIZATION, BEARER_HEADER_PREFIX +  tokenResponse.access_token);
        final HttpEntity<?> tokenReqEntity = new HttpEntity<>(httpHeaders);
        // delegate to restTemplate
        return restTemplate.exchange(url, HttpMethod.GET, tokenReqEntity, type);
    }

    public Boolean execute(
            String url,
            HttpMethod httpMethod,
            ResponseExtractor<Boolean> responseExtractor,
            MultiValueMap<String, String> headers,
            Map<String, String> uriVariables,
            Class<?> type) {

        LinkedMultiValueMap<String, String> h = new LinkedMultiValueMap<>(headers);
        h.set(HttpHeaders.AUTHORIZATION, BEARER_HEADER_PREFIX + tokenResponse.access_token);
        RequestCallback requestCallback = restTemplate.httpEntityCallback( new HttpEntity<>(h), type);

        return restTemplate.execute(url, httpMethod, requestCallback, responseExtractor, uriVariables);
    }

    public <T> ResponseEntity<T> exchange(
            final String url,
            final HttpMethod method,
            final HttpEntity<?> entity,
            final Class<T> type,
            final Map<String, String> uriVariables) {

        // set auth header with access token
        LinkedMultiValueMap<String, String> headers = new LinkedMultiValueMap<>(entity.getHeaders());
        headers.set(HttpHeaders.AUTHORIZATION, BEARER_HEADER_PREFIX + tokenResponse.access_token);
        // delegate to restTemplate
        return restTemplate.exchange(url, method, new HttpEntity<>(entity.getBody(), headers), type, uriVariables);
    }

    public <T> ResponseEntity<T> exchange(
            final String url,
            final HttpMethod method,
            final HttpEntity<?> entity,
            final Class<T> type) {

        LinkedMultiValueMap<String, String> headers = new LinkedMultiValueMap<>(entity.getHeaders());
        headers.set(HttpHeaders.AUTHORIZATION, BEARER_HEADER_PREFIX + tokenResponse.access_token);
        // delegate to restTemplate
        return restTemplate.exchange(url, method, new HttpEntity<>(entity.getBody(), headers), type);
    }

    public <T> ResponseEntity<T> exchange(
            final String url,
            final HttpMethod method,
            final Object body,
            final HttpHeaders httpHeaders,
            final Class<T> type) {

        // set auth header with access token
        LinkedMultiValueMap<String, String> headers = new LinkedMultiValueMap<>(httpHeaders);
        headers.set(HttpHeaders.AUTHORIZATION, BEARER_HEADER_PREFIX + tokenResponse.access_token);
        final HttpEntity<?> reqEntity = new HttpEntity<>(body, headers);
        // delegate to restTemplate
        return restTemplate.exchange(url, method, reqEntity, type);
    }

    public <T> ResponseEntity<T> exchange(
            final String url,
            final HttpMethod method,
            final Object body,
            final HttpHeaders httpHeaders,
            final ParameterizedTypeReference<T> responseType) {

        // set auth header with access token
        httpHeaders.set(HttpHeaders.AUTHORIZATION, BEARER_HEADER_PREFIX + tokenResponse.access_token);
        final HttpEntity<?> reqEntity = new HttpEntity<>(body, httpHeaders);
        // delegate to restTemplate
        return restTemplate.exchange(url, method, reqEntity, responseType);
    }

    public ResponseEntity<String>  exchange(
            final String url,
            final HttpMethod method,
            final Object body,
            final HttpHeaders httpHeaders) {

        checkAccessToken();

        // set auth header with access token
        httpHeaders.set(HttpHeaders.AUTHORIZATION, BEARER_HEADER_PREFIX + tokenResponse.access_token);
        final HttpEntity<?> reqEntity = new HttpEntity<>(body, httpHeaders);
        // delegate to restTemplate
        return restTemplate.exchange(url, method, reqEntity, String.class);
    }

    private void checkAccessToken() {
        if (tokenResponse == null || Utils.getMillisecondsNow() > expiresAt) {
            requestAccessToken();
        }
    }

    private void requestAccessToken() {
        try {

            // headers and base auth
            final LinkedMultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);
            headers.add(HttpHeaders.AUTHORIZATION, this.clientSettingsProvider.getBasicAuthHeader());
            final String body = this.clientSettingsProvider.getOAuthBody();
            final HttpEntity<String> tokenReqEntity = new HttpEntity<>(body, headers);

            if (log.isDebugEnabled()) {
                log.debug("Request Access Token at: {} with: {}", this.apiURL + this.tokenPath, tokenReqEntity);
            }

            final ResponseEntity<String> response = restTemplate.exchange(
                    this.apiURL + this.tokenPath,
                    HttpMethod.POST,
                    tokenReqEntity,
                    String.class);

            if (response.getStatusCode() != HttpStatus.OK) {
                log.error("Error Response on access token request: {}", response);
                throw new OAuthClientException("Error Response on access token request: " + response.getStatusCode());
            }

            this.tokenResponse = jsonMapper.readValue(response.getBody(), TokenResponse.class);
            if (this.tokenResponse != null && this.tokenResponse.expires_in != null) {
                this.expiresAt = Utils.getMillisecondsNow() + this.tokenResponse.expires_in * Constants.SECOND_IN_MILLIS;
            } else {
                expiresAt = -1;
            }

        } catch (final Exception e) {
            log.error("Failed to gain OAUth access token at: {} cause: {}", this.apiURL + this.tokenPath, e.getMessage());
            throw new OAuthClientException("Failed to gain access token for OAUth API at: " +
                    this.apiURL + this.tokenPath + " cause: " + e.getMessage());
        }

    }

    public boolean checkAccessTokenExpiresIn(long offset) {
        if (expiresAt > 0) {
            return Utils.getMillisecondsNow() + offset > expiresAt;
        } else {
            // potentially never expires
            return false;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class TokenResponse {
        public final String access_token;
        public final String refresh_token;
        public final String scope;
        public final String token_type;
        public final Long expires_in;

        @JsonCreator
        public TokenResponse(
                @JsonProperty("access_token") String accessToken,
                @JsonProperty("refresh_token") String refreshToken,
                @JsonProperty("scope") String scope,
                @JsonProperty("token_type") String tokenType,
                @JsonProperty("expires_in") Long expiresIn) {

            access_token = accessToken;
            refresh_token = refreshToken;
            this.scope = scope;
            token_type = tokenType;
            expires_in = expiresIn;
        }
    }

    public interface ClientSettingsProvider {
        String getClientId();
        CharSequence getClientSecret();
        String getScopes();
        CharSequence getPassword();
        String getUsername();

        String getBasicAuthHeader();
        String getOAuthBody();
    }

    public static final class DefaultClientSettingsProvider implements ClientSettingsProvider {
        private final String clientId;
        private final CharSequence clientSecret;
        private final String username;
        private final CharSequence password;
        private String scopes;

        public DefaultClientSettingsProvider(
                String clientId,
                CharSequence clientSecret,
                String username,
                CharSequence password,
                String scopes) {

            this.clientId = clientId;
            this.clientSecret = clientSecret;
            this.username = username;
            this.password = password;
            this.scopes = scopes;
        }

        public void setScopes(String scopes) {
            this.scopes = scopes;
        }

        public String getClientId() {
            return clientId;
        }

        public CharSequence getClientSecret() {
            return clientSecret;
        }

        public String getScopes() {
            return scopes;
        }

        public CharSequence getPassword() {
            return password;
        }

        public String getUsername() {
            return username;
        }

        @Override
        public String getBasicAuthHeader() {
            final String auth = clientId + Constants.COLON + clientSecret;
            final String authEncoded = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

            return "Basic " + authEncoded;
        }

        @Override
        public String getOAuthBody() {
            String body;
            if (username != null) {
                // password flow
                body =  "grant_type=password&username=" + username + "&password=" + password;
            } else {
                // client credential flow
                body =  "grant_type=client_credentials";
            }

            if (scopes != null) {
                body += "&scope=" + scopes;
            }

            return body;
        }
    }

}
