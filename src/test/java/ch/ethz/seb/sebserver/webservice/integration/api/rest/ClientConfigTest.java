/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.integration.api.rest;

import static org.junit.Assert.*;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityProcessingReport;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.SEBClientConfig;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.SEBClientConfig.VDIType;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.integration.api.rest.api.RestServiceImpl;
import ch.ethz.seb.sebserver.webservice.integration.api.rest.api.seb.clientconfig.*;
import org.junit.Test;
import org.springframework.test.context.jdbc.Sql;

@Sql(scripts = { "classpath:schema-test.sql", "classpath:data-test.sql" })
public class ClientConfigTest extends GuiIntegrationTest {

    @Test
    public void testNewClientConfigWithQueryParam() {
        final RestServiceImpl restService = createRestServiceForUser("admin", "admin", new NewClientConfig());

        final Result<SEBClientConfig> call = restService.getBuilder(NewClientConfig.class)
                .withQueryParam(Domain.SEB_CLIENT_CONFIGURATION.ATTR_NAME, "new client config")
                .withFormParam("Test", "new client config")
                .withFormParam(SEBClientConfig.ATTR_CONFIG_PURPOSE, SEBClientConfig.ConfigPurpose.START_EXAM.name())
                .call();

        assertNotNull(call);
        assertFalse(call.hasError());
        final SEBClientConfig createdConfig = call.get();
        assertEquals(Long.valueOf(1), createdConfig.id);
        assertEquals("new client config", createdConfig.name);
        assertFalse(createdConfig.active);
    }

    @Test
    public void testNewClientConfigWithURLEncodedForm() {
        final RestServiceImpl restService = createRestServiceForUser("admin", "admin", new NewClientConfig());

        final Result<SEBClientConfig> call = restService.getBuilder(NewClientConfig.class)
                .withFormParam(Domain.SEB_CLIENT_CONFIGURATION.ATTR_NAME, "new client config")
                .withFormParam(SEBClientConfig.ATTR_CONFIG_PURPOSE, SEBClientConfig.ConfigPurpose.START_EXAM.name())
                .call();

        assertNotNull(call);
        assertFalse(call.hasError());
        final SEBClientConfig createdConfig = call.get();
        assertEquals(Long.valueOf(1), createdConfig.id);
        assertEquals("new client config", createdConfig.name);
        assertFalse(createdConfig.active);
    }

    @Test
    public void testCreate_Get_Activate_Save_Deactivate_ClientConfig() {
        final RestServiceImpl restService = createRestServiceForUser("admin", "admin",
                new NewClientConfig(),
                new GetClientConfig(),
                new ActivateClientConfig(),
                new SaveClientConfig(),
                new DeactivateClientConfig());

        // create one
        final SEBClientConfig config = restService.getBuilder(NewClientConfig.class)
                .withQueryParam(Domain.SEB_CLIENT_CONFIGURATION.ATTR_NAME, "new client config")
                .withFormParam(SEBClientConfig.ATTR_CONFIG_PURPOSE, SEBClientConfig.ConfigPurpose.START_EXAM.name())
                .call()
                .getOrThrow();

        // get
        final Result<SEBClientConfig> call = restService.getBuilder(GetClientConfig.class)
                .withURIVariable(API.PARAM_MODEL_ID, config.getModelId())
                .call();

        assertNotNull(call);
        assertFalse(call.hasError());
        final SEBClientConfig createdConfig = call.get();
        assertEquals(config.id, createdConfig.id);
        assertEquals("new client config", createdConfig.name);
        assertFalse(createdConfig.active);

        // activate
        final EntityProcessingReport activationReport = restService.getBuilder(ActivateClientConfig.class)
                .withURIVariable(API.PARAM_MODEL_ID, config.getModelId())
                .call()
                .getOrThrow();

        assertTrue(activationReport.errors.isEmpty());
        assertEquals(
                "EntityKey [modelId=1, entityType=SEB_CLIENT_CONFIGURATION]",
                activationReport.getSingleSource().toString());

        // save with password (no confirm) expecting validation error
        final Result<?> valError = restService.getBuilder(SaveClientConfig.class)
                .withBody(new SEBClientConfig(
                        config.id,
                        config.institutionId,
                        "new client config",
                        SEBClientConfig.ConfigPurpose.START_EXAM,
                        1000L,
                        VDIType.NO, null, null, null,
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
                        "password",
                        null,
                        "certAlias",
                        false,
                        null,
                        null,
                        null,
                        null))
                .call();

        assertTrue(valError.hasError());
        final Throwable error = valError.getError();
        assertTrue(error.getMessage().contains("confirm_encrypt_secret"));
        assertTrue(error.getMessage().contains("password.mismatch"));

        // save with new password
        final SEBClientConfig newConfig = restService.getBuilder(SaveClientConfig.class)
                .withBody(new SEBClientConfig(
                        config.id,
                        config.institutionId,
                        "new client config",
                        SEBClientConfig.ConfigPurpose.START_EXAM,
                        1000L,
                        VDIType.NO, null, null, null,
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
                        "password",
                        "password",
                        "certAlias",
                        false,
                        null,
                        null,
                        null,
                        null))
                .call()
                .getOrThrow();

        assertEquals(config.id, newConfig.id);
        assertEquals("new client config", newConfig.name);
        assertTrue(newConfig.active);
        assertNotNull(newConfig.getEncryptSecret());

        // deactivate
        final EntityProcessingReport deactivationReport = restService.getBuilder(DeactivateClientConfig.class)
                .withURIVariable(API.PARAM_MODEL_ID, config.getModelId())
                .call()
                .getOrThrow();

        assertTrue(deactivationReport.errors.isEmpty());
        assertEquals(
                "EntityKey [modelId=1, entityType=SEB_CLIENT_CONFIGURATION]",
                deactivationReport.getSingleSource().toString());
    }

}
