/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.ethz.seb.sebserver.gbl.Constants;

public final class Utils {

    public static <T> List<T> immutableListOf(final Collection<T> collection) {
        return (collection != null)
                ? Collections.unmodifiableList(new ArrayList<>(collection))
                : Collections.emptyList();
    }

    public static <T> Collection<T> immutableCollectionOf(final Collection<T> collection) {
        return (collection != null)
                ? Collections.unmodifiableCollection(collection)
                : Collections.emptySet();
    }

    /** Use this to create an immutable Collection of specified type from varargs
     *
     * @param values elements of the new immutable Collection
     * @return an immutable Collection of specified type with given elements */
    @SafeVarargs
    public static <T> Collection<T> immutableCollectionOf(final T... values) {
        if (values == null || values.length <= 0) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableCollection(Arrays.asList(values));
    }

    public static <T> Set<T> immutableSetOf(final Collection<T> collection) {
        return immutableSetOf(new HashSet<>(collection));
    }

    public static <T> Set<T> immutableSetOf(final Set<T> set) {
        return (set != null)
                ? Collections.unmodifiableSet(set)
                : Collections.emptySet();
    }

    /** Use this to create an immutable Set of specified type from varargs
     *
     * @param values elements of the new immutable Set
     * @return an immutable Set of specified type with given elements */
    @SafeVarargs
    public static <T> Set<T> immutableSetOf(final T... items) {
        if (items == null || items.length <= 0) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(items)));
    }

    public static <T> List<T> asImmutableList(final T[] array) {
        return (array != null)
                ? Collections.unmodifiableList(Arrays.asList(array))
                : Collections.emptyList();
    }

    public static Map<String, String> immutableMapOf(final Map<String, String> params) {
        return (params != null)
                ? Collections.unmodifiableMap(params)
                : Collections.emptyMap();
    }

    public static <T extends Enum<T>> Collection<Tuple<String>> createSelectionResource(final Class<T> enumClass) {
        return Collections.unmodifiableCollection(Arrays.asList(
                enumClass.getEnumConstants())
                .stream()
                .map(e -> new Tuple<>(e.name(), e.name()))
                .collect(Collectors.toList()));
    }

    public static Result<Long> dateTimeStringToTimestamp(final String startTime) {
        return Result.tryCatch(() -> {
            return DateTime.parse(startTime, Constants.DATE_TIME_PATTERN_UTC_NO_MILLIS).getMillis();
        });
    }

    public static Long dateTimeStringToTimestamp(final String startTime, final Long defaultValue) {
        return dateTimeStringToTimestamp(startTime)
                .getOr(defaultValue);
    }

    public static <M extends Map<K, V>, K, V> M mapPut(final M map, final K key, final V value) {
        map.put(key, value);
        return map;
    }

    public static <M extends Map<K, V>, K, V> M mapPutAll(final M map1, final M map2) {
        map1.putAll(map2);
        return map1;
    }

    public static <M extends Map<K, Collection<V>>, K, V> M mapCollect(final M map, final K key, final V value) {
        final List<V> list = (List<V>) map.computeIfAbsent(key, k -> new ArrayList<>());
        list.add(value);
        return map;
    }

    public static DateTime toDateTime(final String dateString) {
        if (StringUtils.isBlank(dateString)) {
            return null;
        }

        if (dateString.contains(".")) {
            return DateTime.parse(dateString, Constants.DATE_TIME_PATTERN_UTC_MILLIS);
        } else {
            return DateTime.parse(dateString, Constants.DATE_TIME_PATTERN_UTC_NO_MILLIS);
        }
    }

    public static Long toMilliSeconds(final String dateString) {
        if (StringUtils.isBlank(dateString)) {
            return null;
        }

        return toDateTime(dateString).getMillis();
    }

    public static String toJsonArray(final String string) {
        if (string == null) {
            return null;
        }

        final List<String> asList = Arrays.asList(StringUtils.split(string, Constants.LIST_SEPARATOR_CHAR));
        try {
            return new ObjectMapper().writeValueAsString(asList);
        } catch (final JsonProcessingException e) {
            return string;
        }
    }

    public static final String formatHTMLLines(final String message) {
        return (message != null)
                ? message.replace("\n", "<br/>")
                : null;
    }

}
