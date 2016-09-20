package edu.oregonstate.cartography.gui.bivariate;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

/**
 * An interface for a renderer that uses a color look-up table to assign colors
 * to a grid.
 *
 * @author Bernhard Jenny, School of Science - Geospatial Science, RMIT
 * University, Melbourne
 */
public interface ColorLUTInterface {

    /**
     * Returns the color reference points.
     *
     * @return
     */
    public ArrayList<BivariateColorPoint> getPoints();

    /**
     * Add a color reference point.
     *
     * @param p
     */
    public void addPoint(BivariateColorPoint p);

    /**
     * Remove a point from the color reference points. Will not remove the point
     * if it is the only point.
     *
     * @param point point to remove.
     */
    public void removePoint(BivariateColorPoint point);

    /**
     * Interpolates a color for a particular location in the look-up table from
     * all color reference points.
     *
     * @param h the horizontal coordinate of the location in the look-up table
     * between 0 and 1.
     * @param v the vertical coordinate of the location in the look-up table
     * between 0 and 1.
     * @return
     */
    public int interpolateValue(double h, double v);

    /**
     * Updates the cached values of the color look-up table. Needs to be called
     * after a point or any attribute that changes the colors in the look-up
     * table has been altered.
     */
    public void colorPointsChanged();

    /**
     * Returns the exponent for weight calculations.
     *
     * @return the exponent
     */
    double getExponentP();

    public void setExponentP(double exp);

    /**
     * @return the useIDW
     */
    boolean isUseIDW();

    public void setUseIDW(boolean selected);

    public BufferedImage getDiagramImage(int width, int height);

    public String getWarning();
}
