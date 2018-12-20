/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class Utils {

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

    public static <T extends Enum<T>> Collection<Tuple<String>> createSelectionResource(final Class<T> enumClass) {
        return Collections.unmodifiableCollection(Arrays.asList(
                enumClass.getEnumConstants())
                .stream()
                .map(e -> new Tuple<>(e.name(), e.name()))
                .collect(Collectors.toList()));
    }

    public static String toSQLWildcard(final String text) {
        return (text == null) ? null : "%" + text + "%";
    }

}
