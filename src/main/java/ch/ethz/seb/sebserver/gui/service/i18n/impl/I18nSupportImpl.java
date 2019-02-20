/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.i18n.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

import org.eclipse.rap.rwt.RWT;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser;

@Lazy
@Component
public class I18nSupportImpl implements I18nSupport {

    private static final Logger log = LoggerFactory.getLogger(I18nSupportImpl.class);

    public static final String EMPTY_DISPLAY_VALUE = "--";
    private static final String ATTR_CURRENT_SESSION_LOCALE = "CURRENT_SESSION_LOCALE";

    private final DateTimeFormatter displayDateFormatter;
    private final CurrentUser currentUser;
    private final MessageSource messageSource;
    private final Locale defaultLocale = Locale.ENGLISH;

    public I18nSupportImpl(
            final CurrentUser currentUser,
            final MessageSource messageSource,
            @Value("${sebserver.gui.date.displayformat}") final String displayDateFormat) {

        this.currentUser = currentUser;
        this.messageSource = messageSource;
        this.displayDateFormatter = DateTimeFormat
                .forPattern(displayDateFormat)
                .withZoneUTC();
    }

    private static final Collection<Locale> SUPPORTED_LANGUAGES = Arrays.asList(Locale.ENGLISH, Locale.GERMAN);

    @Override
    public Collection<Locale> supportedLanguages() {
        return SUPPORTED_LANGUAGES;
    }

    @Override
    public void setSessionLocale(final Locale locale) {
        try {
            RWT.getUISession()
                    .getHttpSession()
                    .setAttribute(ATTR_CURRENT_SESSION_LOCALE, locale);
            RWT.setLocale(locale);
        } catch (final IllegalStateException e) {
            log.error("Set current locale for session failed: ", e);
        }
    }

    @Override
    public Locale getCurrentLocale() {
        // first session-locale if available
        try {
            final Locale sessionLocale = (Locale) RWT.getUISession()
                    .getHttpSession()
                    .getAttribute(ATTR_CURRENT_SESSION_LOCALE);
            if (sessionLocale != null) {
                return sessionLocale;
            }
        } catch (final IllegalStateException e) {
            log.warn("Get current locale for session failed: {}", e.getMessage());
        }

        // second user-locale if available
        if (this.currentUser.isAvailable()) {
            return this.currentUser.get().locale;
        }

        // last the default locale
        return this.defaultLocale;
    }

    @Override
    public String formatDisplayDate(final DateTime date) {
        if (date == null) {
            return EMPTY_DISPLAY_VALUE;
        }
        return date.toString(this.displayDateFormatter);
    }

    @Override
    public String getText(final LocTextKey key) {
        return getText(key.name, key.args);
    }

    @Override
    public String getText(final String key, final Object... args) {
        return getText(key, key, args);
    }

    @Override
    public String getText(final String key, final String def, final Object... args) {
        return this.messageSource.getMessage(key, args, def, this.getCurrentLocale());
    }

    @Override
    public String getText(final String key, final Locale locale, final Object... args) {
        return getText(key, locale, key, args);
    }

    @Override
    public String getText(final String key, final Locale locale, final String def, final Object... args) {
        return this.messageSource.getMessage(key, args, def, locale);
    }

}
