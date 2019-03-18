/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.async;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.Constants;
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
 * This circuit breaker implementation has three states, CLODED, HALF_OPEN and OPEN. The normal and initial state of the
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
 * @param <T> The of the result of the suppling function */
public class CircuitBreakerSupplier<T> implements Supplier<Result<T>> {

    private static final Logger log = LoggerFactory.getLogger(CircuitBreakerSupplier.class);

    public static final int DEFAULT_MAX_FAILING_ATTEMPTS = 5;
    public static final long DEFAULT_MAX_BLOCKING_TIME = Constants.MINUTE_IN_MILLIS;
    public static final long DEFAULT_TIME_TO_RECOVER = Constants.MINUTE_IN_MILLIS * 10;

    public enum State {
        CLOSED,
        HALF_OPEN,
        OPEN
    }

    private final AsyncRunner asyncRunner;
    private final Supplier<T> supplierThatCanFailOrBlock;
    private final int maxFailingAttempts;
    private final long maxBlockingTime;
    private final long timeToRecover;

    private State state = State.CLOSED;
    private final AtomicInteger failingCount = new AtomicInteger(0);
    private long lastSuccessTime;

    private final boolean memoizing;
    private final Result<T> notAvailable = Result.ofRuntimeError("No chached resource available");
    private Result<T> cached = null;

    /** Create new CircuitBreakerSupplier.
     *
     * @param asyncRunner the AsyncRunner used to create asynchronous calls on the given supplier function
     * @param supplierThatCanFailOrBlock The Supplier function that can fail or block for a long time
     * @param memoizing whether the memoizing functionality is on or off */
    CircuitBreakerSupplier(
            final AsyncRunner asyncRunner,
            final Supplier<T> supplierThatCanFailOrBlock,
            final boolean memoizing) {

        this(
                asyncRunner,
                supplierThatCanFailOrBlock,
                DEFAULT_MAX_FAILING_ATTEMPTS,
                DEFAULT_MAX_BLOCKING_TIME,
                DEFAULT_TIME_TO_RECOVER,
                memoizing);
    }

    /** Create new CircuitBreakerSupplier.
     *
     * @param asyncRunner the AsyncRunner used to create asynchronous calls on the given supplier function
     * @param supplierThatCanFailOrBlock The Supplier function that can fail or block for a long time
     * @param maxFailingAttempts the number of maximal failing attempts before go form CLOSE into HALF_OPEN state
     * @param maxBlockingTime the maximal time that an call attempt can block until an error is responded
     * @param timeToRecover the time the circuit breaker needs to cool-down on OPEN-STATE before going back to HALF_OPEN
     *            state
     * @param memoizing whether the memoizing functionality is on or off */
    CircuitBreakerSupplier(
            final AsyncRunner asyncRunner,
            final Supplier<T> supplierThatCanFailOrBlock,
            final int maxFailingAttempts,
            final long maxBlockingTime,
            final long timeToRecover,
            final boolean memoizing) {

        this.asyncRunner = asyncRunner;
        this.supplierThatCanFailOrBlock = supplierThatCanFailOrBlock;
        this.maxFailingAttempts = maxFailingAttempts;
        this.maxBlockingTime = maxBlockingTime;
        this.timeToRecover = timeToRecover;
        this.memoizing = memoizing;
    }

    @Override
    public Result<T> get() {

        final long currentTime = System.currentTimeMillis();

        if (log.isDebugEnabled()) {
            log.debug("Called on: {} current state is: {} failing count: {}",
                    currentTime,
                    this.state,
                    this.failingCount);
        }

        switch (this.state) {
            case CLOSED:
                return handleClosed(currentTime);
            case HALF_OPEN:
                return handleHalfOpen(currentTime);
            case OPEN:
                return handelOpen(currentTime);
            default:
                throw new IllegalStateException();
        }
    }

    public State getState() {
        return this.state;
    }

    private Result<T> handleClosed(final long currentTime) {

        if (log.isDebugEnabled()) {
            log.debug("Handle Closed on: {}", currentTime);
        }

        final Result<T> result = attempt();
        if (result.hasError()) {

            if (log.isDebugEnabled()) {
                log.debug("Attempt failed. failing count: {}", this.failingCount);
            }

            final int failing = this.failingCount.incrementAndGet();
            if (failing > this.maxFailingAttempts) {

                if (log.isDebugEnabled()) {
                    log.debug("Changing state from Open to Half Open and return cached value");
                }

                this.state = State.HALF_OPEN;
                this.failingCount.set(0);
                return getCached(result);
            } else {
                return get();
            }
        } else {
            this.lastSuccessTime = System.currentTimeMillis();
            if (this.memoizing) {
                this.cached = result;
            }
            return result;
        }
    }

    private Result<T> handleHalfOpen(final long currentTime) {

        if (log.isDebugEnabled()) {
            log.debug("Handle Half Open on: {}", currentTime);
        }

        final Result<T> result = attempt();
        if (result.hasError()) {
            if (log.isDebugEnabled()) {
                log.debug("Changing state from Half Open to Open and return cached value");
            }

            this.state = State.OPEN;
            return getCached(result);
        } else {

            if (log.isDebugEnabled()) {
                log.debug("Changing state from Half Open to Closed and return value");
            }

            this.state = State.CLOSED;
            this.failingCount.set(0);
            return result;
        }
    }

    /** As long as time to recover is not reached, return from cache
     * If time to recover is reached go to half open state and try again */
    private Result<T> handelOpen(final long currentTime) {

        if (log.isDebugEnabled()) {
            log.debug("Handle Open on: {}", currentTime);
        }

        if (currentTime - this.lastSuccessTime >= this.timeToRecover) {
            if (log.isDebugEnabled()) {
                log.debug("Time to recover reached. Changing state from Open to Half Open");
            }

            this.state = State.HALF_OPEN;

        }

        return getCached(this.notAvailable);
    }

    private Result<T> attempt() {
        try {
            return Result.of(this.asyncRunner.runAsync(this.supplierThatCanFailOrBlock)
                    .get(this.maxBlockingTime, TimeUnit.MILLISECONDS));
        } catch (final Exception e) {
            return Result.ofError(e);
        }
    }

    private Result<T> getCached(final Result<T> error) {
        if (!this.memoizing) {
            return error;
        }
        if (this.cached != null) {
            return this.cached;
        } else {
            return error;
        }
    }

    T getChached() {
        if (this.cached == null) {
            return null;
        }

        return this.cached.get();
    }

    @Override
    public String toString() {
        return "MemoizingCircuitBreaker [maxFailingAttempts=" + this.maxFailingAttempts + ", maxBlockingTime="
                + this.maxBlockingTime + ", timeToRecover=" + this.timeToRecover + ", state=" + this.state
                + ", failingCount="
                + this.failingCount + ", lastSuccessTime=" + this.lastSuccessTime + ", cached=" + this.cached + "]";
    }

}
