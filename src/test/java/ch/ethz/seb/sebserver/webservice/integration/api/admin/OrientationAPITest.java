/*
 * Copyright (c) 2022 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.integration.api.admin;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.EnumSet;
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
import ch.ethz.seb.sebserver.gbl.model.Page;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Orientation;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.TitleOrientation;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.model.user.UserRole;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.impl.SEBServerUser;
import ch.ethz.seb.sebserver.webservice.servicelayer.authorization.impl.UserServiceImpl;
import ch.ethz.seb.sebserver.webservice.weblayer.api.OrientationController;

@Sql(scripts = { "classpath:schema-test.sql", "classpath:data-test.sql", "classpath:data-test-additional.sql" })
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class OrientationAPITest extends AdministrationAPIIntegrationTester {

    @Autowired
    private OrientationController orientationController;
    @Autowired
    private UserServiceImpl userServiceImpl;
    @Mock
    private HttpServletRequest mockRequest;

    private final MultiValueMap<String, String> params = new LinkedMultiValueMap<>();

    @Before
    public void init() {
        this.userServiceImpl.setAuthenticationIfAbsent(new SEBServerUser(
                -1L,
                new UserInfo("user1", 1L, null, "admin", null, null, null, true, true, true, null, null,
                        EnumSet.allOf(UserRole.class).stream().map(r -> r.name()).collect(Collectors.toSet()),
                        Collections.emptyList(),
                        Collections.emptyList()),
                null));
        Mockito.when(this.mockRequest.getQueryString()).thenReturn("");
    }

    @Test
    @Order(1)
    public void test1_GetPage() {
        final Page<Orientation> page = this.orientationController.getPage(
                1L, 0, 100, null,
                new LinkedMultiValueMap<String, String>(),
                this.mockRequest);

        assertNotNull(page);
        assertFalse(page.content.isEmpty());
        assertEquals("100", String.valueOf(page.content.size()));
    }

    @Test
    @Order(5)
    public void test5_CreateAndSaveAndDelete() {
        this.params.clear();
        this.params.add(Domain.ORIENTATION.ATTR_CONFIG_ATTRIBUTE_ID, "1");
        this.params.add(Domain.ORIENTATION.ATTR_GROUP_ID, "testAttribute");
        this.params.add(Domain.ORIENTATION.ATTR_HEIGHT, "1");
        this.params.add(Domain.ORIENTATION.ATTR_TEMPLATE_ID, "0");
        this.params.add(Domain.ORIENTATION.ATTR_TITLE, "LEFT");
        this.params.add(Domain.ORIENTATION.ATTR_VIEW_ID, "1");
        this.params.add(Domain.ORIENTATION.ATTR_WIDTH, "1");
        this.params.add(Domain.ORIENTATION.ATTR_X_POSITION, "1");
        this.params.add(Domain.ORIENTATION.ATTR_Y_POSITION, "1");
        this.params.add(Domain.ORIENTATION.TYPE_NAME, "testAttribute");

        final Orientation create = this.orientationController.create(
                this.params,
                1L,
                this.mockRequest);

        assertNotNull(create);
        assertNotNull(create.id);
        assertEquals("testAttribute", create.groupId);
        assertEquals(1, create.height);
        assertEquals(1, create.width);
        assertEquals(1, create.xPosition);
        assertEquals(1, create.yPosition);
        assertEquals(TitleOrientation.LEFT, create.title);

        final Orientation savePut = this.orientationController.savePut(new Orientation(
                create.id,
                null, null, null, null, null, null, null, null,
                TitleOrientation.RIGHT));

        assertNotNull(savePut);
        assertNotNull(savePut.id);
        assertEquals("testAttribute", savePut.groupId);
        assertEquals(TitleOrientation.RIGHT, savePut.title);

    }

}
