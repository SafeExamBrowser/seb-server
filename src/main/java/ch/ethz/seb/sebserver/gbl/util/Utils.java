/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.swt.graphics.RGB;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.JSONMapper;

public final class Utils {

    public static final int DARK_COLOR_THRESHOLD = 400;
    public static final Predicate<?> TRUE_PREDICATE = v -> true;
    public static final Predicate<?> FALSE_PREDICATE = v -> false;
    public static final Runnable EMPTY_EXECUTION = () -> {
    };

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
                    if (list == null || list.size() == 0) {
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
     * @param values elements
     * @return immutable List */
    @SafeVarargs
    public static <T> List<T> immutableListOf(final T... values) {
        if (values == null) {
            return Collections.emptyList();
        }

        return immutableListOf(Arrays.asList(values));
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
     * @param items elements of the new immutable Set
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

    public static <T extends Collection<V>, V> T addAll(final T target, final T source) {
        target.addAll(source);
        return target;
    }

    public static <K, V> Map<K, V> immutableMapOf(final Map<K, V> params) {
        return (params != null)
                ? Collections.unmodifiableMap(params)
                : Collections.emptyMap();
    }

    public static <T extends Enum<T>> Collection<Tuple<String>> createSelectionResource(final Class<T> enumClass) {
        return Collections.unmodifiableList(Arrays.stream(enumClass.getEnumConstants())
                .map(e -> new Tuple<>(e.name(), e.name()))
                .collect(Collectors.toList()));
    }

    public static Collection<String> getListOfLines(final String list) {
        if (list == null) {
            return Collections.emptyList();
        }

        return Arrays.asList(StringUtils.split(
                streamlineCarriageReturn(list),
                Constants.CARRIAGE_RETURN));
    }

    public static String convertCarriageReturnToListSeparator(final String value) {
        if (value == null) {
            return null;
        }

        return streamlineCarriageReturn(value.trim())
                .replace(Constants.CARRIAGE_RETURN, Constants.LIST_SEPARATOR_CHAR);
    }

    public static String convertListSeparatorToCarriageReturn(final String value) {
        if (value == null) {
            return null;
        }

        return value
                .trim()
                .replace(Constants.LIST_SEPARATOR_CHAR, Constants.CARRIAGE_RETURN);
    }

    public static String streamlineCarriageReturn(final String value) {
        if (value == null) {
            return null;
        }

        return value.replace('\r', '\n')
                .replace("\r\n", "\n");
    }

    public static List<String> getListFromString(final String list) {
        return getListFromString(list, Constants.LIST_SEPARATOR);
    }

    public static List<String> getListFromString(final String list, final String separator) {
        if (list == null) {
            return Collections.emptyList();
        }

        return Arrays.asList(StringUtils.split(list, separator));
    }

    public static Result<Long> dateTimeStringToTimestamp(final String startTime) {
        return Result.tryCatch(() -> DateTime
                .parse(startTime, Constants.STANDARD_DATE_TIME_FORMATTER)
                .getMillis());
    }

    public static String formatDate(final DateTime dateTime) {
        return dateTime.toString(Constants.STANDARD_DATE_TIME_MILLIS_FORMATTER);
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

    public static DateTime toDateTimeUTC(final String dateString) {
        final DateTime dateTime = toDateTime(dateString);
        if (dateTime == null) {
            return null;
        }

        return dateTime.withZone(DateTimeZone.UTC);
    }

    public static DateTime toDateTimeUTC(final Long timestamp) {
        if (timestamp == null) {
            return null;
        } else {
            return toDateTimeUTC(timestamp.longValue());
        }
    }

    public static DateTime toDateTimeUTC(final long timestamp) {
        return new DateTime(timestamp, DateTimeZone.UTC);
    }

    public static DateTime toDateTimeUTCUnix(final Long timestamp) {
        if (timestamp == null || timestamp <= 0) {
            return null;
        } else {
            return toDateTimeUTCUnix(timestamp.longValue());
        }
    }

    public static DateTime toDateTimeUTCUnix(final long timestamp) {
        return new DateTime(timestamp * 1000, DateTimeZone.UTC);
    }

    public static final long toUnixTimeInSeconds(final DateTime time) {
        return time.getMillis() / 1000;
    }

    public static Long toTimestamp(final String dateString) {
        if (StringUtils.isBlank(dateString)) {
            return null;
        }

        return Objects.requireNonNull(toDateTime(dateString)).getMillis();
    }

    public static Long toTimestampUTC(final String dateString) {
        if (StringUtils.isBlank(dateString)) {
            return null;
        }

        return Objects.requireNonNull(toDateTimeUTC(dateString)).getMillis();
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

    public static String formatHTMLLines(final String message) {
        return (message != null)
                ? message.replace("\n", "<br/>")
                : null;
    }

    public static String formatHTMLLinesForceEscaped(final String message) {
        return (message != null)
                ? message.replace("\n", "<br/>").replace("\\n", "<br/>")
                : null;
    }

    public static String formatLineBreaks(final String text) {
        if (text == null) {
            return null;
        }

        return text
                .replace("<br/>", "\n")
                .replace("<br></br>", "\n");
    }

    public static String encodeFormURL_UTF_8(final String value) {
        if (StringUtils.isBlank(value)) {
            return value;
        }

        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (final UnsupportedEncodingException e) {
            log.error("Failed to encode FormURL_UTF_8 for: {}", value, e);
            return value;
        }
    }

    public static String decodeFormURL_UTF_8(final String value) {
        if (StringUtils.isBlank(value)) {
            return value;
        }

        try {
            return URLDecoder.decode(
                    (value.indexOf('+') >= 0)
                            ? value.replaceAll("\\+", "%2b")
                            : value,
                    StandardCharsets.UTF_8.name());
        } catch (final UnsupportedEncodingException e) {
            log.error("Failed to decode FormURL_UTF_8 for: {}", value, e);
            return value;
        }
    }

    public static void clearCharArray(final char[] array) {
        if (array == null) {
            return;
        }

        Arrays.fill(array, (char) 0);
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
     * @param chars CharSequence
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
        if (byteArray == null) {
            return null;
        }

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

    public static void clear(final CharSequence charSequence) {
        clearCharArray(toCharArray(charSequence));
    }

    public static String toString(final CharSequence charSequence) {
        if (charSequence == null) {
            return null;
        }

        return String.valueOf(charSequence);
    }

    public static String escapeHTML_XML_EcmaScript(final String string) {
        return StringEscapeUtils.escapeXml11(
                StringEscapeUtils.escapeHtml4(
                        StringEscapeUtils.escapeEcmaScript(string)));
    }

    // https://www.owasp.org/index.php/HTTP_Response_Splitting
    public static String preventResponseSplittingAttack(final String string) {
        final int xni = string.indexOf('\n');
        final int xri = string.indexOf('\r');
        if (xni >= 0 || xri >= 0) {
            throw new IllegalArgumentException("Illegal argument: " + string);
        }

        return string;
    }

    public static String toSQLWildcard(final String text) {
        return (text == null) ? null : Constants.PERCENTAGE + text.replace("%", "\\%") + Constants.PERCENTAGE;
    }

    public static String hash_SHA_256_Base_16(final CharSequence chars) {
        if (chars == null) {
            return null;
        }

        try {
            final MessageDigest digest = MessageDigest.getInstance(Constants.SHA_256);
            final byte[] encodedHash = digest.digest(toByteArray(chars));
            return Hex.encodeHexString(encodedHash);
        } catch (final NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to hash text: ", e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> Predicate<T> truePredicate() {
        return (Predicate<T>) TRUE_PREDICATE;
    }

    @SuppressWarnings("unchecked")
    public static <T> Predicate<T> falsePredicate() {
        return (Predicate<T>) FALSE_PREDICATE;
    }

    public static long getMillisecondsNow() {
        return DateTimeUtils.currentTimeMillis();
    }

    public static long getSecondsNow() {
        return getMillisecondsNow() / 1000;
    }

    public static long toSeconds(final long millis) {
        return millis / 1000;
    }

    public static RGB toRGB(final String rgbString) {
        if (StringUtils.isNotBlank(rgbString)) {

            final String rgbVal = (rgbString.startsWith(Constants.HASH_TAG_STRING))
                    ? rgbString.substring(1)
                    : rgbString;

            return new RGB(
                    Integer.parseInt(rgbVal.substring(0, 2), 16),
                    Integer.parseInt(rgbVal.substring(2, 4), 16),
                    Integer.parseInt(rgbVal.substring(4, 6), 16));
        } else {
            return new RGB(255, 255, 255);
        }
    }

    public static MultiValueMap<String, String> createJsonContentHeader() {
        final MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.set(
                HttpHeaders.CONTENT_TYPE,
                org.springframework.http.MediaType.APPLICATION_JSON_VALUE);
        return headers;
    }

    public static String getErrorCauseMessage(final Exception e) {
        if (e == null || e.getCause() == null) {
            return Constants.EMPTY_NOTE;
        }
        return e.getCause().getClass().getName() + " : " + e.getCause().getMessage();
    }

    /** Indicates if a dark background or contrast color must be used for the given text or foreground color.
     *
     * @param rgb foreground or text color
     * @return true of the background color for given foreground color shall be dark or false if it shall be light */
    public static boolean darkColorContrast(final RGB rgb) {
        return rgb.red + rgb.green + rgb.blue > DARK_COLOR_THRESHOLD;
    }

    public static String parseColorString(final RGB color) {
        if (color == null) {
            return null;
        }

        return toColorFractionString(color.red)
                + toColorFractionString(color.green)
                + toColorFractionString(color.blue);
    }

    public static RGB parseRGB(final String colorString) {
        if (StringUtils.isBlank(colorString)) {
            return null;
        }

        final int r = Integer.parseInt(colorString.substring(0, 2), 16);
        final int g = Integer.parseInt(colorString.substring(2, 4), 16);
        final int b = Integer.parseInt(colorString.substring(4, 6), 16);

        return new RGB(r, g, b);
    }

    public static String toColorFractionString(final int fraction) {
        final String hexString = Integer.toHexString(fraction);
        return (hexString.length() < 2) ? "0" + hexString : hexString;
    }

    public static String toAppFormUrlEncodedBody(final MultiValueMap<String, String> attributes) {
        if (attributes == null) {
            return StringUtils.EMPTY;
        }

        return attributes
                .entrySet()
                .stream()
                .reduce(
                        new StringBuilder(),
                        (sb, entry) -> {
                            final String name = entry.getKey();
                            final List<String> values = entry.getValue();
                            if (values == null || values.isEmpty()) {
                                return sb;
                            }
                            if (sb.length() > 0) {
                                sb.append(Constants.AMPERSAND);
                            }
                            if (sb.length() == 1) {
                                return sb.append(name).append(Constants.EQUALITY_SIGN).append(values.get(0));
                            }
                            return sb.append(toAppFormUrlEncodedBody(name, values));
                        },
                        StringBuilder::append)
                .toString();
    }

    public static String toAppFormUrlEncodedBody(@NotNull final String name, final Collection<String> array) {
        if (array == null) {
            return StringUtils.EMPTY;
        }

        final String _name = name.contains(String.valueOf(Constants.SQUARE_BRACE_OPEN)) || array.size() <= 1
                ? name
                : name + Constants.SQUARE_BRACE_OPEN + Constants.SQUARE_BRACE_CLOSE;

        return array
                .stream()
                .reduce(
                        new StringBuilder(),
                        (sb, entry) -> {
                            if (sb.length() > 0) {
                                sb.append(Constants.AMPERSAND);
                            }
                            return sb.append(_name).append(Constants.EQUALITY_SIGN).append(entry);
                        },
                        StringBuilder::append)
                .toString();
    }

    public static String truncateText(final String text, final int toChars) {
        if (text == null || toChars < 3) {
            return text;
        }

        if (text.length() <= toChars) {
            return text;
        }

        return StringUtils.truncate(text, toChars - 3) + "...";
    }

    public static String valueOrEmptyNote(final String value) {
        return StringUtils.isBlank(value) ? Constants.EMPTY_NOTE : value;
    }

    /** This is used to verify if a given URL is available (valid)
     * Uses java.net.Socket to try to connect to the given URL
     *
     * @param urlString the URL string
     * @return true if SEB Server was able to ping the address. */
    public static boolean pingHost(final String urlString) {
        try (Socket socket = new Socket()) {
            final URL url = new URL(urlString);
            final int port = (url.getPort() >= 0) ? url.getPort() : 80;
            socket.connect(new InetSocketAddress(url.getHost(), port), (int) Constants.SECOND_IN_MILLIS * 5);
            return true;
        } catch (final IOException e) {
            return false; // Either timeout or unreachable or failed DNS lookup.
        }
    }

    public static String replaceAll(final String template, final Map<String, String> values) {
        String result = template;
        for (final Map.Entry<String, String> e : values.entrySet()) {
            result = result.replace(e.getKey(), e.getValue());
        }
        return result;
    }

    public static CharSequence trim(final CharSequence sequence) {
        if (sequence == null) {
            return null;
        }

        return StringUtils.trim(sequence.toString());
    }

    public static String toCSVString(final String text) {
        if (StringUtils.isBlank(text)) {
            return StringUtils.EMPTY;
        }
        return Constants.DOUBLE_QUOTE +
                text
                        .replace("\r\n", "\n")
                        .replace("\"", "\"\"")
                + Constants.DOUBLE_QUOTE;
    }

    public static String getOrEmptyDisplayValue(final String value) {
        if (StringUtils.isBlank(value)) {
            return Constants.EMPTY_NOTE;
        }
        return value;
    }

    public static StringBuilder formatStackTracePrint(final int length, final StackTraceElement[] stackTrace) {
        final int size = Math.min(stackTrace.length, length);
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < size; i++) {
            builder.append("  --> ").append(stackTrace[i].toString());
            if (i + 1 < size) {
                builder.append(Constants.CARRIAGE_RETURN);
            }
        }
        return builder;
    }

    public static Map<String, String> jsonToMap(final String attribute, final JSONMapper mapper) {
        if (StringUtils.isBlank(attribute)) {
            return Collections.emptyMap();
        }
        try {
            return mapper.readValue(attribute, new TypeReference<Map<String, String>>() {
            });
        } catch (final Exception e) {
            log.error("Failed to parse json to map: ", e);
            return Collections.emptyMap();
        }
    }

}
