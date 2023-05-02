package de.javaabc.particlesimulation.input;

import de.javaabc.particlesimulation.Simulation;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import static java.awt.event.KeyEvent.VK_G;
import static java.awt.event.KeyEvent.VK_SPACE;

/**
 * Handles keyboard input from the user.
 */
public class KeyInput implements KeyListener {
    /**
     * the reference back to main simulation instance
     */
    private final Simulation simulation;

    /**
     * the array of currently pressed keys
     */
    private final boolean[] pressed;

    /**
     * Creates a new KeyInput instance.
     *
     * @param simulation the reference back to main simulation instance
     */
    public KeyInput(Simulation simulation) {
        this.simulation = simulation;
        pressed = new boolean[0x1_0000];
        simulation.addKeyListener(this);
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // ignore
    }

    @Override
    public void keyPressed(KeyEvent e) {
        pressed[e.getKeyCode()] = true;

        switch (e.getKeyCode()) {
            case VK_SPACE -> simulation.togglePause(); // Pause or resume simulation
            case VK_G -> simulation.toggleGravity(); // Enable or disable gravity
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        pressed[e.getKeyCode()] = false;
    }

    public boolean isPressed(int keyCode) {
        return pressed[keyCode];
    }
}
