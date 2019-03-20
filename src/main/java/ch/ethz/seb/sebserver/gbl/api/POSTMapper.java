/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.api;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import ch.ethz.seb.sebserver.gbl.util.Utils;

public class POSTMapper {

    public static final POSTMapper EMPTY_MAP = new POSTMapper(null);

    protected final MultiValueMap<String, String> params;

    public POSTMapper(final MultiValueMap<String, String> params) {
        super();
        this.params = new LinkedMultiValueMap<>();
        if (params != null) {
            for (final Map.Entry<String, List<String>> entry : params.entrySet()) {
                this.params.put(
                        entry.getKey(),
                        entry.getValue()
                                .stream()
                                .map(encoded -> {
                                    try {
                                        return URLDecoder.decode(encoded, "UTF-8");
                                    } catch (final UnsupportedEncodingException e) {
                                        return encoded;
                                    }
                                })
                                .collect(Collectors.toList()));
            }
        }
    }

    public String getString(final String name) {
        return this.params.getFirst(name);
    }

    public Long getLong(final String name) {
        final String value = this.params.getFirst(name);
        if (StringUtils.isBlank(value)) {
            return null;
        }

        return Long.parseLong(value);
    }

    public Integer getInteger(final String name) {
        final String value = this.params.getFirst(name);
        if (value == null) {
            return null;
        }

        return Integer.parseInt(value);
    }

    public Locale getLocale(final String name) {
        final String value = this.params.getFirst(name);
        if (value == null) {
            return null;
        }

        return Locale.forLanguageTag(value);
    }

    public boolean getBoolean(final String name) {
        return BooleanUtils.toBoolean(this.params.getFirst(name));
    }

    public Boolean getBooleanObject(final String name) {
        return BooleanUtils.toBooleanObject(this.params.getFirst(name));
    }

    public Integer getBooleanAsInteger(final String name) {
        final Boolean booleanObject = getBooleanObject(name);
        if (booleanObject == null) {
            return null;
        }
        return BooleanUtils.toIntegerObject(booleanObject);
    }

    public DateTimeZone getDateTimeZone(final String name) {
        final String value = this.params.getFirst(name);
        if (value == null) {
            return null;
        }
        try {
            return DateTimeZone.forID(value);
        } catch (final Exception e) {
            return null;
        }
    }

    public Set<String> getStringSet(final String name) {
        final List<String> list = this.params.get(name);
        if (list == null) {
            return Collections.emptySet();
        }
        return Utils.immutableSetOf(list);
    }

    public <T extends Enum<T>> T getEnum(final String name, final Class<T> type, final T defaultValue) {
        final T result = getEnum(name, type);
        if (result == null) {
            return defaultValue;
        }

        return result;
    }

    public <T extends Enum<T>> T getEnum(final String name, final Class<T> type) {
        final String value = this.params.getFirst(name);
        if (value == null) {
            return null;
        }
        try {
            return Enum.valueOf(type, value);
        } catch (final Exception e) {
            return null;
        }
    }

    public DateTime getDateTime(final String name) {
        final String value = this.params.getFirst(name);
        if (value == null) {
            return null;
        }

        return Utils.toDateTime(value);
    }

    @SuppressWarnings("unchecked")
    public <T extends POSTMapper> T putIfAbsent(final String name, final String value) {
        this.params.putIfAbsent(name, Arrays.asList(value));
        return (T) this;
    }

}
