/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.async;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Lazy
@Service
public class AsyncService {

    private static final Logger log = LoggerFactory.getLogger(AsyncService.class);

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

    public <T> MemoizingCircuitBreaker<T> createMemoizingCircuitBreaker(
            final Supplier<T> blockingSupplier) {

        return new MemoizingCircuitBreaker<>(this.asyncRunner, blockingSupplier, true);
    }

    public <T> MemoizingCircuitBreaker<T> createMemoizingCircuitBreaker(
            final Supplier<T> blockingSupplier,
            final int maxFailingAttempts,
            final long maxBlockingTime,
            final long timeToRecover,
            final boolean momoized) {

        return new MemoizingCircuitBreaker<>(
                this.asyncRunner,
                blockingSupplier,
                maxFailingAttempts,
                maxBlockingTime,
                timeToRecover,
                momoized);
    }

    public void pipeToOutputStream(
            final OutputStream output,
            final Consumer<PipedOutputStream> consumer) {

        this.asyncRunner.runAsync(() -> {

            PipedOutputStream pout = null;
            PipedInputStream pin = null;
            try {
                pout = new PipedOutputStream();
                pin = new PipedInputStream(pout);

                consumer.accept(pout);

                IOUtils.copyLarge(pin, output);

                pin.close();
                pout.flush();
                pout.close();

            } catch (final IOException e) {
                log.error("Error while pipe stream data: ", e);
            } finally {
                try {
                    pin.close();
                } catch (final IOException e1) {
                    log.error("Failed to close PipedInputStream: ", e1);
                }
                try {
                    pout.close();
                } catch (final IOException e1) {
                    log.error("Failed to close PipedOutputStream: ", e1);
                }
            }
        });
    }

}
