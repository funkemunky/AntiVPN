/*
 * Copyright (C) 2007 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package dev.brighten.antivpn.utils;

import java.io.Serializable;

import static dev.brighten.antivpn.utils.NullnessCasts.uncheckedCastNullableTToT;
import static dev.brighten.antivpn.utils.Preconditions.checkNotNull;
import static java.util.Objects.requireNonNull;

/**
 * Useful suppliers.
 *
 * <p>All methods return serializable suppliers as long as they're given serializable parameters.
 *
 * @author Laurence Gonsalves
 * @author Harry Heymann
 * @since 2.0
 */
public final class Suppliers {
    private Suppliers() {}

    /**
     * Returns a supplier which caches the instance retrieved during the first call to {@code get()}
     * and returns that value on subsequent calls to {@code get()}. See: <a
     * href="http://en.wikipedia.org/wiki/Memoization">memoization</a>
     *
     * <p>The returned supplier is thread-safe. The delegate's {@code get()} method will be invoked at
     * most once unless the underlying {@code get()} throws an exception. The supplier's serialized
     * form does not contain the cached value, which will be recalculated when {@code get()} is called
     * on the reserialized instance.
     *
     * <p>When the underlying delegate throws an exception then this memoizing supplier will keep
     * delegating calls until it returns valid data.
     *
     * <p>If {@code delegate} is an instance created by an earlier call to {@code memoize}, it is
     * returned directly.
     */
    public static <T> Supplier<T> memoize(Supplier<T> delegate) {
        if (delegate instanceof NonSerializableMemoizingSupplier
                || delegate instanceof MemoizingSupplier) {
            return delegate;
        }
        return delegate instanceof Serializable
                ? new MemoizingSupplier<>(delegate)
                : new NonSerializableMemoizingSupplier<>(delegate);
    }

    static class MemoizingSupplier<T> implements Supplier<T>, Serializable {
        final Supplier<T> delegate;
        transient volatile boolean initialized;
        // "value" does not need to be volatile; visibility piggy-backs
        // on volatile read of "initialized".
        transient T value;

        MemoizingSupplier(Supplier<T> delegate) {
            this.delegate = checkNotNull(delegate);
        }

        @Override
        public T get() {
            // A 2-field variant of Double Checked Locking.
            if (!initialized) {
                synchronized (this) {
                    if (!initialized) {
                        T t = delegate.get();
                        value = t;
                        initialized = true;
                        return t;
                    }
                }
            }
            // This is safe because we checked `initialized.`
            return uncheckedCastNullableTToT(value);
        }

        @Override
        public String toString() {
            return "Suppliers.memoize("
                    + (initialized ? "<supplier that returned " + value + ">" : delegate)
                    + ")";
        }

        private static final long serialVersionUID = 0;
    }

    static class NonSerializableMemoizingSupplier<T> implements Supplier<T> {
        volatile Supplier<T> delegate;
        volatile boolean initialized;
        // "value" does not need to be volatile; visibility piggy-backs
        // on volatile read of "initialized".
        T value;

        NonSerializableMemoizingSupplier(Supplier<T> delegate) {
            this.delegate = checkNotNull(delegate);
        }

        @Override
        public T get() {
            // A 2-field variant of Double Checked Locking.
            if (!initialized) {
                synchronized (this) {
                    if (!initialized) {
                        /*
                         * requireNonNull is safe because we read and write `delegate` under synchronization.
                         *
                         * TODO(cpovirk): To avoid having to check for null, replace `delegate` with a singleton
                         * `Supplier` that always throws an exception.
                         */
                        T t = requireNonNull(delegate).get();
                        value = t;
                        initialized = true;
                        // Release the delegate to GC.
                        delegate = null;
                        return t;
                    }
                }
            }
            // This is safe because we checked `initialized.`
            return uncheckedCastNullableTToT(value);
        }

        @Override
        public String toString() {
            Supplier<T> delegate = this.delegate;
            return "Suppliers.memoize("
                    + (delegate == null ? "<supplier that returned " + value + ">" : delegate)
                    + ")";
        }
    }
}
