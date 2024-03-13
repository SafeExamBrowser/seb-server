/*
 * Copyright (c) 2022 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session;

import org.springframework.context.ApplicationEvent;

import ch.ethz.seb.sebserver.gbl.model.exam.Exam;

public class ExamResetEvent extends ApplicationEvent {

    private static final long serialVersionUID = -2854284031889020212L;

    public final Exam exam;

    public ExamResetEvent(final Exam exam) {
        super(exam);
        this.exam = exam;
    }

}
