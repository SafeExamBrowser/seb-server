/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

import ch.ethz.seb.sebserver.gbl.model.exam.Indicator;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.Threshold;
import ch.ethz.seb.sebserver.gbl.util.Utils;

final class IndicatorData {

    final int index;
    final int tableIndex;
    final Indicator indicator;
    final Color defaultColor;
    final Color defaultTextColor;
    final ThresholdColor[] thresholdColor;

    protected IndicatorData(
            final Indicator indicator,
            final int index,
            final int tableIndex,
            final ColorData colorData,
            final Display display) {

        this.indicator = indicator;
        this.index = index;
        this.tableIndex = tableIndex;
        this.defaultColor = new Color(display, Utils.toRGB(indicator.defaultColor), 255);
        this.defaultTextColor = Utils.darkColorContrast(this.defaultColor.getRGB())
                ? colorData.darkColor
                : colorData.lightColor;

        this.thresholdColor = new ThresholdColor[indicator.thresholds.size()];
        final ArrayList<Threshold> sortedThresholds = new ArrayList<>(indicator.thresholds);
        sortedThresholds.sort(Comparator.comparing(t -> t.value));
        for (int i = 0; i < indicator.thresholds.size(); i++) {
            this.thresholdColor[i] = new ThresholdColor(sortedThresholds.get(i), display, colorData);
        }
    }

    static Map<Long, IndicatorData> createFormIndicators(
            final Collection<Indicator> indicators,
            final Display display,
            final ColorData colorData,
            final int tableIndexOffset) {

        final Map<Long, IndicatorData> indicatorMapping = new HashMap<>();
        int i = 0;
        for (final Indicator indicator : indicators) {
            indicatorMapping.put(indicator.id, new IndicatorData(
                    indicator,
                    i,
                    i + tableIndexOffset,
                    colorData,
                    display));
            i++;
        }
        return indicatorMapping;
    }

    static int getWeight(final IndicatorData indicatorData, final double value) {
        for (int j = 0; j < indicatorData.thresholdColor.length; j++) {
            if (value < indicatorData.thresholdColor[j].value) {
                return (j == 0) ? -1 : j - 1;
            }
        }

        return indicatorData.thresholdColor.length - 1;
    }

    static final class ThresholdColor {
        final double value;
        final Color color;
        final Color textColor;

        protected ThresholdColor(final Threshold threshold, final Display display, final ColorData colorData) {
            this.value = threshold.value;
            this.color = new Color(display, Utils.toRGB(threshold.color), 255);
            this.textColor = Utils.darkColorContrast(this.color.getRGB())
                    ? colorData.darkColor
                    : colorData.lightColor;
        }
    }

}