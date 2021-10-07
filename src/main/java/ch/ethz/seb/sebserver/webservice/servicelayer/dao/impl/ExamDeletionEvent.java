/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao.impl;

import java.util.List;

import org.springframework.context.ApplicationEvent;

import ch.ethz.seb.sebserver.gbl.util.Utils;

public class ExamDeletionEvent extends ApplicationEvent {

    private static final long serialVersionUID = -8910368901893082365L;

    public final List<Long> ids;

    public ExamDeletionEvent(final List<Long> ids) {
        super(ids);
        this.ids = Utils.immutableListOf(ids);
    }

}
