package de.javaabc.particlesimulation.util.math;

import java.awt.geom.Point2D;

/**
 * A 2D double vector.
 */
public class Vec extends Point2D.Double {
    /**
     * the zero vector
     */
    public static final Vec ZERO = new Vec(0.0, 0.0);

    public Vec(double x, double y) {
        super(x, y);
    }

    public Vec add(Vec addend) {
        return new Vec(x + addend.x, y + addend.y);
    }

    public Vec subtract(Vec subtrahend) {
        return new Vec(x - subtrahend.x, y - subtrahend.y);
    }

    public Vec scale(double factor) {
        return new Vec(x * factor, y * factor);
    }

    /**
     * @return the euclidean length of this vector, squared
     */
    public double sqLength() {
        return x * x + y * y;
    }

    /**
     * Converts this double {@link Vec}tor to an {@link IntVec}tor.
     *
     * @return an {@link IntVec}tor with floor values of this instance
     */
    public IntVec toIntVec() {
        return new IntVec((int) x, (int) y);
    }

    @Override
    public String toString() {
        return "(" + x + "|" + y + ")";
    }
}
