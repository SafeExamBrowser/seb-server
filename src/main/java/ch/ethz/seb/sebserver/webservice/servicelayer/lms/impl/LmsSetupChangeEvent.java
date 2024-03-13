/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms.impl;

import org.springframework.context.ApplicationEvent;

import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;

public class LmsSetupChangeEvent extends ApplicationEvent {

    private static final long serialVersionUID = -7239994198026689531L;

    public LmsSetupChangeEvent(final LmsSetup source) {
        super(source);
    }

    public LmsSetup getLmsSetup() {
        return (LmsSetup) this.source;
    }

}
