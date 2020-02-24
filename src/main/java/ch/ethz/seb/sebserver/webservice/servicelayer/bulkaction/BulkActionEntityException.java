/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.bulkaction;

import ch.ethz.seb.sebserver.gbl.model.EntityKey;

public class BulkActionEntityException extends RuntimeException {

    private static final long serialVersionUID = 3071344358080887083L;

    public final EntityKey key;

    public BulkActionEntityException(final EntityKey key) {
        super("Unexpected error during bulk action for entity: " + key);
        this.key = key;
    }

    public BulkActionEntityException(final EntityKey key, final Throwable t) {
        super("Unexpected error during bulk action for entity: " + key, t);
        this.key = key;
    }

}
