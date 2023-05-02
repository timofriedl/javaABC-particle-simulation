package de.javaabc.particlesimulation;

import de.javaabc.particlesimulation.display.Display;
import de.javaabc.particlesimulation.display.Renderable;
import de.javaabc.particlesimulation.input.KeyInput;
import de.javaabc.particlesimulation.input.MouseInput;
import de.javaabc.particlesimulation.particle.FixedParticle;
import de.javaabc.particlesimulation.particle.Particle;
import de.javaabc.particlesimulation.particle.ParticleContainer;
import de.javaabc.particlesimulation.util.math.Vec;

import javax.swing.*;
import java.awt.*;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A simulation of dot {@link Particle}s.
 */
public class Simulation extends JFrame implements Renderable {
    /**
     * the default mass of a particle in kg (unit doesn't matter though)
     */
    private static final double DEFAULT_PARTICLE_MASS = 1.0;

    /**
     * the default epsilon value for the Lennard-Jones-Potential computation of particles
     */
    private static final double DEFAULT_EPSILON = 0.0000001;

    /**
     * the default sigma value for the Lennard-Jones-Potential computation of particles
     */
    private static final double DEFAULT_SIGMA = 200.0;

    /**
     * the number of frames and ticks per second
     */
    private final double fps, tps;

    /**
     * a {@link Vec}tor representing the gravitational force applied on each particle
     */
    private Vec gravity;

    /**
     * the container of particles
     */
    private final ParticleContainer particles;

    /**
     * the last nanosecond time stamp a frame was rendered
     */
    private long lastFrameTime;

    /**
     * the option to pause this simulation
     */
    private boolean pause = false;

    /**
     * the maximum distance to even compute forces between particles
     */
    private final double cutoffDistance;

    /**
     * the keyboard input handler
     */
    private final KeyInput keyInput;

    /**
     * the mouse input handler
     */
    private final MouseInput mouseInput;

    /**
     * Creates a new dot particle simulation.
     *
     * @param fps            the number of frames per second
     * @param tps            the number of calculations per second
     * @param gravity        the gravitational force acting on each particle
     * @param cutoffDistance the maximum distance to even compute forces between particles
     */
    public Simulation(double fps, double tps, Vec gravity, double cutoffDistance) {
        super("Simulation");
        this.fps = fps;
        this.tps = tps;
        this.gravity = gravity;
        this.cutoffDistance = cutoffDistance;

        setSize(Toolkit.getDefaultToolkit().getScreenSize());
        setResizable(false);
        setUndecorated(true);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setContentPane(new Display(this));

        int blockWidth = (int) Math.ceil(getWidth() / cutoffDistance);
        int blockHeight = (int) Math.ceil(getHeight() / cutoffDistance);
        particles = new ParticleContainer(blockWidth, blockHeight, cutoffDistance);

        keyInput = new KeyInput(this);
        mouseInput = new MouseInput(this);

        setVisible(true);
        startLoop();
    }

    /**
     * Starts the tick-render-loop of this simulation.
     */
    private void startLoop() {
        lastFrameTime = System.nanoTime();
        long nanosPerFrame = Math.round(1E9 / fps);
        double dt = 1.0 / tps;

        ScheduledExecutorService exe = Executors.newSingleThreadScheduledExecutor();
        exe.scheduleAtFixedRate(() -> {
            try {
                long now = System.nanoTime();

                if (!pause)
                    tick(dt);

                if (now - lastFrameTime > nanosPerFrame) {
                    getContentPane().repaint();
                    lastFrameTime = now;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0L, Math.round(dt * 1E9), TimeUnit.NANOSECONDS);
    }

    /**
     * Performs one calculation in this simulation.
     *
     * @param dt the time between two ticks in seconds
     */
    private void tick(double dt) {
        particles.forEachPair(Particle::tickForceTo);
        particles.forEachParallel(Particle::tickSpecialForces);
        particles.forEachParallel(p -> p.tickSpeed(dt));
        particles.forEachParallel(p -> p.tickPos(dt));
        particles.forEachParallel(Particle::tickReset);
        particles.updateBlocks();
    }

    @Override
    public void render(Graphics2D g) {
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, getWidth(), getHeight());

        // particles.renderGrid(g);
        particles.forEach(p -> p.render(g));

        mouseInput.render(g);
    }

    /**
     * Adds a particle at a given position.
     *
     * @param pos the position of the particle to add
     */
    public void addParticleAt(Vec pos) {
        particles.add(new Particle(this, pos, DEFAULT_PARTICLE_MASS, DEFAULT_EPSILON, DEFAULT_SIGMA));
        System.out.println("Created particle at " + pos);
    }

    /**
     * Adds a non-moving particle at a given position.
     *
     * @param pos the position of the particle to add
     */
    public void addFixedParticleAt(Vec pos) {
        particles.add(new FixedParticle(this, pos, DEFAULT_PARTICLE_MASS, DEFAULT_EPSILON, DEFAULT_SIGMA));
        System.out.println("Created fixed particle at " + pos);
    }

    /**
     * Tries to find a particle at a given position.
     *
     * @param pos the position to find a particle at
     * @return an {@link Optional} containing the found particle or an empty Optional if nothing found
     */
    public Optional<Particle> findParticle(Vec pos) {
        return particles.find(pos);
    }

    public static void main(String[] args) {
        new Simulation(60.0, 10000.0, new Vec(0.0, 0.1), 200.0);
    }

    public ParticleContainer getParticles() {
        return particles;
    }

    public void togglePause() {
        pause = !pause;
    }

    public void toggleGravity() {
        gravity = gravity == Vec.ZERO ? new Vec(0.0, 0.1) : Vec.ZERO;
    }

    public Vec getGravity() {
        return gravity;
    }

    public double getCutoffDistance() {
        return cutoffDistance;
    }

    public KeyInput getKeyInput() {
        return keyInput;
    }
}
