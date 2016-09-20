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
    public int interpolateColor(double h, double v);

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

    /**
     * Set the exponent for weight calculations.
     *
     * @param exp the exponent
     */
    public void setExponentP(double exp);

    /**
     * Returns true if inverse distance weighting is used. Returns true if a
     * Gaussian bell curve is used for weighting.
     *
     * @return true=IDW, false=Gauss
     */
    boolean isUseIDW();

    /**
     * Set whether inverse distance weighting or a Gaussian bell curve is used
     * for weighting.
     *
     * @param useIDW true=IDW, false=Gauss
     */
    public void setUseIDW(boolean useIDW);

    /**
     * Renders an image with all possible colors. The image can be used to
     * display the color look-up table.
     *
     * @param width width of the image
     * @param height height of the image
     * @return The new image.
     */
    public BufferedImage getDiagramImage(int width, int height);

    /**
     * Returns a warning string that can be displayed to the user.
     *
     * @return a string warning the user about some illegal condition.
     */
    public String getWarning();
}
