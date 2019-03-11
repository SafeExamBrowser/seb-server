/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.seb;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import ch.ethz.seb.sebserver.gbl.model.institution.Institution;
import ch.ethz.seb.sebserver.gbl.model.institution.SebClientConfig;
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

    private final InstitutionDAO institutionDAO;
    private final SebClientConfigDAO sebClientConfigDAO;
    private final ClientCredentialService clientCredentialService;
    private final String httpScheme;
    private final String serverAddress;
    private final String serverPort;
    private final String sebClientAPIEndpoint;

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
                    true);
        })
                .flatMap(this.sebClientConfigDAO::createNew);
    }

    @Override
    public Result<InputStream> exportSebClientConfigurationOfInstitution(final Long institutionId) {
        return this.sebClientConfigDAO.all(institutionId, true)
                .flatMap(l -> l.stream()
                        .sorted((sc1, sc2) -> sc1.date.compareTo(sc2.date))
                        .findFirst()
                        .or(() -> Optional.of(
                                autoCreateSebClientConfigurationForIntitution(institutionId).getOrThrow()))
                        .map(this::createExport)
                        .get());
    }

    private final static String SEB_CLIENT_CONFIG_EXAMPLE_XML =
            "<SEBClientConfig>"
                    + "<SEBServerConnection>"
                    + "<SEBServerAddress>%s</SEBServerAddress>"
                    + "<InstitutionIdentifier>%s</InstitutionIdentifier>"
                    + "<ClientName>%s</ClientName>"
                    + "<ClientSecret>%s</ClientSecret>"
                    + "</SEBServerConnection>"
                    + "</SEBClientConfig>";

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
                    .path(this.sebClientAPIEndpoint)
                    .toUriString();

            final ClientCredentials sebClientCredentials = this.sebClientConfigDAO
                    .getSebClientCredentials(config.getModelId())
                    .getOrThrow();

            final CharSequence plainClientId = this.clientCredentialService
                    .getPlainClientId(sebClientCredentials);
            final CharSequence plainClientSecret = this.clientCredentialService
                    .getPlainClientSecret(sebClientCredentials);

            try {
                return new ByteArrayInputStream(
                        String.format(
                                SEB_CLIENT_CONFIG_EXAMPLE_XML,
                                serverURL,
                                String.valueOf(config.institutionId),
                                plainClientId,
                                plainClientSecret)
                                .getBytes("UTF-8"));
            } catch (final UnsupportedEncodingException e) {
                throw new RuntimeException("cause: ", e);
            }

        });
    }

}
