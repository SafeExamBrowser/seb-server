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

import org.eclipse.swt.widgets.Composite;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.Configuration;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Orientation;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.View;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gui.service.examconfig.impl.AttributeMapping;
import ch.ethz.seb.sebserver.gui.service.examconfig.impl.ViewContext;
import ch.ethz.seb.sebserver.gui.service.page.PageContext;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory;

public interface ExamConfigurationService {

    public static final String ATTRIBUTE_LABEL_LOC_TEXT_PREFIX = "sebserver.examconfig.props.label.";
    public static final String GROUP_LABEL_LOC_TEXT_PREFIX = "sebserver.examconfig.props.group.";

    WidgetFactory getWidgetFactory();

    InputFieldBuilder getInputFieldBuilder(
            ConfigurationAttribute attribute,
            Orientation orientation);

    Result<AttributeMapping> getAttributes(Long templateId);

    List<View> getViews(AttributeMapping allAttributes);

    ViewContext createViewContext(
            PageContext pageContext,
            Configuration configuration,
            View view,
            AttributeMapping attributeMapping,
            int columns,
            int rows);

    Composite createViewGrid(
            Composite parent,
            ViewContext viewContext);

    void initInputFieldValues(
            Long configurationId,
            Collection<ViewContext> viewContexts);

}
