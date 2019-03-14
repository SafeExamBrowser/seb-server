/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

/** Interface for all domain model objects that has a model identifier */
public interface ModelIdAware {

    /** Get the model identifier of the domain model object
     *
     * @return the model identifier of the domain model object */
    @JsonIgnore
    String getModelId();

}
