package edu.oregonstate.cartography.gui.bivariate;

import edu.oregonstate.cartography.app.ColorUtils;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

/**
 * An interactive panel for placing color points for color interpolation.
 *
 * @author Bernhard Jenny, Cartography and Geovisualization Group, Oregon State
 * University
 */
public class BivariateColorPanel extends BivariateColorPreview {

    private static final int RECT_DIM = 10;

    /**
     * The currently selected point that is being dragged.
     */
    private BivariateColorPoint selectedPoint = null;

    /**
     * True while point is being dragged, false otherwise.
     */
    private boolean draggingPoint = false;

    /**
     * horizontal distance between the last mouse click and the center of the
     * selected point.
     */
    private int dragDX = 0;
    /**
     * horizontal distance between the last mouse click and the center of the
     * selected point.
     */
    private int dragDY = 0;

    private double crossXPerc = -1;
    private double crossYPerc = -1;

    public BivariateColorPanel() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                BivariateColorPoint pt = findPoint(e.getX(), e.getY());
                if (pt != null) {
                    selectPoint(pt);
                    dragDX = attr1ToPixelX(pt.getAttribute1()) - e.getX();
                    dragDY = attr2ToPixelY(pt.getAttribute2()) - e.getY();
                }
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                BivariateColorPoint pt = findPoint(e.getX(), e.getY());
                if (pt == null) {
                    pt = addPoint(e.getX(), e.getY());
                }
                selectPoint(pt);
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (draggingPoint) {
                    movePoint(e.getX(), e.getY());
                }
                draggingPoint = false;
                firePropertyChange("colorChanged", null, null);
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (getSelectedPoint() != null) {
                    draggingPoint = true;
                    movePoint(e.getX(), e.getY());
                    firePropertyChange("colorChanged", null, null);
                }
            }
        });

        // listen to delete and backspace key strokes
        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deletePoint");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "deletePoint");
        getActionMap().put("deletePoint", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getRenderer().removePoint(selectedPoint);
                selectPoint(null);
                repaint();
                firePropertyChange("colorDeleted", null, null);
            }
        });
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        if (getRenderer() != null) {
            //Antialiasing ON
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            ArrayList<BivariateColorPoint> points = getRenderer().getPoints();
            for (BivariateColorPoint point : points) {
                int px = attr1ToPixelX(point.getAttribute1());
                int py = attr2ToPixelY(point.getAttribute2());
                g2d.setColor(point.getColor());
                if (point.isLonLatDefined()) {
                    g2d.fillOval(px - RECT_DIM / 2, py - RECT_DIM / 2, RECT_DIM, RECT_DIM);
                } else {
                    g2d.fillRect(px - RECT_DIM / 2, py - RECT_DIM / 2, RECT_DIM, RECT_DIM);
                }
                if (point == selectedPoint) {
                    if (ColorUtils.difference(Color.RED, point.getColor()) > 100) {
                        g2d.setColor(Color.RED);
                    } else {
                        g2d.setColor(Color.CYAN);
                    }
                } else {
                    if (ColorUtils.getBrightness(point.getColor()) > 100) {
                        g2d.setColor(Color.BLACK);
                    } else {
                        g2d.setColor(Color.WHITE);
                    }
                }
                if (point.isLonLatDefined()) {
                    g2d.drawOval(px - RECT_DIM / 2, py - RECT_DIM / 2, RECT_DIM, RECT_DIM);
                } else {
                    g2d.drawRect(px - RECT_DIM / 2, py - RECT_DIM / 2, RECT_DIM, RECT_DIM);
                }
            }

            int D = 3;
            g2d.setColor(Color.BLACK);
            if (crossXPerc >= 0 && crossYPerc >= 0) {
                int x = (int) Math.round(crossXPerc * getWidth() / 100d);
                int y = (int) Math.round(crossYPerc * getHeight() / 100d);
                g2d.drawLine(x - D, y, x + D, y);
                g2d.drawLine(x, y - D, x, y + D);
            }
        }
        paintWarningString(g2d);
    }

    private int attr1ToPixelX(double attr1) {
        int insetX = getInsets().left;
        int w = getWidth() - getInsets().left - getInsets().right;
        return insetX + (int) Math.round(attr1 * w);
    }

    private int attr2ToPixelY(double attr2) {
        int insetY = getInsets().top;
        int h = getHeight() - getInsets().top - getInsets().bottom;
        return insetY + (int) Math.round((1 - attr2) * h);
    }

    private double pixelXToAttr1(int x) {
        int insetX = getInsets().left;
        double w = getWidth() - getInsets().left - getInsets().right;
        return (x - insetX) / w;
    }

    private double pixelYToAttr2(int y) {
        int insetY = getInsets().top;
        double h = getHeight() - getInsets().top - getInsets().bottom;
        return (h - y + insetY) / h;
    }

    private BivariateColorPoint addPoint(int pixelX, int pixelY) {
        BivariateColorPoint p = new BivariateColorPoint();
        p.setAttribute1(pixelXToAttr1(pixelX));
        p.setAttribute2(pixelYToAttr2(pixelY));
        Color color = new Color(getRenderer().interpolateColor(p.getAttribute1(), p.getAttribute2()));
        p.setColor(color);
        getRenderer().addPoint(p);
        return p;
    }

    private BivariateColorPoint findPoint(int pixelX, int pixelY) {
        ArrayList<BivariateColorPoint> points = getRenderer().getPoints();
        for (BivariateColorPoint point : points) {
            int px = attr1ToPixelX(point.getAttribute1());
            int py = attr2ToPixelY(point.getAttribute2());
            Rectangle rect = new Rectangle(px - RECT_DIM / 2, py - RECT_DIM / 2,
                    RECT_DIM + 1, RECT_DIM + 1);
            if (rect.contains(pixelX, pixelY)) {
                return point;
            }
        }
        return null;
    }

    private void movePoint(int mouseX, int mouseY) {
        if (selectedPoint == null) {
            return;
        }
        double attr1 = pixelXToAttr1(mouseX + dragDX);
        attr1 = Math.min(Math.max(0d, attr1), 1d);
        selectedPoint.setAttribute1(attr1);

        double attr2 = pixelYToAttr2(mouseY + dragDY);
        attr2 = Math.min(Math.max(0d, attr2), 1d);
        selectedPoint.setAttribute2(attr2);

        selectedPoint.setLonLat(Double.NaN, Double.NaN);

        getRenderer().colorPointsChanged();
        repaint();
    }

    /**
     * @return the dragPoint
     */
    public BivariateColorPoint getSelectedPoint() {
        return selectedPoint;
    }

    /**
     * Sets the selectedPoint field to the passed point and fires a property
     * change event.
     *
     * @param pt
     */
    public void selectPoint(BivariateColorPoint pt) {
        selectedPoint = pt;
        repaint();
        firePropertyChange("selectedPoint", null, selectedPoint);
    }

    /**
     * Selects the first point
     */
    public void selectFirstPoint() {
        ArrayList<BivariateColorPoint> points = getRenderer().getPoints();
        if (points.size() > 0) {
            selectPoint(points.get(0));
        }
    }

    public void setSelectedColor(Color color) {
        if (selectedPoint != null) {
            selectedPoint.setColor(color);
            renderer.colorPointsChanged();
            repaint();
            firePropertyChange("colorChanged", null, null);
        }
    }

    /**
     * Returns whether the user drags a point.
     *
     * @return True if point is dragged, false otherwise.
     */
    public boolean isValueAdjusting() {
        return draggingPoint;
    }

    public void setCrossPerc(double crossXPerc, double crossYPerc) {
        this.crossXPerc = crossXPerc;
        this.crossYPerc = crossYPerc;
        repaint();
    }
}
