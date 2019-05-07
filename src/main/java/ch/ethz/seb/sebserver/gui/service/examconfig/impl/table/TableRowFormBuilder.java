/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.examconfig.impl.table;

import java.util.Map;
import java.util.function.Supplier;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationTableValue.TableValue;
import ch.ethz.seb.sebserver.gui.service.page.ModalInputDialogComposer;

public class TableRowFormBuilder implements ModalInputDialogComposer<Map<Long, TableValue>> {

    private final Map<Long, TableValue> rowValues;

    public TableRowFormBuilder(final Map<Long, TableValue> rowValues) {
        this.rowValues = rowValues;
    }

    @Override
    public Supplier<Map<Long, TableValue>> compose(final Composite parent) {
        final Label test = new Label(parent, SWT.NONE);
        test.setText("TEST");
        return () -> null;
    }

}
