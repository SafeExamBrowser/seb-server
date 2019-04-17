/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.CharBuffer;
import java.util.Collection;
import java.util.UUID;

import org.cryptonode.jncryptor.AES256JNCryptor;
import org.cryptonode.jncryptor.CryptorException;
import org.cryptonode.jncryptor.JNCryptor;
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
import ch.ethz.seb.sebserver.webservice.servicelayer.client.ClientCredentialService;
import ch.ethz.seb.sebserver.webservice.servicelayer.client.ClientCredentials;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.InstitutionDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.SebClientConfigDAO;

@Lazy
@Service
@WebServiceProfile
public class SebClientConfigServiceImpl implements SebClientConfigService {

    private static final Logger log = LoggerFactory.getLogger(SebClientConfigServiceImpl.class);

    private final InstitutionDAO institutionDAO;
    private final SebClientConfigDAO sebClientConfigDAO;
    private final ClientCredentialService clientCredentialService;
    private final String httpScheme;
    private final String serverAddress;
    private final String serverPort;
    private final String sebClientAPIEndpoint;

    private final JNCryptor cryptor = new AES256JNCryptor();

    protected SebClientConfigServiceImpl(
            final InstitutionDAO institutionDAO,
            final SebClientConfigDAO sebClientConfigDAO,
            final ClientCredentialService clientCredentialService,
            @Value("${sebserver.webservice.http.scheme}") final String httpScheme,
            @Value("${server.address}") final String serverAddress,
            @Value("${server.port}") final String serverPort,
            @Value("${sebserver.webservice.api.exam.endpoint}") final String sebClientAPIEndpoint) {

        this.institutionDAO = institutionDAO;
        this.sebClientConfigDAO = sebClientConfigDAO;
        this.clientCredentialService = clientCredentialService;
        this.httpScheme = httpScheme;
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.sebClientAPIEndpoint = sebClientAPIEndpoint;
    }

    @Override
    public boolean hasSebClientConfigurationForIntitution(final Long institutionId) {
        final Result<Collection<SebClientConfig>> all = this.sebClientConfigDAO.all(institutionId, true);
        return all != null && !all.hasError() && !all.getOrThrow().isEmpty();
    }

    @Override
    public Result<SebClientConfig> autoCreateSebClientConfigurationForIntitution(final Long institutionId) {
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

            try {

                final byte[] plainTextConfig = String.format(
                        SEB_CLIENT_CONFIG_EXAMPLE_XML,
                        serverURL,
                        String.valueOf(config.institutionId),
                        plainClientId,
                        plainClientSecret,
                        API.OAUTH_TOKEN_ENDPOINT,
                        this.sebClientAPIEndpoint + API.EXAM_API_HANDSHAKE_ENDPOINT,
                        this.sebClientAPIEndpoint + API.EXAM_API_CONFIGURATION_REQUEST_ENDPOINT,
                        this.sebClientAPIEndpoint + API.EXAM_API_PING_ENDPOINT,
                        this.sebClientAPIEndpoint + API.EXAM_API_EVENT_ENDPOINT)
                        .getBytes("UTF-8");

                if (encryptionPassword != null) {
                    final CharSequence encryptionPasswordPlaintext = this.clientCredentialService
                            .decrypt(encryptionPassword);

                    return new ByteArrayInputStream(encode(
                            plainTextConfig,
                            encryptionPasswordPlaintext));
                } else {
                    return new ByteArrayInputStream(plainTextConfig);
                }

            } catch (final UnsupportedEncodingException e) {
                throw new RuntimeException("cause: ", e);
            }

        });
    }

    private byte[] encode(final byte[] plainTextConfig, final CharSequence secret) {

        // TODO format the plainTextConfig for SEB Client encoding format

        try {
            // TODO do we need salt
            return this.cryptor.encryptData(plainTextConfig, CharBuffer.wrap(secret).array());
        } catch (final CryptorException e) {
            log.error("Unexpected error while trying to encrypt SEB Client configuration: ", e);
            return plainTextConfig;
        }
    }

}
