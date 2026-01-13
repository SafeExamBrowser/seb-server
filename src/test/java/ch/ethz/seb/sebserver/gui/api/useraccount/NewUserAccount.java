/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.api.useraccount;

import ch.ethz.seb.sebserver.gbl.api.API;
import ch.ethz.seb.sebserver.gbl.api.EntityType;
import ch.ethz.seb.sebserver.gbl.model.user.UserInfo;

import ch.ethz.seb.sebserver.gui.api.RestCall;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Lazy
@Component

public class NewUserAccount extends RestCall<UserInfo> {

    public NewUserAccount() {
        super(new TypeKey<>(
                CallType.NEW,
                EntityType.USER,
                new TypeReference<UserInfo>() {
                }),
                HttpMethod.POST,
                MediaType.APPLICATION_FORM_URLENCODED,
                API.USER_ACCOUNT_ENDPOINT);
    }

}
