/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl.converter;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;

import ch.ethz.seb.sebserver.gbl.client.ClientCredentialService;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.AttributeType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ConfigurationAttributeDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.dao.ConfigurationValueDAO;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.AttributeValueConverter;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.AttributeValueConverterService;

public class TableConverterTest {

    // ********************************
    // **** table
    // ********************************
    private final ConfigurationAttribute TABLE_ATTR =
            new ConfigurationAttribute(1L, null, "table", AttributeType.TABLE, null, null, null, null);
    private final ConfigurationValue TABLE_VALUE =
            new ConfigurationValue(1L, 1L, 1L, 1L, 0, null);

    private final ConfigurationAttribute COLUMN_ATTR_1 =
            new ConfigurationAttribute(2L, 1L, "attr1", AttributeType.TEXT_FIELD, null, null, null, null);
    private final ConfigurationAttribute COLUMN_ATTR_2 =
            new ConfigurationAttribute(3L, 1L, "attr2", AttributeType.TEXT_FIELD, null, null, null, null);

    private final ConfigurationValue ROW_1_ATTR_1 =
            new ConfigurationValue(2L, 1L, 1L, 2L, 0, "1");
    private final ConfigurationValue ROW_1_ATTR_2 =
            new ConfigurationValue(3L, 1L, 1L, 3L, 0, "2");

    private final ConfigurationValue ROW_2_ATTR_1 =
            new ConfigurationValue(4L, 1L, 1L, 2L, 1, "3");
    private final ConfigurationValue ROW_2_ATTR_2 =
            new ConfigurationValue(5L, 1L, 1L, 3L, 1, "4");

    private final Collection<ConfigurationAttribute> TABLE_COLUMNS = Arrays.asList(
            this.COLUMN_ATTR_1,
            this.COLUMN_ATTR_2);

    private final List<List<ConfigurationValue>> TABLE_VALUES = Arrays.asList(
            Arrays.asList(this.ROW_1_ATTR_1, this.ROW_1_ATTR_2),
            Arrays.asList(this.ROW_2_ATTR_1, this.ROW_2_ATTR_2));

    // ********************************
    // **** Composite table
    // ********************************
    private final ConfigurationAttribute COMPOSITE_TABLE_ATTR =
            new ConfigurationAttribute(1L, null, "table", AttributeType.COMPOSITE_TABLE, null, null, null, null);
    private final ConfigurationValue COMPOSITE_TABLE_VALUE =
            new ConfigurationValue(1L, 1L, 1L, 1L, 0, null);

    private final ConfigurationAttribute COMPOSITE_COLUMN_ATTR_1 =
            new ConfigurationAttribute(2L, 1L, "attr1", AttributeType.TEXT_FIELD, null, null, null, null);
    private final ConfigurationAttribute COMPOSITE_COLUMN_ATTR_2 =
            new ConfigurationAttribute(3L, 1L, "attr2", AttributeType.TEXT_FIELD, null, null, null, null);

    private final ConfigurationValue COMPOSITE_ROW_1_ATTR_1 =
            new ConfigurationValue(2L, 1L, 1L, 2L, 0, "1");
    private final ConfigurationValue COMPOSITE_ROW_1_ATTR_2 =
            new ConfigurationValue(3L, 1L, 1L, 3L, 0, "2");

    private final Collection<ConfigurationAttribute> COMPOSITE_TABLE_ENTRIES = Arrays.asList(
            this.COMPOSITE_COLUMN_ATTR_1,
            this.COMPOSITE_COLUMN_ATTR_2);

    private final List<List<ConfigurationValue>> COMPOSITE_TABLE_VALUES = Arrays.asList(
            Arrays.asList(this.COMPOSITE_ROW_1_ATTR_1, this.COMPOSITE_ROW_1_ATTR_2));

    @Test
    public void testXMLNormalTable() throws Exception {

        final ConfigurationAttributeDAO configurationAttributeDAO =
                Mockito.mock(ConfigurationAttributeDAO.class);
        Mockito.when(configurationAttributeDAO.allMatching(Mockito.any()))
                .thenReturn(Result.of(this.TABLE_COLUMNS));

        final ConfigurationValueDAO configurationValueDAO =
                Mockito.mock(ConfigurationValueDAO.class);
        Mockito.when(configurationValueDAO.getOrderedTableValues(1L, 1L, 1L))
                .thenReturn(Result.of(this.TABLE_VALUES));

        final TableConverter tableConverter = new TableConverter(configurationAttributeDAO, configurationValueDAO);
        tableConverter.init(createAttributeValueConverterService());
        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        tableConverter.convertToXML(out, this.TABLE_ATTR, attr -> this.TABLE_VALUE);

        final String xmlString = new String(out.toByteArray());
        assertEquals(
                "<key>table</key>"
                        + "<array>"
                        + "<dict>"
                        + "<key>attr1</key>"
                        + "<string>1</string>"
                        + "<key>attr2</key>"
                        + "<string>2</string>"
                        + "</dict>"
                        + "<dict>"
                        + "<key>attr1</key>"
                        + "<string>3</string>"
                        + "<key>attr2</key>"
                        + "<string>4</string>"
                        + "</dict>"
                        + "</array>",
                xmlString);

    }

    @Test
    public void testXMLNormalTableNoValues() throws Exception {

        final ConfigurationAttributeDAO configurationAttributeDAO =
                Mockito.mock(ConfigurationAttributeDAO.class);
        Mockito.when(configurationAttributeDAO.allMatching(Mockito.any()))
                .thenReturn(Result.of(this.TABLE_COLUMNS));

        final ConfigurationValueDAO configurationValueDAO =
                Mockito.mock(ConfigurationValueDAO.class);
        Mockito.when(configurationValueDAO.getOrderedTableValues(1L, 1L, 1L))
                .thenReturn(Result.of(Collections.emptyList()));

        final TableConverter tableConverter = new TableConverter(configurationAttributeDAO, configurationValueDAO);
        tableConverter.init(createAttributeValueConverterService());
        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        tableConverter.convertToXML(out, this.TABLE_ATTR, attr -> this.TABLE_VALUE);

        final String xmlString = new String(out.toByteArray());
        assertEquals(
                "<key>table</key><array />",
                xmlString);

    }

    @Test
    public void testXMLCompositeTable() throws Exception {

        final ConfigurationAttributeDAO configurationAttributeDAO =
                Mockito.mock(ConfigurationAttributeDAO.class);
        Mockito.when(configurationAttributeDAO.allMatching(Mockito.any()))
                .thenReturn(Result.of(this.COMPOSITE_TABLE_ENTRIES));

        final ConfigurationValueDAO configurationValueDAO =
                Mockito.mock(ConfigurationValueDAO.class);
        Mockito.when(configurationValueDAO.getOrderedTableValues(1L, 1L, 1L))
                .thenReturn(Result.of(this.COMPOSITE_TABLE_VALUES));

        final TableConverter tableConverter = new TableConverter(configurationAttributeDAO, configurationValueDAO);
        tableConverter.init(createAttributeValueConverterService());
        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        tableConverter.convertToXML(out, this.COMPOSITE_TABLE_ATTR, attr -> this.COMPOSITE_TABLE_VALUE);

        final String xmlString = new String(out.toByteArray());
        assertEquals(
                "<key>table</key>"
                        + "<dict>"
                        + "<key>attr1</key>"
                        + "<string>1</string>"
                        + "<key>attr2</key>"
                        + "<string>2</string>"
                        + "</dict>",
                xmlString);

    }

    @Test
    public void testXMLCompositeTableEmpty() throws Exception {

        final ConfigurationAttributeDAO configurationAttributeDAO =
                Mockito.mock(ConfigurationAttributeDAO.class);
        Mockito.when(configurationAttributeDAO.allMatching(Mockito.any()))
                .thenReturn(Result.of(this.COMPOSITE_TABLE_ENTRIES));

        final ConfigurationValueDAO configurationValueDAO =
                Mockito.mock(ConfigurationValueDAO.class);
        Mockito.when(configurationValueDAO.getOrderedTableValues(1L, 1L, 1L))
                .thenReturn(Result.of(Collections.emptyList()));

        final TableConverter tableConverter = new TableConverter(configurationAttributeDAO, configurationValueDAO);
        tableConverter.init(createAttributeValueConverterService());
        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        tableConverter.convertToXML(out, this.COMPOSITE_TABLE_ATTR, attr -> this.COMPOSITE_TABLE_VALUE);

        final String xmlString = new String(out.toByteArray());
        assertEquals(
                "",
                xmlString);

    }

    @Test
    public void testJSONNormalTable() throws Exception {

        final ConfigurationAttributeDAO configurationAttributeDAO =
                Mockito.mock(ConfigurationAttributeDAO.class);
        Mockito.when(configurationAttributeDAO.allMatching(Mockito.any()))
                .thenReturn(Result.of(this.TABLE_COLUMNS));

        final ConfigurationValueDAO configurationValueDAO =
                Mockito.mock(ConfigurationValueDAO.class);
        Mockito.when(configurationValueDAO.getOrderedTableValues(1L, 1L, 1L))
                .thenReturn(Result.of(this.TABLE_VALUES));

        final TableConverter tableConverter = new TableConverter(configurationAttributeDAO, configurationValueDAO);
        tableConverter.init(createAttributeValueConverterService());
        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        tableConverter.convertToJSON(out, this.TABLE_ATTR, attr -> this.TABLE_VALUE);

        final String xmlString = new String(out.toByteArray());
        // expected : "table":[{"attr1":"1","attr2":"2"},{"attr1":"3","attr2":"4"}]
        assertEquals(
                "\"table\":[{\"attr1\":\"1\",\"attr2\":\"2\"},{\"attr1\":\"3\",\"attr2\":\"4\"}]",
                xmlString);

    }

    @Test
    public void testJSONNormalTableEmpty() throws Exception {

        final ConfigurationAttributeDAO configurationAttributeDAO =
                Mockito.mock(ConfigurationAttributeDAO.class);
        Mockito.when(configurationAttributeDAO.allMatching(Mockito.any()))
                .thenReturn(Result.of(this.TABLE_COLUMNS));

        final ConfigurationValueDAO configurationValueDAO =
                Mockito.mock(ConfigurationValueDAO.class);
        Mockito.when(configurationValueDAO.getOrderedTableValues(1L, 1L, 1L))
                .thenReturn(Result.of(Collections.emptyList()));

        final TableConverter tableConverter = new TableConverter(configurationAttributeDAO, configurationValueDAO);
        tableConverter.init(createAttributeValueConverterService());
        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        tableConverter.convertToJSON(out, this.TABLE_ATTR, attr -> this.TABLE_VALUE);

        final String xmlString = new String(out.toByteArray());
        // expected : "table":[]
        assertEquals(
                "\"table\":[]",
                xmlString);

    }

    @Test
    public void testJSONCompositeTable() throws Exception {

        final ConfigurationAttributeDAO configurationAttributeDAO =
                Mockito.mock(ConfigurationAttributeDAO.class);
        Mockito.when(configurationAttributeDAO.allMatching(Mockito.any()))
                .thenReturn(Result.of(this.COMPOSITE_TABLE_ENTRIES));

        final ConfigurationValueDAO configurationValueDAO =
                Mockito.mock(ConfigurationValueDAO.class);
        Mockito.when(configurationValueDAO.getOrderedTableValues(1L, 1L, 1L))
                .thenReturn(Result.of(this.COMPOSITE_TABLE_VALUES));

        final TableConverter tableConverter = new TableConverter(configurationAttributeDAO, configurationValueDAO);
        tableConverter.init(createAttributeValueConverterService());
        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        tableConverter.convertToJSON(out, this.COMPOSITE_TABLE_ATTR, attr -> this.COMPOSITE_TABLE_VALUE);

        final String xmlString = new String(out.toByteArray());
        // expected : "table":{"attr1":"1","attr2":"2"}
        assertEquals(
                "\"table\":{\"attr1\":\"1\",\"attr2\":\"2\"}",
                xmlString);

    }

    @Test
    public void testJSONCompositeTableEmpty() throws Exception {

        final ConfigurationAttributeDAO configurationAttributeDAO =
                Mockito.mock(ConfigurationAttributeDAO.class);
        Mockito.when(configurationAttributeDAO.allMatching(Mockito.any()))
                .thenReturn(Result.of(this.COMPOSITE_TABLE_ENTRIES));

        final ConfigurationValueDAO configurationValueDAO =
                Mockito.mock(ConfigurationValueDAO.class);
        Mockito.when(configurationValueDAO.getOrderedTableValues(1L, 1L, 1L))
                .thenReturn(Result.of(Collections.emptyList()));

        final TableConverter tableConverter = new TableConverter(configurationAttributeDAO, configurationValueDAO);
        tableConverter.init(createAttributeValueConverterService());
        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        tableConverter.convertToJSON(out, this.COMPOSITE_TABLE_ATTR, attr -> this.COMPOSITE_TABLE_VALUE);

        final String xmlString = new String(out.toByteArray());
        // expected :
        assertEquals(
                "",
                xmlString);

    }

    private AttributeValueConverterService createAttributeValueConverterService() {
        final ClientCredentialService clientCredentialServiceMock = Mockito.mock(ClientCredentialService.class);
        final List<AttributeValueConverter> converter = new ArrayList<>();
        converter.add(new StringConverter(clientCredentialServiceMock));
        return new AttributeValueConverterServiceImpl(converter);
    }

}
