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

    public static final String ATTR_FALLBACK_START_URL = "fallback_start_url";
    public static final String ATTR_CONFIRM_ENCRYPT_SECRET = "confirm_encrypt_secret";

    public static final String FILTER_ATTR_CREATION_DATE = "creation_date";

    @JsonProperty(SEB_CLIENT_CONFIGURATION.ATTR_ID)
    public final Long id;

    @NotNull
    @JsonProperty(SEB_CLIENT_CONFIGURATION.ATTR_INSTITUTION_ID)
    public final Long institutionId;

    @NotNull(message = "clientconfig:name:notNull")
    @Size(min = 3, max = 255, message = "clientconfig:name:size:{min}:{max}:${validatedValue}")
    @JsonProperty(SEB_CLIENT_CONFIGURATION.ATTR_NAME)
    public final String name;

    @JsonProperty(ATTR_FALLBACK_START_URL)
    @URL(message = "clientconfig:fallback_start_url:invalidURL")
    public final String fallbackStartURL;

    @JsonProperty(SEB_CLIENT_CONFIGURATION.ATTR_DATE)
    public final DateTime date;

    @JsonProperty(SEB_CLIENT_CONFIGURATION.ATTR_ENCRYPT_SECRET)
    public final CharSequence encryptSecret;

    @JsonProperty(ATTR_CONFIRM_ENCRYPT_SECRET)
    public final CharSequence confirmEncryptSecret;

    @JsonProperty(SEB_CLIENT_CONFIGURATION.ATTR_ACTIVE)
    public final Boolean active;

    @JsonCreator
    public SebClientConfig(
            @JsonProperty(SEB_CLIENT_CONFIGURATION.ATTR_ID) final Long id,
            @JsonProperty(SEB_CLIENT_CONFIGURATION.ATTR_INSTITUTION_ID) final Long institutionId,
            @JsonProperty(SEB_CLIENT_CONFIGURATION.ATTR_NAME) final String name,
            @JsonProperty(ATTR_FALLBACK_START_URL) final String fallbackStartURL,
            @JsonProperty(SEB_CLIENT_CONFIGURATION.ATTR_DATE) final DateTime date,
            @JsonProperty(SEB_CLIENT_CONFIGURATION.ATTR_ENCRYPT_SECRET) final CharSequence encryptSecret,
            @JsonProperty(ATTR_CONFIRM_ENCRYPT_SECRET) final CharSequence confirmEncryptSecret,
            @JsonProperty(SEB_CLIENT_CONFIGURATION.ATTR_ACTIVE) final Boolean active) {

        this.id = id;
        this.institutionId = institutionId;
        this.name = name;
        this.fallbackStartURL = fallbackStartURL;
        this.date = date;
        this.encryptSecret = encryptSecret;
        this.confirmEncryptSecret = confirmEncryptSecret;
        this.active = active;
    }

    public SebClientConfig(final Long institutionId, final POSTMapper postParams) {
        this.id = null;
        this.institutionId = institutionId;
        this.name = postParams.getString(Domain.SEB_CLIENT_CONFIGURATION.ATTR_NAME);
        this.fallbackStartURL = postParams.getString(ATTR_FALLBACK_START_URL);
        this.date = postParams.getDateTime(Domain.SEB_CLIENT_CONFIGURATION.ATTR_DATE);
        this.encryptSecret = postParams.getCharSequence(Domain.SEB_CLIENT_CONFIGURATION.ATTR_ENCRYPT_SECRET);
        this.confirmEncryptSecret = postParams.getCharSequence(ATTR_CONFIRM_ENCRYPT_SECRET);
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

    public DateTime getDate() {
        return this.date;
    }

    @JsonIgnore
    public CharSequence getEncryptSecret() {
        return this.encryptSecret;
    }

    @JsonIgnore
    public CharSequence getConfirmEncryptSecret() {
        return this.confirmEncryptSecret;
    }

    @JsonIgnore
    public boolean hasEncryptionSecret() {
        return this.encryptSecret != null && this.encryptSecret.length() > 0;
    }

    public Boolean getActive() {
        return this.active;
    }

    @Override
    public Entity printSecureCopy() {
        return new SebClientConfig(
                this.id,
                this.institutionId,
                this.name,
                this.fallbackStartURL,
                this.date,
                Constants.EMPTY_NOTE,
                Constants.EMPTY_NOTE,
                this.active);
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("SebClientConfig [id=");
        builder.append(this.id);
        builder.append(", institutionId=");
        builder.append(this.institutionId);
        builder.append(", name=");
        builder.append(this.name);
        builder.append(", fallbackStartURL=");
        builder.append(this.fallbackStartURL);
        builder.append(", date=");
        builder.append(this.date);
        builder.append(", active=");
        builder.append(this.active);
        builder.append("]");
        return builder.toString();
    }

    public static final SebClientConfig createNew(final Long institutionId) {
        return new SebClientConfig(
                null,
                institutionId,
                null,
                null,
                DateTime.now(DateTimeZone.UTC),
                null,
                null,
                false);
    }

}
