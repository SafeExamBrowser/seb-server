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
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.exam.Indicator.Threshold;
import ch.ethz.seb.sebserver.gbl.util.Utils;

public class POSTMapper {

    private static final Logger log = LoggerFactory.getLogger(POSTMapper.class);

    public static final POSTMapper EMPTY_MAP = new POSTMapper(null);

    protected final MultiValueMap<String, String> params;

    public POSTMapper(final MultiValueMap<String, String> params) {
        super();
        this.params = params != null
                ? new LinkedMultiValueMap<>(params)
                : new LinkedMultiValueMap<>();
    }

    public String getString(final String name) {
        final String first = this.params.getFirst(name);
        if (StringUtils.isNotBlank(first)) {
            try {
                return URLDecoder.decode(first, "UTF-8");
            } catch (final UnsupportedEncodingException e) {
                log.warn("Failed to decode form URL formatted string value: ", e);
                return first;
            }
        }
        return first;
    }

    public char[] getCharArray(final String name) {
        final String value = getString(name);
        if (value == null || value.length() <= 0) {
            return null;
        }

        return value.toCharArray();
    }

    public CharSequence getCharSequence(final String name) {
        final char[] charArray = getCharArray(name);
        if (charArray == null) {
            return null;
        }

        return CharBuffer.wrap(charArray);
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

    public List<Threshold> getThresholds() {
        final List<String> thresholdStrings = this.params.get(Domain.THRESHOLD.REFERENCE_NAME);
        if (thresholdStrings == null || thresholdStrings.isEmpty()) {
            return Collections.emptyList();
        }

        return thresholdStrings.stream()
                .map(ts -> {
                    try {
                        final String[] split = StringUtils.split(ts, Constants.EMBEDDED_LIST_SEPARATOR);
                        return new Threshold(Double.parseDouble(split[0]), split[1]);
                    } catch (final Exception e) {
                        return null;
                    }
                })
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public <T extends POSTMapper> T putIfAbsent(final String name, final String value) {
        this.params.putIfAbsent(name, Arrays.asList(value));
        return (T) this;
    }

}
