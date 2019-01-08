/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.user;

public final class UserFilter {

    public static final String FILTER_ATTR_ACTIVE = "active";
    public static final String FILTER_ATTR_NAME = "name";
    public static final String FILTER_ATTR_USER_NAME = "username";
    public static final String FILTER_ATTR_EMAIL = "email";
    public static final String FILTER_ATTR_LOCALE = "locale";
    public static final String FILTER_ATTR_INSTITUTION = "institutionId";

    public final Boolean active;
    public final Long institutionId;
    public final String name;
    public final String username;
    public final String email;
    public final String locale;

    public UserFilter(
            final Long institutionId,
            final String name,
            final String username,
            final String email,
            final Boolean active,
            final String locale) {

        this.institutionId = institutionId;
        this.name = name;
        this.username = username;
        this.email = email;
        this.active = active; //(active != null) ? active : true;
        this.locale = locale;
    }

    public Long getInstitutionId() {
        return this.institutionId;
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

    public Boolean getActive() {
        return this.active;
    }

    public String getLocale() {
        return this.locale;
    }

    @Override
    public String toString() {
        return "UserFilter [active=" + this.active + ", institutionId=" + this.institutionId + ", name=" + this.name
                + ", username="
                + this.username + ", email=" + this.email + ", locale=" + this.locale + "]";
    }

}
