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

/** A circuit breaker with three states (CLOSED, HALF_OPEN, OPEN) and memoizing.
 *
 * // TODO more docu:
 *
 * @param <T> */
public class MemoizingCircuitBreaker<T> implements Supplier<Result<T>> {

    private static final Logger log = LoggerFactory.getLogger(MemoizingCircuitBreaker.class);

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

    private final Result<T> notAvailable = Result.ofRuntimeError("No chached resource available");
    private Result<T> cached = null;

    MemoizingCircuitBreaker(
            final AsyncRunner asyncRunner,
            final Supplier<T> supplierThatCanFailOrBlock) {

        this(
                asyncRunner,
                supplierThatCanFailOrBlock,
                DEFAULT_MAX_FAILING_ATTEMPTS,
                DEFAULT_MAX_BLOCKING_TIME,
                DEFAULT_TIME_TO_RECOVER);
    }

    MemoizingCircuitBreaker(
            final AsyncRunner asyncRunner,
            final Supplier<T> supplierThatCanFailOrBlock,
            final int maxFailingAttempts,
            final long maxBlockingTime,
            final long timeToRecover) {

        this.asyncRunner = asyncRunner;
        this.supplierThatCanFailOrBlock = supplierThatCanFailOrBlock;
        this.maxFailingAttempts = maxFailingAttempts;
        this.maxBlockingTime = maxBlockingTime;
        this.timeToRecover = timeToRecover;
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

    T getChached() {
        if (this.cached == null) {
            return null;
        }

        return this.cached.get();
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
            this.cached = result;
            return result;
        }
    }

    private Result<T> handleHalfOpen(final long currentTime) {

        if (log.isDebugEnabled()) {
            log.debug("Handle Half Open on: {}", currentTime);
        }

        final Result<T> result = attempt();
        if (result.hasError()) {
            final int fails = this.failingCount.incrementAndGet();
            if (fails > this.maxFailingAttempts) {

                if (log.isDebugEnabled()) {
                    log.debug("Changing state from Half Open to Open and return cached value");
                }

                this.state = State.OPEN;
                this.failingCount.set(0);
            }
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

        if (currentTime - this.lastSuccessTime < this.timeToRecover) {
            return getCached(this.notAvailable);
        } else {

            if (log.isDebugEnabled()) {
                log.debug("Time to recover reached. Changing state from Closed to Half Open and try agian");
            }

            this.state = State.HALF_OPEN;
            this.failingCount.set(0);
            return get();
        }

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
        if (this.cached != null) {
            return this.cached;
        } else {
            return error;
        }
    }

    @Override
    public String toString() {
        return "MemoizingCircuitBreaker [maxFailingAttempts=" + this.maxFailingAttempts + ", maxBlockingTime="
                + this.maxBlockingTime + ", timeToRecover=" + this.timeToRecover + ", state=" + this.state
                + ", failingCount="
                + this.failingCount + ", lastSuccessTime=" + this.lastSuccessTime + ", cached=" + this.cached + "]";
    }

}
