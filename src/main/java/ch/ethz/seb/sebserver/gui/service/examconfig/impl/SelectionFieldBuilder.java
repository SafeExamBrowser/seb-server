/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.examconfig.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.util.Tuple;
import ch.ethz.seb.sebserver.gui.service.examconfig.ExamConfigurationService;

public abstract class SelectionFieldBuilder {

    protected List<Tuple<String>> getLocalizedResources(
            final ConfigurationAttribute attribute,
            final ViewContext viewContext) {

        return getLocalizedRes(attribute, viewContext, false);
    }

    protected List<Tuple<String>> getLocalizedResourcesAsToolTip(
            final ConfigurationAttribute attribute,
            final ViewContext viewContext) {

        return getLocalizedRes(attribute, viewContext, true);
    }

    private List<Tuple<String>> getLocalizedRes(
            final ConfigurationAttribute attribute,
            final ViewContext viewContext,
            final boolean toolTipResources) {

        if (attribute == null) {
            return Collections.emptyList();
        }

        final String prefix =
                (ConfigurationAttribute.hasDependency(
                        ConfigurationAttribute.DEPENDENCY_RESOURCE_LOC_TEXT_KEY,
                        attribute))
                                ? ConfigurationAttribute.getDependencyValue(
                                        ConfigurationAttribute.DEPENDENCY_RESOURCE_LOC_TEXT_KEY,
                                        attribute) + "."
                                : ExamConfigurationService.ATTRIBUTE_LABEL_LOC_TEXT_PREFIX + attribute.name + ".";

        return Arrays.stream(StringUtils.split(
                attribute.resources,
                Constants.LIST_SEPARATOR))
                .map(value -> {
                    final String key = prefix + value + ((toolTipResources)
                            ? ExamConfigurationService.TOOL_TIP_SUFFIX
                            : "");
                    final String text = viewContext.i18nSupport.getText(key, "");
                    return new Tuple<>(value, (StringUtils.isBlank(text))
                            ? (toolTipResources)
                                    ? text
                                    : value
                            : text);
                })
                .collect(Collectors.toList());
    }

}
