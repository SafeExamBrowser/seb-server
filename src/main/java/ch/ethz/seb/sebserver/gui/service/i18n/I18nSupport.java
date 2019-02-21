/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.i18n;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import ch.ethz.seb.sebserver.gbl.util.Tuple;

public interface I18nSupport {

    /** Get all supported languages as a collection of Locale
     *
     * @return all supported languages as a collection of Locale */
    Collection<Locale> supportedLanguages();

    /** Get the current Locale either form a user if this is called from a logged in user context or the
     * applications default locale.
     *
     * @return the current Locale to use in context */
    Locale getCurrentLocale();

    void setSessionLocale(Locale locale);

    /** Format a DateTime to a text format to display.
     *
     * @param date
     * @return */
    String formatDisplayDate(DateTime date);

    /** Get localized text of specified key for currently set Locale.
     *
     * @param key the key name of localized text
     * @param args additional arguments to parse the localized text
     * @return the text in current language parsed from localized text */
    String getText(String key, Object... args);

    /** Get localized text of specified key for currently set Locale.
     *
     * @param key LocTextKey instance
     * @return the text in current language parsed from localized text */
    String getText(LocTextKey key);

    /** Get localized text of specified key for currently set Locale.
     *
     * @param key the key name of localized text
     * @param def default text that is returned if no localized test with specified key was found
     * @param args additional arguments to parse the localized text
     * @return the text in current language parsed from localized text */
    String getText(final String key, String def, Object... args);

    /** Get localized text of specified key and Locale.
     *
     * @param key the key name of localized text
     * @param locale the Locale
     * @param args additional arguments to parse the localized text
     * @return the text in current language parsed from localized text */
    String getText(String key, Locale locale, Object... args);

    /** Get localized text of specified key and Locale.
     *
     * @param key the key name of localized text
     * @param locale the Locale
     * @param def default text that is returned if no localized test with specified key was found
     * @param args additional arguments to parse the localized text
     * @return the text in current language parsed from localized text */
    String getText(String key, Locale locale, String def, Object... args);

    default List<Tuple<String>> getLanguageResources() {
        return getLanguageResources(this);
    }

    default List<Tuple<String>> getTimeZoneResources() {
        return getTimeZoneResources(this);
    }

    /** Get a list of language key/name tuples for all supported languages in the
     * language of the current users locale.
     *
     * @param i18nSupport I18nSupport to get the actual current users locale
     * @return list of language key/name tuples for all supported languages in the language of the current users
     *         locale */
    static List<Tuple<String>> getLanguageResources(final I18nSupport i18nSupport) {
        final Locale currentLocale = i18nSupport.getCurrentLocale();
        return i18nSupport.supportedLanguages()
                .stream()
                .map(locale -> new Tuple<>(locale.toLanguageTag(), locale.getDisplayLanguage(currentLocale)))
                .filter(tuple -> StringUtils.isNoneBlank(tuple._2))
                .sorted((t1, t2) -> t1._2.compareTo(t2._2))
                .collect(Collectors.toList());
    }

    static List<Tuple<String>> getTimeZoneResources(final I18nSupport i18nSupport) {
        final Locale currentLocale = i18nSupport.getCurrentLocale();
        return DateTimeZone
                .getAvailableIDs()
                .stream()
                .map(id -> new Tuple<>(id, DateTimeZone.forID(id).getName(0, currentLocale) + " (" + id + ")"))
                .sorted((t1, t2) -> t1._2.compareTo(t2._2))
                .collect(Collectors.toList());
    }

}
