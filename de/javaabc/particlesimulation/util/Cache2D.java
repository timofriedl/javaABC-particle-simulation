package de.javaabc.particlesimulation.util;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A cache that maps the combination of two input values to one output value.
 *
 * @param <A> the type of the first input value
 * @param <B> the type of the second input value
 * @param <Y> the stored output type
 */
public class Cache2D<A, B, Y> {
    /**
     * a map of maps that stores all values
     */
    private final Map<A, Map<B, Y>> map;

    public Cache2D() {
        map = new HashMap<>();
    }

    /**
     * Returns the cached value for the inputs (a, b) or generates a new input and caches the result.
     *
     * @param a         the first key
     * @param b         the second key
     * @param generator a {@link Supplier} that generates the value for a and b if it is not computed already
     * @return the cached value, or the generated value if there has no value been cached yet
     */
    public Y storeIfAbsent(A a, B b, Supplier<Y> generator) {
        Y res;

        var inner = map.get(a);
        if (inner == null) {
            inner = new HashMap<>(1);
            inner.put(b, res = generator.get());
            map.put(a, inner);
        } else {
            res = inner.get(b);
            if (res == null)
                inner.put(b, res = generator.get());
        }

        return res;
    }
}
