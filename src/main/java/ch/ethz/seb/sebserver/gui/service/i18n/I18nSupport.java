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

}
