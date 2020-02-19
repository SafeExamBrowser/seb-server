/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.sebconfig;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.URL;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.POSTMapper;
import ch.ethz.seb.sebserver.gbl.model.Activatable;
import ch.ethz.seb.sebserver.gbl.model.Domain;
import ch.ethz.seb.sebserver.gbl.model.Domain.SEB_CLIENT_CONFIGURATION;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.GrantEntity;

public final class SebClientConfig implements GrantEntity, Activatable {

    public static final String ATTR_CONFIG_PURPOSE = "sebConfigPurpose";
    public static final String ATTR_FALLBACK = "sebServerFallback ";
    public static final String ATTR_FALLBACK_START_URL = "startURL";
    public static final String ATTR_FALLBACK_TIMEOUT = "sebServerFallbackTimeout";
    public static final String ATTR_FALLBACK_ATTEMPTS = "sebServerFallbackAttempts";
    public static final String ATTR_FALLBACK_ATTEMPT_INTERVAL = "sebServerFallbackAttemptInterval";
    public static final String ATTR_FALLBACK_PASSWORD = "sebServerFallbackPasswordHash";
    public static final String ATTR_FALLBACK_PASSWORD_CONFIRM = "sebServerFallbackPasswordHashConfirm";
    public static final String ATTR_QUIT_PASSWORD = "hashedQuitPassword";
    public static final String ATTR_QUIT_PASSWORD_CONFIRM = "hashedQuitPasswordConfirm";
    public static final String ATTR_ENCRYPT_SECRET_CONFIRM = "confirm_encrypt_secret";

    public static final String FILTER_ATTR_CREATION_DATE = "creation_date";

    public enum ConfigPurpose {
        START_EXAM,
        CONFIGURE_CLIENT
    }

    @JsonProperty(SEB_CLIENT_CONFIGURATION.ATTR_ID)
    public final Long id;

    @NotNull
    @JsonProperty(SEB_CLIENT_CONFIGURATION.ATTR_INSTITUTION_ID)
    public final Long institutionId;

    @NotNull(message = "clientconfig:name:notNull")
    @Size(min = 3, max = 255, message = "clientconfig:name:size:{min}:{max}:${validatedValue}")
    @JsonProperty(SEB_CLIENT_CONFIGURATION.ATTR_NAME)
    public final String name;

    @NotNull(message = "clientconfig:sebConfigPurpose:notNull")
    @JsonProperty(ATTR_CONFIG_PURPOSE)
    public final ConfigPurpose configPurpose;

    @JsonProperty(ATTR_FALLBACK)
    public final Boolean fallback;

    @JsonProperty(ATTR_FALLBACK_START_URL)
    @URL(message = "clientconfig:startURL:invalidURL")
    public final String fallbackStartURL;

    @JsonProperty(ATTR_FALLBACK_TIMEOUT)
    public final Long fallbackTimeout;

    @JsonProperty(ATTR_FALLBACK_ATTEMPTS)
    public final Short fallbackAttempts;

    @JsonProperty(ATTR_FALLBACK_ATTEMPT_INTERVAL)
    public final Short fallbackAttemptInterval;

    @JsonProperty(ATTR_FALLBACK_PASSWORD)
    public final CharSequence fallbackPassword;

    @JsonProperty(ATTR_FALLBACK_PASSWORD_CONFIRM)
    public final CharSequence fallbackPasswordConfirm;

    @JsonProperty(ATTR_QUIT_PASSWORD)
    public final CharSequence quitPassword;

    @JsonProperty(ATTR_QUIT_PASSWORD_CONFIRM)
    public final CharSequence quitPasswordConfirm;

    @JsonProperty(SEB_CLIENT_CONFIGURATION.ATTR_DATE)
    public final DateTime date;

    @JsonProperty(SEB_CLIENT_CONFIGURATION.ATTR_ENCRYPT_SECRET)
    public final CharSequence encryptSecret;

    @JsonProperty(ATTR_ENCRYPT_SECRET_CONFIRM)
    public final CharSequence encryptSecretConfirm;

    @JsonProperty(SEB_CLIENT_CONFIGURATION.ATTR_ACTIVE)
    public final Boolean active;

    @JsonCreator
    public SebClientConfig(
            @JsonProperty(SEB_CLIENT_CONFIGURATION.ATTR_ID) final Long id,
            @JsonProperty(SEB_CLIENT_CONFIGURATION.ATTR_INSTITUTION_ID) final Long institutionId,
            @JsonProperty(SEB_CLIENT_CONFIGURATION.ATTR_NAME) final String name,
            @JsonProperty(ATTR_CONFIG_PURPOSE) final ConfigPurpose configPurpose,
            @JsonProperty(ATTR_FALLBACK) final Boolean fallback,
            @JsonProperty(ATTR_FALLBACK_START_URL) final String fallbackStartURL,
            @JsonProperty(ATTR_FALLBACK_TIMEOUT) final Long fallbackTimeout,
            @JsonProperty(ATTR_FALLBACK_ATTEMPTS) final Short fallbackAttempts,
            @JsonProperty(ATTR_FALLBACK_ATTEMPT_INTERVAL) final Short fallbackAttemptInterval,
            @JsonProperty(ATTR_FALLBACK_PASSWORD) final CharSequence fallbackPassword,
            @JsonProperty(ATTR_FALLBACK_PASSWORD_CONFIRM) final CharSequence fallbackPasswordConfirm,
            @JsonProperty(ATTR_QUIT_PASSWORD) final CharSequence quitPassword,
            @JsonProperty(ATTR_QUIT_PASSWORD_CONFIRM) final CharSequence quitPasswordConfirm,
            @JsonProperty(SEB_CLIENT_CONFIGURATION.ATTR_DATE) final DateTime date,
            @JsonProperty(SEB_CLIENT_CONFIGURATION.ATTR_ENCRYPT_SECRET) final CharSequence encryptSecret,
            @JsonProperty(ATTR_ENCRYPT_SECRET_CONFIRM) final CharSequence encryptSecretConfirm,
            @JsonProperty(SEB_CLIENT_CONFIGURATION.ATTR_ACTIVE) final Boolean active) {

        this.id = id;
        this.institutionId = institutionId;
        this.name = name;
        this.configPurpose = configPurpose;
        this.fallback = fallback;
        this.fallbackStartURL = fallbackStartURL;
        this.fallbackTimeout = fallbackTimeout;
        this.fallbackAttempts = fallbackAttempts;
        this.fallbackAttemptInterval = fallbackAttemptInterval;
        this.fallbackPassword = fallbackPassword;
        this.fallbackPasswordConfirm = fallbackPasswordConfirm;
        this.quitPassword = quitPassword;
        this.quitPasswordConfirm = quitPasswordConfirm;
        this.date = date;
        this.encryptSecret = encryptSecret;
        this.encryptSecretConfirm = encryptSecretConfirm;
        this.active = active;
    }

    public SebClientConfig(final Long institutionId, final POSTMapper postParams) {
        this.id = null;
        this.institutionId = institutionId;
        this.name = postParams.getString(Domain.SEB_CLIENT_CONFIGURATION.ATTR_NAME);
        this.configPurpose = postParams.getEnum(ATTR_CONFIG_PURPOSE, ConfigPurpose.class);
        this.fallback = postParams.getBoolean(ATTR_FALLBACK);
        this.fallbackStartURL = postParams.getString(ATTR_FALLBACK_START_URL);
        this.fallbackTimeout = postParams.getLong(ATTR_FALLBACK_TIMEOUT);
        this.fallbackAttempts = postParams.getShort(ATTR_FALLBACK_ATTEMPTS);
        this.fallbackAttemptInterval = postParams.getShort(ATTR_FALLBACK_ATTEMPT_INTERVAL);
        this.fallbackPassword = postParams.getCharSequence(ATTR_FALLBACK_PASSWORD);
        this.fallbackPasswordConfirm = postParams.getCharSequence(ATTR_FALLBACK_PASSWORD_CONFIRM);
        this.quitPassword = postParams.getCharSequence(ATTR_QUIT_PASSWORD);
        this.quitPasswordConfirm = postParams.getCharSequence(ATTR_QUIT_PASSWORD_CONFIRM);
        this.date = postParams.getDateTime(Domain.SEB_CLIENT_CONFIGURATION.ATTR_DATE);
        this.encryptSecret = postParams.getCharSequence(Domain.SEB_CLIENT_CONFIGURATION.ATTR_ENCRYPT_SECRET);
        this.encryptSecretConfirm = postParams.getCharSequence(ATTR_ENCRYPT_SECRET_CONFIRM);
        this.active = false;
    }

    @Override
    public EntityType entityType() {
        return EntityType.SEB_CLIENT_CONFIGURATION;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public String getFallbackStartURL() {
        return this.fallbackStartURL;
    }

    @Override
    public String getModelId() {
        return (this.id != null)
                ? String.valueOf(this.id)
                : null;
    }

    @Override
    public boolean isActive() {
        return this.active;
    }

    @Override
    public Long getInstitutionId() {
        return this.institutionId;
    }

    public Long getId() {
        return this.id;
    }

    public ConfigPurpose getConfigPurpose() {
        return configPurpose;
    }

    public Boolean getFallback() {
        return fallback;
    }

    public Long getFallbackTimeout() {
        return fallbackTimeout;
    }

    public Short getFallbackAttempts() {
        return fallbackAttempts;
    }

    public Short getFallbackAttemptInterval() {
        return fallbackAttemptInterval;
    }

    public CharSequence getFallbackPassword() {
        return fallbackPassword;
    }

    @JsonIgnore
    public CharSequence getFallbackPasswordConfirm() {
        return fallbackPasswordConfirm;
    }

    public CharSequence getQuitPassword() {
        return quitPassword;
    }

    @JsonIgnore
    public CharSequence getQuitPasswordConfirm() {
        return quitPasswordConfirm;
    }

    public DateTime getDate() {
        return this.date;
    }

    public CharSequence getEncryptSecret() {
        return this.encryptSecret;
    }

    @JsonIgnore
    public CharSequence getEncryptSecretConfirm() {
        return this.encryptSecretConfirm;
    }

    @JsonIgnore
    public boolean hasEncryptionSecret() {
        return this.encryptSecret != null && this.encryptSecret.length() > 0;
    }

    @JsonIgnore
    public boolean hasFallbackPassword() {
        return this.fallbackPassword != null && this.fallbackPassword.length() > 0;
    }

    @JsonIgnore
    public boolean hasQuitPassword() {
        return this.quitPassword != null && this.quitPassword.length() > 0;
    }

    public Boolean getActive() {
        return this.active;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SebClientConfig{");
        sb.append("id=").append(id);
        sb.append(", institutionId=").append(institutionId);
        sb.append(", name='").append(name).append('\'');
        sb.append(", configPurpose=").append(configPurpose);
        sb.append(", fallback=").append(fallback);
        sb.append(", fallbackStartURL='").append(fallbackStartURL).append('\'');
        sb.append(", fallbackTimeout=").append(fallbackTimeout);
        sb.append(", fallbackAttempts=").append(fallbackAttempts);
        sb.append(", fallbackAttemptInterval=").append(fallbackAttemptInterval);
        sb.append(", fallbackPassword=").append(fallbackPassword);
        sb.append(", fallbackPasswordConfirm=").append(fallbackPasswordConfirm);
        sb.append(", date=").append(date);
        sb.append(", encryptSecret=").append(encryptSecret);
        sb.append(", encryptSecretConfirm=").append(encryptSecretConfirm);
        sb.append(", active=").append(active);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public Entity printSecureCopy() {
        return new SebClientConfig(
                this.id,
                this.institutionId,
                this.name,
                this.configPurpose,
                this.fallback,
                this.fallbackStartURL,
                this.fallbackTimeout,
                this.fallbackAttempts,
                this.fallbackAttemptInterval,
                Constants.EMPTY_NOTE,
                Constants.EMPTY_NOTE,
                Constants.EMPTY_NOTE,
                Constants.EMPTY_NOTE,
                this.date,
                Constants.EMPTY_NOTE,
                Constants.EMPTY_NOTE,
                this.active);
    }

    public static SebClientConfig createNew(final Long institutionId) {
        return new SebClientConfig(
                null,
                institutionId,
                null,
                ConfigPurpose.START_EXAM,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                DateTime.now(DateTimeZone.UTC),
                null,
                null,
                false);
    }

}
