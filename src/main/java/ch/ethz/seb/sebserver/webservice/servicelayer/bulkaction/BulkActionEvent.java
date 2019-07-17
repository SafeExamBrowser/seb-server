/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction;

import org.springframework.context.ApplicationEvent;

public class BulkActionEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1102149193640832829L;

    public BulkActionEvent(final BulkAction source) {
        super(source);
    }

    public BulkAction getBulkAction() {
        return (BulkAction) this.source;
    }

}
