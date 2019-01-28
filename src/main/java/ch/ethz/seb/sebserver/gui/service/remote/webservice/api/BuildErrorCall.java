/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.remote.webservice.api;

import ch.ethz.seb.sebserver.gbl.util.Result;

public class BuildErrorCall<T> extends RestCall<T> {

    private final Throwable error;

    protected BuildErrorCall(final Throwable error) {
        super(null, null, null, null);
        this.error = error;
    }

    @Override
    protected Result<T> exchange(final RestCallBuilder builder) {
        return Result.ofError(this.error);
    }
}
