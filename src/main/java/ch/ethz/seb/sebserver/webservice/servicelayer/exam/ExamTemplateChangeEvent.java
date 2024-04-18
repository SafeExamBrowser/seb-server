/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.exam;

import ch.ethz.seb.sebserver.gbl.model.exam.ExamTemplate;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup;
import org.springframework.context.ApplicationEvent;

public class ExamTemplateChangeEvent extends ApplicationEvent {

    private static final long serialVersionUID = -7239994198026689531L;

    public ExamTemplateChangeEvent(final ExamTemplate source) {
        super(source);
    }

    public ExamTemplate getExamTemplate() {
        return (ExamTemplate) this.source;
    }

}
