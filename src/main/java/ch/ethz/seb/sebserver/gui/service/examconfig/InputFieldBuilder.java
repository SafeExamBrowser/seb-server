/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.examconfig;

import org.apache.commons.lang3.BooleanUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.Orientation;
import ch.ethz.seb.sebserver.gui.service.examconfig.impl.InputFieldBuilderSupplier;
import ch.ethz.seb.sebserver.gui.service.examconfig.impl.ViewContext;

public interface InputFieldBuilder {

    /** Called by the InputFieldBuilderSupplier bean instance on initialization to avoid
     * circular dependencies.
     *
     * This method must not be called from other then InputFieldBuilderSupplier
     * For default this does nothing and a InputFieldBuilder that uses a reference to
     * the calling InputFieldBuilderSupplier must override this to get the reference
     *
     * @param inputFieldBuilderSupplier reference of InputFieldBuilderSupplier */
    default void init(final InputFieldBuilderSupplier inputFieldBuilderSupplier) {
        // NOOP for default
    }

    boolean builderFor(
            ConfigurationAttribute attribute,
            Orientation orientation);

    InputField createInputField(
            final Composite parent,
            final ConfigurationAttribute attribute,
            final ViewContext viewContext);

    static Composite createInnerGrid(
            final Composite parent,
            final ConfigurationAttribute attribute,
            final Orientation orientation) {

        return createInnerGrid(parent, attribute, orientation, 1);
    }

    static Composite createInnerGrid(
            final Composite parent,
            final ConfigurationAttribute attribute,
            final Orientation orientation,
            final int numColumns) {

        final Composite comp = new Composite(parent, SWT.NONE);
        final GridLayout gridLayout = new GridLayout(numColumns, true);
        gridLayout.verticalSpacing = 0;
        gridLayout.marginHeight = 1;
        gridLayout.marginWidth = 0;
        gridLayout.marginRight = 5;
        comp.setLayout(gridLayout);

        final GridData gridData = new GridData(
                SWT.FILL, SWT.FILL,
                true, false,
                (orientation != null && isOnView(attribute)) ? orientation.width() : 1,
                (orientation != null && isOnView(attribute)) ? orientation.height() : 1);
        comp.setLayoutData(gridData);
        return comp;
    }

    static boolean isOnView(final ConfigurationAttribute attribute) {
        return attribute.parentId == null || BooleanUtils.toBoolean(ConfigurationAttribute.getDependencyValue(
                ConfigurationAttribute.DEPENDENCY_SHOW_IN_VIEW,
                attribute));
    }

}
