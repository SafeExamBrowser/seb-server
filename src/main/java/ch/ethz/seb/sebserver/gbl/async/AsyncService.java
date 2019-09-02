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

    public <T> CircuitBreaker<T> createCircuitBreaker() {

        return new CircuitBreaker<>(this.asyncRunner);
    }

    public <T> CircuitBreaker<T> createCircuitBreaker(
            final int maxFailingAttempts,
            final long maxBlockingTime,
            final long timeToRecover) {

        return new CircuitBreaker<>(
                this.asyncRunner,
                maxFailingAttempts,
                maxBlockingTime,
                timeToRecover);
    }

//    public <T> MemoizingCircuitBreaker<T> createMemoizingCircuitBreaker(
//            final Supplier<T> blockingSupplier) {
//
//        return new MemoizingCircuitBreaker<>(
//                this.asyncRunner,
//                blockingSupplier,
//                true,
//                Constants.HOUR_IN_MILLIS);
//    }

    public <T> MemoizingCircuitBreaker<T> createMemoizingCircuitBreaker(
            final Supplier<T> blockingSupplier,
            final int maxFailingAttempts,
            final long maxBlockingTime,
            final long timeToRecover,
            final boolean momoized,
            final long maxMemoizingTime) {

        return new MemoizingCircuitBreaker<>(
                this.asyncRunner,
                blockingSupplier,
                maxFailingAttempts,
                maxBlockingTime,
                timeToRecover,
                momoized,
                maxMemoizingTime);
    }

}
