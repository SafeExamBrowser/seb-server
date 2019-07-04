/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.user;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

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

    @JsonCreator
    public ExamineeAccountDetails(
            @JsonProperty(ATTR_ID) final String id,
            @JsonProperty(ATTR_NAME) final String name,
            @JsonProperty(ATTR_USER_NAME) final String username,
            @JsonProperty(ATTR_EMAIL) final String email) {

        this.id = id;
        this.name = name;
        this.username = username;
        this.email = email;
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
        builder.append("]");
        return builder.toString();
    }

}
