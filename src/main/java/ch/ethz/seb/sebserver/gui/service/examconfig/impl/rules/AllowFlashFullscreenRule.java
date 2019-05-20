/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
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
public class AllowFlashFullscreenRule implements ValueChangeRule {

    public static final String KEY_THIRD_PART = "allowSwitchToApplications";
    public static final String KEY_FULL_SCREEN = "allowFlashFullscreen";

    @Override
    public boolean observesAttribute(final ConfigurationAttribute attribute) {
        return KEY_THIRD_PART.equals(attribute.name);
    }

    @Override
    public void applyRule(
            final ViewContext context,
            final ConfigurationAttribute attribut,
            final ConfigurationValue value) {

        if (BooleanUtils.toBoolean(value.value)) {
            context.enable(KEY_FULL_SCREEN);
        } else {
            context.disable(KEY_FULL_SCREEN);
        }
    }
}
