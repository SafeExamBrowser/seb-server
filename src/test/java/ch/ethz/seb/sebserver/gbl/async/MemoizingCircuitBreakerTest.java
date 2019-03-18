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

import ch.ethz.seb.sebserver.gbl.async.CircuitBreakerSupplier.State;
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
        final CircuitBreakerSupplier<String> circuitBreaker = this.asyncService.createCircuitBreaker(
                tester(100, 5, 10), 3, 500, 1000, true);

        assertNull(circuitBreaker.getChached());

        Result<String> result = circuitBreaker.get(); // 1. call...
        assertFalse(result.hasError());
        assertEquals("Hello", result.get());
        assertEquals("Hello", circuitBreaker.getChached());
        assertEquals(State.CLOSED, circuitBreaker.getState());

        circuitBreaker.get(); // 2. call...
        circuitBreaker.get(); // 3. call...
        circuitBreaker.get(); // 4. call...

        result = circuitBreaker.get(); // 5. call... still available
        assertFalse(result.hasError());
        assertEquals("Hello", result.get());
        assertEquals("Hello", circuitBreaker.getChached());
        assertEquals(State.CLOSED, circuitBreaker.getState());

        result = circuitBreaker.get(); // 6. call... after the 5. call the tester is unavailable until the 10. call...
        assertFalse(result.hasError());
        assertEquals("Hello", result.get());
        assertEquals("Hello", circuitBreaker.getChached());
        assertEquals(State.HALF_OPEN, circuitBreaker.getState());

        result = circuitBreaker.get(); // 9. call... after fail again, go to OPEN state
        assertEquals(State.OPEN, circuitBreaker.getState());

        // now the time to recover comes into play
        Thread.sleep(1100);
        result = circuitBreaker.get(); // 10. call...
        assertEquals(State.HALF_OPEN, circuitBreaker.getState());
        assertEquals("Hello", result.get());

        // back again
        result = circuitBreaker.get(); // 15. call... 2. call in Half Open state...
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

    // TODO timeout test: test also the behavior on timeout, is the thread being interrupted and released or not (should!)

}
