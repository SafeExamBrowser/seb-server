/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.institution;

import java.util.Arrays;
import java.util.EnumSet;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.URL;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.api.POSTMapper;
import ch.ethz.seb.sebserver.gbl.model.Activatable;
import ch.ethz.seb.sebserver.gbl.model.Domain.INSTITUTION;
import ch.ethz.seb.sebserver.gbl.model.Domain.LMS_SETUP;
import ch.ethz.seb.sebserver.gbl.model.Entity;
import ch.ethz.seb.sebserver.gbl.model.EntityName;
import ch.ethz.seb.sebserver.gbl.model.GrantEntity;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class LmsSetup implements GrantEntity, Activatable {

    public static final String FILTER_ATTR_LMS_SETUP = "lms_setup";
    public static final String FILTER_ATTR_LMS_TYPE = "lms_type";

    /** LMS binding and API features */
    public enum Features {
        /** The course API allows the application to securely connect to a LMS service
         * and request course or quiz data from that LMS as well as requesting some
         * limited LMS user account data like user name or display name. */
        COURSE_API,
        /** The SEB restriction API allows the application to securely connect to a LMS service
         * and place or release SEB restrictions, for a particular course or quiz, on the LMS.
         * The SEB restriciton is usually in the form of certain hash keys and addition
         * restriction settings that prompt the LMS to check access on course/quiz connection and
         * allow only access for a dedicated SEB client with the right configuration in place. */
        SEB_RESTRICTION
    }

    /** Defines the supported types if LMS bindings.
     * Also defines the supports feature(s) for each type of LMS binding. */
    public enum LmsType {
        /** Mockup LMS type used to create test setups */
        MOCKUP(Features.COURSE_API),
        /** The Open edX LMS binding features both APIs, course access as well as SEB restriction */
        OPEN_EDX(Features.COURSE_API, Features.SEB_RESTRICTION),
        /** The Moodle binding features only the course access API so far */
        MOODLE(Features.COURSE_API /* , Features.SEB_RESTRICTION */),
        /** The Ans Delft binding is on the way */
        ANS_DELFT(Features.COURSE_API, Features.SEB_RESTRICTION),
        /** The OpenOLAT binding is on the way */
        OPEN_OLAT(Features.COURSE_API, Features.SEB_RESTRICTION);

        public final EnumSet<Features> features;

        LmsType(final Features... features) {
            if (features != null && features.length >= 1) {
                this.features = EnumSet.copyOf(Arrays.asList(features));
            } else {
                this.features = EnumSet.noneOf(Features.class);
            }
        }
    }

    @JsonProperty(LMS_SETUP.ATTR_ID)
    public final Long id;

    @NotNull
    @JsonProperty(LMS_SETUP.ATTR_INSTITUTION_ID)
    public final Long institutionId;

    @NotNull(message = "lmsSetup:name:notNull")
    @Size(min = 3, max = 255, message = "lmsSetup:name:size:{min}:{max}:${validatedValue}")
    @JsonProperty(LMS_SETUP.ATTR_NAME)
    public final String name;

    @JsonProperty(LMS_SETUP.ATTR_LMS_TYPE)
    @NotNull(message = "lmsSetup:lmsType:notNull")
    public final LmsType lmsType;

    @JsonProperty(LMS_SETUP.ATTR_LMS_CLIENTNAME)
    public final String lmsAuthName;

    @JsonProperty(LMS_SETUP.ATTR_LMS_CLIENTSECRET)
    public final String lmsAuthSecret;

    @JsonProperty(LMS_SETUP.ATTR_LMS_URL)
    @URL(message = "lmsSetup:lmsUrl:invalidURL")
    public final String lmsApiUrl;

    @JsonProperty(LMS_SETUP.ATTR_LMS_REST_API_TOKEN)
    public final String lmsRestApiToken;

    @JsonProperty(LMS_SETUP.ATTR_LMS_PROXY_HOST)
    public final String proxyHost;

    @JsonProperty(LMS_SETUP.ATTR_LMS_PROXY_PORT)
    public final Integer proxyPort;

    @JsonProperty(LMS_SETUP.ATTR_LMS_PROXY_AUTH_USERNAME)
    public final String proxyAuthUsername;

    @JsonProperty(LMS_SETUP.ATTR_LMS_PROXY_AUTH_SECRET)
    public final String proxyAuthSecret;

    /** Indicates whether this LmsSetup is active or not */
    @JsonProperty(LMS_SETUP.ATTR_ACTIVE)
    public final Boolean active;

    @JsonProperty(LMS_SETUP.ATTR_UPDATE_TIME)
    public final Long updateTime;

    @JsonCreator
    public LmsSetup(
            @JsonProperty(LMS_SETUP.ATTR_ID) final Long id,
            @JsonProperty(LMS_SETUP.ATTR_INSTITUTION_ID) final Long institutionId,
            @JsonProperty(LMS_SETUP.ATTR_NAME) final String name,
            @JsonProperty(LMS_SETUP.ATTR_LMS_TYPE) final LmsType lmsType,
            @JsonProperty(LMS_SETUP.ATTR_LMS_CLIENTNAME) final String lmsAuthName,
            @JsonProperty(LMS_SETUP.ATTR_LMS_CLIENTSECRET) final String lmsAuthSecret,
            @JsonProperty(LMS_SETUP.ATTR_LMS_URL) final String lmsApiUrl,
            @JsonProperty(LMS_SETUP.ATTR_LMS_REST_API_TOKEN) final String lmsRestApiToken,
            @JsonProperty(LMS_SETUP.ATTR_LMS_PROXY_HOST) final String proxyHost,
            @JsonProperty(LMS_SETUP.ATTR_LMS_PROXY_PORT) final Integer proxyPort,
            @JsonProperty(LMS_SETUP.ATTR_LMS_PROXY_AUTH_USERNAME) final String proxyAuthUsername,
            @JsonProperty(LMS_SETUP.ATTR_LMS_PROXY_AUTH_SECRET) final String proxyAuthSecret,
            @JsonProperty(INSTITUTION.ATTR_ACTIVE) final Boolean active,
            @JsonProperty(LMS_SETUP.ATTR_UPDATE_TIME) final Long updateTime) {

        this.id = id;
        this.institutionId = institutionId;
        this.name = name;
        this.lmsType = lmsType;
        this.lmsAuthName = lmsAuthName;
        this.lmsAuthSecret = lmsAuthSecret;
        this.lmsApiUrl = lmsApiUrl;
        this.lmsRestApiToken = lmsRestApiToken;
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.proxyAuthUsername = proxyAuthUsername;
        this.proxyAuthSecret = proxyAuthSecret;
        this.active = (active != null) ? active : Boolean.FALSE;
        this.updateTime = updateTime;
    }

    public LmsSetup(final String modelId, final POSTMapper mapper) {
        this.id = (modelId != null) ? Long.parseLong(modelId) : null;
        this.institutionId = mapper.getLong(LMS_SETUP.ATTR_INSTITUTION_ID);
        this.name = mapper.getString(LMS_SETUP.ATTR_NAME);
        this.lmsType = mapper.getEnum(LMS_SETUP.ATTR_LMS_TYPE, LmsType.class);
        this.lmsAuthName = mapper.getString(LMS_SETUP.ATTR_LMS_CLIENTNAME);
        this.lmsAuthSecret = mapper.getString(LMS_SETUP.ATTR_LMS_CLIENTSECRET);
        this.lmsApiUrl = mapper.getString(LMS_SETUP.ATTR_LMS_URL);
        this.lmsRestApiToken = mapper.getString(LMS_SETUP.ATTR_LMS_REST_API_TOKEN);
        this.proxyHost = mapper.getString(LMS_SETUP.ATTR_LMS_PROXY_HOST);
        this.proxyPort = mapper.getInteger(LMS_SETUP.ATTR_LMS_PROXY_PORT);
        this.proxyAuthUsername = mapper.getString(LMS_SETUP.ATTR_LMS_PROXY_AUTH_USERNAME);
        this.proxyAuthSecret = mapper.getString(LMS_SETUP.ATTR_LMS_PROXY_AUTH_SECRET);
        this.active = mapper.getBooleanObject(LMS_SETUP.ATTR_ACTIVE);
        this.updateTime = mapper.getLong(LMS_SETUP.ATTR_UPDATE_TIME);
    }

    @Override
    public EntityType entityType() {
        return EntityType.LMS_SETUP;
    }

    public Long getId() {
        return this.id;
    }

    @Override
    public boolean isActive() {
        return this.active;
    }

    @JsonIgnore
    @Override
    public String getModelId() {
        return (this.id != null)
                ? String.valueOf(this.id)
                : null;
    }

    @Override
    public Long getInstitutionId() {
        return this.institutionId;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public LmsType getLmsType() {
        return this.lmsType;
    }

    public String getLmsAuthName() {
        return this.lmsAuthName;
    }

    public String getLmsAuthSecret() {
        return this.lmsAuthSecret;
    }

    public String getLmsApiUrl() {
        return this.lmsApiUrl;
    }

    public String getLmsRestApiToken() {
        return this.lmsRestApiToken;
    }

    public String getProxyHost() {
        return this.proxyHost;
    }

    public Integer getProxyPort() {
        return this.proxyPort;
    }

    public String getProxyAuthUsername() {
        return this.proxyAuthUsername;
    }

    public String getProxyAuthSecret() {
        return this.proxyAuthSecret;
    }

    public Boolean getActive() {
        return this.active;
    }

    @Override
    public Entity printSecureCopy() {
        return new LmsSetup(
                this.id,
                this.institutionId,
                this.name,
                this.lmsType,
                this.lmsAuthName,
                Constants.EMPTY_NOTE,
                this.lmsApiUrl,
                Constants.EMPTY_NOTE,
                this.proxyHost,
                this.proxyPort,
                this.proxyAuthUsername,
                Constants.EMPTY_NOTE,
                this.active,
                this.updateTime);
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("LmsSetup [id=");
        builder.append(this.id);
        builder.append(", institutionId=");
        builder.append(this.institutionId);
        builder.append(", name=");
        builder.append(this.name);
        builder.append(", lmsType=");
        builder.append(this.lmsType);
        builder.append(", lmsAuthName=");
        builder.append(this.lmsAuthName);
        builder.append(", lmsAuthSecret=");
        builder.append(this.lmsAuthSecret);
        builder.append(", lmsApiUrl=");
        builder.append(this.lmsApiUrl);
        builder.append(", lmsRestApiToken=");
        builder.append(this.lmsRestApiToken);
        builder.append(", proxyHost=");
        builder.append(this.proxyHost);
        builder.append(", proxyPort=");
        builder.append(this.proxyPort);
        builder.append(", proxyAuthUsername=");
        builder.append(this.proxyAuthUsername);
        builder.append(", proxyAuthSecret=");
        builder.append(this.proxyAuthSecret);
        builder.append(", active=");
        builder.append(this.active);
        builder.append("]");
        return builder.toString();
    }

    public static EntityName toName(final LmsSetup lmsSetup) {
        return new EntityName(
                String.valueOf(lmsSetup.id),
                EntityType.LMS_SETUP,
                lmsSetup.name);
    }

    public static LmsSetup createNew(final Long institutionId) {
        return new LmsSetup(
                null, institutionId, null, null, null, null, null, null, null, null, null, null, false, null);
    }

}
