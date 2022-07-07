/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.integration.api.admin;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.EntityName;
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.AttributeType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.impl.SEBServerUser;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.impl.UserServiceImpl;
import ch.ethz.seb.sebserver.webservice.weblayer.api.ConfigurationAttributeController;

@Sql(scripts = { "classpath:schema-test.sql", "classpath:data-test.sql", "classpath:data-test-additional.sql" })
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ConfigurationAttributeAPITest extends AdministrationAPIIntegrationTester {

    @Autowired
    private ConfigurationAttributeController configurationAttributeController;
    @Autowired
    private UserServiceImpl userServiceImpl;
    @Mock
    private HttpServletRequest mockRequest;

    private final MultiValueMap<String, String> params = new LinkedMultiValueMap<>();

    @Before
    public void init() {
        this.userServiceImpl.setAuthenticationIfAbsent(new SEBServerUser(
                -1L,
                new UserInfo("user1", 1L, null, "admin", null, null, null, true, null, null,
                        EnumSet.allOf(UserRole.class).stream().map(r -> r.name()).collect(Collectors.toSet())),
                null));
        Mockito.when(this.mockRequest.getQueryString()).thenReturn("");
    }

    @Test
    @Order(1)
    public void test1_GetPage() {
        final Page<ConfigurationAttribute> page = this.configurationAttributeController.getPage(
                1L, 0, 100, null,
                new LinkedMultiValueMap<String, String>(),
                this.mockRequest);

        assertNotNull(page);
        assertFalse(page.content.isEmpty());
        assertEquals("100", String.valueOf(page.content.size()));
    }

    @Test
    @Order(2)
    public void test2_GetNames() {
        Collection<EntityName> names = this.configurationAttributeController.getNames(
                1L,
                new LinkedMultiValueMap<String, String>(),
                this.mockRequest);

        assertNotNull(names);
        assertFalse(names.isEmpty());
        assertEquals("241", String.valueOf(names.size()));

        this.params.clear();
        this.params.add(ConfigurationAttribute.FILTER_ATTR_TYPE, AttributeType.CHECKBOX.name());

        names = this.configurationAttributeController.getNames(
                1L,
                this.params,
                this.mockRequest);

        assertNotNull(names);
        assertFalse(names.isEmpty());
        assertEquals("139", String.valueOf(names.size()));
    }

    @Test
    @Order(3)
    public void test3_GetSingle() {
        final ConfigurationAttribute attr = this.configurationAttributeController.getBy("1");

        assertNotNull(attr);
        assertEquals("hashedAdminPassword", attr.name);
    }

    @Test
    @Order(4)
    public void test4_GetList() {
        List<ConfigurationAttribute> forIds = this.configurationAttributeController.getForIds("1,2");

        assertNotNull(forIds);
        assertEquals("2", String.valueOf(forIds.size()));

        forIds = this.configurationAttributeController.getForIds(null);

        assertNotNull(forIds);
        assertEquals("241", String.valueOf(forIds.size()));
    }

    @Test
    @Order(5)
    public void test5_CreateAndSaveAndDelete() {
        this.params.clear();
        this.params.add(Domain.CONFIGURATION_ATTRIBUTE.ATTR_PARENT_ID, null);
        this.params.add(Domain.CONFIGURATION_ATTRIBUTE.ATTR_NAME, "testAttribute");
        this.params.add(Domain.CONFIGURATION_ATTRIBUTE.ATTR_TYPE, AttributeType.CHECKBOX.name());
        this.params.add(Domain.CONFIGURATION_ATTRIBUTE.ATTR_RESOURCES, "");
        this.params.add(Domain.CONFIGURATION_ATTRIBUTE.ATTR_VALIDATOR, "");
        this.params.add(Domain.CONFIGURATION_ATTRIBUTE.ATTR_DEPENDENCIES, "");
        this.params.add(Domain.CONFIGURATION_ATTRIBUTE.ATTR_DEFAULT_VALUE, "true");

        final ConfigurationAttribute create = this.configurationAttributeController.create(
                this.params,
                1L,
                this.mockRequest);

        assertNotNull(create);
        assertNotNull(create.id);
        assertEquals("testAttribute", create.name);
        assertEquals("true", create.defaultValue);

        final ConfigurationAttribute savePut = this.configurationAttributeController.savePut(new ConfigurationAttribute(
                create.id,
                null, null, null, null, null, null,
                "false"));

        assertNotNull(savePut);
        assertNotNull(savePut.id);
        assertEquals("testAttribute", savePut.name);
        assertEquals("false", savePut.defaultValue);

    }

    @Test
    @Order(6)
    public void test6_NoDeletionSupport() {

        try {
            this.configurationAttributeController.hardDeleteAll(
                    Arrays.asList("1,2,3"),
                    false,
                    null,
                    1L);
            fail("Error expected here");
        } catch (final Exception e) {
            assertEquals(
                    "No bulk action support for: BulkAction [type=HARD_DELETE, sourceType=CONFIGURATION_ATTRIBUTE, sources=[]]",
                    e.getMessage());
        }

        try {
            this.configurationAttributeController.hardDelete(
                    "1",
                    false,
                    null);
            fail("Error expected here");
        } catch (final Exception e) {
            assertEquals(
                    "No bulk action support for: BulkAction [type=HARD_DELETE, sourceType=CONFIGURATION_ATTRIBUTE, sources=[EntityName [entityType=CONFIGURATION_ATTRIBUTE, modelId=1, name=hashedAdminPassword]]]",
                    e.getMessage());
        }

    }

}
