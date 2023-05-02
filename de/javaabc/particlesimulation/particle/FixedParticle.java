package de.javaabc.particlesimulation.particle;

import de.javaabc.particlesimulation.Simulation;
import de.javaabc.particlesimulation.util.math.Vec;

import java.awt.*;

/**
 * A particle that cannot move.
 */
public class FixedParticle extends Particle {
    /**
     * Creates a new unmovable particle.
     *
     * @param simulation the reference back to the main simulation instance
     * @param pos the final position of this unmovable particle
     * @param mass the mass of this particle
     * @param epsilon the epsilon parameter for the Lennard-Jones potential
     * @param sigma the sigma parameter for the Lennard-Jones potential
     */
    public FixedParticle(Simulation simulation, Vec pos, double mass, double epsilon, double sigma) {
        super(simulation, pos, mass, epsilon, sigma);
    }

    @Override
    public void tickBoundaryForce() {
    }

    @Override
    protected synchronized void addForce(Vec force) {
    }

    @Override
    protected synchronized void subtractForce(Vec force) {
    }

    @Override
    public void tickSpeed(double dt) {
    }

    @Override
    public void tickPos(double dt) {
    }

    @Override
    public void tickReset() {
    }

    @Override
    public void render(Graphics2D g) {
        super.renderConnections(g);
        g.setColor(Color.ORANGE);
        g.fill(getRenderBounds());
    }
}
