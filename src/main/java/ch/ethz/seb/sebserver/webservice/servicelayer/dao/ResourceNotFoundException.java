/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.dao;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public final class ResourceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 8319235723086949618L;

    public ResourceNotFoundException(final String resourceName, final String resourceId) {
        this(resourceName, resourceId, null);
    }

    public ResourceNotFoundException(final String resourceName, final String resourceId, final Throwable cause) {
        super("Resource " + resourceName + " with ID: " + resourceId + " not found", cause);
    }

}
