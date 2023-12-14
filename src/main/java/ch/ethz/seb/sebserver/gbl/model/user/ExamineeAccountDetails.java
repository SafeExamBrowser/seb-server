/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.user;

import java.util.Map;

import ch.ethz.seb.sebserver.gbl.api.API;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.ethz.seb.sebserver.gbl.util.Utils;

public class ExamineeAccountDetails {

    public static final String ATTR_ID = "id";
    public static final String ATTR_NAME = "name";
    public static final String ATTR_USER_NAME = "username";
    public static final String ATTR_EMAIL = "email";

    @JsonProperty(ATTR_ID)
    public final String id;

    @JsonProperty(ATTR_NAME)
    public final String name;

    @JsonProperty(ATTR_USER_NAME)
    public final String username;

    @JsonProperty(ATTR_EMAIL)
    public final String email;

    @JsonProperty(API.PARAM_ADDITIONAL_ATTRIBUTES)
    public final Map<String, String> additionalAttributes;

    @JsonCreator
    public ExamineeAccountDetails(
            @JsonProperty(ATTR_ID) final String id,
            @JsonProperty(ATTR_NAME) final String name,
            @JsonProperty(ATTR_USER_NAME) final String username,
            @JsonProperty(ATTR_EMAIL) final String email,
            @JsonProperty(API.PARAM_ADDITIONAL_ATTRIBUTES) final Map<String, String> additionalAttributes) {

        this.id = id;
        this.name = name;
        this.username = username;
        this.email = email;
        this.additionalAttributes = Utils.immutableMapOf(additionalAttributes);
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getUsername() {
        return this.username;
    }

    public String getEmail() {
        return this.email;
    }

    public Map<String, String> getAdditionalAttributes() {
        return this.additionalAttributes;
    }

    public String getDisplayName() {
        if (this.name == null) {
            return this.id;
        }
        return (this.name.equals(this.id)) ? this.id : this.name + " (" + this.id + ")";
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("ExamineeAccountDetails [id=");
        builder.append(this.id);
        builder.append(", name=");
        builder.append(this.name);
        builder.append(", username=");
        builder.append(this.username);
        builder.append(", email=");
        builder.append(this.email);
        builder.append(", additionalAttributes=");
        builder.append(this.additionalAttributes);
        builder.append("]");
        return builder.toString();
    }

}
