package de.javaabc.particlesimulation.util.math;

public class MathUtil {
    /**
     * Helper method to (more or less) efficiently compute the 6th power of a given double x.
     *
     * @param x the value to raise to the 6th power
     * @return x^6
     */
    public static double fastPow6(double x) {
        x = x * x;
        return x * x * x;
    }
}
