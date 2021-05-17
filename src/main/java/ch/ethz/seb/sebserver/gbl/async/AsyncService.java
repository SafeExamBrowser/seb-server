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
/** Implements a asynchronous service to manly support CircuitBreaker and MemoizingCircuitBreaker functionality. */
public class AsyncService {

    private final AsyncRunner asyncRunner;

    public AsyncService(final AsyncRunner asyncRunner) {
        this.asyncRunner = asyncRunner;
    }

    /** Create a CircuitBreaker of specified type with the default parameter defined in the CircuitBreaker class
     *
     * @param <T> the type of the CircuitBreaker
     * @return a CircuitBreaker of specified type with the default parameter defined in the CircuitBreaker class */
    public <T> CircuitBreaker<T> createCircuitBreaker() {
        return new CircuitBreaker<>(this.asyncRunner);
    }

    /** Create a CircuitBreaker of specified type.
     *
     * @param maxFailingAttempts maximal number of attempts the CircuitBreaker allows before going onto open state.
     * @param maxBlockingTime maximal time since call CircuitBreaker waits for a response before going onto open state.
     * @param timeToRecover the time the CircuitBreaker takes to recover form open state.
     * @param <T> the type of the CircuitBreaker
     * @return a CircuitBreaker of specified type */
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

    /** Create a MemoizingCircuitBreaker of specified type that memoize a successful result and return the last
     * successful result on fail as long as maxMemoizingTime is not exceeded.
     *
     * @param blockingSupplier the blocking result supplier that the MemoizingCircuitBreaker must call
     * @param maxFailingAttempts maximal number of attempts the CircuitBreaker allows before going onto open state.
     * @param maxBlockingTime maximal time since call CircuitBreaker waits for a response before going onto open state.
     * @param timeToRecover the time the CircuitBreaker takes to recover form open state.
     * @param momoized whether the memoizing functionality is on or off
     * @param maxMemoizingTime the maximal time memorized data is valid
     * @param <T> the type of the CircuitBreaker
     * @return a CircuitBreaker of specified type */
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
