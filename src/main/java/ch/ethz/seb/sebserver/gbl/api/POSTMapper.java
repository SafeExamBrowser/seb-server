/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.api;

import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
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

/** A POST parameter mapper that wraps all parameter from a POST request given by a MultiValueMap<String, String> and
 * defines API specific convenience functions to access this parameter with given type and conversion of needed. */
public class POSTMapper {

    private static final Logger log = LoggerFactory.getLogger(POSTMapper.class);

    public static final POSTMapper EMPTY_MAP = new POSTMapper(null, null);

    protected final MultiValueMap<String, String> params;

    public POSTMapper(final MultiValueMap<String, String> params, final String uriQueryString) {
        super();
        this.params = params != null
                ? new LinkedMultiValueMap<>(params)
                : new LinkedMultiValueMap<>();

        if (uriQueryString != null) {
            handleEncodedURIParams(uriQueryString);
        }
    }

    // NOTE: this is a workaround since URI parameter are not automatically decoded in the HTTPServletRequest
    //       while parameter from form-urlencoded body part are.
    //       I also tried to set application property: server.tomcat.uri-encoding=UTF-8 bit with no effect.
    // TODO  Didn't found a better solution for now but if there is some time, we should find a better solution
    private void handleEncodedURIParams(final String uriQueryString) {
        final MultiValueMap<String, String> override = new LinkedMultiValueMap<>();
        this.params
                .entrySet()
                .stream()
                .forEach(entry -> {
                    if (uriQueryString.contains(entry.getKey())) {
                        override.put(
                                entry.getKey(),
                                entry.getValue().stream()
                                        .map(val -> decode(val))
                                        .collect(Collectors.toList()));
                    }
                });

        if (!override.isEmpty()) {
            this.params.putAll(override);
        }
    }

    private String decode(final String val) {
        try {
            return Utils.decodeFormURL_UTF_8(val);
        } catch (final Exception e) {
            return val;
        }
    }

    public String getString(final String name) {
        return this.params.getFirst(name);
    }

    public char[] getCharArray(final String name) {
        final String value = getString(name);
        if (value == null || value.length() <= 0) {
            return new char[] {};
        }

        return value.toCharArray();
    }

    public byte[] getBinary(final String name) {
        final String value = getString(name);
        if (value == null || value.length() <= 0) {
            return new byte[0];
        }

        return Utils.toByteArray(value);
    }

    public byte[] getBinaryFromBase64(final String name) {
        final String value = getString(name);
        if (value == null || value.length() <= 0) {
            return new byte[0];
        }

        return Base64.getDecoder().decode(value);
    }

    public CharSequence getCharSequence(final String name) {
        return CharBuffer.wrap(getCharArray(name));
    }

    public Long getLong(final String name) {
        final String value = this.params.getFirst(name);
        if (StringUtils.isBlank(value)) {
            return null;
        }

        try {
            return Long.parseLong(value);
        } catch (final Exception e) {
            log.error("Failed to parse long value for attribute: {}", name, e.getMessage());
            return null;
        }
    }

    public Short getShort(final String name) {
        final String value = this.params.getFirst(name);
        if (StringUtils.isBlank(value)) {
            return null;
        }

        return Short.parseShort(value);
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

    public Map<String, String> getSubMap(final Set<String> actionAttributes) {
        return this.params
                .keySet()
                .stream()
                .filter(actionAttributes::contains)
                .collect(Collectors.toMap(Function.identity(), k -> this.params.getFirst(k)));
    }

    public List<Threshold> getThresholds() {
        final List<String> thresholdStrings = this.params.get(Domain.THRESHOLD.REFERENCE_NAME);
        if (thresholdStrings == null || thresholdStrings.isEmpty()) {
            return Collections.emptyList();
        }

        return thresholdStrings
                .stream()
                .map(ts -> {
                    try {
                        final String[] split = StringUtils.split(ts, Constants.EMBEDDED_LIST_SEPARATOR);
                        return new Threshold(Double.parseDouble(
                                split[0]),
                                (split.length > 1) ? split[1] : null,
                                (split.length > 2) ? split[2] : null);
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
