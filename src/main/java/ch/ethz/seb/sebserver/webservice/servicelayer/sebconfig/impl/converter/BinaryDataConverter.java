package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl.converter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Base64;
import java.util.Collections;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.AttributeType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.AttributeValueConverter;

@Lazy
@Component
public class BinaryDataConverter implements AttributeValueConverter {

    private static final String XML_TEMPLATE = "<key>%s</key><data>%s</data>";
    private static final String XML_TEMPLATE_EMPTY = "<key>%s</key><data />";

    private static final String JSON_TEMPLATE = "\"%s\":\"%s\"";
    private static final String JSON_TEMPLATE_EMPTY = "\"%s\":\"\"";

    @Override
    public Set<AttributeType> types() {
        return Collections.singleton(AttributeType.FILE_UPLOAD);
    }

    @Override
    public void convertToXML(
            final OutputStream out,
            final ConfigurationAttribute attribute,
            final Function<ConfigurationAttribute, ConfigurationValue> valueSupplier) throws IOException {

        final ConfigurationValue cValue = valueSupplier.apply(attribute);
        final String val = (cValue != null && cValue.value != null)
                ? cValue.value
                : attribute.getDefaultValue();

        convert(
                out,
                attribute,
                val,
                XML_TEMPLATE, XML_TEMPLATE_EMPTY);
    }

    @Override
    public void convertToJSON(
            final OutputStream out,
            final ConfigurationAttribute attribute,
            final Function<ConfigurationAttribute, ConfigurationValue> valueSupplier) throws IOException {

        final ConfigurationValue cValue = valueSupplier.apply(attribute);
        final String val = (cValue != null && cValue.value != null)
                ? cValue.value
                : attribute.getDefaultValue();

        convert(
                out,
                attribute,
                val,
                JSON_TEMPLATE, JSON_TEMPLATE_EMPTY);
    }

    private void convert(
            final OutputStream out,
            final ConfigurationAttribute attribute,
            final String value,
            final String template,
            final String emptyTemplate) throws IOException {

        final String realName = AttributeValueConverter.extractName(attribute);
        if (StringUtils.isNotBlank(value)) {
            out.write(Utils.toByteArray(String.format(
                    template,
                    realName,
                    normalize(value))));
        } else {
            out.write(Utils.toByteArray(String.format(
                    emptyTemplate,
                    realName)));
        }
    }

    // plist data may wrap Base64 lines. Decoding also rejects invalid data instead of exporting it.
    private String normalize(final String value) {
        return Base64.getEncoder().encodeToString(
                Base64.getDecoder().decode(StringUtils.deleteWhitespace(value)));
    }

}
