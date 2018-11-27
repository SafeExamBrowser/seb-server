/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.user;

import java.util.Set;

import org.joda.time.DateTime;

/** TODO what filter criteria do we need? */
public final class UserFilter {

    public final Set<Long> institutionIds;
    public final String name;
    public final String username;
    public final String email;
    public final DateTime creationDateFrom;
    public final DateTime creationDateTo;
    public final Set<Long> createdById;
    public final Boolean active;
    public final Set<String> locales;
    public final Set<String> timeZones;
    public final Set<String> roles;

    public UserFilter(
            final Set<Long> institutionIds,
            final String name,
            final String username,
            final String email,
            final DateTime creationDateFrom,
            final DateTime creationDateTo,
            final Set<Long> createdById,
            final Boolean active,
            final Set<String> locales,
            final Set<String> timeZones,
            final Set<String> roles) {

        this.institutionIds = institutionIds;
        this.name = name;
        this.username = username;
        this.email = email;
        this.creationDateFrom = creationDateFrom;
        this.creationDateTo = creationDateTo;
        this.createdById = createdById;
        this.active = active;
        this.locales = locales;
        this.timeZones = timeZones;
        this.roles = roles;
    }

}
