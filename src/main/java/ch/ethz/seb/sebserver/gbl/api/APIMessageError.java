/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.api;

import java.util.Collection;

/** Defines an API message error holder that supplies a List of APIMessage if error happened */
public interface APIMessageError {

    /** Get a List of APIMessage errors if error happened
     *
     * @return a List of APIMessage errors if error happened or empty list of not */
    Collection<APIMessage> getAPIMessages();

}
