/*
 * Copyright (c) 2018 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gbl.util;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 *          } catch (Exception error) {
 *              return Result.ofError(t);
 *          }
 *      }
 * </pre>
 *
 * Or use the Results tryCatch that wraps the given code block in a try catch internally
 *
 * <pre>
 *      public Result<String> compute(String s1, String s2) {
 *          return Result.tryCatch(() -> {
 *              ... do some computation
 *              return result;
 *          });
 *      }
 * </pre>
 *
 * If you are familiar with <code>java.util.Optional</code> think of Result like an Optional with the
 * capability to report an error.
 *
 * @param <T> The type of the result value */
public final class Result<T> {

    private static final Logger log = LoggerFactory.getLogger(Result.class);

    /** This unique Result instance marks an empty result (value == null) that has no errors
     * and can be used for as result for void return types. */
    public static final Result<Void> EMPTY = new Result<>(null);

    /** The resulting value. May be null if an error occurred */
    private final T value;
    /** The error when happened otherwise null */
    private final Exception error;

    private Result(final T value) {
        this.value = value;
        this.error = null;
    }

    private Result(final Exception error) {
        this.value = null;
        this.error = error;
    }

    /** @return the Value of the Result or null if there was an error */
    public T get() {
        return this.value;
    }

    /** Use this to get the resulting value. In an error case, a given error handling
     * function is used that receives the error and returns a resulting value instead
     * (or throw some error instead)
     *
     * @param errorHandler the error handling function
     * @return the resulting value of processing with Result */
    public T get(final Function<Exception, T> errorHandler) {
        return this.error != null ? errorHandler.apply(this.error) : this.value;
    }

    /** Use this to get the referenced result element or on error case, use the given error handler
     * to handle the error and use a given supplier to get an alternative element for further processing
     *
     * @param errorHandler the error handler to handle an error if happened
     * @param supplier supplies an alternative result element on error case
     * @return returns the referenced result element or the alternative element given by the supplier on error */
    public T get(final Consumer<Exception> errorHandler, final Supplier<T> supplier) {
        if (this.error != null) {
            errorHandler.accept(this.error);
            return supplier.get();
        } else {
            return this.value;
        }
    }

    /** Apply a given error handler that consumes the error if there is one.
     *
     * @param errorHandler the error handler */
    public void handleError(final Consumer<Exception> errorHandler) {
        if (this.error != null) {
            errorHandler.accept(this.error);
        }
    }

    /** Use this to get the resulting value or (if null) to get a given other value
     *
     * @param other the other value to get if the computed value is null
     * @return return either the computed value if existing or a given other value */
    public T getOr(final T other) {
        return this.value != null ? this.value : other;
    }

    /** Use this to get the resulting value if existing or throw an Runtime exception with
     * given message otherwise.
     *
     * @param message the message for the RuntimeException in error case
     * @return the resulting value */
    public T getOrThrowRuntime(final String message) {
        if (this.error != null) {
            throw new RuntimeException(message, this.error);
        }

        return this.value;
    }

    /** Get the referenced result element or in error case, throws the referenced error
     *
     * @return the referenced result element */
    public T getOrThrow() {
        if (this.error != null) {
            if (this.error instanceof RuntimeException) {
                throw (RuntimeException) this.error;
            } else {
                throw new RuntimeException("RuntimeExceptionWrapper cause: ", this.error);
            }
        }

        return this.value;
    }

    public T getOrThrow(final Function<Exception, RuntimeException> errorWrapper) {
        if (this.error != null) {
            throw errorWrapper.apply(this.error);
        }

        return this.value;
    }

    /** Use this to get the resulting value or (if null) to get a given other value
     *
     * @param supplier supplier to get the value from if the computed value is null
     * @return return either the computed value if existing or a given other value */
    public T getOrElse(final Supplier<T> supplier) {
        return this.value != null ? this.value : supplier.get();
    }

    public Result<T> orElse(final Supplier<Result<T>> supplier) {
        return this.value != null ? this : supplier.get();
    }

    public Result<T> orElseTry(final Supplier<T> supplier) {
        return this.value != null ? this : Result.tryCatch(supplier::get);
    }

    /** @return the error if some was reporter or null if there was no error */
    public Exception getError() {
        return this.error;
    }

    /** Indicates whether this Result refers to an error or not.
     *
     * @return true if this Result refers to an error */
    public boolean hasError() {
        return this.error != null;
    }

    /** Indicates whether this Result refers to a value or not.
     *
     * @return true if this Result refers to a value (not null) and has no error */
    public boolean hasValue() {
        return this.value != null && this.error == null;
    }

    /** If a value is present, performs the given action with the value,
     * otherwise performs the given empty-based action.
     *
     * @param action the action to be performed, if a value is present
     * @param emptyAction the empty-based action to be performed, if no value is
     *            present
     * @throws NullPointerException if a value is present and the given action
     *             is {@code null}, or no value is present and the given empty-based
     *             action is {@code null}. */
    public void ifOrElse(final Consumer<? super T> action, final Runnable emptyAction) {
        if (this.value != null) {
            action.accept(this.value);
        } else {
            emptyAction.run();
        }
    }

    public void ifPresent(final Consumer<T> consumer) {
        if (this == EMPTY) {
            consumer.accept(this.value);
            return;
        }
        if (this.value != null) {
            consumer.accept(this.value);
        }
    }

    /** Use this to map a given Result of type T to another Result of type U
     * within a given mapping function.
     *
     * @param mapFunction the mapping function
     * @return mapped Result of type U */
    public <U> Result<U> map(final Function<? super T, ? extends U> mapFunction) {
        if (this.error == null) {
            try {
                final U result = mapFunction.apply(this.value);
                if (result instanceof Result) {
                    throw new IllegalArgumentException("Use flatMap instead!");
                }
                return Result.of(result);
            } catch (final Exception e) {
                return Result.ofError(e);
            }
        } else {
            return Result.ofError(this.error);
        }
    }

    /** Use this to map a given Result of type T to another Result of type U
     * within a given mapping function.
     *
     * <p>
     * This method is similar to {@link #map(Function)}, but the mapping
     * function is one whose result is already an {@code Result}, and if
     * invoked, {@code flatMap} does not wrap it within an additional
     * {@code Result}.
     *
     * @param mapFunction the mapping function
     * @return mapped Result of type U */
    public <U> Result<U> flatMap(final Function<? super T, Result<U>> mapFunction) {
        if (this.error == null) {
            try {
                return mapFunction.apply(this.value);
            } catch (final Exception e) {
                return Result.ofError(e);
            }
        } else {
            return Result.ofError(this.error);
        }
    }

    public Result<T> onSuccess(final Consumer<T> handler) {
        if (this.error == null) {
            handler.accept(this.value);
        }
        return this;
    }

    /** Uses a given error handler to apply an error if there is one and returning itself again
     * for further processing.
     *
     * @param errorHandler the error handler
     * @return self reference */
    public Result<T> onError(final Consumer<Exception> errorHandler) {
        if (this.error != null) {
            errorHandler.accept(this.error);
        }
        return this;
    }

    public Result<T> onErrorDo(final Function<Exception, T> errorHandler) {
        if (this.error != null) {
            return new Result<>(errorHandler.apply(this.error));
        }
        return this;
    }

    public Result<T> onErrorDo(final Function<Exception, T> errorHandler, final Class<? extends Exception> errorType) {
        if (this.error != null && errorType.isAssignableFrom(this.error.getClass())) {
            return new Result<>(errorHandler.apply(this.error));
        }
        return this;
    }

    /** Use this to create a Result of a given resulting value.
     *
     * @param value resulting value
     * @return Result instance contains a resulting value and no error */
    public static <T> Result<T> of(final T value) {
        if (value == null) {
            throw new IllegalArgumentException("value has null reference");
        }
        return new Result<>(value);
    }

    /** Use this to create a Result with error
     *
     * @param error the error that is wrapped within the created Result
     * @return Result of specified error */
    public static <T> Result<T> ofError(final Exception error) {
        assert error != null : "error has null reference";
        return new Result<>(error);
    }

    /** Use this to create a Result with error
     *
     * @param message the error message
     * @return Result of specified error */
    public static <T> Result<T> ofRuntimeError(final String message) {
        return ofError(new RuntimeException(message));
    }

    public static <T> Result<T> tryCatch(final TryCatchSupplier<T> supplier) {
        try {
            return Result.of(supplier.get());
        } catch (final Exception e) {
            return Result.ofError(e);
        }
    }

    public static Result<Void> tryCatch(final TryCatchRunnable runnable) {
        try {
            runnable.run();
            return Result.EMPTY;
        } catch (final Exception e) {
            return Result.ofError(e);
        }
    }

    public static <T> Stream<T> skipOnError(final Result<T> result) {
        if (result.error != null) {
            return Stream.empty();
        } else {
            return Stream.of(result.value);
        }
    }

    public static <T> Stream<T> onErrorLogAndSkip(final Result<T> result) {
        if (result.error != null) {
            log.error("Unexpected error on result. Cause: ", result.error);
            return Stream.empty();
        } else {
            return Stream.of(result.value);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> Result<T> ofEmpty() {
        return (Result<T>) EMPTY;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.value == null) ? 0 : this.value.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final Result<?> other = (Result<?>) obj;
        if (this.value == null) {
            if (other.value != null)
                return false;
        } else if (!this.value.equals(other.value))
            return false;
        return true;
    }

    @Override
    public String toString() {
        throw new RuntimeException("Result.toString is probably called by mistake !!!");
    }

    public interface TryCatchSupplier<T> {
        T get() throws Exception;
    }

    public interface TryCatchRunnable {
        void run() throws Exception;
    }

}
