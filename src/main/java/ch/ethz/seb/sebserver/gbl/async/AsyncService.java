/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.async;

import java.util.function.Supplier;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Lazy
@Service
public class AsyncService {

    private final AsyncRunner asyncRunner;

    protected AsyncService(final AsyncRunner asyncRunner) {
        this.asyncRunner = asyncRunner;
    }

    public <T> MemoizingCircuitBreaker<T> createCircuitBreaker(final Supplier<T> blockingSupplier) {
        return new MemoizingCircuitBreaker<>(this.asyncRunner, blockingSupplier);
    }

    public <T> MemoizingCircuitBreaker<T> createCircuitBreaker(
            final Supplier<T> blockingSupplier,
            final long maxBlockingTime) {

        return new MemoizingCircuitBreaker<>(
                this.asyncRunner,
                blockingSupplier,
                MemoizingCircuitBreaker.DEFAULT_MAX_FAILING_ATTEMPTS,
                maxBlockingTime,
                MemoizingCircuitBreaker.DEFAULT_TIME_TO_RECOVER);
    }

    public <T> MemoizingCircuitBreaker<T> createCircuitBreaker(
            final Supplier<T> blockingSupplier,
            final int maxFailingAttempts,
            final long maxBlockingTime,
            final long timeToRecover) {

        return new MemoizingCircuitBreaker<>(
                this.asyncRunner,
                blockingSupplier,
                maxFailingAttempts,
                maxBlockingTime,
                timeToRecover);
    }

}
