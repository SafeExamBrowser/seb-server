/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.weblayer.api;

import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.LocaleUtils;

import ch.ethz.seb.sebserver.gbl.util.Utils;

public class APIParamsMap {

    public final Map<String, String> params;

    public APIParamsMap(final Map<String, String> params) {
        super();
        this.params = Utils.immutableMapOf(params);
    }

    public String getString(final String name) {
        return this.params.get(name);
    }

    public Long getLong(final String name) {
        final String value = this.params.get(name);
        if (value == null) {
            return null;
        }

        return Long.parseLong(value);
    }

    public Integer getInteger(final String name) {
        final String value = this.params.get(name);
        if (value == null) {
            return null;
        }

        return Integer.parseInt(value);
    }

    public Locale getLocale(final String name) {
        final String value = this.params.get(name);
        if (value == null) {
            return null;
        }

        return LocaleUtils.toLocale(name);
    }

    public boolean getBoolean(final String name) {
        return BooleanUtils.toBoolean(this.params.get(name));
    }

    public Boolean getBooleanObject(final String name) {
        return BooleanUtils.toBooleanObject(this.params.get(name));
    }

    public Integer getBooleanAsInteger(final String name) {
        final Boolean booleanObject = getBooleanObject(this.params.get(name));
        if (booleanObject == null) {
            return null;
        }
        return BooleanUtils.toIntegerObject(booleanObject);
    }

}
