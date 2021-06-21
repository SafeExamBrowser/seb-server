/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.examconfig.impl.rules;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationAttribute;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.ConfigurationValue;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gui.service.examconfig.ValueChangeRule;
import ch.ethz.seb.sebserver.gui.service.examconfig.impl.ViewContext;

@Lazy
@Service
@GuiProfile
public class BrowserViewModeRule implements ValueChangeRule {

    private static final Logger log = LoggerFactory.getLogger(BrowserViewModeRule.class);

    private static final String KEY_BROWSER_VIEW_MODE = "browserViewMode";
    private static final String KEY_MAIN_WINDOW_GROUP = "mainBrowserWindowWidth";
    private static final String KEY_TOUCH_OPTIMIZED = "touchOptimized";

    @Override
    public boolean observesAttribute(final ConfigurationAttribute attribute) {
        return KEY_BROWSER_VIEW_MODE.equals(attribute.name) || KEY_TOUCH_OPTIMIZED.equals(attribute.name);
    }

    @Override
    public void applyRule(
            final ViewContext context,
            final ConfigurationAttribute attribute,
            final ConfigurationValue value) {

        if (context.isReadonly() || StringUtils.isBlank(value.value)) {
            return;
        }

        try {
            if (KEY_TOUCH_OPTIMIZED.equals(attribute.name)) {
                if (BooleanUtils.toBoolean(value.value)) {
                    context.setValue(KEY_BROWSER_VIEW_MODE, "2");
                    context.disableGroup(KEY_MAIN_WINDOW_GROUP);
                } else {
                    if (context.getValue(KEY_BROWSER_VIEW_MODE) == null) {
                        context.setValue(KEY_BROWSER_VIEW_MODE, "0");
                    }
                }

                return;
            }

            if (KEY_BROWSER_VIEW_MODE.equals(attribute.name)) {
                switch (Integer.parseInt(value.value)) {
                    case 0: {
                        if (!context.getValue(KEY_TOUCH_OPTIMIZED).equals(Constants.FALSE_STRING)) {
                            context.setValue(KEY_TOUCH_OPTIMIZED, Constants.FALSE_STRING);
                        }
                        context.enableGroup(KEY_MAIN_WINDOW_GROUP);
                        break;
                    }
                    case 1: {
                        if (!context.getValue(KEY_TOUCH_OPTIMIZED).equals(Constants.FALSE_STRING)) {
                            context.setValue(KEY_TOUCH_OPTIMIZED, Constants.FALSE_STRING);
                        }
                        context.disableGroup(KEY_MAIN_WINDOW_GROUP);
                        break;
                    }
                    default: {
                    }
                }
            }
        } catch (final Exception e) {
            log.warn("Failed to apply rule: ", e);
        }
    }

}
