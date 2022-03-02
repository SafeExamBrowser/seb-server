/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.async;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;

/** A circuit breaker with three states (CLOSED, HALF_OPEN, OPEN)
 * <p>
 * A <code>CircuitBreaker</code> can be used to make a save call within a Supplier function.
 * This Supplier function usually access remote data on different processes probably on different
 * machines over the Internet and that can fail or hang with unexpected result.
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
public final class CircuitBreaker<T> {

    private static final Logger log = LoggerFactory.getLogger(CircuitBreaker.class);

    public static final int DEFAULT_MAX_FAILING_ATTEMPTS = 5;
    public static final long DEFAULT_MAX_BLOCKING_TIME = Constants.MINUTE_IN_MILLIS;
    public static final long DEFAULT_TIME_TO_RECOVER = Constants.MINUTE_IN_MILLIS * 10;

    public static final RuntimeException OPEN_STATE_EXCEPTION =
            new RuntimeException("Open CircuitBreaker");

    public enum State {
        CLOSED,
        HALF_OPEN,
        OPEN
    }

    private final AsyncRunner asyncRunner;
    private final int maxFailingAttempts;
    private final long maxBlockingTime;
    private final long timeToRecover;

    private State state = State.CLOSED;
    private final AtomicInteger failingCount = new AtomicInteger(0);
    private long lastSuccessTime;
    private long lastOpenTime;

    /** Create new CircuitBreakerSupplier.
     *
     * @param asyncRunner the AsyncRunner used to create asynchronous calls on the given supplier function */
    CircuitBreaker(final AsyncRunner asyncRunner) {
        this(
                asyncRunner,
                DEFAULT_MAX_FAILING_ATTEMPTS,
                DEFAULT_MAX_BLOCKING_TIME,
                DEFAULT_TIME_TO_RECOVER);
    }

    /** Create new CircuitBreakerSupplier.
     *
     * @param asyncRunner the AsyncRunner used to create asynchronous calls on the given supplier function
     * @param maxFailingAttempts the number of maximal failing attempts before go form CLOSE into HALF_OPEN state
     * @param maxBlockingTime the maximal time that an call attempt can block until an error is responded
     * @param timeToRecover the time the circuit breaker needs to cool-down on OPEN-STATE before going back to HALF_OPEN
     *            state */
    CircuitBreaker(
            final AsyncRunner asyncRunner,
            final int maxFailingAttempts,
            final long maxBlockingTime,
            final long timeToRecover) {

        this.asyncRunner = asyncRunner;
        this.maxFailingAttempts = maxFailingAttempts;
        this.maxBlockingTime = maxBlockingTime;
        this.timeToRecover = timeToRecover;
        // Initialize with creation time to get expected cool-down phase time if never was successful since
        this.lastOpenTime = Utils.getMillisecondsNow();
    }

    public int getMaxFailingAttempts() {
        return this.maxFailingAttempts;
    }

    public long getMaxBlockingTime() {
        return this.maxBlockingTime;
    }

    public long getTimeToRecover() {
        return this.timeToRecover;
    }

    public AtomicInteger getFailingCount() {
        return this.failingCount;
    }

    public long getLastSuccessTime() {
        return this.lastSuccessTime;
    }

    public synchronized Result<T> protectedRun(final Supplier<T> supplier) {
        final long currentTime = Utils.getMillisecondsNow();

        if (log.isDebugEnabled()) {
            log.debug("Called on: {} current state is: {} failing count: {}",
                    currentTime,
                    this.state,
                    this.failingCount);
        }

        switch (this.state) {
            case CLOSED:
                return handleClosed(currentTime, supplier);
            case HALF_OPEN:
                return handleHalfOpen(currentTime, supplier);
            case OPEN:
                return handelOpen(currentTime, supplier);
            default:
                return Result.ofError(new IllegalStateException());
        }
    }

    public State getState() {
        return this.state;
    }

    private Result<T> handleClosed(
            final long startTime,
            final Supplier<T> supplier) {

        if (log.isDebugEnabled()) {
            log.debug("Handle Closed on: {}", startTime);
        }

        // try once
        final Result<T> result = attempt(supplier);
        if (result.hasError()) {

            if (log.isDebugEnabled()) {
                log.debug("Attempt failed. failing count: {}", this.failingCount);
            }

            final long currentBlockingTime = Utils.getMillisecondsNow() - startTime;
            final int failing = this.failingCount.incrementAndGet();
            if (failing > this.maxFailingAttempts || currentBlockingTime > this.maxBlockingTime) {
                // brake thought to HALF_OPEN state and return error
                if (log.isDebugEnabled()) {
                    log.debug("Changing state from Open to Half Open and return cached value");
                }

                this.state = State.HALF_OPEN;
                this.failingCount.set(0);
                return Result.ofError(new RuntimeException(
                        "Set CircuitBeaker to half-open state. Cause: " + result.getError(),
                        result.getError()));
            } else {
                // try again
                return protectedRun(supplier);
            }
        } else {
            this.lastSuccessTime = Utils.getMillisecondsNow();
            return result;
        }
    }

    private Result<T> handleHalfOpen(
            final long startTime,
            final Supplier<T> supplier) {

        if (log.isDebugEnabled()) {
            log.debug("Handle Half Open on: {}", startTime);
        }

        // try once
        final Result<T> result = attempt(supplier);
        if (result.hasError()) {
            // on fail go to OPEN state
            if (log.isDebugEnabled()) {
                log.debug("Changing state from Half Open to Open and return cached value");
            }

            this.lastOpenTime = Utils.getMillisecondsNow();
            this.state = State.OPEN;
            return Result.ofError(new RuntimeException(
                    "Set CircuitBeaker to open state. Cause: " + result.getError(),
                    result.getError()));
        } else {
            // on success go to CLOSED state
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
    private Result<T> handelOpen(
            final long startTime,
            final Supplier<T> supplier) {

        if (log.isDebugEnabled()) {
            log.debug("Handle Open on: {}", startTime);
        }

        if (startTime - this.lastOpenTime >= this.timeToRecover) {
            // if cool-down period is over, go back to HALF_OPEN state and try again
            if (log.isDebugEnabled()) {
                log.debug("Time to recover reached. Changing state from Open to Half Open");
            }

            this.state = State.HALF_OPEN;
            return protectedRun(supplier);
        }

        return Result.ofError(OPEN_STATE_EXCEPTION);
    }

    private Result<T> attempt(final Supplier<T> supplier) {
        final Future<T> future = this.asyncRunner.runAsync(supplier);
        try {
            return Result.of(future.get(this.maxBlockingTime, TimeUnit.MILLISECONDS));
        } catch (final Exception e) {
            future.cancel(false);
            log.warn("Max blocking timeout exceeded: {}, {}", this.maxBlockingTime, this.state);
            return Result.ofError(e);
        }
    }

    @Override
    public String toString() {
        return "CircuitBreaker [asyncRunner=" + this.asyncRunner + ", maxFailingAttempts=" + this.maxFailingAttempts
                + ", maxBlockingTime=" + this.maxBlockingTime + ", timeToRecover=" + this.timeToRecover + ", state="
                + this.state
                + ", failingCount=" + this.failingCount + ", lastSuccessTime=" + this.lastSuccessTime + "]";
    }

}
