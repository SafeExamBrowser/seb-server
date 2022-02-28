/*
 * Copyright (c) 2020 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;

import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings;
import ch.ethz.seb.sebserver.gbl.model.exam.ProctoringServiceSettings.ProctoringServerType;

public class ProctoringSettingsValidator
        implements ConstraintValidator<ValidProctoringSettings, ProctoringServiceSettings> {

    @Override
    public boolean isValid(final ProctoringServiceSettings value, final ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }

        //if (value.enableProctoring) {
        if (value.serverType == ProctoringServerType.JITSI_MEET || value.serverType == ProctoringServerType.ZOOM) {
            boolean passed = true;

            if (StringUtils.isBlank(value.serverURL)) {
                context.disableDefaultConstraintViolation();
                context
                        .buildConstraintViolationWithTemplate("proctoringSettings:serverURL:notNull")
                        .addPropertyNode("serverURL").addConstraintViolation();
                passed = false;
            }

            if (StringUtils.isBlank(value.appKey)) {
                context.disableDefaultConstraintViolation();
                context
                        .buildConstraintViolationWithTemplate("proctoringSettings:appKey:notNull")
                        .addPropertyNode("appKey").addConstraintViolation();
                passed = false;
            }

            if (StringUtils.isBlank(value.appSecret)) {
                context.disableDefaultConstraintViolation();
                context
                        .buildConstraintViolationWithTemplate("proctoringSettings:appSecret:notNull")
                        .addPropertyNode("appSecret").addConstraintViolation();
                passed = false;
            }

            return passed;
        }
        //}

        return true;
    }

}
