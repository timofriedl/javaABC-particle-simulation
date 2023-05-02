package de.javaabc.particlesimulation.particle;

import de.javaabc.particlesimulation.Simulation;
import de.javaabc.particlesimulation.display.Renderable;
import de.javaabc.particlesimulation.util.Cache2D;
import de.javaabc.particlesimulation.util.math.Vec;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

import static de.javaabc.particlesimulation.util.math.MathUtil.fastPow6;

/**
 * A dot particle that attracts and repels other particles.
 */
public class Particle implements Renderable {
    /**
     * the spring factor k that scales the spring force
     */
    private static final double CONNECTION_STIFFNESS = 1E-4;

    /**
     * The render radius of this particle.
     * Dot particles do not have a size, but in order to visualize, we define one.
     */
    public static final double RENDER_RADIUS = 10.0;

    /**
     * the friction factor to artificially slow down all particles
     */
    private static final double FRICTION_FACTOR = 0.5;

    /**
     * the cache for the computation of combined epsilon values to speed up force computation
     */
    private static final Cache2D<Double, Double, Double> EPSILON_CACHE = new Cache2D<>();

    /**
     * the reference back to the main simulation instance
     */
    private final Simulation simulation;

    /**
     * the current position of this particle in px
     */
    protected Vec pos;

    /**
     * the current speed of this particle in px / s
     */
    private Vec speed;

    /**
     * the currently acting force on this particle in (kg px) / s^2
     */
    private Vec force;

    /**
     * the previously acting force on this particle
     */
    private Vec oldForce;

    /**
     * the mass of this particle in kg
     */
    protected double mass;

    /**
     * the depth of the Lennard-Jones-Potential
     */
    protected double epsilon;

    /**
     * the particle distance where the Lennard-Jones-Potential is equal to zero
     */
    protected double sigma;

    /**
     * the minimal distance to even start computing forces, squared
     */
    protected final double sqCutoffDis;

    /**
     * the current {@link Shape} object representing the rendering bounds of this particle
     */
    private Shape renderBounds;

    /**
     * The {@link List} of all particles that are connected to this particle.
     * Connections are stored unidirectional, i.e. there is no connected particle that also stores this instance as connected.
     */
    private final List<Particle> connections;

    /**
     * Creates a new dot particle instance.
     *
     * @param simulation the reference back to the main simulation instance
     * @param pos        the initial position of this particle in px
     * @param mass       the mass of this particle in kg
     * @param epsilon    the depth of the Lennard-Jones-Potential
     * @param sigma      the particle distance where the Lennard-Jones-Potential is equal to zero
     */
    public Particle(Simulation simulation, Vec pos, double mass, double epsilon, double sigma) {
        this.simulation = simulation;
        this.pos = pos;
        speed = force = oldForce = Vec.ZERO;
        this.mass = mass;
        this.epsilon = epsilon;
        this.sigma = sigma;

        sqCutoffDis = simulation.getCutoffDistance() * simulation.getCutoffDistance();
        connections = new ArrayList<>();
    }

    /**
     * Combines two epsilon values from different particles to one single value for force computation.
     *
     * @param e1 the first epsilon value
     * @param e2 the second epsilon value
     * @return the geometric mean of both values
     */
    private static double combinedEpsilon(double e1, double e2) {
        return EPSILON_CACHE.storeIfAbsent(e1, e2, () -> Math.sqrt(e1 * e2));
    }

    /**
     * Computes the Lennard-Jones-Potential between this particle and another.
     *
     * @param p the particle to compute the force to
     */
    public void tickForceTo(Particle p) {
        Vec dx = pos.subtract(p.pos); // Positional difference
        double sqDis = dx.sqLength(); // Distance (squared)

        if (sqDis == 0.0) {
            System.err.println("Particles at same exact position; Skipping force calculation");
            return;
        } else if (sqDis > sqCutoffDis)
            return; // Approximate force with zero if distance is larger than cutoff distance

        double d = Math.sqrt(sqDis); // Distance

        // Parameters for Lennard-Jones-Potential
        double epsilon = combinedEpsilon(this.epsilon, p.epsilon);
        double sigma = 0.5 * (this.sigma + p.sigma);
        double sigmaPerDisPow6 = fastPow6(sigma / d);
        double sigmaPerDisPow12 = sigmaPerDisPow6 * sigmaPerDisPow6;

        Vec df = dx.scale(24.0 * epsilon / sqDis * (sigmaPerDisPow6 - 2.0 * sigmaPerDisPow12)); // Lennard-Jones-Potential
        subtractForce(df); // Apply force on this particle
        p.addForce(df); // Apply force on p
    }

    /**
     * Computes forces that make particles bounce off the display boundary using ghost particles.
     */
    public void tickBoundaryForce() {
        double d = simulation.getCutoffDistance();
        double r = d / 2.0;

        if (pos.getX() < r)
            tickForceTo(new Particle(simulation, new Vec(-pos.getX(), pos.getY()), mass, epsilon, sigma));
        else if (pos.getX() > simulation.getWidth() - r)
            tickForceTo(new Particle(simulation, new Vec(2 * simulation.getWidth() - pos.getX(), pos.getY()), mass, epsilon, sigma));

        if (pos.getY() < r)
            tickForceTo(new Particle(simulation, new Vec(pos.getX(), -pos.getY()), mass, epsilon, sigma));
        else if (pos.getY() > simulation.getHeight() - r)
            tickForceTo(new Particle(simulation, new Vec(pos.getX(), 2 * simulation.getHeight() - pos.getY()), mass, epsilon, sigma));
    }

    /**
     * Computes the spring attraction to a connected particle.
     *
     * @param p the connected particle
     */
    private void tickAttractionTo(Particle p) {
        Vec dx = pos.subtract(p.pos); // Positional difference
        double d = Math.sqrt(dx.sqLength()); // Distance
        double force = CONNECTION_STIFFNESS * d; // Spring force: F = k * d
        Vec df = dx.scale(force); // Split force in x/y
        subtractForce(df); // Apply spring force on this particle
        p.addForce(df); // Apply spring force on the other particle
    }

    /**
     * Computes forces other than the Lennard-Jones-Potential.
     */
    public void tickSpecialForces() {
        tickBoundaryForce();
        connections.forEach(this::tickAttractionTo);
    }

    /**
     * Safely adds a force to the total force applied on this particle.
     *
     * @param df the force difference to add
     */
    protected synchronized void addForce(Vec df) {
        force = force.add(df);
    }

    /**
     * Safely subtracts a force from the total force applied on this particle.
     *
     * @param df the force difference to subtract
     */
    protected synchronized void subtractForce(Vec df) {
        force = force.subtract(df);
    }

    /**
     * Computes the current speed of this particle using Verlet integration.
     *
     * @param dt the time between two ticks in this simulation in seconds
     */
    public void tickSpeed(double dt) {
        Vec friction = speed.scale(1.0 - FRICTION_FACTOR);
        force = force.subtract(friction);

        // Verlet velocity calculation
        Vec dv = oldForce.add(force).scale(dt / (2.0 * mass));
        speed = speed.add(dv);
    }

    /**
     * Computes the current position of this particle using Verlet integration.
     *
     * @param dt the time between two ticks in this simulation in seconds
     */
    public void tickPos(double dt) {
        // Verlet position calculation
        Vec dx = speed.add(oldForce.scale(dt / (2.0 * mass)).scale(dt));
        pos = pos.add(dx);

        renderBounds = null;
    }

    /**
     * Resets values after all computations.
     */
    public void tickReset() {
        oldForce = force;
        force = simulation.getGravity();
    }

    /**
     * Computes a {@link Shape} representing the rendering bounds of this particle.
     * Dot particles do not have a size, but in order to visualize we use a circle with predefined radius.
     *
     * @return the computed shape
     */
    public Shape getRenderBounds() {
        if (renderBounds != null)
            return renderBounds;

        return renderBounds = new Ellipse2D.Double(pos.getX() - RENDER_RADIUS, pos.getY() - RENDER_RADIUS, RENDER_RADIUS * 2.0, RENDER_RADIUS * 2.0);
    }

    @Override
    public void render(Graphics2D g) {
        renderConnections(g);
        g.fill(getRenderBounds());
    }

    /**
     * Draws lines to all connected particles.
     *
     * @param g the graphics instance to draw
     */
    protected void renderConnections(Graphics2D g) {
        g.setColor(Color.BLACK);
        connections.forEach(p -> g.draw(new Line2D.Double(pos.x, pos.y, p.pos.x, p.pos.y)));
    }

    /**
     * Adds a particle to the list of connected particles.
     * Conditions:
     * - A particle cannot be connected to itself
     * - A particle cannot be connected twice to another particle
     * - A particle that holds this instance in its list of connected particles cannot be added to this particles list.
     *
     * @param p the particle to connect with
     */
    public void connectWith(Particle p) {
        synchronized (connections) {
            if (p != this && !connections.contains(p) && !p.connections.contains(this))
                connections.add(p);
        }
    }

    /**
     * Removes a given particle from the list of connected particles.
     *
     * @param p the particle to disconnect
     */
    public void removeConnectionTo(Particle p) {
        synchronized (connections) {
            connections.remove(p);
        }
    }

    @Override
    public String toString() {
        return "particle at pos " + pos.toIntVec();
    }

    public Vec getPos() {
        return pos;
    }
}
