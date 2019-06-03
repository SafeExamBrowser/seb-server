/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.seb;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class RunningExam {

    @JsonProperty()
    public final String examId;
    @JsonProperty()
    public final String name;
    @JsonProperty()
    public final String url;

    public RunningExam(final String examId, final String name, final String url) {
        super();
        this.examId = examId;
        this.name = name;
        this.url = url;
    }

}
