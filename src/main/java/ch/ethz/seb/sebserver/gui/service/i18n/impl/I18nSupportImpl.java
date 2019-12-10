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
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.rap.rwt.RWT;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;
import ch.ethz.seb.sebserver.gbl.profile.GuiProfile;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.remote.webservice.auth.CurrentUser;

@Lazy
@Service
@GuiProfile
public class I18nSupportImpl implements I18nSupport {

    private static final Logger log = LoggerFactory.getLogger(I18nSupportImpl.class);

    private static final String SUPPORTED_LANGUAGES_KEY = "sebserver.gui.supported.languages";
    private static final String MULTILINGUAL_KEY = "sebserver.gui.multilingual";
    private static final String TIME_DISPLAYFORMAT_KEY = "sebserver.gui.time.displayformat";
    private static final String DATETIME_DISPLAYFORMAT_KEY = "sebserver.gui.datetime.displayformat";
    private static final String DATE_DISPLAYFORMAT_KEY = "sebserver.gui.date.displayformat";
    private static final String DATE_DISPLAYFORMAT_TIMEZONE_KEY = "sebserver.gui.date.displayformat.timezone";
    private static final String ATTR_CURRENT_SESSION_LOCALE = "CURRENT_SESSION_LOCALE";

    private final DateTimeFormatter timeZoneFormatter;
    private final DateTimeFormatter displayDateFormatter;
    private final DateTimeFormatter displayDateTimeFormatter;
    private final DateTimeFormatter displayTimeFormatter;
    private final CurrentUser currentUser;
    private final MessageSource messageSource;
    private final Locale defaultLocale = Locale.ENGLISH;
    private final Collection<Locale> supportedLanguages;

    public I18nSupportImpl(
            final CurrentUser currentUser,
            final MessageSource messageSource,
            final Environment environment) {

        this.currentUser = currentUser;
        this.messageSource = messageSource;

        this.timeZoneFormatter = DateTimeFormat
                .forPattern(environment.getProperty(
                        DATE_DISPLAYFORMAT_TIMEZONE_KEY,
                        Constants.TIME_ZONE_OFFSET_TAIL_FORMAT));

        this.displayDateFormatter = DateTimeFormat
                .forPattern(environment.getProperty(
                        DATE_DISPLAYFORMAT_KEY,
                        Constants.DEFAULT_DISPLAY_DATE_FORMAT))
                .withZoneUTC();

        this.displayDateTimeFormatter = DateTimeFormat
                .forPattern(environment.getProperty(
                        DATETIME_DISPLAYFORMAT_KEY,
                        Constants.DEFAULT_DIPLAY_DATE_TIME_FORMAT))
                .withZoneUTC();

        this.displayTimeFormatter = DateTimeFormat
                .forPattern(environment.getProperty(
                        TIME_DISPLAYFORMAT_KEY,
                        Constants.DEFAULT_TIME_FORMAT))
                .withZoneUTC();

        final boolean multilingual = BooleanUtils.toBoolean(environment.getProperty(
                MULTILINGUAL_KEY,
                Constants.FALSE_STRING));
        if (multilingual) {
            final String languagesString = environment.getProperty(
                    SUPPORTED_LANGUAGES_KEY,
                    Locale.ENGLISH.getLanguage());

            this.supportedLanguages = Utils.immutableCollectionOf(
                    Arrays.asList(StringUtils.split(languagesString, Constants.LIST_SEPARATOR))
                            .stream()
                            .map(s -> Locale.forLanguageTag(s))
                            .collect(Collectors.toList()));

        } else {
            this.supportedLanguages = Utils.immutableCollectionOf(Locale.ENGLISH);
        }

    }

    @Override
    public Collection<Locale> supportedLanguages() {
        return this.supportedLanguages;
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
            return this.currentUser.get().language;
        }

        // last the default locale
        return this.defaultLocale;
    }

    @Override
    public String formatDisplayDate(final DateTime date) {
        return formatDisplayDate(date, this.displayDateFormatter);
    }

    @Override
    public String formatDisplayDateTime(final DateTime date) {
        return formatDisplayDate(date, this.displayDateTimeFormatter);
    }

    @Override
    public String formatDisplayTime(final DateTime date) {
        return formatDisplayDate(date, this.displayTimeFormatter);
    }

    @Override
    public String getUsersTimeZoneTitleSuffix() {
        final UserInfo userInfo = this.currentUser.get();
        if (userInfo.timeZone == null || userInfo.timeZone.equals(DateTimeZone.UTC)) {
            return "";
        } else {
            return "(UTC|" + this.currentUser.get().timeZone.getID() + ")";
        }
    }

    @Override
    public String getText(final String key, final String def, final Object... args) {
        return this.messageSource.getMessage(key, args, def, this.getCurrentLocale());
    }

    @Override
    public String getText(final String key, final Locale locale, final String def, final Object... args) {
        return this.messageSource.getMessage(key, args, def, locale);
    }

    @Override
    public boolean hasText(final LocTextKey key) {
        if (key == null) {
            return false;
        }

        return getText(key.name, (String) null) != null;
    }

    private String formatDisplayDate(final DateTime date, final DateTimeFormatter formatter) {
        if (date == null) {
            return Constants.EMPTY_NOTE;
        }

        DateTime dateUTC = date;
        if (date.getZone() != DateTimeZone.UTC) {
            log.warn("Date that has not UTC timezone used. "
                    + "Reset to UTC timezone with any change on time instance for further processing");
            dateUTC = date.withZone(DateTimeZone.UTC);
        }

        final String dateTimeStringUTC = dateUTC.toString(formatter);
        final UserInfo userInfo = this.currentUser.get();
        if (userInfo != null && userInfo.timeZone != null && !userInfo.timeZone.equals(DateTimeZone.UTC)) {

            return dateTimeStringUTC + dateUTC
                    .withZone(userInfo.timeZone)
                    .toString(this.timeZoneFormatter);
        } else {
            return dateTimeStringUTC;
        }
    }

}
