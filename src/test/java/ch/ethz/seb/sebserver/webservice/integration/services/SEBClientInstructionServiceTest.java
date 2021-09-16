/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.integration.services;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection;
import ch.ethz.seb.sebserver.gbl.model.session.ClientConnection.ConnectionStatus;
import ch.ethz.seb.sebserver.gbl.model.session.ClientInstruction.InstructionType;
import ch.ethz.seb.sebserver.webservice.datalayer.batis.model.ClientInstructionRecord;
import ch.ethz.seb.sebserver.webservice.integration.api.admin.AdministrationAPIIntegrationTester;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientConnectionDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ClientInstructionDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.session.SEBClientInstructionService;

@Sql(scripts = { "classpath:schema-test.sql", "classpath:data-test.sql", "classpath:data-test-additional.sql" })
public class SEBClientInstructionServiceTest extends AdministrationAPIIntegrationTester {

    @Autowired
    private SEBClientInstructionService sebClientInstructionService;
    @Autowired
    private ClientConnectionDAO clientConnectionDAO;
    @Autowired
    private ClientInstructionDAO clientInstructionDAO;

    @Before
    public void initSEBConnection() {
        final ClientConnection cc = this.clientConnectionDAO.byConnectionToken("testToken").getOr(null);
        if (cc == null) {
            this.clientConnectionDAO.createNew(new ClientConnection(
                    null, 1L, 2L, ConnectionStatus.ACTIVE, "testToken", "user1", "0.0.0.0", false, null, null))
                    .getOrThrow();
        }
    }

    @Test
    public void testRegister() {
        // check no instructions in DB
        Collection<ClientInstructionRecord> all = this.clientInstructionDAO
                .getAllActive()
                .getOrThrow();

        assertNotNull(all);
        assertTrue(all.isEmpty());

        // register instruction
        this.sebClientInstructionService.registerInstruction(
                2L, InstructionType.SEB_QUIT, Collections.emptyMap(), "testToken", false);

        // check on DB
        all = this.clientInstructionDAO
                .getAllActive()
                .getOrThrow();

        assertNotNull(all);
        assertFalse(all.isEmpty());
        final ClientInstructionRecord instrRec = all.iterator().next();
        assertEquals("testToken", instrRec.getConnectionToken());
        assertEquals(InstructionType.SEB_QUIT.name(), instrRec.getType());
        assertEquals(null, instrRec.getAttributes());

        // get instruction JSON
        final String json = this.sebClientInstructionService.getInstructionJSON("testToken");
        assertEquals("{\"instruction\":\"SEB_QUIT\"}", json);

        // check no instruction anymore
        final String secondPing = this.sebClientInstructionService.getInstructionJSON("testToken");
        assertNull(secondPing);

        // check DB is empty again
        all = this.clientInstructionDAO
                .getAllActive()
                .getOrThrow();
        assertNotNull(all);
        assertTrue(all.isEmpty());
    }

    @Test
    public void testRegisterWithConfirm() {
        // register instruction
        this.sebClientInstructionService.registerInstruction(
                2L, InstructionType.SEB_RECONFIGURE_SETTINGS, Collections.emptyMap(), "testToken", true);

        // check on DB
        Collection<ClientInstructionRecord> all = this.clientInstructionDAO
                .getAllActive()
                .getOrThrow();

        // get instruction JSON
        final String json = this.sebClientInstructionService.getInstructionJSON("testToken");
        assertEquals(
                "{\"instruction\":\"SEB_RECONFIGURE_SETTINGS\",\"attributes\":{\"instruction-confirm\":\"1\"}}",
                json);

        // check insturction is beeing resent until confirmed
        final String secondPing = this.sebClientInstructionService.getInstructionJSON("testToken");
        assertEquals(
                "{\"instruction\":\"SEB_RECONFIGURE_SETTINGS\",\"attributes\":{\"instruction-confirm\":\"1\"}}",
                secondPing);

        // confirm instruction
        this.sebClientInstructionService.confirmInstructionDone("testToken", "1");

        // check no instruction anymore
        final String nextPing = this.sebClientInstructionService.getInstructionJSON("testToken");
        assertNull(nextPing);

        // check DB is empty again
        all = this.clientInstructionDAO
                .getAllActive()
                .getOrThrow();
        assertNotNull(all);
        assertTrue(all.isEmpty());

    }

    @Test
    public void testRegisterWithConfirmAndAttributes() {
        // register instruction
        final Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("attr1", "123");
        attributes.put("attr2", "345");
        this.sebClientInstructionService.registerInstruction(
                2L, InstructionType.SEB_RECONFIGURE_SETTINGS, attributes, "testToken", true);

        // check on DB
        Collection<ClientInstructionRecord> all = this.clientInstructionDAO
                .getAllActive()
                .getOrThrow();

        // get instruction JSON
        final String json = this.sebClientInstructionService.getInstructionJSON("testToken");
        assertEquals(
                "{\"instruction\":\"SEB_RECONFIGURE_SETTINGS\",\"attributes\":{\"attr1\":\"123\",\"attr2\":\"345\",\"instruction-confirm\":\"1\"}}",
                json);

        // check insturction is beeing resent until confirmed
        final String secondPing = this.sebClientInstructionService.getInstructionJSON("testToken");
        assertEquals(
                "{\"instruction\":\"SEB_RECONFIGURE_SETTINGS\",\"attributes\":{\"attr1\":\"123\",\"attr2\":\"345\",\"instruction-confirm\":\"1\"}}",
                secondPing);

        // confirm instruction
        this.sebClientInstructionService.confirmInstructionDone("testToken", "1");

        // check no instruction anymore
        final String nextPing = this.sebClientInstructionService.getInstructionJSON("testToken");
        assertNull(nextPing);

        // check DB is empty again
        all = this.clientInstructionDAO
                .getAllActive()
                .getOrThrow();
        assertNotNull(all);
        assertTrue(all.isEmpty());

    }

    @Test
    public void testRegisterWithConfirmAndAttributes2() {
        // register instruction
        final Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("attr1", "123");
        attributes.put("attr2", "345");

        this.sebClientInstructionService.registerInstruction(
                2L, InstructionType.SEB_RECONFIGURE_SETTINGS, attributes,
                Stream.of("testToken").collect(Collectors.toSet()), true);

        // check on DB
        Collection<ClientInstructionRecord> all = this.clientInstructionDAO
                .getAllActive()
                .getOrThrow();

        // get instruction JSON
        final String json = this.sebClientInstructionService.getInstructionJSON("testToken");
        assertEquals(
                "{\"instruction\":\"SEB_RECONFIGURE_SETTINGS\",\"attributes\":{\"attr1\":\"123\",\"attr2\":\"345\",\"instruction-confirm\":\"1\"}}",
                json);

        // check insturction is beeing resent until confirmed
        final String secondPing = this.sebClientInstructionService.getInstructionJSON("testToken");
        assertEquals(
                "{\"instruction\":\"SEB_RECONFIGURE_SETTINGS\",\"attributes\":{\"attr1\":\"123\",\"attr2\":\"345\",\"instruction-confirm\":\"1\"}}",
                secondPing);

        // confirm instruction
        this.sebClientInstructionService.confirmInstructionDone("testToken", "1");

        // check no instruction anymore
        final String nextPing = this.sebClientInstructionService.getInstructionJSON("testToken");
        assertNull(nextPing);

        // check DB is empty again
        all = this.clientInstructionDAO
                .getAllActive()
                .getOrThrow();
        assertNotNull(all);
        assertTrue(all.isEmpty());

    }

}
