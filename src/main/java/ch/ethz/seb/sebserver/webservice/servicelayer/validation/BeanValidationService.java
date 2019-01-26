/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.validation;

import org.springframework.stereotype.Service;
import org.springframework.validation.DirectFieldBindingResult;
import org.springframework.validation.Validator;

import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;
import ch.ethz.seb.sebserver.gbl.util.Result;

@Service
@WebServiceProfile
public class BeanValidationService {

    private final Validator validator;

    public BeanValidationService(final Validator validator) {
        this.validator = validator;
    }

    public <T> Result<T> validateBean(final T bean) {
        final DirectFieldBindingResult errors = new DirectFieldBindingResult(bean, "");
        this.validator.validate(bean, errors);
        if (errors.hasErrors()) {
            return Result.ofError(new BeanValidationException(errors));
        }

        return Result.of(bean);
    }

}
