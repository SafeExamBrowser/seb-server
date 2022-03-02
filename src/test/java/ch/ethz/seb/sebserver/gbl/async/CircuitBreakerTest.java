/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.async;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import ch.ethz.seb.sebserver.gbl.async.CircuitBreaker.State;
import ch.ethz.seb.sebserver.gbl.util.Result;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { AsyncServiceSpringConfig.class, AsyncRunner.class, AsyncService.class })
public class CircuitBreakerTest {

    private static final Logger log = LoggerFactory.getLogger(CircuitBreakerTest.class);

    @Autowired
    AsyncService asyncService;

    @Test
    public void testInit() {
        assertNotNull(this.asyncService);
    }

    @Test
    public void roundtrip1() throws InterruptedException {
        final CircuitBreaker<String> circuitBreaker =
                this.asyncService.createCircuitBreaker(3, 500, 1000);

        final Supplier<String> tester = tester(100, 5, 10);

        Result<String> result = circuitBreaker.protectedRun(tester); // 1. call...
        assertFalse(result.hasError());
        assertEquals("Hello", result.get());
        assertEquals(State.CLOSED, circuitBreaker.getState());

        circuitBreaker.protectedRun(tester); // 2. call...
        circuitBreaker.protectedRun(tester); // 3. call...
        circuitBreaker.protectedRun(tester); // 4. call...

        result = circuitBreaker.protectedRun(tester); // 5. call... still available
        assertFalse(result.hasError());
        assertEquals("Hello", result.get());
        assertEquals(State.CLOSED, circuitBreaker.getState());

        result = circuitBreaker.protectedRun(tester); // 6. call... after the 5. call the tester is unavailable until the 10. call...
        assertTrue(result.hasError());
        //assertEquals(CircuitBreaker.OPEN_STATE_EXCEPTION, result.getError());
        assertEquals(State.HALF_OPEN, circuitBreaker.getState());

        result = circuitBreaker.protectedRun(tester); // 9. call... after fail again, go to OPEN state
        assertEquals(State.OPEN, circuitBreaker.getState());
        //assertEquals(CircuitBreaker.OPEN_STATE_EXCEPTION, result.getError());

        // not cooled down yet
        Thread.sleep(100);
        result = circuitBreaker.protectedRun(tester); // 10. call...
        assertEquals(State.OPEN, circuitBreaker.getState());
        assertEquals(CircuitBreaker.OPEN_STATE_EXCEPTION, result.getError());

        // wait time to recover
        Thread.sleep(1000);
        result = circuitBreaker.protectedRun(tester); // 11. call...
        assertEquals(State.CLOSED, circuitBreaker.getState());
        assertEquals("Hello back again", result.get());

    }

    private Supplier<String> tester(final long delay, final int unavailableAfter, final int unavailableUntil) {
        final AtomicInteger count = new AtomicInteger(0);
        final AtomicBoolean wasUnavailable = new AtomicBoolean(false);
        return () -> {
            final int attempts = count.getAndIncrement();

            log.info("tester answers {} {}", attempts, Thread.currentThread());

            try {
                Thread.sleep(delay);
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }

            if (attempts >= unavailableAfter && attempts < unavailableUntil) {
                wasUnavailable.set(true);
                throw new RuntimeException("Error");
            }

            return (wasUnavailable.get()) ? "Hello back again" : "Hello";
        };
    }
}
