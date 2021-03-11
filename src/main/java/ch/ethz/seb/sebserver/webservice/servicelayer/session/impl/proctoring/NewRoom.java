/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.session.impl.proctoring;

public class NewRoom {

    public final String name;
    public final String subject;
    public final CharSequence joinKey;

    public NewRoom(final String name, final String subject, final CharSequence joinKey) {
        super();
        this.name = name;
        this.subject = subject;
        this.joinKey = joinKey;
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
}
