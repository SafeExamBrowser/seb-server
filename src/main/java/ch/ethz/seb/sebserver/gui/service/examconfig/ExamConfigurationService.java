/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.examconfig;

import org.eclipse.swt.widgets.Composite;

import ch.ethz.seb.sebserver.gui.service.examconfig.impl.AttributeMapping;
import ch.ethz.seb.sebserver.gui.service.examconfig.impl.ViewContext;

public interface ExamConfigurationService {

    AttributeMapping getAttributes(String template);

    default ViewContext createViewContext(
            final String template,
            final Composite parent,
            final String name,
            final String configurationId,
            final int columns,
            final int rows) {

        return createViewContext(
                getAttributes(template),
                parent,
                name,
                configurationId,
                columns,
                rows);
    }

    ViewContext createViewContext(
            final AttributeMapping attributeMapping,
            final Composite parent,
            final String name,
            final String configurationId,
            final int columns,
            final int rows);

    ViewContext initInputFieldValues(final ViewContext viewContext);

}
