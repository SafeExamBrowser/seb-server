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

/** Service offers functionality to compose and update SEB exam configuration properties page */
public interface ExamConfigurationService {

    String ATTRIBUTE_LABEL_LOC_TEXT_PREFIX = "sebserver.examconfig.props.label.";
    String GROUP_LABEL_LOC_TEXT_PREFIX = "sebserver.examconfig.props.group.";
    String TOOL_TIP_SUFFIX = ".tooltip";
    String TABLE_ROW_TITLE_SUFFIX = ".row.title";

    /** Get the WidgetFactory service
     *
     * @return the WidgetFactory service */
    WidgetFactory getWidgetFactory();

    InputFieldBuilder getInputFieldBuilder(
            ConfigurationAttribute attribute,
            Orientation orientation);

    /** Get the attribute mapping of a specified template.
     *
     * @param templateId The template identifier
     * @return Result refer to the attribute mapping or to an error if happened. */
    Result<AttributeMapping> getAttributes(Long templateId);

    /** Get the attribute mapping for a specific template attribute with default orientation
     * for fallback.
     *
     * @param attribute The template attribute instance.
     * @param defaultOrientation the default orientation.
     * @return Result refer to the attribute mapping or to an error if happened. */
    Result<AttributeMapping> getAttributes(
            final TemplateAttribute attribute,
            final Orientation defaultOrientation);

    /** Get the list of defined views for a AttributeMapping.
     *
     * @param allAttributes AttributeMapping with all attributes
     * @return list of defined views for a AttributeMapping */
    List<View> getViews(AttributeMapping allAttributes);

    /** Create to ViewContext to compose a exam configuration property page,
     * The ViewContext is the internal state of a exam configuration property page
     * and is passed through all composers while composing the page.
     * 
     * @param pageContext The original PageContext that holds the state of the overall page.
     * @param configuration The configuration on which the exam configuration property page is based on.
     * @param view The View of the context
     * @param attributeMapping The attribute mapping if the properties page
     * @param rows Number of rows supported for the view.
     * @param readonly Indicates if the view shall be composed in read-only mode.
     * @return ViewContext instance. */
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
