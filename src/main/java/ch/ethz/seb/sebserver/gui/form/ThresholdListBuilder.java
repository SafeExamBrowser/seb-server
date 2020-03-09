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

import ch.ethz.seb.sebserver.gui.service.page.PageService;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.IndicatorType;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.Threshold;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.widget.ThresholdList;

public class ThresholdListBuilder extends FieldBuilder<Collection<Threshold>> {

    private final Collection<Threshold> thresholds;

    protected ThresholdListBuilder(
            final String name,
            final LocTextKey label,
            final Collection<Threshold> thresholds) {

        super(name, label, thresholds);
        this.thresholds = thresholds;
    }

    @Override
    void build(final FormBuilder builder) {
        final Control titleLabel = createTitleLabel(builder.formParent, builder, this);
        if (!(builder.readonly || this.readonly)) {
            final Composite fieldGrid = createFieldGrid(builder.formParent, this.spanInput);

            final ThresholdList thresholdList = builder.widgetFactory.thresholdList(
                    fieldGrid,
                    fieldGrid.getParent().getParent(),
                    this.thresholds,
                    () -> {
                        try {
                            final String fieldValue = builder.form.getFieldValue(Domain.INDICATOR.ATTR_TYPE);
                            return IndicatorType.valueOf(fieldValue);
                        } catch (final Exception e) {
                            return null;
                        }
                    });

            final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
            thresholdList.setLayoutData(gridData);

            final Label errorLabel = createErrorLabel(fieldGrid);
            builder.form.putField(this.name, titleLabel, thresholdList, errorLabel);
            builder.setFieldVisible(this.visible, this.name);

            if (builder.pageService.getFormTooltipMode() == PageService.FormTooltipMode.INPUT) {
                builder.pageService.getPolyglotPageService().injectI18nTooltip(
                        thresholdList, this.tooltip);
            }
        }

    }

    public static String thresholdsToFormURLEncodedStringValue(final Collection<Threshold> thresholds) {
        if (thresholds == null || thresholds.isEmpty()) {
            return null;
        }

        // thresholds={value}|{color},thresholds={value}|{color}...
        return StringUtils.join(thresholds.stream()
                .map(t -> Domain.THRESHOLD.REFERENCE_NAME
                        + Constants.FORM_URL_ENCODED_NAME_VALUE_SEPARATOR
                        + t.getValue()
                        + Constants.EMBEDDED_LIST_SEPARATOR
                        + t.getColor())
                .collect(Collectors.toList()),
                Constants.LIST_SEPARATOR);
    }

}
