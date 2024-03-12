/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.impl.converter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.AttributeType;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig.AttributeValueConverter;

@Lazy
@Component
@WebServiceProfile
public class KioskModeConverter implements AttributeValueConverter {

    public static final String ATTR_NAME_KIOSK_MODE = "kioskMode";
    public static final String ATTR_NAME_CREATE_NEW_DESKTOP = "createNewDesktop";
    public static final String ATTR_NAME_KILL_SHELL = "killExplorerShell";

    public static final Set<String> NAMES = Utils.immutableSetOf(
            ATTR_NAME_KIOSK_MODE,
            ATTR_NAME_CREATE_NEW_DESKTOP,
            ATTR_NAME_KILL_SHELL);

    private static final String XML_TEMPLATE = "<key>%s</key><%s />";
    private static final String JSON_TEMPLATE = "\"%s\":%s";

    @Override
    public Set<String> names() {
        return NAMES;
    }

    @Override
    public Set<AttributeType> types() {
        return Collections.emptySet();
    }

    @Override
    public Stream<ConfigurationAttribute> convertAttribute(final ConfigurationAttribute attr) {
        return Stream.of(
                convertFrom(attr, ATTR_NAME_CREATE_NEW_DESKTOP),
                convertFrom(attr, ATTR_NAME_KILL_SHELL));
    }

    private ConfigurationAttribute convertFrom(final ConfigurationAttribute attr, final String name) {
        return new ConfigurationAttribute(
                attr.id, attr.parentId, name,
                attr.type, attr.resources, attr.validator,
                attr.dependencies, attr.defaultValue);
    }

    @Override
    public void convertToXML(
            final OutputStream out,
            final ConfigurationAttribute attribute,
            final Function<ConfigurationAttribute, ConfigurationValue> valueSupplier) throws IOException {

        convert(out, valueSupplier.apply(attribute), attribute.name, XML_TEMPLATE);
    }

    @Override
    public void convertToJSON(
            final OutputStream out,
            final ConfigurationAttribute attribute,
            final Function<ConfigurationAttribute, ConfigurationValue> valueSupplier) throws IOException {

        convert(out, valueSupplier.apply(attribute), attribute.name, JSON_TEMPLATE);
    }

    private void convert(
            final OutputStream out,
            final ConfigurationValue value,
            final String name,
            final String template) throws IOException {

        final String val = (ATTR_NAME_CREATE_NEW_DESKTOP.equals(name))
                ? (value == null || value.getValue() == null)
                        ? Constants.TRUE_STRING
                        : "0".equals(value.getValue())
                                ? Constants.TRUE_STRING
                                : Constants.FALSE_STRING
                : (value == null || value.getValue() == null)
                        ? Constants.FALSE_STRING
                        : "1".equals(value.getValue())
                                ? Constants.TRUE_STRING
                                : Constants.FALSE_STRING;

        out.write(Utils.toByteArray(String.format(template, name, val)));
    }

}
