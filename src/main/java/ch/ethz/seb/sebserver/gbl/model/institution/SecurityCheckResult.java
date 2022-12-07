/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.institution;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SecurityCheckResult {

    public static final SecurityCheckResult NO_GRANT = new SecurityCheckResult(false, false, false);

    public final boolean globalGranted;
    public final boolean examGranted;
    public final boolean numericallyGranted;

    @JsonCreator
    public SecurityCheckResult(
            final boolean globalGranted,
            final boolean examGranted,
            final boolean numericallyGranted) {

        this.globalGranted = globalGranted;
        this.examGranted = examGranted;
        this.numericallyGranted = numericallyGranted;
    }

    public boolean isGlobalGranted() {
        return this.globalGranted;
    }

    public boolean isExamGranted() {
        return this.examGranted;
    }

    public boolean isNumericallyGranted() {
        return this.numericallyGranted;
    }

    public boolean hasAnyGrant() {
        return this.globalGranted | this.examGranted | this.numericallyGranted;
    }

}
