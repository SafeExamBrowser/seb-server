/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.examconfig;

import org.eclipse.swt.widgets.Composite;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Orientation;
import ch.ethz.seb.sebserver.gui.service.examconfig.impl.ViewContext;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;

public interface InputFieldBuilder {

    String RES_BUNDLE_KEY_PREFIX = "sebserver.examconfig.attribute.";

    boolean builderFor(
            ConfigurationAttribute attribute,
            Orientation orientation);

    InputField createInputField(
            final Composite parent,
            final ConfigurationAttribute attribute,
            final ViewContext viewContext);

    static LocTextKey createResourceBundleKey(final String paramName, final String value) {
        return new LocTextKey(RES_BUNDLE_KEY_PREFIX + paramName + "." + value);
    }

}
