package de.javaabc.particlesimulation.util.math;

/**
 * An integer vector holding an x and y value.
 *
 * @param x the horizontal entry
 * @param y the vertical entry
 */
public record IntVec(int x, int y) {
    /**
     * the zero vector
     */
    public static final IntVec ZERO = new IntVec(0, 0);

    public IntVec add(IntVec addend) {
        return new IntVec(x + addend.x, y + addend.y);
    }

    public IntVec subtract(IntVec subtrahend) {
        return new IntVec(x - subtrahend.x, y - subtrahend.y);
    }

    /**
     * Checks if this vector is inside a given range.
     *
     * @param minXIncl the minimum required x position, inclusive
     * @param minYIncl the minimum required y position, inclusive
     * @param maxXExcl the maximum allowed x position, exclusive
     * @param maxYExcl the maximum allowed y position, exclusive
     * @return true iff this vector is inside the specified rectangle
     */
    public boolean isInRange(int minXIncl, int minYIncl, int maxXExcl, int maxYExcl) {
        return x >= minXIncl && y >= minYIncl && x < maxXExcl && y < maxYExcl;
    }

    @Override
    public String toString() {
        return "(" + x + "|" + y + ")";
    }
}
