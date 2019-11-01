/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.lms;

import java.util.Collection;
import java.util.Map;

import ch.ethz.seb.sebserver.gbl.model.exam.Exam;
import ch.ethz.seb.sebserver.gbl.util.Utils;

public final class SebRestrictionData {

    public final Exam exam;

    public final Collection<String> configKeys;

    public final Collection<String> browserExamKeys;

    public final Map<String, String> additionalAttributes;

    public SebRestrictionData(
            final Exam exam,
            final Collection<String> configKeys,
            final Collection<String> browserExamKeys,
            final Map<String, String> additionalAttributes) {

        this.exam = exam;
        this.configKeys = Utils.immutableCollectionOf(configKeys);
        this.browserExamKeys = Utils.immutableCollectionOf(browserExamKeys);
        this.additionalAttributes = Utils.immutableMapOf(additionalAttributes);
    }

    public Exam getExam() {
        return this.exam;
    }

    public Collection<String> getConfigKeys() {
        return this.configKeys;
    }

    public Collection<String> getBrowserExamKeys() {
        return this.browserExamKeys;
    }

    public Map<String, String> getAdditionalAttributes() {
        return this.additionalAttributes;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("SebRestrictionData [exam=");
        builder.append(this.exam);
        builder.append(", configKeys=");
        builder.append(this.configKeys);
        builder.append(", browserExamKeys=");
        builder.append(this.browserExamKeys);
        builder.append(", additionalAttributes=");
        builder.append(this.additionalAttributes);
        builder.append("]");
        return builder.toString();
    }
}
