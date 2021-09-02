/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.remote.webservice.api.exam;

import java.util.List;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.exam.ExamTemplate;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.api.PageToListCallAdapter;

@Lazy
@Component
@GuiProfile
public class GetExamTemplates extends PageToListCallAdapter<ExamTemplate> {

    public GetExamTemplates() {
        super(
                GetExamTemplatePage.class,
                EntityType.EXAM_TEMPLATE,
                new TypeReference<List<ExamTemplate>>() {
                },
                API.EXAM_TEMPLATE_ENDPOINT);
    }

}
