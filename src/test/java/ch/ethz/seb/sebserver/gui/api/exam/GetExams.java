/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.api.exam;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.exam.Exam;

import ch.ethz.seb.sebserver.gui.api.PageToListCallAdapter;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;

@Lazy
@Component

public class GetExams extends PageToListCallAdapter<Exam> {

    public GetExams() {
        super(
                GetExamPage.class,
                EntityType.EXAM,
                new TypeReference<List<Exam>>() {
                },
                API.EXAM_ADMINISTRATION_ENDPOINT);
    }

}
