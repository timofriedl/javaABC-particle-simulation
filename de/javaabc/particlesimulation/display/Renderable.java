package de.javaabc.particlesimulation.display;

import java.awt.*;

/**
 * Interface to implement in all classes that somehow render things on screen.
 */
public interface Renderable {
    void render(Graphics2D g);
}
