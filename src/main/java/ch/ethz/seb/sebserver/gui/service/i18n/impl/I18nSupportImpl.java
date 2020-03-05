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

    private final Locale defaultFormatLocale;
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

        final String defaultFormatLocaleString = environment.getProperty(
                FORMAL_LOCALE_KEY,
                Constants.DEFAULT_LANG_CODE);

        this.defaultFormatLocale = Locale.forLanguageTag(defaultFormatLocaleString);

        final boolean multilingual = BooleanUtils.toBoolean(environment.getProperty(
                MULTILINGUAL_KEY,
                Constants.FALSE_STRING));
        if (multilingual) {
            final String languagesString = environment.getProperty(
                    SUPPORTED_LANGUAGES_KEY,
                    Locale.ENGLISH.getLanguage());

            this.supportedLanguages = Utils.immutableCollectionOf(
                    Arrays.stream(StringUtils.split(languagesString, Constants.LIST_SEPARATOR))
                            .map(Locale::forLanguageTag)
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
    public Locale getUsersFormatLocale() {
        // TODO here also a user based format locale can be verified on the future
        return this.defaultFormatLocale;
    }

    @Override
    public Locale getUsersLanguageLocale() {
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
        final String pattern = DateTimeFormat.patternForStyle("M-", getUsersFormatLocale());
        return formatDisplayDate(date, DateTimeFormat.forPattern(pattern));
    }

    @Override
    public String formatDisplayDateTime(final DateTime date) {
        final String pattern = DateTimeFormat.patternForStyle("MS", getUsersFormatLocale());
        return formatDisplayDate(date, DateTimeFormat.forPattern(pattern));
    }

    @Override
    public String formatDisplayTime(final DateTime date) {
        final String pattern = DateTimeFormat.patternForStyle("-S", getUsersFormatLocale());
        return formatDisplayDate(date, DateTimeFormat.forPattern(pattern));
    }

    @Override
    public String getUsersTimeZoneTitleSuffix() {
        final UserInfo userInfo = this.currentUser.get();
        if (userInfo.timeZone == null || userInfo.timeZone.equals(DateTimeZone.UTC)) {
            return "";
        } else {
            return "(" + this.currentUser.get().timeZone.getID() + ")";
        }
    }

    @Override
    public String getText(final String key, final String def, final Object... args) {
        return this.messageSource.getMessage(key, args, def, this.getUsersLanguageLocale());
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

        return StringUtils.isNotBlank(getText(key.name, (String) null));
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

        final UserInfo userInfo = this.currentUser.get();
        if (userInfo != null && userInfo.timeZone != null && !userInfo.timeZone.equals(DateTimeZone.UTC)) {
            return dateUTC.toString(formatter.withZone(userInfo.timeZone));
        } else {
            return dateUTC.toString(formatter);
        }
    }

}
