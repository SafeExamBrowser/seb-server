/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.examconfig.impl.rules;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

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

    public static final String KEY_BROWSER_VIEW_MODE = "browserViewMode";
    public static final String KEY_TOUCH_EXIT = "enableTouchExit";
    public static final String KEY_MAIN_WINDOW_GROUP = "mainBrowserWindowWidth";

    @Override
    public boolean observesAttribute(final ConfigurationAttribute attribute) {
        return KEY_BROWSER_VIEW_MODE.equals(attribute.name);
    }

    @Override
    public void applyRule(
            final ViewContext context,
            final ConfigurationAttribute attribute,
            final ConfigurationValue value) {

        if (StringUtils.isBlank(value.value)) {
            return;
        }

        try {
            context.enable(KEY_TOUCH_EXIT);
            context.enableGroup(KEY_MAIN_WINDOW_GROUP);

            switch (Integer.parseInt(value.value)) {
                case 0: {
                    context.disable(KEY_TOUCH_EXIT);
                    break;
                }
                case 1: {
                    context.disable(KEY_TOUCH_EXIT);
                    context.disableGroup(KEY_MAIN_WINDOW_GROUP);
                    break;
                }
                case 2: {
                    context.disableGroup(KEY_MAIN_WINDOW_GROUP);
                    break;
                }
            }
        } catch (final Exception e) {
            log.warn("Failed to apply rule: ", e);
        }

    }

}
