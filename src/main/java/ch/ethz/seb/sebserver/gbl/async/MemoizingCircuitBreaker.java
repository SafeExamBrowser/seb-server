/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.async;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.async.CircuitBreaker.State;
import ch.ethz.seb.sebserver.gbl.util.Result;

/** A circuit breaker with three states (CLOSED, HALF_OPEN, OPEN) and memoizing functionality
 * that wraps and safe a Supplier function of the same type.
 * <p>
 * A <code>CircuitBreakerSupplier</code> can be used to make a save call within a Supplier function. This Supplier
 * function
 * usually access remote data on different processes probably on different machines over the Internet
 * and that can fail or hang with unexpected result.
 * <p>
 * The circuit breaker pattern can safe resources like Threads, Data-Base-Connections, from being taken and
 * hold for long time not released. What normally lead into some kind of no resource available error state.
 * For more information please visit: https://martinfowler.com/bliki/CircuitBreaker.html
 * <p>
 * This circuit breaker implementation has three states, CLOSED, HALF_OPEN and OPEN. The normal and initial state of the
 * circuit breaker is CLOSED. A call on the circuit breaker triggers a asynchronous call on the given supplier waiting
 * a given time-period for a response or on fail, trying a given number of times to call again.
 * If the time to wait went out or if the failing count is reached, the circuit breaker goes into the HALF-OPEN state
 * and respond with an error.
 * A call on a circuit breaker in HALF-OPEN state will trigger another single try to get the result from given supplier
 * and on success the circuit breaker goes back to CLOSED state and on fail, the circuit breaker goes to OPEN state
 * In the OPEN state the time to recover comes into play. The circuit breaker stays at least for this time into the
 * OPEN state and respond to all calls within this time period with an error. After the time to recover has reached
 * the circuit breaker goes back to HALF_OPEN state.
 * <p>
 * This circuit breaker implementation comes with a memoizing functionality where on successful calls the result get
 * cached and the circuit breaker respond on error cases with the cached result if available.
 *
 *
 * @param <T> The of the result of the supplying function */
public final class MemoizingCircuitBreaker<T> implements Supplier<Result<T>> {

    private static final Logger log = LoggerFactory.getLogger(MemoizingCircuitBreaker.class);

    private final CircuitBreaker<T> delegate;
    private Supplier<T> supplier;

    private final boolean memoizing;
    private final long maxMemoizingTime;
    private long lastMemoizingTime = 0;
    private Result<T> cached = null;

    /** Create new CircuitBreakerSupplier.
     *
     * @param asyncRunner the AsyncRunner used to create asynchronous calls on the given supplier function
     * @param supplier The Supplier function that can fail or block for a long time
     * @param memoizing whether the memoizing functionality is on or off
     * @param maxMemoizingTime the maximal time memorized data is valid */
    MemoizingCircuitBreaker(
            final AsyncRunner asyncRunner,
            final Supplier<T> supplier,
            final boolean memoizing,
            final long maxMemoizingTime) {

        this.delegate = new CircuitBreaker<>(asyncRunner);
        this.supplier = supplier;
        this.memoizing = memoizing;
        this.maxMemoizingTime = maxMemoizingTime;
    }

    public CircuitBreaker<T> getDelegate() {
        return this.delegate;
    }

    public boolean isMemoizing() {
        return this.memoizing;
    }

    public long getMaxMemoizingTime() {
        return this.maxMemoizingTime;
    }

    public long getLastMemoizingTime() {
        return this.lastMemoizingTime;
    }

    /** Create new MemoizingCircuitBreaker.
     *
     * @param asyncRunner the AsyncRunner used to create asynchronous calls on the given supplier function
     * @param supplier The Supplier function that can fail or block for a long time
     * @param maxFailingAttempts the number of maximal failing attempts before go form CLOSE into HALF_OPEN state
     * @param maxBlockingTime the maximal time that an call attempt can block until an error is responded
     * @param timeToRecover the time the circuit breaker needs to cool-down on OPEN-STATE before going back to HALF_OPEN
     *            state
     * @param memoizing whether the memoizing functionality is on or off
     * @param maxMemoizingTime the maximal time memorized data is valid */
    MemoizingCircuitBreaker(
            final AsyncRunner asyncRunner,
            final Supplier<T> supplier,
            final int maxFailingAttempts,
            final long maxBlockingTime,
            final long timeToRecover,
            final boolean memoizing,
            final long maxMemoizingTime) {

        this.delegate = new CircuitBreaker<>(
                asyncRunner,
                maxFailingAttempts,
                maxBlockingTime,
                timeToRecover);
        this.supplier = supplier;
        this.memoizing = memoizing;
        this.maxMemoizingTime = maxMemoizingTime;
    }

    public void setSupplier(final Supplier<T> supplier) {
        this.supplier = supplier;
    }

    @Override
    public synchronized Result<T> get() {
        final Result<T> result = this.delegate.protectedRun(this.supplier);
        if (result.hasError()) {
            if (this.memoizing && this.cached != null) {
                final long currentTimeMillis = System.currentTimeMillis();
                if (currentTimeMillis - this.lastMemoizingTime > this.maxMemoizingTime) {
                    if (log.isDebugEnabled()) {
                        log.warn("Max memoizing time reached. Return cached. Error: {}",
                                result.getError().getMessage());
                    }
                    return result;
                }

                log.warn("Return cached at: {} error: {}",
                        System.currentTimeMillis(),
                        result.getError().getMessage());
                return this.cached;
            }

        } else {
            if (this.memoizing) {
                if (log.isDebugEnabled()) {
                    log.debug("Memoizing result at: {}", System.currentTimeMillis());
                }

                this.cached = result;
                this.lastMemoizingTime = System.currentTimeMillis();
            }
        }
        return result;
    }

    public State getState() {
        return this.delegate.getState();
    }

    public T getCached() {
        if (this.cached == null) {
            return null;
        }

        return this.cached.get();
    }

}
