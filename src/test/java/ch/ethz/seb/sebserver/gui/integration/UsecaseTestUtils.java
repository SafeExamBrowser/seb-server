/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.integration;

import static org.junit.Assert.*;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import ch.ethz.seb.sebserver.gbl.util.Cryptor;
import org.apache.tomcat.util.buf.StringUtils;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationTableValues;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.RestServiceImpl;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetConfigAttributes;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.seb.examconfig.GetConfigurationTableValues;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl.init.XMLAttributeLoader;
import org.mockito.Mockito;

public abstract class UsecaseTestUtils {

    static ConfigurationTableValues testProhibitedProcessesInit(
            final String configId,
            final RestServiceImpl restService) {

        final ConfigurationTableValues tableValues = getTableValues("93", configId, restService);

        assertNotNull(tableValues);
        assertFalse(tableValues.values.isEmpty());
        final String names = StringUtils.join(
                tableValues.values
                        .stream()
                        .filter(attr -> attr.attributeId == 98)
                        .map(attr -> attr.value)
                        .sorted()
                        .collect(Collectors.toList()),
                Constants.LIST_SEPARATOR_CHAR);

        // get all configuration attributes
        final Map<String, ConfigurationAttribute> attributes = restService
                .getBuilder(GetConfigAttributes.class)
                .call()
                .getOrThrow()
                .stream()
                .collect(Collectors.toMap(attr -> attr.name, Function.identity()));

        final XMLAttributeLoader xmlAttributeLoader = new XMLAttributeLoader(Mockito.mock(Cryptor.class));
        final String configuraedNames = StringUtils.join(xmlAttributeLoader.loadFromXML(
                1L,
                Long.parseLong(configId),
                attrName -> attributes.get(attrName),
                "config/initialProhibitedProcesses.xml")
                .stream()
                .filter(attr -> attr.attributeId == 98)
                .map(attr -> attr.value)
                .sorted()
                .collect(Collectors.toList()),
                Constants.LIST_SEPARATOR_CHAR);

        assertEquals(configuraedNames, names);

        return tableValues;
    }

    static ConfigurationTableValues getTableValues(
            final String attributeId,
            final String configId,
            final RestServiceImpl restService) {
        final ConfigurationTableValues tableValues = restService.getBuilder(GetConfigurationTableValues.class)
                .withQueryParam(
                        Domain.CONFIGURATION_VALUE.ATTR_CONFIGURATION_ATTRIBUTE_ID,
                        attributeId)
                .withQueryParam(
                        Domain.CONFIGURATION_VALUE.ATTR_CONFIGURATION_ID,
                        configId)
                .call()
                .getOrThrow();
        return tableValues;
    }

    static ConfigurationTableValues testPermittedProcessesInit(
            final String configId,
            final RestServiceImpl restService) {

        final ConfigurationTableValues tableValues = getTableValues("73", configId, restService);

        assertNotNull(tableValues);
        assertFalse(tableValues.values.isEmpty());
        final String names = StringUtils.join(
                tableValues.values
                        .stream()
                        .filter(attr -> attr.attributeId == 76)
                        .map(attr -> attr.value)
                        .sorted()
                        .collect(Collectors.toList()),
                Constants.LIST_SEPARATOR_CHAR);

        // get all configuration attributes
        final Map<String, ConfigurationAttribute> attributes = restService
                .getBuilder(GetConfigAttributes.class)
                .call()
                .getOrThrow()
                .stream()
                .collect(Collectors.toMap(attr -> attr.name, Function.identity()));

        final XMLAttributeLoader xmlAttributeLoader = new XMLAttributeLoader(Mockito.mock(Cryptor.class));
        final String configuraedNames = StringUtils.join(xmlAttributeLoader.loadFromXML(
                1L,
                Long.parseLong(configId),
                attrName -> attributes.get(attrName),
                "config/initialPermittedProcesses.xml")
                .stream()
                .filter(attr -> attr.attributeId == 76)
                .map(attr -> attr.value)
                .sorted()
                .collect(Collectors.toList()),
                Constants.LIST_SEPARATOR_CHAR);

        assertEquals(configuraedNames, names);

        return tableValues;
    }

}
