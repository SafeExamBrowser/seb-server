/*
 * Copyright (c) 2018 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface Activatable {

    public enum ActivationAction {
        NONE,
        ACTIVATE,
        DEACTIVATE
    }

    @JsonIgnore
    boolean isActive();

}
