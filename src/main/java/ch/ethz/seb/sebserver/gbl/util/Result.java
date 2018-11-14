/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.util;

import java.util.function.Function;
import java.util.function.Supplier;

/** A result of a computation that can either be the resulting value of the computation
 * or an error if an exception/error has been thrown during the computation.
 *
 * Use this to make code more resilient at the same time as avoid many try-catch-blocks
 * on root exceptions/errors and the need of nested exceptions.
 * More specific: use this if a none void method should always give a result and never throw an exception
 * by its own but reporting an exception when happen within the Result.
 *
 * <pre>
 *      public Result<String> compute(String s1, String s2) {
 *          try {
 *              ... do some computation
 *              return Result.of(result);
 *          } catch (Throwable t) {
 *              return Result.ofError(t);
 *          }
 *      }
 * </pre>
 *
 * If you are familiar with <code>java.util.Optional</code> think of Result like an Optional with the
 * capability to report an error.
 *
 * @param <T> The type of the result value */
public final class Result<T> {

    /** The resulting value. May be null if an error occurred */
    private final T value;
    /** The error when happened otherwise null */
    private final Throwable error;

    private Result(final T value) {
        this.value = value;
        this.error = null;
    }

    private Result(final Throwable error) {
        this.value = null;
        this.error = error;
    }

    /** @return the Value of the Result or null if there was an error */
    public T get() {
        return this.value;
    }

    /** @return the error if some was reporter or null if there was no error */
    public Throwable getError() {
        return this.error;
    }

    /** Use this to get the resulting value or (if null) to get a given other value
     *
     * @param other the other value to get if the computed value is null
     * @return return either the computed value if existing or a given other value */
    public T orElse(final T other) {
        return this.value != null ? this.value : other;
    }

    public T orElse(final Supplier<T> supplier) {
        return this.value != null ? this.value : supplier.get();
    }

    public <U> Result<U> map(final Function<? super T, ? extends U> mapf) {
        if (this.error == null) {
            return Result.of(mapf.apply(this.value));
        } else {
            return Result.ofError(this.error);
        }
    }

    public <U> Result<U> flatMap(final Function<? super T, Result<U>> mapf) {
        if (this.error == null) {
            return mapf.apply(this.value);
        } else {
            return Result.ofError(this.error);
        }
    }

    public T onError(final Function<Throwable, T> errorHandler) {
        return this.error != null ? errorHandler.apply(this.error) : this.value;
    }

    public static final <T> Result<T> of(final T value) {
        return new Result<>(value);
    }

    public static final <T> Result<T> ofError(final Throwable error) {
        return new Result<>(error);
    }

    /** Use this to get the resulting value of existing or throw an Runtime exception with
     * given message otherwise.
     *
     * @param message the message for the RuntimeException in error case
     * @return the resulting value */
    public T onErrorThrow(final String message) {
        if (this.error != null) {
            throw new RuntimeException(message, this.error);
        }

        return this.value;
    }
}
