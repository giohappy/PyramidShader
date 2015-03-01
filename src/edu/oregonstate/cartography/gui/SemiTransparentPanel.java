package edu.oregonstate.cartography.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import javax.swing.JPanel;

public class SemiTransparentPanel extends JPanel {

    {
        setOpaque(false);
    }
    
    public void paintComponent(Graphics g) {
        Color c = getBackground();
        g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 127));
        Rectangle r = g.getClipBounds();
        g.fillRect(r.x, r.y, r.width, r.height);
        super.paintComponent(g);
    }
}
