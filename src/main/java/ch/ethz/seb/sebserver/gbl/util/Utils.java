/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.ethz.seb.sebserver.gbl.Constants;

public final class Utils {

    private static final Logger log = LoggerFactory.getLogger(Utils.class);

    /** This Collector can be used within stream collect to get one expected singleton element from
     * the given Stream.
     * This first collects the given Stream to a list and then check if there is one expected element.
     * If not a IllegalStateException is thrown.
     *
     * @return the expected singleton element
     * @throws IllegalStateException if the given stream was empty or has more then one element */
    public static <T> Collector<T, ?, T> toSingleton() {
        return Collectors.collectingAndThen(
                Collectors.toList(),
                list -> {
                    if (list == null) {
                        throw new IllegalStateException(
                                "Expected one elements in the given list but is empty");
                    }
                    if (list.size() != 1) {
                        throw new IllegalStateException(
                                "Expected only one elements in the given list but size is: " + list.size());
                    }
                    return list.get(0);
                });
    }

    public static <T> T toSingleton(final Collection<T> collection) {
        return collection.stream().collect(toSingleton());
    }

    /** Get an immutable List from a Collection of elements
     *
     * @param collection Collection of elements
     * @return immutable List */
    public static <T> List<T> immutableListOf(final Collection<T> collection) {
        return (collection != null)
                ? Collections.unmodifiableList(new ArrayList<>(collection))
                : Collections.emptyList();
    }

    /** Get a immutable Collection from a Collection of elements
     *
     * @param collection Collection of elements
     * @return immutable Collection */
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

    /** Get a immutable Set from a Collection of elements
     *
     * @param collection Collection of elements
     * @return immutable Set */
    public static <T> Set<T> immutableSetOf(final Collection<T> collection) {
        return immutableSetOf(new HashSet<>(collection));
    }

    /** Get a immutable Set from a Set of elements
     *
     * @param set Set of elements
     * @return immutable Set */
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

    public static <K, V> Map<K, V> immutableMapOf(final Map<K, V> params) {
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
            return DateTime.parse(startTime, Constants.STANDARD_DATE_TIME_FORMATTER).getMillis();
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

        return DateTime.parse(dateString, Constants.STANDARD_DATE_TIME_FORMATTER);
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

    public static final String encodeFormURL_UTF_8(final String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (final UnsupportedEncodingException e) {
            log.error("Unexpected error while trying to encode to from URL UTF-8: ", e);
            return value;
        }
    }

    public static void clearCharArray(final char[] array) {
        if (array == null) {
            return;
        }

        for (int i = 0; i < array.length; i++) {
            array[i] = 0;
        }

    }

    public static byte[] toByteArray(final ByteBuffer buffer) {
        if (buffer == null) {
            return new byte[0];
        }

        buffer.rewind();
        final byte[] result = new byte[buffer.limit()];
        buffer.get(result);
        return result;
    }

    /** Formats the given CharSequence to a UTF-8 and convert to byte array
     *
     * @param chars
     * @return UTF-8 formatted byte array of given CharSequence */
    public static byte[] toByteArray(final CharSequence chars) {
        return toByteArray(toByteBuffer(chars));
    }

    public static ByteBuffer toByteBuffer(final CharSequence chars) {
        if (chars == null) {
            return ByteBuffer.allocate(0);
        }

        return StandardCharsets.UTF_8.encode(CharBuffer.wrap(chars));
    }

    public static CharBuffer toCharBuffer(final ByteBuffer byteBuffer) {
        if (byteBuffer == null) {
            return CharBuffer.allocate(0);
        }

        byteBuffer.rewind();
        return StandardCharsets.UTF_8.decode(byteBuffer);
    }

    public static String toString(final ByteBuffer byteBuffer) {
        return toCharBuffer(byteBuffer).toString();
    }

    public static String toString(final byte[] byteArray) {
        return toString(ByteBuffer.wrap(byteArray));
    }

    public static char[] toCharArray(final CharBuffer buffer) {
        if (buffer == null) {
            return new char[0];
        }

        buffer.rewind();
        final char[] result = new char[buffer.limit()];
        buffer.get(result);
        return result;
    }

    public static char[] toCharArray(final CharSequence chars) {
        if (chars == null) {
            return new char[0];
        }

        return toCharArray(CharBuffer.wrap(chars));
    }

    public static String toString(final CharSequence charSequence) {
        if (charSequence == null) {
            return null;
        }

        final StringBuilder builder = new StringBuilder();
        builder.append(charSequence);
        return builder.toString();
    }

}
