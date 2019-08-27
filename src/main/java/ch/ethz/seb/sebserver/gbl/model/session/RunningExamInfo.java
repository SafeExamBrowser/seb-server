/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.session;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.model.institution.LmsSetup.LmsType;

public final class RunningExamInfo {

    @JsonProperty("examId")
    public final String examId;
    @JsonProperty("name")
    public final String name;
    @JsonProperty("url")
    public final String url;
    @JsonProperty("lmsType")
    public final String lmsType;

    @JsonCreator
    public RunningExamInfo(
            @JsonProperty("examId") final String examId,
            @JsonProperty("name") final String name,
            @JsonProperty("url") final String url,
            @JsonProperty("lmsType") final String lmsType) {

        this.examId = examId;
        this.name = name;
        this.url = url;
        this.lmsType = lmsType;
    }

    public RunningExamInfo(final Exam exam, final LmsType lmsType) {
        this.examId = exam.getModelId();
        this.name = exam.name;
        this.url = exam.startURL;
        this.lmsType = (lmsType == null) ? "" : lmsType.name();
    }

    public String getExamId() {
        return this.examId;
    }

    public String getName() {
        return this.name;
    }

    public String getUrl() {
        return this.url;
    }

    public String getLmsType() {
        return this.lmsType;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("RunningExam [examId=");
        builder.append(this.examId);
        builder.append(", name=");
        builder.append(this.name);
        builder.append(", url=");
        builder.append(this.url);
        builder.append(", lmsType=");
        builder.append(this.lmsType);
        builder.append("]");
        return builder.toString();
    }

}
