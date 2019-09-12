/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.session;

import java.util.Collection;
import java.util.EnumMap;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

import ch.ethz.seb.sebserver.gbl.model.exam.Indicator;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.IndicatorType;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.Threshold;
import ch.ethz.seb.sebserver.gbl.util.Utils;

final class IndicatorData {

    final int index;
    final Indicator indicator;
    final Color defaultColor;
    final ThresholdColor[] thresholdColor;

    protected IndicatorData(final Indicator indicator, final int index, final Display display) {
        this.indicator = indicator;
        this.index = index;
        this.defaultColor = new Color(display, Utils.toRGB(indicator.defaultColor), 255);
        this.thresholdColor = new ThresholdColor[indicator.thresholds.size()];
        for (int i = 0; i < indicator.thresholds.size(); i++) {
            this.thresholdColor[i] = new ThresholdColor(indicator.thresholds.get(i), display);
        }
    }

    static final EnumMap<IndicatorType, IndicatorData> createFormIndicators(
            final Collection<Indicator> indicators,
            final Display display,
            final int indexOffset) {

        final EnumMap<IndicatorType, IndicatorData> indicatorMapping = new EnumMap<>(IndicatorType.class);
        int i = indexOffset;
        for (final Indicator indicator : indicators) {
            indicatorMapping.put(indicator.type, new IndicatorData(indicator, i, display));
            i++;
        }
        return indicatorMapping;
    }

    static final int getColorIndex(final IndicatorData indicatorData, final double value) {
        for (int j = 0; j < indicatorData.thresholdColor.length; j++) {
            if (value > indicatorData.thresholdColor[j].value && value < indicatorData.thresholdColor[j].value) {
                return j;
            }
        }

        return -1;
    }

    static final class ThresholdColor {
        final double value;
        final Color color;

        protected ThresholdColor(final Threshold threshold, final Display display) {
            this.value = threshold.value;
            this.color = new Color(display, Utils.toRGB(threshold.color), 255);
        }
    }

}