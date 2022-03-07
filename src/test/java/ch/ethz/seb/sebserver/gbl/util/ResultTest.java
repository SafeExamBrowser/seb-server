/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.util;

import static org.junit.Assert.*;

import java.util.concurrent.ExecutionException;

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

        assertEquals("ONE", resultOf.getOr("TWO"));
        assertEquals("TWO", resultOfError.getOr("TWO"));

        assertEquals("ONE", resultOf.getOrElse(() -> "TWO"));
        assertEquals("TWO", resultOfError.getOrElse(() -> "TWO"));

        Result<String> orElse = resultOfError.orElse(() -> {
            return "Else";
        });
        assertNotNull(orElse);
        assertFalse(orElse.hasError());
        assertEquals("Else", orElse.get());

        orElse = resultOf.orElse(() -> {
            return "Else";
        });

        assertNotNull(orElse);
        assertFalse(orElse.hasError());
        assertEquals("ONE", orElse.get());
    }

    @Test
    public void testOnError() {
        final Result<String> resultOf = Result.of("ONE");
        final Result<String> resultOfError = Result.ofError(new RuntimeException("Some Error"));

        assertEquals("ONE", resultOf.get(t -> t.getMessage()));
        assertEquals("Some Error", resultOfError.get(t -> t.getMessage()));

        assertEquals("ONE", resultOf.getOrThrowRuntime("Should not be thrown"));
        try {
            resultOfError.getOrThrowRuntime("Should be thrown");
            fail("Excpetion expected here");
        } catch (final Throwable t) {
            assertEquals("Should be thrown", t.getMessage());
            assertEquals("Some Error", t.getCause().getMessage());
        }
    }

    @Test
    public void testGetWithError() {
        final Result<String> resultOf = Result.of("ONE");
        String string = resultOf.get(error -> fail("should net be called"), () -> "ERROR");
        assertEquals("ONE", string);
        string = resultOf.getOrThrow();
        assertEquals("ONE", string);

        final Result<String> errorOf = Result.ofError(new RuntimeException("error"));
        string = errorOf.get(error -> assertEquals("error", error.getMessage()), () -> "ERROR");
        assertEquals("ERROR", string);

        errorOf.handleError(error -> assertEquals("error", error.getMessage()));
        try {
            errorOf.getOrThrow();
        } catch (final Exception e) {
            assertEquals("error", e.getMessage());
        }
        try {
            errorOf.getOrThrow(error -> new RuntimeException("error2"));
        } catch (final Exception e) {
            assertEquals("error2", e.getMessage());
        }

        final String orThrow = resultOf.getOrThrow();
        assertEquals("ONE", orThrow);

        assertTrue(resultOf.hasValue());
        assertFalse(errorOf.hasValue());

        resultOf.ifPresent(t -> {
            assertEquals("ONE", t);
        });

        errorOf.ifPresent(t -> {
            fail("Should not be called here");
        });

        final Result<String> onSuccess = resultOf.onSuccess(t -> {
            assertEquals("ONE", t);
        });
        assertNotNull(onSuccess);

        errorOf.onSuccess(t -> {
            fail("Should not be called here");
        });

        final Result<String> onError = errorOf.onError(error -> {
            assertNotNull(error);
            assertEquals("error", error.getMessage());
        });
        assertNotNull(onError);

        final Result<String> onErrorDo = errorOf.onErrorDo(error -> {
            assertNotNull(error);
            assertEquals("error", error.getMessage());
            return error.getMessage();
        });
        assertNotNull(onErrorDo);
        assertEquals("error", onErrorDo.get());

        errorOf.onErrorDo(error -> {
            assertNotNull(error);
            assertEquals("error", error.getMessage());
            return error.getMessage();
        }, RuntimeException.class);

        errorOf.onErrorDo(error -> {
            fail("Should not be called here");
            return "";
        }, ExecutionException.class);

    }

    @Test
    public void testEquals() {
        final Result<String> one = Result.of("ONE");
        final Result<String> two = Result.of("TWO");
        final Result<String> one_ = Result.of("ONE");

        assertEquals(one, one);
        assertEquals(one, one_);
        assertNotEquals(one, two);
        assertNotEquals(null, two);
        assertNotEquals(one, null);
    }

}
