/*
 *  Copyright (c) 2019 ETH Zürich, IT Services
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.authorization;

public final class AdHocAccountData {
    public final String userId;
    public final String username;
    public final String userMail;
    public final String firstName;
    public final String lastName;
    public final String timezone;

    public AdHocAccountData(
            final String userId,
            final String username,
            final String userMail,
            final String firstName,
            final String lastName,
            final String timezone) {

        this.userId = userId;
        this.username = username;
        this.userMail = userMail;
        this.firstName = firstName;
        this.lastName = lastName;
        this.timezone = timezone;
    }
}