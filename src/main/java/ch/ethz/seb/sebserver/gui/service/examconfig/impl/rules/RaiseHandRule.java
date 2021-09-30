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
public class RaiseHandRule implements ValueChangeRule {

    public static final String KEY_RAISE_HAND = "raiseHandButtonShow";
    public static final String KEY_PROMPT_MESSAGE = "raiseHandButtonAlwaysPromptMessage";

    @Override
    public boolean observesAttribute(final ConfigurationAttribute attribute) {
        return KEY_RAISE_HAND.equals(attribute.name);
    }

    @Override
    public void applyRule(final ViewContext context, final ConfigurationAttribute attribute,
            final ConfigurationValue value) {

        if (context.isReadonly()) {
            return;
        }

        if (BooleanUtils.toBoolean(value.value)) {
            context.enable(KEY_PROMPT_MESSAGE);
        } else {
            context.disable(KEY_PROMPT_MESSAGE);
            context.setValue(
                    KEY_PROMPT_MESSAGE,
                    context.getAttributeByName(KEY_PROMPT_MESSAGE).defaultValue);
        }
    }

}
