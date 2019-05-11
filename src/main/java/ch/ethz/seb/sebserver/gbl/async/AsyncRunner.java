/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.async;

import java.util.concurrent.Future;
import java.util.function.Supplier;

import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;

/** An asynchronous runner that can be used to run a task asynchronously within the Spring context */
@Component
@EnableAsync
public class AsyncRunner {

    /** Calls a given Supplier asynchronously in a new thread and returns a CompletableFuture
     * to get and handle the result later
     *
     * @param supplier The Supplier that gets called asynchronously
     * @return CompletableFuture of the result of the Supplier */
    @Async(AsyncServiceSpringConfig.EXECUTOR_BEAN_NAME)
    public <T> Future<T> runAsync(final Supplier<T> supplier) {
        return new AsyncResult<>(supplier.get());
    }

    @Async(AsyncServiceSpringConfig.EXECUTOR_BEAN_NAME)
    public void runAsync(final Runnable block) {
        block.run();
    }

}
