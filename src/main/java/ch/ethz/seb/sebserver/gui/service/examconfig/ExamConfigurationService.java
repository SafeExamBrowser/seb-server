/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.examconfig;

import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.widgets.Composite;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.Configuration;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Orientation;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.TemplateAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.View;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gui.service.examconfig.impl.AttributeMapping;
import ch.ethz.seb.sebserver.gui.service.examconfig.impl.ViewContext;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.service.page.impl.PageAction;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

public interface ExamConfigurationService {

    public static final String ATTRIBUTE_LABEL_LOC_TEXT_PREFIX = "sebserver.examconfig.props.label.";
    public static final String GROUP_LABEL_LOC_TEXT_PREFIX = "sebserver.examconfig.props.group.";
    public static final String TOOL_TIP_SUFFIX = ".tooltip";
    public static final String TABLE_ROW_TITLE_SUFFIX = ".row.title";

    WidgetFactory getWidgetFactory();

    InputFieldBuilder getInputFieldBuilder(
            ConfigurationAttribute attribute,
            Orientation orientation);

    Result<AttributeMapping> getAttributes(Long templateId);

    Result<AttributeMapping> getAttributes(
            final TemplateAttribute attribute,
            final Orientation defaultOrientation);

    List<View> getViews(AttributeMapping allAttributes);

    ViewContext createViewContext(
            PageContext pageContext,
            Configuration configuration,
            View view,
            AttributeMapping attributeMapping,
            int rows,
            boolean readonly);

    Composite createViewGrid(
            Composite parent,
            ViewContext viewContext);

    void initInputFieldValues(
            Long configurationId,
            Collection<ViewContext> viewContexts);

    PageAction resetToDefaults(PageAction action);

    PageAction removeFromView(PageAction action);

    PageAction attachToDefaultView(final PageAction action);

    static String attributeNameKey(final ConfigurationAttribute attribute) {
        if (attribute == null) {
            return null;
        }

        return ATTRIBUTE_LABEL_LOC_TEXT_PREFIX + attribute.name;
    }

    static LocTextKey attributeNameLocKey(final ConfigurationAttribute attribute) {
        if (attribute == null) {
            return null;
        }

        return new LocTextKey(attributeNameKey(attribute));
    }

    static LocTextKey getToolTipKey(
            final ConfigurationAttribute attribute,
            final I18nSupport i18nSupport) {

        final String attributeNameKey = ExamConfigurationService.attributeNameKey(attribute) + TOOL_TIP_SUFFIX;
        if (StringUtils.isBlank(i18nSupport.getText(attributeNameKey, ""))) {
            return null;
        } else {
            return new LocTextKey(attributeNameKey);
        }
    }

    static LocTextKey getTablePopupTitleKey(
            final ConfigurationAttribute attribute,
            final I18nSupport i18nSupport) {

        return new LocTextKey(ATTRIBUTE_LABEL_LOC_TEXT_PREFIX + attribute.name + TABLE_ROW_TITLE_SUFFIX);
    }

}
