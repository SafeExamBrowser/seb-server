/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.examconfig.impl.rules;

import org.apache.commons.lang3.BooleanUtils;
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
public class BrowserWindowToolbarRule implements ValueChangeRule {

    public static final String KEY_ENABLE_TOOLBAR = "enableBrowserWindowToolbar";
    public static final String KEY_ADDBAR_MAIN = "browserWindowAllowAddressBar";
    public static final String KEY_ADDBAR = "newBrowserWindowAllowAddressBar";
    public static final String KEY_DEV_CON = "allowDeveloperConsole";
    public static final String KEY_HIDE_TOOLBAR = "hideBrowserWindowToolbar";

    @Override
    public boolean observesAttribute(final ConfigurationAttribute attribute) {
        return KEY_ENABLE_TOOLBAR.equals(attribute.name);
    }

    @Override
    public void applyRule(
            final ViewContext context,
            final ConfigurationAttribute attribute,
            final ConfigurationValue value) {

        if (context.isReadonly()) {
            return;
        }

        if (BooleanUtils.toBoolean(value.value)) {
            context.enable(KEY_ADDBAR_MAIN);
            context.enable(KEY_ADDBAR);
            context.enable(KEY_DEV_CON);
            context.enable(KEY_HIDE_TOOLBAR);
        } else {
            context.disable(KEY_ADDBAR_MAIN);
            context.disable(KEY_ADDBAR);
            context.disable(KEY_DEV_CON);
            context.disable(KEY_HIDE_TOOLBAR);
        }
    }

}
