package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl.converter;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;

import org.junit.Test;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.AttributeType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;

public class BinaryDataConverterTest {

    private final ConfigurationAttribute attribute = new ConfigurationAttribute(
            1L,
            null,
            "attribute",
            AttributeType.FILE_UPLOAD,
            null, null, null, "AQI=");

    @Test
    public void testXML() throws Exception {
        final BinaryDataConverter binaryDataConverter = new BinaryDataConverter();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        binaryDataConverter.convertToXML(
                out,
                this.attribute,
                attr -> new ConfigurationValue(1l, 1l, 1l, 1l, 0, "AAEC/w=="));

        final String xmlString = new String(out.toByteArray());
        assertEquals(
                "<key>attribute</key><data>AAEC/w==</data>",
                xmlString);
    }

    @Test
    public void testWrappedXML() throws Exception {
        final BinaryDataConverter binaryDataConverter = new BinaryDataConverter();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        binaryDataConverter.convertToXML(
                out,
                this.attribute,
                attr -> new ConfigurationValue(1l, 1l, 1l, 1l, 0, "\n\tAAEC\n\t/w==\n\t"));

        final String xmlString = new String(out.toByteArray());
        assertEquals(
                "<key>attribute</key><data>AAEC/w==</data>",
                xmlString);
    }

    @Test
    public void testEmptyXML() throws Exception {
        final BinaryDataConverter binaryDataConverter = new BinaryDataConverter();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        binaryDataConverter.convertToXML(
                out,
                this.attribute,
                attr -> new ConfigurationValue(1l, 1l, 1l, 1l, 0, ""));

        final String xmlString = new String(out.toByteArray());
        assertEquals(
                "<key>attribute</key><data />",
                xmlString);
    }

    @Test
    public void testNullXML() throws Exception {
        final BinaryDataConverter binaryDataConverter = new BinaryDataConverter();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        binaryDataConverter.convertToXML(
                out,
                this.attribute,
                attr -> new ConfigurationValue(1l, 1l, 1l, 1l, 0, null));

        final String xmlString = new String(out.toByteArray());
        assertEquals(
                "<key>attribute</key><data>AQI=</data>",
                xmlString);
    }

    @Test
    public void testJSON() throws Exception {
        final BinaryDataConverter binaryDataConverter = new BinaryDataConverter();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        binaryDataConverter.convertToJSON(
                out,
                this.attribute,
                attr -> new ConfigurationValue(1l, 1l, 1l, 1l, 0, "\n\tAAEC\n\t/w==\n\t"));

        final String jsonString = new String(out.toByteArray());
        assertEquals(
                "\"attribute\":\"AAEC/w==\"",
                jsonString);
    }

    @Test
    public void testEmptyJSON() throws Exception {
        final BinaryDataConverter binaryDataConverter = new BinaryDataConverter();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        binaryDataConverter.convertToJSON(
                out,
                this.attribute,
                attr -> new ConfigurationValue(1l, 1l, 1l, 1l, 0, ""));

        final String jsonString = new String(out.toByteArray());
        assertEquals(
                "\"attribute\":\"\"",
                jsonString);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidData() throws Exception {
        final BinaryDataConverter binaryDataConverter = new BinaryDataConverter();

        binaryDataConverter.convertToXML(
                new ByteArrayOutputStream(),
                this.attribute,
                attr -> new ConfigurationValue(1l, 1l, 1l, 1l, 0, "AA!C"));
    }

}
