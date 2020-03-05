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

import ch.ethz.seb.sebserver.gbl.util.Utils;

public interface I18nSupport {

    String SUPPORTED_LANGUAGES_KEY = "sebserver.gui.supported.languages";
    String MULTILINGUAL_KEY = "sebserver.gui.multilingual";
    String FORMAL_LOCALE_KEY = "sebserver.gui.date.displayformat";
    String ATTR_CURRENT_SESSION_LOCALE = "CURRENT_SESSION_LOCALE";

    /** Get all supported languages as a collection of Locale
     *
     * @return all supported languages as a collection of Locale */
    Collection<Locale> supportedLanguages();

    /** Get the current users language based Locale (from user info language selection)
     * Or the default language Locale if the user has not defined any language
     *
     * @return the current user language Locale to use in context */
    Locale getUsersLanguageLocale();

    /** Get the current users format based Locale (from user info format selection)
     * Or the default format Locale if the user has not defined any language
     *
     * @return the current user format Locale to use in context */
    Locale getUsersFormatLocale();

    /** Format a DateTime to a text format to display.
     * This uses the date-format defined by either the attribute 'sebserver.gui.date.display format'
     * or the Constants.DEFAULT_DISPLAY_DATE_FORMAT
     *
     * Adds time-zone offset information if the currents user time-zone is different form UTC
     *
     * @param date the DateTime instance
     * @return date formatted date String to display */
    String formatDisplayDate(DateTime date);

    /** Format a DateTime to a text format to display with additional time zone name at the end.
     * This uses the date-format defined by either the attribute 'sebserver.gui.date.display format'
     * or the Constants.DEFAULT_DISPLAY_DATE_FORMAT
     *
     * Adds time-zone offset information if the currents user time-zone is different form UTC
     *
     * @param date the DateTime instance
     * @return date formatted date String to display */
    default String formatDisplayDateWithTimeZone(final DateTime date) {
        return formatDisplayDateTime(date) + " " + this.getUsersTimeZoneTitleSuffix();
    }

    /** Format a time-stamp (milliseconds) to a text format to display.
     * This uses the date-format defined by either the attribute 'sebserver.gui.date.display format'
     * or the Constants.DEFAULT_DISPLAY_DATE_FORMAT
     *
     * Adds time-zone information if the currents user time-zone is different form UTC
     *
     * @param timestamp the unix-timestamp in milliseconds
     * @return date formatted date String to display */
    default String formatDisplayDate(final Long timestamp) {
        return formatDisplayDate(Utils.toDateTimeUTC(timestamp));
    }

    /** Format a DateTime to a text format to display.
     * This uses the date-format defined by either the attribute 'sebserver.gui.datetime.display format'
     * or the Constants.DEFAULT_DISPLAY_DATE_TIME_FORMAT
     *
     * Adds time-zone information if the currents user time-zone is different form UTC
     *
     * @param date the DateTime instance
     * @return date formatted date time String to display */
    String formatDisplayDateTime(DateTime date);

    /** Format a time-stamp (milliseconds) to a text format to display.
     * This uses the date-format defined by either the attribute 'sebserver.gui.datetime.display format'
     * or the Constants.DEFAULT_DISPLAY_DATE_TIME_FORMAT
     *
     * Adds time-zone information if the currents user time-zone is different form UTC
     *
     * @param timestamp the unix-timestamp in milliseconds
     * @return date formatted date time String to display */
    default String formatDisplayDateTime(final Long timestamp) {
        return formatDisplayDateTime(Utils.toDateTimeUTC(timestamp));
    }

    /** Format a DateTime to a text format to display.
     * This uses the date-format defined by either the attribute 'sebserver.gui.time.display format'
     * or the Constants.DEFAULT_DISPLAY_TIME_FORMAT
     *
     * Adds time-zone information if the currents user time-zone is different form UTC
     *
     * @param date the DateTime instance
     * @return date formatted time String to display */
    String formatDisplayTime(DateTime date);

    /** Format a time-stamp (milliseconds) to a text format to display.
     * This uses the date-format defined by either the attribute 'sebserver.gui.time.display format'
     * or the Constants.DEFAULT_DISPLAY_TIME_FORMAT
     *
     * Adds time-zone information if the currents user time-zone is different form UTC
     *
     * @param timestamp the unix-timestamp in milliseconds
     * @return date formatted time String to display */
    default String formatDisplayTime(final Long timestamp) {
        return formatDisplayTime(Utils.toDateTimeUTC(timestamp));
    }

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
        return getText(key.name, key.name, key.args);
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

    /** Indicates if there is a localized text defined for a specified LocTextKey
     *
     * @param locTextKey the LocTextKey instance
     * @return true if there is a localized text defined for a specified LocTextKey, false otherwise */
    boolean hasText(LocTextKey locTextKey);

}
