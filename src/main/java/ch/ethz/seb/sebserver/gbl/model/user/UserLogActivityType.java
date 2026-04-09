/*
 * Copyright (c) 2019 ETH Zürich, IT Services
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.model.user;

import io.swagger.v3.oas.annotations.media.Schema;

/** All activity types */
@Schema(description = "Type of user activity logged in the audit log")
public enum UserLogActivityType {
    REGISTER,
    CREATE,
    IMPORT,
    EXPORT,
    MODIFY,
    PASSWORD_CHANGE,
    DEACTIVATE,
    ACTIVATE,
    FINISHED,
    DELETE,
    LOGIN,
    LOGOUT,
    ARCHIVE,
    REGISTER_INSTRUCTION
}