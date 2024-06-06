/*
 *  Copyright (c) 2019 ETH Zürich, IT Services
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.user;

import ch.ethz.seb.sebserver.gbl.model.EntityKey;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class LoginForward {
    @JsonProperty("entityKey")
    public final EntityKey entityKey;
    @JsonProperty("actionName")
    public final String actionName;

    public LoginForward(
            @JsonProperty("entityKey") final EntityKey entityKey,
            @JsonProperty("actionName") final String actionName) {
        this.entityKey = entityKey;
        this.actionName = actionName;
    }
}
