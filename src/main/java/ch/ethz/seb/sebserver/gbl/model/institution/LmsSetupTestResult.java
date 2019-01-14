/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.institution;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LmsSetupTestResult {

    public static final String ATTR_OK = "ok";

    @JsonProperty(ATTR_OK)
    @NotNull
    public final Boolean ok;

    // TODO

    public LmsSetupTestResult(
            @JsonProperty(value = ATTR_OK, required = true) final Boolean ok) {

        this.ok = ok;
    }

}
