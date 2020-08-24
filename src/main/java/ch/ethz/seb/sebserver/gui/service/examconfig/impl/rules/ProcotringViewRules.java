/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
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
public class ProcotringViewRules implements ValueChangeRule {

    public static final String KEY_ENABLE_AI = "proctoringAIEnable";
    public static final String KEY_ENABLE_JITSI = "jitsiMeetEnable";

    public static final String JITSI_GROUP_FEATURES = "jitsiMeetFeatureFlagChat";
    public static final String JITSI_GROUP_CONTROLS = "jitsiMeetAudioMuted";

    public static final String AI_GROUP_FACE_NUMBER = "proctoringDetectFaceCount";
    public static final String AI_GROUP_FACE_ANGLE = "proctoringDetectFaceYaw";

    @Override
    public boolean observesAttribute(final ConfigurationAttribute attribute) {
        return KEY_ENABLE_AI.equals(attribute.name) || KEY_ENABLE_JITSI.equals(attribute.name);
    }

    @Override
    public void applyRule(
            final ViewContext context,
            final ConfigurationAttribute attribute,
            final ConfigurationValue value) {

        if (KEY_ENABLE_JITSI.equals(attribute.name)) {
            if (BooleanUtils.toBoolean(value.value)) {
                context.enableGroup(JITSI_GROUP_FEATURES);
                context.enableGroup(JITSI_GROUP_CONTROLS);
            } else {
                context.disableGroup(JITSI_GROUP_FEATURES);
                context.disableGroup(JITSI_GROUP_CONTROLS);
            }
        } else if (KEY_ENABLE_AI.equals(attribute.name)) {
            if (BooleanUtils.toBoolean(value.value)) {
                context.enableGroup(AI_GROUP_FACE_NUMBER);
                context.enableGroup(AI_GROUP_FACE_ANGLE);
            } else {
                context.disableGroup(AI_GROUP_FACE_NUMBER);
                context.disableGroup(AI_GROUP_FACE_ANGLE);
            }
        }
    }

}
