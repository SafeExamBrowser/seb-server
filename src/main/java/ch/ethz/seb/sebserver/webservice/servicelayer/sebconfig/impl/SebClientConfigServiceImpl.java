/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.model.institution.Institution;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.SebClientConfig;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.client.ClientCredentialService;
import ch.ethz.seb.sebserver.webservice.servicelayer.client.ClientCredentials;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.InstitutionDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.SebClientConfigDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SebClientConfigService;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SebConfigEncryptionService;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.SebConfigEncryptionService.Strategy;

@Lazy
@Service
@WebServiceProfile
public class SebClientConfigServiceImpl implements SebClientConfigService {

    private static final Logger log = LoggerFactory.getLogger(SebClientConfigServiceImpl.class);

    private final InstitutionDAO institutionDAO;
    private final SebClientConfigDAO sebClientConfigDAO;
    private final ClientCredentialService clientCredentialService;
    private final SebConfigEncryptionService sebConfigEncryptionService;
    private final String httpScheme;
    private final String serverAddress;
    private final String serverPort;
    private final String sebClientAPIEndpoint;

    protected SebClientConfigServiceImpl(
            final InstitutionDAO institutionDAO,
            final SebClientConfigDAO sebClientConfigDAO,
            final ClientCredentialService clientCredentialService,
            final SebConfigEncryptionService sebConfigEncryptionService,
            @Value("${sebserver.webservice.http.scheme}") final String httpScheme,
            @Value("${server.address}") final String serverAddress,
            @Value("${server.port}") final String serverPort,
            @Value("${sebserver.webservice.api.exam.endpoint}") final String sebClientAPIEndpoint) {

        this.institutionDAO = institutionDAO;
        this.sebClientConfigDAO = sebClientConfigDAO;
        this.clientCredentialService = clientCredentialService;
        this.sebConfigEncryptionService = sebConfigEncryptionService;
        this.httpScheme = httpScheme;
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.sebClientAPIEndpoint = sebClientAPIEndpoint;
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
                    true);
        })
                .flatMap(this.sebClientConfigDAO::createNew);
    }

    @Override
    public Result<InputStream> exportSebClientConfiguration(final String modelId) {
        return this.sebClientConfigDAO.byModelId(modelId)
                .flatMap(this::createExport);
    }

    private final Result<InputStream> createExport(final SebClientConfig config) {
        // TODO implementation of creation of SEB client configuration for specified Institution
        // A SEB start configuration should at least contain the SEB-Client-Credentials to access the SEB Server API
        // and the SEB Server URL
        //
        // To Clarify : The format of a SEB start configuration
        // To Clarify : How the file should be encrypted (use case) maybe we need another encryption-secret for this that can be given by
        //              an administrator on SEB start configuration creation time

        return Result.tryCatch(() -> {

            final String serverURL = UriComponentsBuilder.newInstance()
                    .scheme(this.httpScheme)
                    .host(this.serverAddress)
                    .port(this.serverPort)
                    .toUriString();

            final ClientCredentials sebClientCredentials = this.sebClientConfigDAO
                    .getSebClientCredentials(config.getModelId())
                    .getOrThrow();

            final CharSequence encryptionPassword = this.sebClientConfigDAO
                    .getConfigPasswortCipher(config.getModelId())
                    .getOrThrow();

            final CharSequence plainClientId = this.clientCredentialService
                    .getPlainClientId(sebClientCredentials);
            final CharSequence plainClientSecret = this.clientCredentialService
                    .getPlainClientSecret(sebClientCredentials);

            final String plainTextConfig = String.format(
                    SEB_CLIENT_CONFIG_EXAMPLE_XML,
                    serverURL,
                    String.valueOf(config.institutionId),
                    plainClientId,
                    plainClientSecret,
                    API.OAUTH_TOKEN_ENDPOINT,
                    this.sebClientAPIEndpoint + API.EXAM_API_HANDSHAKE_ENDPOINT,
                    this.sebClientAPIEndpoint + API.EXAM_API_CONFIGURATION_REQUEST_ENDPOINT,
                    this.sebClientAPIEndpoint + API.EXAM_API_PING_ENDPOINT,
                    this.sebClientAPIEndpoint + API.EXAM_API_EVENT_ENDPOINT);

            if (encryptionPassword != null) {

                log.debug("Try to encrypt seb client configuration with password based encryption");

                final CharSequence encryptionPasswordPlaintext = this.clientCredentialService
                        .decrypt(encryptionPassword);

                final ByteBuffer encryptedConfig = this.sebConfigEncryptionService.encryptWithPassword(
                        plainTextConfig,
                        Strategy.PASSWORD_PWCC,
                        encryptionPasswordPlaintext)
                        .getOrThrow();

                return new ByteArrayInputStream(encryptedConfig.array());
            } else {

                log.debug("Serve plain text seb configuration with specified header");

                final ByteBuffer encryptedConfig = this.sebConfigEncryptionService.plainText(plainTextConfig)
                        .getOrThrow();

                return new ByteArrayInputStream(Utils.toByteArray(encryptedConfig));
            }
        });
    }

}
