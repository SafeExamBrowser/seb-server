/*
 * Copyright (c) 2021 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice;

import ch.ethz.seb.sebserver.gbl.util.Result;

/** Defines a data base integrity check and fix if available. */
public interface DBIntegrityCheck {

    /** The name of the database integrity check */
    String name();

    /** The description of the database integrity check */
    String description();

    /** Apply the check and fix it if not correct and a fix id provided
     *
     * @param tryFix indicates if a fix shall be applied if the check fails and a fix is implemented
     * @return check result message */
    Result<String> applyCheck(boolean tryFix);

}
