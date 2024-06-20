/*
 *  Copyright (c) 2019 ETH Zürich, IT Services
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session;

import org.springframework.context.ApplicationEvent;

public class ExamConfigUpdateEvent extends ApplicationEvent {

    public final Long examId;
    public ExamConfigUpdateEvent(final Long examId) {
        super(examId);
        this.examId = examId;
    }
}
