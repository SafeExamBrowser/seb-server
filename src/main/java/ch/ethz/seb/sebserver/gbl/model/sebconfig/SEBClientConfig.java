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

public final class SEBClientConfig implements GrantEntity, Activatable {

    public static final String ATTR_CONFIG_PURPOSE = "sebConfigPurpose";
    public static final String ATTR_PING_INTERVAL = "sebServerPingTime";
    public static final String ATTR_VDI_TYPE = "vdiSetup";
    public static final String ATTR_VDI_EXECUTABLE = "vdiExecutable";
    public static final String ATTR_VDI_PATH = "vdiPath";
    public static final String ATTR_VDI_ARGUMENTS = "vdiArguments";

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
    public static final String ATTR_ENCRYPT_CERTIFICATE_ALIAS = "cert_alias";
    public static final String ATTR_ENCRYPT_CERTIFICATE_ASYM = "cert_encryption_asym";

    public static final String FILTER_ATTR_CREATION_DATE = "creation_date";

    public enum ConfigPurpose {
        START_EXAM,
        CONFIGURE_CLIENT
    }

    public enum VDIType {
        NO,
        VM_WARE(
                "VMware View",
                "vmware-view.exe",
                "VMware\\VMware Horizon View Client",
                "--LoginAsCurrentUser true\n--desktopLayout fullscreen\n--desktopProtocol PCOIP");

        public final String title;
        public final String defaultExecutable;
        public final String defaultPath;
        public final String defaultArguments;

        private VDIType() {
            this.title = "NONE";
            this.defaultExecutable = null;
            this.defaultPath = null;
            this.defaultArguments = null;
        }

        private VDIType(
                final String title,
                final String defaultExecutable,
                final String defaultPath,
                final String defaultArguments) {

            this.title = title;
            this.defaultExecutable = defaultExecutable;
            this.defaultPath = defaultPath;
            this.defaultArguments = defaultArguments;
        }

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

    @JsonProperty(ATTR_PING_INTERVAL)
    public final Long sebServerPingTime;

    @JsonProperty(ATTR_VDI_TYPE)
    public final VDIType vdiType;

    @JsonProperty(ATTR_VDI_EXECUTABLE)
    public final String vdiExecutable;

    @JsonProperty(ATTR_VDI_PATH)
    public final String vdiPath;

    @JsonProperty(ATTR_VDI_ARGUMENTS)
    public final String vdiArguments;

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

    @JsonProperty(ATTR_ENCRYPT_CERTIFICATE_ALIAS)
    public final String encryptCertificateAlias;

    @JsonProperty(ATTR_ENCRYPT_CERTIFICATE_ASYM)
    public final Boolean encryptCertificateAsym;

    @JsonProperty(SEB_CLIENT_CONFIGURATION.ATTR_ACTIVE)
    public final Boolean active;

    @JsonCreator
    public SEBClientConfig(
            @JsonProperty(SEB_CLIENT_CONFIGURATION.ATTR_ID) final Long id,
            @JsonProperty(SEB_CLIENT_CONFIGURATION.ATTR_INSTITUTION_ID) final Long institutionId,
            @JsonProperty(SEB_CLIENT_CONFIGURATION.ATTR_NAME) final String name,
            @JsonProperty(ATTR_CONFIG_PURPOSE) final ConfigPurpose configPurpose,

            @JsonProperty(ATTR_PING_INTERVAL) final Long sebServerPingTime,
            @JsonProperty(ATTR_VDI_TYPE) final VDIType vdiType,
            @JsonProperty(ATTR_VDI_EXECUTABLE) final String vdiExecutable,
            @JsonProperty(ATTR_VDI_PATH) final String vdiPath,
            @JsonProperty(ATTR_VDI_ARGUMENTS) final String vdiArguments,

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
            @JsonProperty(ATTR_ENCRYPT_CERTIFICATE_ALIAS) final String encryptCertificateAlias,
            @JsonProperty(ATTR_ENCRYPT_CERTIFICATE_ASYM) final Boolean encryptCertificateAsym,
            @JsonProperty(SEB_CLIENT_CONFIGURATION.ATTR_ACTIVE) final Boolean active) {

        this.id = id;
        this.institutionId = institutionId;
        this.name = name;
        this.configPurpose = configPurpose;

        this.sebServerPingTime = sebServerPingTime;
        this.vdiType = vdiType != null ? vdiType : VDIType.NO;
        this.vdiExecutable = vdiExecutable != null
                ? vdiExecutable
                : vdiType != null ? vdiType.defaultExecutable : null;
        this.vdiPath = vdiPath != null
                ? vdiPath
                : vdiType != null ? vdiType.defaultPath : null;
        this.vdiArguments = vdiArguments != null
                ? vdiArguments
                : vdiType != null ? vdiType.defaultArguments : null;

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
        this.encryptCertificateAlias = encryptCertificateAlias;
        this.encryptCertificateAsym = encryptCertificateAsym;
        this.active = active;
    }

    public SEBClientConfig(final Long institutionId, final POSTMapper postParams) {
        this.id = null;
        this.institutionId = institutionId;
        this.name = postParams.getString(Domain.SEB_CLIENT_CONFIGURATION.ATTR_NAME);
        this.configPurpose = postParams.getEnum(ATTR_CONFIG_PURPOSE, ConfigPurpose.class);

        this.sebServerPingTime = postParams.getLong(ATTR_PING_INTERVAL) != null
                ? postParams.getLong(ATTR_PING_INTERVAL)
                : 1000;
        this.vdiType = postParams.getEnum(ATTR_VDI_TYPE, VDIType.class) != null
                ? postParams.getEnum(ATTR_VDI_TYPE, VDIType.class)
                : VDIType.NO;
        this.vdiExecutable = postParams.getString(ATTR_VDI_EXECUTABLE) != null
                ? postParams.getString(ATTR_VDI_EXECUTABLE)
                : this.vdiType.defaultExecutable;
        this.vdiPath = postParams.getString(ATTR_VDI_PATH) != null
                ? postParams.getString(ATTR_VDI_PATH)
                : this.vdiType.defaultPath;
        this.vdiArguments = postParams.getString(ATTR_VDI_ARGUMENTS) != null
                ? postParams.getString(ATTR_VDI_ARGUMENTS)
                : this.vdiType.defaultArguments;

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
        this.encryptCertificateAlias = postParams.getString(ATTR_ENCRYPT_CERTIFICATE_ALIAS);
        this.encryptCertificateAsym = postParams.getBooleanObject(ATTR_ENCRYPT_CERTIFICATE_ASYM);
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
        return this.configPurpose;
    }

    public Boolean getFallback() {
        return this.fallback;
    }

    public Long getFallbackTimeout() {
        return this.fallbackTimeout;
    }

    public Short getFallbackAttempts() {
        return this.fallbackAttempts;
    }

    public Short getFallbackAttemptInterval() {
        return this.fallbackAttemptInterval;
    }

    public CharSequence getFallbackPassword() {
        return this.fallbackPassword;
    }

    @JsonIgnore
    public CharSequence getFallbackPasswordConfirm() {
        return this.fallbackPasswordConfirm;
    }

    public CharSequence getQuitPassword() {
        return this.quitPassword;
    }

    @JsonIgnore
    public CharSequence getQuitPasswordConfirm() {
        return this.quitPasswordConfirm;
    }

    public DateTime getDate() {
        return this.date;
    }

    public CharSequence getEncryptSecret() {
        return this.encryptSecret;
    }

    public String getEncryptCertificateAlias() {
        return this.encryptCertificateAlias;
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
        final StringBuilder builder = new StringBuilder();
        builder.append("SEBClientConfig [id=");
        builder.append(this.id);
        builder.append(", institutionId=");
        builder.append(this.institutionId);
        builder.append(", name=");
        builder.append(this.name);
        builder.append(", configPurpose=");
        builder.append(this.configPurpose);
        builder.append(", sebServerPingTime=");
        builder.append(this.sebServerPingTime);
        builder.append(", vdiType=");
        builder.append(this.vdiType);
        builder.append(", vdiExecutable=");
        builder.append(this.vdiExecutable);
        builder.append(", vdiPath=");
        builder.append(this.vdiPath);
        builder.append(", vdiArguments=");
        builder.append(this.vdiArguments);
        builder.append(", fallback=");
        builder.append(this.fallback);
        builder.append(", fallbackStartURL=");
        builder.append(this.fallbackStartURL);
        builder.append(", fallbackTimeout=");
        builder.append(this.fallbackTimeout);
        builder.append(", fallbackAttempts=");
        builder.append(this.fallbackAttempts);
        builder.append(", fallbackAttemptInterval=");
        builder.append(this.fallbackAttemptInterval);
        builder.append(", fallbackPassword=");
        builder.append(this.fallbackPassword);
        builder.append(", fallbackPasswordConfirm=");
        builder.append(this.fallbackPasswordConfirm);
        builder.append(", quitPassword=");
        builder.append(this.quitPassword);
        builder.append(", quitPasswordConfirm=");
        builder.append(this.quitPasswordConfirm);
        builder.append(", date=");
        builder.append(this.date);
        builder.append(", encryptSecret=");
        builder.append(this.encryptSecret);
        builder.append(", encryptSecretConfirm=");
        builder.append(this.encryptSecretConfirm);
        builder.append(", active=");
        builder.append(this.active);
        builder.append("]");
        return builder.toString();
    }

    @Override
    public Entity printSecureCopy() {
        return new SEBClientConfig(
                this.id,
                this.institutionId,
                this.name,
                this.configPurpose,
                this.sebServerPingTime,
                this.vdiType,
                this.vdiExecutable,
                this.vdiPath,
                this.vdiArguments,
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
                Constants.EMPTY_NOTE,
                this.encryptCertificateAsym,
                this.active);
    }

    public static SEBClientConfig createNew(final Long institutionId) {
        return new SEBClientConfig(
                null,
                institutionId,
                null,
                ConfigPurpose.CONFIGURE_CLIENT,
                1000L,
                VDIType.NO,
                null,
                null,
                null,
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
                null,
                false,
                false);
    }

}
