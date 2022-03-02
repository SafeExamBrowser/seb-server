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
public class MemoizingCircuitBreakerTest {

    private static final Logger log = LoggerFactory.getLogger(MemoizingCircuitBreakerTest.class);

    @Autowired
    AsyncService asyncService;

    @Test
    public void testInit() {
        assertNotNull(this.asyncService);
    }

    @Test
    public void roundtrip1() throws InterruptedException {
        final MemoizingCircuitBreaker<String> circuitBreaker = this.asyncService.createMemoizingCircuitBreaker(
                tester(100, 5, 10), 3, 500, 1000, true, 1000);

        assertNull(circuitBreaker.getCached());

        Result<String> result = circuitBreaker.get(); // 1. call...
        assertFalse(result.hasError());
        assertEquals("Hello", result.get());
        assertEquals("Hello", circuitBreaker.getCached());
        assertEquals(State.CLOSED, circuitBreaker.getState());

        circuitBreaker.get(); // 2. call...
        circuitBreaker.get(); // 3. call...
        circuitBreaker.get(); // 4. call...

        result = circuitBreaker.get(); // 5. call... still available
        assertFalse(result.hasError());
        assertEquals("Hello", result.get());
        assertEquals("Hello", circuitBreaker.getCached());
        assertEquals(State.CLOSED, circuitBreaker.getState());

        result = circuitBreaker.get(); // 6. call... after the 5. call the tester is unavailable until the 10. call...
        assertFalse(result.hasError());
        assertEquals("Hello", result.get());
        assertEquals("Hello", circuitBreaker.getCached());
        assertEquals(State.HALF_OPEN, circuitBreaker.getState());

        result = circuitBreaker.get(); // 9. call... after fail again, go to OPEN state
        assertEquals(State.OPEN, circuitBreaker.getState());

        // not cooled down yet
        Thread.sleep(100);
        result = circuitBreaker.get(); // 10. call...
        assertEquals(State.OPEN, circuitBreaker.getState());

        // wait time to recover
        Thread.sleep(1000);
        result = circuitBreaker.get(); // 11. call...
        assertEquals(State.CLOSED, circuitBreaker.getState());
        assertEquals("Hello back again", result.get());

    }

    @Test
    public void failing() throws InterruptedException {
        final MemoizingCircuitBreaker<String> circuitBreaker = this.asyncService.createMemoizingCircuitBreaker(
                tester(50, 1, 10), 3, 100, 100, true, 1000);

        assertNull(circuitBreaker.getCached());

        // fist call okay.. for memoizing
        Result<String> result = circuitBreaker.get(); // 1. call...
        assertFalse(result.hasError());
        assertEquals("Hello", result.get());
        assertTrue(circuitBreaker.getLastMemoizingTime() > 0);

        // second call is okay from memoizing
        Thread.sleep(100);
        result = circuitBreaker.get(); // 2. call...
        assertFalse(result.hasError());
        assertEquals("Hello", result.get());
        assertTrue(circuitBreaker.getLastMemoizingTime() > 0);

        // third call is failing because of memoizing timeout
        Thread.sleep(1000);
        result = circuitBreaker.get(); // 3. call...
        assertTrue(result.hasError());
    }

    private Supplier<String> tester(final long delay, final int unavailableAfter, final int unavailableUntil) {
        final AtomicInteger count = new AtomicInteger(0);
        final AtomicBoolean wasUnavailable = new AtomicBoolean(false);
        return () -> {
            final int attempts = count.getAndIncrement();

            log.debug("tester answers {} {}", attempts, Thread.currentThread());

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
