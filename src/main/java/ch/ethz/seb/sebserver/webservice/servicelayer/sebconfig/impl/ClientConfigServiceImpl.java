/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import ch.ethz.seb.sebserver.WebSecurityConfig;
import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.model.institution.Institution;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.SebClientConfig;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.WebserviceInfo;
import ch.ethz.seb.sebserver.webservice.servicelayer.client.ClientCredentialService;
import ch.ethz.seb.sebserver.webservice.servicelayer.client.ClientCredentials;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.InstitutionDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.SebClientConfigDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.ClientConfigService;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SebConfigEncryptionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SebConfigEncryptionService.Strategy;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.ZipService;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl.SebConfigEncryptionServiceImpl.EncryptionContext;
import ch.ethz.seb.sebserver.webservice.weblayer.oauth.WebserviceResourceConfiguration;

@Lazy
@Service
@WebServiceProfile
public class ClientConfigServiceImpl implements ClientConfigService {

    private static final Logger log = LoggerFactory.getLogger(ClientConfigServiceImpl.class);

    private static final String SEB_CLIENT_CONFIG_TEMPLATE_XML =
            "  <dict>%n" +
                    "    <key>sebMode</key>%n" +
                    "    <integer>1</integer>%n" +
                    "    <key>sebConfigPurpose</key>%n" +
                    "    <integer>%s</integer>%n" +
                    "    <key>sebServerFallback</key>%n" +
                    "    <%s />%n" +
                    "%s" +
                    "    <key>sebServerURL</key>%n" +
                    "    <string>%s</string>%n" +
                    "    <key>sebServerConfiguration</key>%n" +
                    "    <dict>%n" +
                    "        <key>institution</key>%n" +
                    "        <string>%s</string>%n" +
                    "        <key>clientName</key>%n" +
                    "        <string>%s</string>%n" +
                    "        <key>clientSecret</key>%n" +
                    "        <string>%s</string>%n" +
                    "        <key>apiDiscovery</key>%n" +
                    "        <string>%s</string>%n" +
                    "    </dict>%n" +
                    "  </dict>%n";

    private final static String SEB_CLIENT_CONFIG_INTEGER_TEMPLATE =
            "    <key>%s</key>%n" +
                    "    <integer>%s</integer>%n";

    private final static String SEB_CLIENT_CONFIG_STRING_TEMPLATE =
            "    <key>%s</key>%n" +
                    "    <string>%s</string>%n";

    private final InstitutionDAO institutionDAO;
    private final SebClientConfigDAO sebClientConfigDAO;
    private final ClientCredentialService clientCredentialService;
    private final SebConfigEncryptionService sebConfigEncryptionService;
    private final PasswordEncoder clientPasswordEncoder;
    private final ZipService zipService;
    private final WebserviceInfo webserviceInfo;

    protected ClientConfigServiceImpl(
            final InstitutionDAO institutionDAO,
            final SebClientConfigDAO sebClientConfigDAO,
            final ClientCredentialService clientCredentialService,
            final SebConfigEncryptionService sebConfigEncryptionService,
            final ZipService zipService,
            @Qualifier(WebSecurityConfig.CLIENT_PASSWORD_ENCODER_BEAN_NAME) final PasswordEncoder clientPasswordEncoder,
            final WebserviceInfo webserviceInfo) {

        this.institutionDAO = institutionDAO;
        this.sebClientConfigDAO = sebClientConfigDAO;
        this.clientCredentialService = clientCredentialService;
        this.sebConfigEncryptionService = sebConfigEncryptionService;
        this.zipService = zipService;
        this.clientPasswordEncoder = clientPasswordEncoder;
        this.webserviceInfo = webserviceInfo;
    }

    @Override
    public boolean hasSebClientConfigurationForInstitution(final Long institutionId) {
        final Result<Collection<SebClientConfig>> all = this.sebClientConfigDAO.all(institutionId, true);
        return all != null && !all.hasError() && !all.getOrThrow().isEmpty();
    }

    @Override
    public Result<SebClientConfig> autoCreateSebClientConfigurationForInstitution(final Long institutionId) {
        return Result.tryCatch(() -> {
            final Institution institution = this.institutionDAO
                    .byPK(institutionId)
                    .getOrThrow();

            return new SebClientConfig(
                    null,
                    institutionId,
                    institution.name + "_" + UUID.randomUUID(),
                    null,
                    false,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    true);
        })
                .flatMap(this.sebClientConfigDAO::createNew);
    }

    @Override
    public Result<ClientDetails> getClientConfigDetails(final String clientName) {
        return this.getEncodedClientConfigSecret(clientName)
                .map(pwd -> {

                    final BaseClientDetails baseClientDetails = new BaseClientDetails(
                            Utils.toString(clientName),
                            WebserviceResourceConfiguration.EXAM_API_RESOURCE_ID,
                            null,
                            Constants.OAUTH2_GRANT_TYPE_CLIENT_CREDENTIALS,
                            StringUtils.EMPTY);

                    baseClientDetails.setScope(Collections.emptySet());
                    baseClientDetails.setClientSecret(Utils.toString(pwd));
                    baseClientDetails.setAccessTokenValiditySeconds(-1); // not expiring

                    if (log.isDebugEnabled()) {
                        log.debug("Created new BaseClientDetails for id: {}", clientName);
                    }

                    return baseClientDetails;
                });
    }

    @Override
    public void exportSebClientConfiguration(
            final OutputStream output,
            final String modelId) {

        final SebClientConfig config = this.sebClientConfigDAO
                .byModelId(modelId).getOrThrow();

        final CharSequence encryptionPassword = this.sebClientConfigDAO
                .getConfigPasswordCipher(config.getModelId())
                .getOr(null);

        final String plainTextXMLContent = extractXMLContent(config);

        PipedOutputStream pOut = null;
        PipedInputStream pIn = null;

        try {

            // zip the plain text
            final InputStream plainIn = IOUtils.toInputStream(
                    Constants.XML_VERSION_HEADER +
                            Constants.XML_DOCTYPE_HEADER +
                            Constants.XML_PLIST_START_V1 +
                            plainTextXMLContent +
                            Constants.XML_PLIST_END,
                    StandardCharsets.UTF_8.name());

            pOut = new PipedOutputStream();
            pIn = new PipedInputStream(pOut);

            this.zipService.write(pOut, plainIn);

            if (encryptionPassword != null) {
                passwordEncryption(output, encryptionPassword, pIn);
            } else {
                this.sebConfigEncryptionService.streamEncrypted(
                        output,
                        pIn,
                        EncryptionContext.contextOfPlainText());
            }

            if (log.isDebugEnabled()) {
                log.debug("*** Finished Seb client configuration download streaming composition");
            }

        } catch (final Exception e) {
            log.error("Error while zip and encrypt seb client config stream: ", e);
            try {
                if (pIn != null) {
                    pIn.close();
                }
            } catch (final IOException e1) {
                log.error("Failed to close PipedInputStream: ", e1);
            }
            try {
                if (pOut != null) {
                    pOut.close();
                }
            } catch (final IOException e1) {
                log.error("Failed to close PipedOutputStream: ", e1);
            }
        }
    }

    private String extractXMLContent(final SebClientConfig config) {

        String fallbackAddition = "";
        if (BooleanUtils.isTrue(config.fallback)) {
            fallbackAddition += String.format(
                    SEB_CLIENT_CONFIG_STRING_TEMPLATE,
                    SebClientConfig.ATTR_FALLBACK_START_URL,
                    config.fallbackStartURL);

            fallbackAddition += String.format(
                    SEB_CLIENT_CONFIG_INTEGER_TEMPLATE,
                    SebClientConfig.ATTR_FALLBACK_TIMEOUT,
                    config.fallbackTimeout);

            fallbackAddition += String.format(
                    SEB_CLIENT_CONFIG_INTEGER_TEMPLATE,
                    SebClientConfig.ATTR_FALLBACK_ATTEMPTS,
                    config.fallbackAttempts);

            fallbackAddition += String.format(
                    SEB_CLIENT_CONFIG_INTEGER_TEMPLATE,
                    SebClientConfig.ATTR_FALLBACK_ATTEMPT_INTERVAL,
                    config.fallbackAttemptInterval);

            if (StringUtils.isNotBlank(config.fallbackPassword)) {
                final CharSequence decrypt = this.clientCredentialService.decrypt(config.fallbackPassword);
                fallbackAddition += String.format(
                        SEB_CLIENT_CONFIG_STRING_TEMPLATE,
                        SebClientConfig.ATTR_FALLBACK_PASSWORD,
                        Utils.hash_SHA_256_Base_16(decrypt));
            }

            if (StringUtils.isNotBlank(config.quitPassword)) {
                final CharSequence decrypt = this.clientCredentialService.decrypt(config.quitPassword);
                fallbackAddition += String.format(
                        SEB_CLIENT_CONFIG_STRING_TEMPLATE,
                        SebClientConfig.ATTR_QUIT_PASSWORD,
                        Utils.hash_SHA_256_Base_16(decrypt));
            }
        }

        final ClientCredentials sebClientCredentials = this.sebClientConfigDAO
                .getSebClientCredentials(config.getModelId())
                .getOrThrow();
        final CharSequence plainClientId = sebClientCredentials.clientId;
        final CharSequence plainClientSecret = this.clientCredentialService
                .getPlainClientSecret(sebClientCredentials);

        final String plainTextConfig = String.format(
                SEB_CLIENT_CONFIG_TEMPLATE_XML,
                config.configPurpose.ordinal(),
                (StringUtils.isNotBlank(config.fallbackStartURL))
                        ? "true"
                        : "false",
                fallbackAddition,
                this.webserviceInfo.getExternalServerURL(),
                config.institutionId,
                plainClientId,
                plainClientSecret,
                this.webserviceInfo.getDiscoveryEndpoint());

        if (log.isDebugEnabled()) {
            log.debug("SEB client configuration export:\n {}", plainTextConfig);
        }

        return plainTextConfig;
    }

    @Override
    public boolean checkAccess(final SebClientConfig config) {
        if (!config.isActive()) {
            return false;
        }

        try {
            final RestTemplate restTemplate = new RestTemplate();
            final String externalServerURL = this.webserviceInfo.getExternalServerURL() +
                    API.OAUTH_TOKEN_ENDPOINT;

            final MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);
            final ClientCredentials credentials = this.sebClientConfigDAO
                    .getSebClientCredentials(config.getModelId())
                    .getOrThrow();
            final CharSequence plainClientSecret = this.clientCredentialService.getPlainClientSecret(credentials);
            final String basicAuth = credentials.clientId +
                    String.valueOf(Constants.COLON) +
                    plainClientSecret;
            final String encoded = Base64.getEncoder()
                    .encodeToString(basicAuth.getBytes(StandardCharsets.UTF_8));

            headers.add(HttpHeaders.AUTHORIZATION, "Basic " + encoded);
            final HttpEntity<String> entity = new HttpEntity<>(
                    "grant_type=client_credentials&scope=read write",
                    headers);

            final ResponseEntity<String> exchange = restTemplate.exchange(
                    externalServerURL,
                    HttpMethod.POST,
                    entity,
                    String.class);

            if (exchange.getStatusCode().value() == HttpStatus.OK.value()) {
                return true;
            } else {
                log.warn("Failed to check access SebClientConfig {} response: {}", config, exchange.getStatusCode());
                return false;
            }
        } catch (final Exception e) {
            log.warn("Failed to check access for SebClientConfig: {} cause: {}", config, e.getMessage());
            return false;
        }
    }

    private void passwordEncryption(
            final OutputStream output,
            final CharSequence encryptionPassword,
            final InputStream input) {

        if (log.isDebugEnabled()) {
            log.debug("*** Seb client configuration with password based encryption");
        }

        final CharSequence encryptionPasswordPlaintext = this.clientCredentialService
                .decrypt(encryptionPassword);

        this.sebConfigEncryptionService.streamEncrypted(
                output,
                input,
                EncryptionContext.contextOf(
                        Strategy.PASSWORD_PSWD,
                        encryptionPasswordPlaintext));
    }

    /** Get a encoded clientSecret for the SebClientConfiguration with specified clientId/clientName.
     *
     * @param clientId the clientId/clientName
     * @return encoded clientSecret for that SebClientConfiguration with clientId or null of not existing */
    private Result<CharSequence> getEncodedClientConfigSecret(final String clientId) {
        return this.sebClientConfigDAO.getConfigPasswordCipherByClientName(clientId)
                .map(cipher -> this.clientPasswordEncoder.encode(this.clientCredentialService.decrypt(cipher)));
    }

}
