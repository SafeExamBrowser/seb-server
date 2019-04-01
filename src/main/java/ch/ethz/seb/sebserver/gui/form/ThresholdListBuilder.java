/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.form;

import java.util.Collection;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Label;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.Threshold;
import ch.ethz.seb.sebserver.gui.widget.ThresholdList;

public class ThresholdListBuilder extends FieldBuilder<Collection<Threshold>> {

    protected ThresholdListBuilder(
            final String name,
            final String label,
            final Collection<Threshold> value) {

        super(name, label, value);
    }

    @Override
    void build(final FormBuilder builder) {
        final Label lab = builder.labelLocalized(builder.formParent, this.label, this.spanLabel);
        if (builder.readonly || this.readonly) {
            // TODO do we need a read-only view for this?
            return;
        } else {
            final ThresholdList thresholdList = builder.widgetFactory.thresholdList(
                    builder.formParent,
                    this.value);

            final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false, this.spanInput, 1);
            thresholdList.setLayoutData(gridData);
            builder.form.putField(this.name, lab, thresholdList);
            builder.setFieldVisible(this.visible, this.name);
        }

    }

    public static final String thresholdsToFormURLEncodedStringValue(final Collection<Threshold> thresholds) {
        if (thresholds == null || thresholds.isEmpty()) {
            return null;
        }

        // thresholds={value}|{color},thresholds={value}|{color}...
        return StringUtils.join(thresholds.stream()
                .map(t -> Domain.THRESHOLD.REFERENCE_NAME
                        + Constants.FORM_URL_ENCODED_NAME_VALUE_SEPARATOR
                        + String.valueOf(t.getValue())
                        + Constants.EMBEDDED_LIST_SEPARATOR
                        + t.getColor())
                .collect(Collectors.toList()),
                Constants.LIST_SEPARATOR);
    }

}
