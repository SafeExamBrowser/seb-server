/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.proctoring;

public final class NewRoom {

    public final String name;
    public final String subject;
    public final CharSequence joinKey;
    public final String additionalRoomData;

    public NewRoom(
            final String name,
            final String subject) {

        this(name, subject, null, null);
    }

    public NewRoom(
            final String name,
            final String subject,
            final CharSequence joinKey,
            final String additionalRoomData) {

        this.name = name;
        this.subject = subject;
        this.joinKey = joinKey;
        this.additionalRoomData = additionalRoomData;
    }

    public String getName() {
        return this.name;
    }

    public String getSubject() {
        return this.subject;
    }

    public CharSequence getJoinKey() {
        return this.joinKey;
    }

    public String getAdditionalRoomData() {
        return this.additionalRoomData;
    }

}
