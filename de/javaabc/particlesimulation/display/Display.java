package de.javaabc.particlesimulation.display;

import de.javaabc.particlesimulation.Simulation;

import javax.swing.*;
import java.awt.*;

public class Display extends JPanel {
    /**
     * reference to the main {@link Simulation}
     */
    private final Simulation simulation;

    public Display(Simulation simulation) {
        super();
        this.simulation = simulation;
    }

    @Override
    public void paint(Graphics g) {
        var g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        simulation.render(g2);
    }
}
