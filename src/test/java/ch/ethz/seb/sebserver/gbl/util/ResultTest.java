/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class ResultTest {

    @Test
    public void testCreate() {
        final Result<String> of = Result.of("VALUE");
        final Result<Object> ofError = Result.ofError(new RuntimeException("Some Error"));

        assertNotNull(of.get());
        assertEquals("VALUE", of.get());

        assertTrue(ofError.hasError());
        assertEquals("Some Error", ofError.getError().getMessage());

        try {
            Result.of(null);
            fail("Exception expected");
        } catch (final Throwable t) {
            assertEquals("value has null reference", t.getMessage());
        }

        try {
            Result.ofError(null);
            fail("Exception expected");
        } catch (final Throwable t) {
            assertEquals("error has null reference", t.getMessage());
        }
    }

    @Test
    public void testMap() {
        final Result<String> resultOf = Result.of("1");
        final Result<String> resultOfError = Result.ofError(new RuntimeException("Some Error"));

        final Result<Integer> numberResult = resultOf.map(r -> Integer.parseInt(r));
        final Result<Integer> numberResultOfError = resultOfError.map(r -> Integer.parseInt(r));

        assertNotNull(numberResult);
        assertEquals(Integer.valueOf(1), numberResult.get());

        assertTrue(numberResultOfError.hasError());
        assertEquals("Some Error", numberResultOfError.getError().getMessage());
    }

    @Test
    public void testFlatMap() {
        final Result<String> resultOf = Result.of("1");
        final Result<String> resultOfError = Result.ofError(new RuntimeException("Some Error"));

        final Result<Integer> numberResult = resultOf.flatMap(r -> Result.of(Integer.parseInt(r)));
        final Result<Integer> numberResultOfError = resultOfError.flatMap(r -> Result.of(Integer.parseInt(r)));

        assertNotNull(numberResult);
        assertEquals(Integer.valueOf(1), numberResult.get());

        assertTrue(numberResultOfError.hasError());
        assertEquals("Some Error", numberResultOfError.getError().getMessage());
    }

    @Test
    public void testOrElse() {
        final Result<String> resultOf = Result.of("ONE");
        final Result<String> resultOfError = Result.ofError(new RuntimeException("Some Error"));

        assertEquals("ONE", resultOf.orElse("TWO"));
        assertEquals("TWO", resultOfError.orElse("TWO"));

        assertEquals("ONE", resultOf.orElse(() -> "TWO"));
        assertEquals("TWO", resultOfError.orElse(() -> "TWO"));
    }

    @Test
    public void testOnError() {
        final Result<String> resultOf = Result.of("ONE");
        final Result<String> resultOfError = Result.ofError(new RuntimeException("Some Error"));

        assertEquals("ONE", resultOf.onError(t -> t.getMessage()));
        assertEquals("Some Error", resultOfError.onError(t -> t.getMessage()));

        assertEquals("ONE", resultOf.onErrorThrow("Should not be thrown"));
        try {
            resultOfError.onErrorThrow("Should be thrown");
            fail("Excpetion expected here");
        } catch (final Throwable t) {
            assertEquals("Should be thrown", t.getMessage());
            assertEquals("Some Error", t.getCause().getMessage());
        }
    }

}
