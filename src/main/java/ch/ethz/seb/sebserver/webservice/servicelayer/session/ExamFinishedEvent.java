/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session;

import org.springframework.context.ApplicationEvent;

import ch.ethz.seb.sebserver.gbl.model.exam.Exam;

/** This event is fired just after an exam has been finished */
public class ExamFinishedEvent extends ApplicationEvent {

    private static final long serialVersionUID = -1528880878532843063L;

    public final Exam exam;

    public ExamFinishedEvent(final Exam exam) {
        super(exam);
        this.exam = exam;
    }
}
