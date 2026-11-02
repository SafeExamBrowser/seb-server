package ch.ethz.seb.sebserver.webservice.weblayer.oauth;

import ch.ethz.seb.sebserver.ClientHttpRequestFactoryService;
import ch.ethz.seb.sebserver.gbl.util.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Lazy
public class OAuthRestTemplateFactory {

    private static final Logger log = LoggerFactory.getLogger(OAuthRestTemplateFactory.class);

    private final ClientHttpRequestFactoryService clientHttpRequestFactoryService;

    public OAuthRestTemplateFactory(ClientHttpRequestFactoryService clientHttpRequestFactoryService) {
        this.clientHttpRequestFactoryService = clientHttpRequestFactoryService;
    }

    public OAuthRestTemplate getOAuth2RestTemplate(
            final String apiURL,
            final String tokenPath,
            final OAuthRestTemplate.ClientSettingsProvider clientSettingsProvider) {

        final RestTemplate restTemplate = new RestTemplate();
        final Result<ClientHttpRequestFactory> clientHttpRequestFactoryRequest = this.clientHttpRequestFactoryService
                .getClientHttpRequestFactory()
                .onSuccess(restTemplate::setRequestFactory)
                .onError(error -> {
                    log.error("Failed to get ClientHttpRequestFactory. Use SimpleClientHttpRequestFactory, cause: ", error);
                    restTemplate.setRequestFactory(new SimpleClientHttpRequestFactory());
                } );

        return new OAuthRestTemplate(apiURL, tokenPath, clientSettingsProvider, restTemplate);
    }

}
