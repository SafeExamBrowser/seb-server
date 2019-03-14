/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.util;

import java.util.function.Supplier;

public class SupplierWithCircuitBreaker<T> implements Supplier<Result<T>> {

    private final Supplier<T> supplierThatCanFailOrBlock;
    private final int maxFailingAttempts;
    private final long maxBlockingTime;

    private final T cached = null;

    public SupplierWithCircuitBreaker(
            final Supplier<T> supplierThatCanFailOrBlock,
            final int maxFailingAttempts,
            final long maxBlockingTime) {

        this.supplierThatCanFailOrBlock = supplierThatCanFailOrBlock;
        this.maxFailingAttempts = maxFailingAttempts;
        this.maxBlockingTime = maxBlockingTime;
    }

    @Override
    public Result<T> get() {

        // TODO start an async task that calls the supplierThatCanFailOrBlock and returns a Future
        // try to get the result periodically until maxBlockingTime
        // if the supplier returns error, try for maxFailingAttempts
        // if success cache and return the result
        // if failed return the cached values
        return Result.tryCatch(() -> this.supplierThatCanFailOrBlock.get());
    }

}
