package edu.oregonstate.cartography.gui.bivariate;

import edu.oregonstate.cartography.grid.ColorLUT;
import edu.oregonstate.cartography.gui.CenteredStringRenderer;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;

/**
 * A non-interactive panel displaying a two-dimensional color look-up table.
 * @author Bernhard Jenny, Cartography and Geovisualization Group, Oregon State
 * University
 */
public class BivariateColorPreview extends JComponent {

    protected ColorLUTInterface renderer = new ColorLUT();

    public BivariateColorPreview() {
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (renderer != null) {
            int x = getInsets().left;
            int y = getInsets().top;
            int w = getWidth() - getInsets().left - getInsets().right;
            int h = getHeight() - getInsets().top - getInsets().bottom;
            if (w > 0 && h > 0) {
                BufferedImage img = renderer.getDiagramImage(w, h);
                g.drawImage(img, x, y, null);
            }
        }
    }

    public void setBivariateColorRenderer(BivariateColorRenderer renderer) {
        this.renderer = renderer;
        this.repaint();
    }

    protected void paintWarningString(Graphics2D g2d) {
        String msg = renderer.getWarning();
        if (msg != null) {
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, 
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, 
                    RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            int x = getWidth() / 2;
            int y = getHeight() / 2;
            g2d.setColor(Color.BLACK);
            CenteredStringRenderer.drawCentered(g2d, msg, x, y, false);
        }
    }

    /**
     * @return the renderer
     */
    public ColorLUTInterface getRenderer() {
        return renderer;
    }

    /**
     * @param renderer the renderer to set
     */
    public void setRenderer(ColorLUTInterface renderer) {
        this.renderer = renderer;
        repaint();
    }
}
