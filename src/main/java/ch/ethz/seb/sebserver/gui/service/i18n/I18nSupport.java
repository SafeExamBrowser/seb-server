/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.i18n;

import java.util.Collection;
import java.util.Locale;

import org.joda.time.DateTime;

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
     * @param addTimeZone indicates whether the time zone shall be added or not
     * @return */
    String formatDisplayDate(DateTime date);

    /** If the current user has another time zone then UTC this will return a tile suffix that describes
     * a date/time column title with adding (UTC|{usersTimeZone}) that can be added to the title.
     *
     * @return date/time column title suffix for current user */
    String getUsersTimeZoneTitleSuffix();

    /** Get localized text of specified key for currently set Locale.
     *
     * @param key LocTextKey instance
     * @param def default text
     * @return the text in current language parsed from localized text */
    default String getText(final LocTextKey key, final String def) {
        return getText(key.name, def, key.args);
    }

    /** Get localized text of specified key for currently set Locale.
     *
     * @param key LocTextKey instance
     * @return the text in current language parsed from localized text */
    default String getText(final LocTextKey key) {
        return getText(key.name, "missing:" + key.name, key.args);
    }

    /** Get localized text of specified key for currently set Locale.
     *
     * @param key the key name of localized text
     * @param args additional arguments to parse the localized text
     * @return the text in current language parsed from localized text */
    default String getText(final String key, final Object... args) {
        return getText(key, key, args);
    }

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
    default String getText(final String key, final Locale locale, final Object... args) {
        return getText(key, locale, key, args);
    }

    /** Get localized text of specified key and Locale.
     *
     * @param key the key name of localized text
     * @param locale the Locale
     * @param def default text that is returned if no localized test with specified key was found
     * @param args additional arguments to parse the localized text
     * @return the text in current language parsed from localized text */
    String getText(String key, Locale locale, String def, Object... args);

    boolean hasText(LocTextKey locTooltipKey);

}
