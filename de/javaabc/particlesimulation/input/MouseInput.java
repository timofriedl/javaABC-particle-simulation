package de.javaabc.particlesimulation.input;

import de.javaabc.particlesimulation.Simulation;
import de.javaabc.particlesimulation.display.Renderable;
import de.javaabc.particlesimulation.particle.Particle;
import de.javaabc.particlesimulation.util.math.Vec;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;

import static java.awt.event.MouseEvent.*;

/**
 * Handles mouse input from the user.
 */
public class MouseInput implements MouseListener, MouseMotionListener, Renderable {
    /**
     * the reference back to the main simulation instance
     */
    private final Simulation simulation;

    /**
     * the current mouse position on screen
     */
    private Vec mousePos = Vec.ZERO;

    /**
     * the {@link Particle} that is currently selected to be connected to another particle
     */
    private Particle connectStart;

    /**
     * the size of the grid on which particles can be placed in px
     */
    private final double gridSnap;

    /**
     * Creates a new MouseInput instance.
     *
     * @param simulation the reference back to the main simulation instance
     */
    public MouseInput(Simulation simulation) {
        this.simulation = simulation;
        gridSnap = simulation.getCutoffDistance() / 5.0;
        simulation.addMouseListener(this);
        simulation.addMouseMotionListener(this);
    }

    @Override
    public void render(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 64));

        if (connectStart == null) {
            // Render currently hovered place position for the next particle
            double x = gridSnap * Math.round(mousePos.x / gridSnap);
            double y = gridSnap * Math.round(mousePos.y / gridSnap);
            g.fill(new Ellipse2D.Double(x - Particle.RENDER_RADIUS, y - Particle.RENDER_RADIUS, 2.0 * Particle.RENDER_RADIUS, 2.0 * Particle.RENDER_RADIUS));
        } else // Render connection line
            g.draw(new Line2D.Double(connectStart.getPos().x, connectStart.getPos().y, mousePos.x, mousePos.y));
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        // ignore
    }

    @Override
    public void mousePressed(MouseEvent e) {
        switch (e.getButton()) {
            case BUTTON1 -> {
                if (connectStart == null) {
                    // Left mouse button: place particle
                    double x = gridSnap * Math.round(e.getX() / gridSnap);
                    double y = gridSnap * Math.round(e.getY() / gridSnap);

                    if (simulation.getKeyInput().isPressed(KeyEvent.VK_A))
                        simulation.addFixedParticleAt(new Vec(x, y));
                    else
                        simulation.addParticleAt(new Vec(x, y));
                }
            }
            case BUTTON2 -> simulation.getParticles().removeAllAt(new Vec(e.getX(), e.getY())); // Middle button: remove particle
            case BUTTON3 -> {
                // Right mouse button: Start particle connection
                if (connectStart == null)
                    simulation.findParticle(new Vec(e.getX(), e.getY())).ifPresent(p -> connectStart = p);
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == BUTTON3) {
            if (connectStart != null) // Select second particle to connect with
                simulation.findParticle(mousePos).ifPresent(connectStart::connectWith);
            connectStart = null;
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // ignore
    }

    @Override
    public void mouseExited(MouseEvent e) {
        // ignore
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        mousePos = new Vec(e.getX(), e.getY());
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mousePos = new Vec(e.getX(), e.getY());
    }
}
