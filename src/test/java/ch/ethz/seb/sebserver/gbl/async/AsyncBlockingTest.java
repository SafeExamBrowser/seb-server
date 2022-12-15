/*
 * Copyright (c) 2022 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.async;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Future;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { AsyncServiceSpringConfig.class, AsyncRunner.class, AsyncService.class })
public class AsyncBlockingTest {

    private static final int TASKS = 10;

    @Autowired
    AsyncRunner asyncRunner;

    @Test
    public void testNoneBlocking() {
        final Collection<Future<String>> features = new ArrayList<>();
        for (int i = 0; i < TASKS; i++) {
            final Future<String> runAsync = this.asyncRunner.runAsync(this::doAsync);
            //System.out.println("*********** run async: " + i);
            features.add(runAsync);
        }
        assertEquals(TASKS, features.size());
        final String reduce = features.stream()
                .map(this::getFromFuture)
                .reduce("", (s1, s2) -> s1 + s2);
        //System.out.println(reduce);
        final int countMatches = StringUtils.countMatches(reduce, "DONE");
        assertEquals(TASKS, countMatches);

        // try to get again, are they cached? --> yes they are!
        final String reduce2 = features.stream()
                .map(this::getFromFuture)
                .reduce("", (s1, s2) -> s1 + s2);
        //System.out.println(reduce2);
        final int countMatches2 = StringUtils.countMatches(reduce2, "DONE");
        assertEquals(TASKS, countMatches2);
    }

    private String getFromFuture(final Future<String> future) {
        try {
            return future.get();
        } catch (final Exception e) {
            return e.getMessage();
        }
    }

    private String doAsync() {
        try {
            Thread.sleep(1000);
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
        return Thread.currentThread().getName() + " --> DONE\n";
    }

}
