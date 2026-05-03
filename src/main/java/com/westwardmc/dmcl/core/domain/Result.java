package com.westwardmc.dmcl.core.domain;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;

public sealed interface Result<T, E> {
    static <T, E> Result<T, E> ok(T value) { return new Ok<>(value); }
    static <T, E> Result<T, E> err(E error) { return new Err<>(error); }

    boolean isOk();
    T unwrap();
    E unwrapErr();
    <U> Result<U, E> map(Function<T, U> fn);
    <F> Result<T, F> mapErr(Function<E, F> fn);

    record Ok<T, E>(T value) implements Result<T, E> {
        public Ok { Objects.requireNonNull(value); }
        public boolean isOk() { return true; }
        public T unwrap() { return value; }
        public E unwrapErr() { throw new NoSuchElementException("Ok has no error"); }
        public <U> Result<U, E> map(Function<T, U> fn) { return new Ok<>(fn.apply(value)); }
        @SuppressWarnings("unchecked")
        public <F> Result<T, F> mapErr(Function<E, F> fn) { return (Result<T, F>) this; }
    }

    record Err<T, E>(E error) implements Result<T, E> {
        public Err { Objects.requireNonNull(error); }
        public boolean isOk() { return false; }
        public T unwrap() { throw new NoSuchElementException("Err has no value"); }
        public E unwrapErr() { return error; }
        @SuppressWarnings("unchecked")
        public <U> Result<U, E> map(Function<T, U> fn) { return (Result<U, E>) this; }
        public <F> Result<T, F> mapErr(Function<E, F> fn) { return new Err<>(fn.apply(error)); }
    }
}
