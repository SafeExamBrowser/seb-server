/*
 * Copyright (c) 2023 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl;

import org.springframework.context.ApplicationEvent;

public class ExamUpdateEvent extends ApplicationEvent {

    private static final long serialVersionUID = -367779249954953708L;

    public final Long examId;

    public ExamUpdateEvent(final Long examId) {
        super(examId);
        this.examId = examId;
    }

}
