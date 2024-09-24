/*
 *  Copyright (c) 2019 ETH Zürich, IT Services
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session;

import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import org.springframework.context.ApplicationEvent;

public class ExamArchivedEvent extends ApplicationEvent {
    
    public final Exam exam;

    public ExamArchivedEvent(final Exam exam) {
        super(exam);
        this.exam = exam;
    }
}
