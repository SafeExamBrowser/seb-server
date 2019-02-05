/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.table;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.springframework.util.MultiValueMap;

import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.FilterAttributeSupplier;

public class TableFilter<ROW extends Entity> extends Composite implements FilterAttributeSupplier {

    private static final long serialVersionUID = -2460403977147440766L;

    TableFilter(final EntityTable<ROW> parent) {
        super(parent, SWT.NONE);
    }

    @Override
    public MultiValueMap<String, String> getAttributes() {
        // TODO Auto-generated method stub
        return null;
    }
}
