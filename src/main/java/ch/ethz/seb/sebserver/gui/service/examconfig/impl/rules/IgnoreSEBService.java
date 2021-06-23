/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
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
public class IgnoreSEBService implements ValueChangeRule {

    public static final String KEY_IGNORE_SEB_SERVICE = "sebServiceIgnore";

    public static final String KEY_SEB_SERVICE_POLICY = "sebServicePolicy";
    public static final String KEY_ATTR_1 = "enableWindowsUpdate";
    public static final String KEY_ATTR_2 = "enableChromeNotifications";
    public static final String KEY_ATTR_3 = "allowScreenSharing";
    public static final String KEY_REG = "registry";
    public static final String KEY_REG_RUNNING_SEB = "insideSebEnableSwitchUser";

    @Override
    public boolean observesAttribute(final ConfigurationAttribute attribute) {
        return KEY_IGNORE_SEB_SERVICE.equals(attribute.name);
    }

    @Override
    public void applyRule(
            final ViewContext context,
            final ConfigurationAttribute attribute,
            final ConfigurationValue value) {

        if (context.isReadonly()) {
            return;
        }

        if (KEY_IGNORE_SEB_SERVICE.equals(attribute.name)) {
            if (BooleanUtils.toBoolean(value.value)) {
                context.disable(KEY_SEB_SERVICE_POLICY);
                context.disable(KEY_ATTR_1);
                context.disable(KEY_ATTR_2);
                context.disable(KEY_ATTR_3);
                context.disableGroup(KEY_REG, KEY_REG_RUNNING_SEB);

                context.setValue(
                        KEY_SEB_SERVICE_POLICY,
                        context.getAttributeByName(KEY_SEB_SERVICE_POLICY).defaultValue);
                context.setValue(
                        KEY_ATTR_1,
                        context.getAttributeByName(KEY_ATTR_1).defaultValue);
                context.setValue(
                        KEY_ATTR_2,
                        context.getAttributeByName(KEY_ATTR_2).defaultValue);
                context.setValue(
                        KEY_ATTR_3,
                        context.getAttributeByName(KEY_ATTR_3).defaultValue);
            } else {
                context.enable(KEY_SEB_SERVICE_POLICY);
                context.enable(KEY_ATTR_1);
                context.enable(KEY_ATTR_2);
                context.enable(KEY_ATTR_3);
                context.enableGroup(KEY_REG, KEY_REG_RUNNING_SEB);
            }
        }

    }

}
