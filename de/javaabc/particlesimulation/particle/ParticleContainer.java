package de.javaabc.particlesimulation.particle;

import de.javaabc.particlesimulation.util.math.IntVec;
import de.javaabc.particlesimulation.util.math.Vec;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A more or less efficiently implemented {@link Particle} container suitable for multithreaded force calculation of neighbored particles.
 */
public class ParticleContainer implements Iterable<Particle> {
    /**
     * A 2D array of {@link Block}s.
     * Each block holds its own list of particles.
     * When computing forces, only neighbored blocks are considered in order to speed up computation.
     */
    private final Block[][] blocks;

    /**
     * the width and height of one block in px
     */
    private final double blockSize;

    /**
     * the scale factor that is defined as 1/blockSize
     */
    private final double scaleFactor;

    /**
     * the current number of particles in this container
     */
    private int size;

    /**
     * Creates a new particle container instance.
     *
     * @param width     the horizontal number of blocks.
     * @param height    the vertical number of blocks.
     * @param blockSize the width and height of one block in px
     */
    public ParticleContainer(int width, int height, double blockSize) {
        blocks = new Block[height][width];
        for (int y = 0; y < getHeight(); y++)
            for (int x = 0; x < getWidth(); x++)
                blocks[y][x] = new Block();

        this.blockSize = blockSize;
        scaleFactor = 1.0 / blockSize;
    }

    /**
     * Converts a pixel position to a block position.
     *
     * @param pos a {@link Vec}tor containing the x and y position on screen
     * @return an {@link IntVec}tor containing the x and y position of the corresponding block
     */
    private IntVec blockPos(Vec pos) {
        return pos.scale(scaleFactor).toIntVec();
    }

    /**
     * Adds a particle to this container, if its position is somewhere inside a block.
     * If not, the particle is not added and all spring connections from and to this particle are removed.
     *
     * @param p the particle to add
     */
    public void add(Particle p) {
        IntVec pos = blockPos(p.getPos());
        if (!pos.isInRange(0, 0, getWidth(), getHeight())) {
            System.err.println("Removed " + p);
            stream(true).forEach(cp -> cp.removeConnectionTo(p));
            return;
        }

        blocks[pos.y()][pos.x()].add(p);
    }

    /**
     * Removes a particle from this container.
     * All spring connections from and to this particle are removed.
     * Only works if the particle's position is matching with the position of the block containing the particle.
     *
     * @param p the particle to remove
     */
    public void remove(Particle p) {
        IntVec pos = blockPos(p.getPos());
        blocks[pos.y()][pos.x()].remove(p);
        stream(true).forEach(cp -> cp.removeConnectionTo(p));
    }

    /**
     * @return the horizontal number of blocks
     */
    private int getWidth() {
        return blocks[0].length;
    }

    /**
     * @return the vertical number of blocks
     */
    private int getHeight() {
        return blocks.length;
    }

    /**
     * @return the number of particles in this container
     */
    public int size() {
        return size;
    }

    /**
     * Creates an {@link Iterator} over all particles within a rectangle of blocks.
     *
     * @param minIncl the top left position of the rectangle to iterate through, inclusive
     * @param maxExcl the bottom right position of the rectangle to iterate through, exclusive
     * @return an iterator over all particles within the rectangle
     */
    public Iterator<Particle> iterator(IntVec minIncl, IntVec maxExcl) {
        int minX = Math.max(0, Math.min(getWidth(), minIncl.x()));
        int minY = Math.max(0, Math.min(getHeight(), minIncl.y()));
        int maxX = Math.max(0, Math.min(getWidth(), maxExcl.x()));
        int maxY = Math.max(0, Math.min(getHeight(), maxExcl.y()));

        return new Iterator<>() {
            private int x = minX, y = minY;
            private Iterator<Particle> currentIt = blocks[y][x].iterator();
            private Particle next;

            @Override
            public boolean hasNext() {
                if (next != null)
                    return true;

                if (y >= maxY)
                    return false;

                while (!currentIt.hasNext()) {
                    if (++x >= maxX) {
                        x = minX;
                        if (++y >= maxY)
                            return false;
                    }
                    currentIt = blocks[y][x].iterator();
                }

                next = currentIt.next();
                return true;
            }

            @Override
            public Particle next() {
                if (!hasNext())
                    throw new NoSuchElementException();

                var res = next;
                next = null;
                return res;
            }
        };
    }

    @Override
    public Iterator<Particle> iterator() {
        return iterator(IntVec.ZERO, new IntVec(getWidth(), getHeight()));
    }

    /**
     * Creates a stream of all particles in this container.
     *
     * @param parallel the option to return a parallel stream
     * @return a stream of all particles
     */
    public Stream<Particle> stream(boolean parallel) {
        return StreamSupport.stream(spliterator(), parallel);
    }

    /**
     * Applies a given action for each particle in this container in a parallel manner.
     *
     * @param action the action to apply for each particle
     */
    public void forEachParallel(Consumer<? super Particle> action) {
        stream(true).forEach(action);
    }

    /**
     * Applies a given action for each distinct pair of particles in this container.
     *
     * @param action the action to apply for each distinct pair
     */
    public void forEachPair(BiConsumer<Particle, Particle> action) {
        forEachParallel(p1 -> {
            IntVec blockPos = blockPos(p1.getPos());
            for (Iterator<Particle> it = iterator(blockPos.subtract(new IntVec(1, 1)), blockPos.add(new IntVec(2, 2))); it.hasNext(); ) {
                var p2 = it.next();
                Vec dx = p2.getPos().subtract(p1.getPos());
                double sum = dx.getX() + dx.getY();
                if (sum > 0.0 || sum == 0.0 && dx.getX() > 0.0)
                    action.accept(p1, p2);
            }
        });
    }

    /**
     * Sorts all particles into the correct block.
     */
    public void updateBlocks() {
        List<Map.Entry<Particle, Block>> wrongParticles = new ArrayList<>();

        for (int y = 0; y < getHeight(); y++)
            for (int x = 0; x < getWidth(); x++) {
                var block = blocks[y][x];
                for (var p : block.particles) {
                    IntVec blockPos = blockPos(p.getPos());
                    if (blockPos.x() != x || blockPos.y() != y)
                        wrongParticles.add(Map.entry(p, block));
                }
            }

        wrongParticles.forEach(e -> {
            var p = e.getKey();
            var block = e.getValue();
            block.remove(p);
            add(p);
        });
    }

    /**
     * Tries to find a particle with rendering bounds containing a given position.
     *
     * @param pos the position to find a particle at
     * @return an {@link Optional} containing the found particle or an empty Optional if there is no particle at this position
     */
    public Optional<Particle> find(Vec pos) {
        IntVec blockPos = blockPos(pos);
        for (var it = iterator(blockPos.subtract(new IntVec(1, 1)), blockPos.add(new IntVec(2, 2))); it.hasNext(); ) {
            Particle p = it.next();
            if (p.getRenderBounds().contains(new Point2D.Double(pos.getX(), pos.getY())))
                return Optional.of(p);
        }
        return Optional.empty();
    }

    /**
     * Removes all particles with rendering bounds containing a given position.
     *
     * @param pos the position to remove all particles at
     */
    public void removeAllAt(Vec pos) {
        IntVec blockPos = blockPos(pos);
        for (var it = iterator(blockPos.subtract(new IntVec(1, 1)), blockPos.add(new IntVec(2, 2))); it.hasNext(); ) {
            Particle p = it.next();
            if (p.getRenderBounds().contains(new Point2D.Double(pos.getX(), pos.getY())))
                remove(p);
        }
    }

    /**
     * Renders a grid representing the blocks in this container.
     *
     * @param g the graphics to draw
     */
    public void renderGrid(Graphics2D g) {
        g.setStroke(new BasicStroke(2F));
        g.setColor(Color.GRAY);
        for (int y = 0; y < getHeight(); y++)
            for (int x = 0; x < getWidth(); x++)
                g.draw(new Rectangle2D.Double(x * blockSize, y * blockSize, blockSize, blockSize));
    }

    /**
     * A block containing a list of particles.
     */
    class Block {
        /**
         * the list of particles in this block
         */
        private final List<Particle> particles;

        private Block() {
            particles = new ArrayList<>();
        }

        private synchronized void add(Particle p) {
            particles.add(p);
            size++;
        }

        private synchronized void remove(Particle p) {
            particles.remove(p);
            size--;
        }

        /**
         * @return a thread save {@link Iterator} over all particles in this block
         */
        private Iterator<Particle> iterator() {
            var _this = this;
            return new Iterator<>() {
                private int pos;

                private Particle next;

                @Override
                public boolean hasNext() {
                    if (next != null)
                        return true;

                    synchronized (_this) {
                        if (pos < particles.size()) {
                            next = particles.get(pos++);
                            return true;
                        }
                    }

                    return false;
                }

                @Override
                public Particle next() {
                    if (!hasNext())
                        throw new NoSuchElementException();

                    var res = next;
                    next = null;
                    return res;
                }
            };
        }
    }
}
