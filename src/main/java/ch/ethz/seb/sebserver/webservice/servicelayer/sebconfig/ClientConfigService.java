/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig;

import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.EventListener;
import org.springframework.security.oauth2.provider.ClientDetails;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.SebClientConfig;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction.impl.BulkActionEvent;

public interface ClientConfigService {

    Logger log = LoggerFactory.getLogger(ClientConfigService.class);

    String EXAM_CLIENT_DETAILS_CACHE = "EXAM_CLIENT_DETAILS_CACHE";

    /** Indicates if there is any SebClientConfiguration for a specified institution.
     *
     * @param institutionId the institution identifier
     * @return true if there is any SebClientConfiguration for a specified institution. False otherwise */
    boolean hasSebClientConfigurationForInstitution(Long institutionId);

    /** Use this to auto-generate a SebClientConfiguration for a specified institution.
     * clientName and clientSecret are randomly generated.
     *
     * @param institutionId the institution identifier
     * @return the created SebClientConfig */
    Result<SebClientConfig> autoCreateSebClientConfigurationForInstitution(Long institutionId);

    /** Use this to export a specified SebClientConfiguration within a given OutputStream.
     * The SEB Client Configuration is exported in the defined SEB Configuration format
     * as described here: https://www.safeexambrowser.org/developer/seb-file-format.html
     *
     * @param out OutputStream to write the export to
     * @param modelId the model identifier of the SebClientConfiguration to export */
    void exportSebClientConfiguration(
            OutputStream out,
            final String modelId);

    /** Get the ClientDetails for given client name that identifies a SebClientConfig entry.
     *
     * @param clientName the client name of a SebClientConfig entry
     * @return Result refer to the ClientDetails for the specified clientName or to an error if happened */
    @Cacheable(
            cacheNames = EXAM_CLIENT_DETAILS_CACHE,
            key = "#clientName",
            unless = "#result.hasError()")
    Result<ClientDetails> getClientConfigDetails(String clientName);

    /** Internally used to check OAuth2 access for a active SebClientConfig.
     *
     * @param config the SebClientConfig to check access
     * @return true if the system was able to gain an access token for the client. False otherwise
     */
    boolean checkAccess(SebClientConfig config);
}
