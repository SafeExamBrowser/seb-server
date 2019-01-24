/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.validation;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.validation.DirectFieldBindingResult;
import org.springframework.validation.Validator;

import com.fasterxml.jackson.databind.ObjectReader;

import ch.ethz.seb.sebserver.gbl.JSONMapper;
import ch.ethz.seb.sebserver.gbl.profile.WebServiceProfile;

@Service
@WebServiceProfile
public class BeanValidationService {

    private final Validator validator;
    private final JSONMapper jsonMapper;

    public BeanValidationService(
            final Validator validator,
            final JSONMapper jsonMapper) {

        this.validator = validator;
        this.jsonMapper = jsonMapper;
    }

    public <T> void validateBean(final T bean) {
        final DirectFieldBindingResult errors = new DirectFieldBindingResult(bean, "");
        this.validator.validate(bean, errors);
        if (errors.hasErrors()) {
            throw new BeanValidationException(errors);
        }
    }

    public <T, M> M validateNewBean(final Map<String, String> params, final Class<M> type) {
        M result = null;
        try {
            final String stringValue = this.jsonMapper.writeValueAsString(params);
            result = this.jsonMapper.readValue(stringValue, type);
        } catch (final Exception e) {
            throw new RuntimeException("Unexpected error: ", e);
        }

        if (result != null) {
            validateBean(result);
        }
        return result;
    }

    public <T, M> M validateModifiedBean(final T bean, final Map<String, String> params, final Class<M> type) {

        M result = null;
        try {
            final String stringValue = this.jsonMapper.writeValueAsString(bean);
            final String paramsString = this.jsonMapper.writeValueAsString(params);
            result = this.jsonMapper.readValue(stringValue, type);
            final ObjectReader updater = this.jsonMapper.readerForUpdating(result);
            result = updater.readValue(paramsString);
        } catch (final Exception e) {
            throw new RuntimeException("Unexpected error: ", e);
        }

        if (result != null) {
            validateBean(result);
        }
        return result;
    }

}
