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
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.WebSecurityConfig;
import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.API.BulkActionType;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import ch.ethz.seb.sebserver.gbl.model.institution.Institution;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.SebClientConfig;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.WebserviceInfo;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkAction;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.BulkActionEvent;
import ch.ethz.seb.sebserver.webservice.servicelayer.client.ClientCredentialService;
import ch.ethz.seb.sebserver.webservice.servicelayer.client.ClientCredentials;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.InstitutionDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.SebClientConfigDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SebClientConfigService;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SebConfigEncryptionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SebConfigEncryptionService.Strategy;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.ZipService;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl.SebConfigEncryptionServiceImpl.EncryptionContext;
import ch.ethz.seb.sebserver.webservice.weblayer.oauth.WebserviceResourceConfiguration;

@Lazy
@Service
@WebServiceProfile
public class SebClientConfigServiceImpl implements SebClientConfigService {

    private static final Logger log = LoggerFactory.getLogger(SebClientConfigServiceImpl.class);

    private final InstitutionDAO institutionDAO;
    private final SebClientConfigDAO sebClientConfigDAO;
    private final ClientCredentialService clientCredentialService;
    private final SebConfigEncryptionService sebConfigEncryptionService;
    private final PasswordEncoder clientPasswordEncoder;
    private final ZipService zipService;
    private final TokenStore tokenStore;
    private final WebserviceInfo webserviceInfo;

    protected SebClientConfigServiceImpl(
            final InstitutionDAO institutionDAO,
            final SebClientConfigDAO sebClientConfigDAO,
            final ClientCredentialService clientCredentialService,
            final SebConfigEncryptionService sebConfigEncryptionService,
            final ZipService zipService,
            final TokenStore tokenStore,
            @Qualifier(WebSecurityConfig.CLIENT_PASSWORD_ENCODER_BEAN_NAME) final PasswordEncoder clientPasswordEncoder,
            final WebserviceInfo webserviceInfo) {

        this.institutionDAO = institutionDAO;
        this.sebClientConfigDAO = sebClientConfigDAO;
        this.clientCredentialService = clientCredentialService;
        this.sebConfigEncryptionService = sebConfigEncryptionService;
        this.zipService = zipService;
        this.clientPasswordEncoder = clientPasswordEncoder;
        this.tokenStore = tokenStore;
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
                .getConfigPasswortCipher(config.getModelId())
                .getOrThrow();

        final String plainTextConfig = getPlainXMLConfig(config);

        PipedOutputStream pOut = null;
        PipedInputStream pIn = null;

        try {

            // zip the plain text
            final InputStream plainIn = IOUtils.toInputStream(
                    Constants.XML_VERSION_HEADER +
                            Constants.XML_DOCTYPE_HEADER +
                            Constants.XML_PLIST_START_V1 +
                            plainTextConfig +
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

        } catch (final Exception e) {
            log.error("Error while zip and encrypt seb client config stream: ", e);
            try {
                if (pIn != null)
                    pIn.close();
            } catch (final IOException e1) {
                log.error("Failed to close PipedInputStream: ", e1);
            }
            try {
                if (pOut != null)
                    pOut.close();
            } catch (final IOException e1) {
                log.error("Failed to close PipedOutputStream: ", e1);
            }
        }
    }

    public String getPlainXMLConfig(final SebClientConfig config) {

        final ClientCredentials sebClientCredentials = this.sebClientConfigDAO
                .getSebClientCredentials(config.getModelId())
                .getOrThrow();

        final CharSequence plainClientId = sebClientCredentials.clientId;
        final CharSequence plainClientSecret = this.clientCredentialService
                .getPlainClientSecret(sebClientCredentials);

        final String plainTextConfig = extractXML(
                config,
                plainClientId,
                plainClientSecret);

        return plainTextConfig;
    }

    private String extractXML(final SebClientConfig config,
            final CharSequence plainClientId,
            final CharSequence plainClientSecret) {

        final String plainTextConfig = String.format(
                SEB_CLIENT_CONFIG_EXAMPLE_XML,
                (StringUtils.isNotBlank(config.fallbackStartURL))
                        ? "true"
                        : "false",
                (StringUtils.isNotBlank(config.fallbackStartURL))
                        ? "    <key>startURL</key>\r\n" +
                                "    <string>" + config.fallbackStartURL + "</string>\r\n"
                        : "",
                this.webserviceInfo.getServerURL(),
                String.valueOf(config.institutionId),
                plainClientId,
                plainClientSecret,
                this.webserviceInfo.getDiscoveryEndpointAddress());

        if (log.isDebugEnabled()) {
            log.debug("SEB client configuration export:\n {}", plainTextConfig);
        }

        return plainTextConfig;
    }

    @Override
    public void flushClientConfigData(final BulkActionEvent event) {
        try {
            final BulkAction bulkAction = event.getBulkAction();

            if (bulkAction.type == BulkActionType.DEACTIVATE ||
                    bulkAction.type == BulkActionType.HARD_DELETE) {

                bulkAction.extractKeys(EntityType.SEB_CLIENT_CONFIGURATION)
                        .stream()
                        .forEach(this::flushClientConfigData);
            }

        } catch (final Exception e) {
            log.error("Unexpected error while trying to flush ClientConfig data ", e);
        }
    }

    private void flushClientConfigData(final EntityKey key) {
        try {
            final String clientName = this.sebClientConfigDAO.getSebClientCredentials(key.modelId)
                    .getOrThrow()
                    .clientIdAsString();

            final Collection<OAuth2AccessToken> tokensByClientId = this.tokenStore.findTokensByClientId(clientName);
            tokensByClientId.stream()
                    .forEach(token -> this.tokenStore.removeAccessToken(token));
        } catch (final Exception e) {
            log.error("Unexpected error while trying to flush ClientConfig data for {}", key, e);
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

        if (log.isDebugEnabled()) {
            log.debug("*** Finished Seb client configuration with password based encryption");
        }
    }

    /** Get a encoded clientSecret for the SebClientConfiguration with specified clientId/clientName.
     *
     * @param clientId the clientId/clientName
     * @return encoded clientSecret for that SebClientConfiguration with clientId or null of not existing */
    private Result<CharSequence> getEncodedClientConfigSecret(final String clientCongifId) {
        return this.sebClientConfigDAO.getConfigPasswortCipherByClientName(clientCongifId)
                .map(cipher -> this.clientPasswordEncoder.encode(this.clientCredentialService.decrypt(cipher)));
    }

}
