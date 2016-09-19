package edu.oregonstate.cartography.gui.bivariate;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

/**
 *
 * @author Bernhard Jenny, School of Science - Geospatial Science, RMIT
 * University, Melbourne
 */
public interface ColorLUTInterface {
    public BufferedImage getDiagramImage(int width, int height);
    
    public String getWarning();

    public ArrayList<BivariateColorPoint> getPoints();

    public int interpolateValue(double attribute1, double attribute2);

    public void addPoint(BivariateColorPoint p);

    public void colorPointsChanged();

    public void removePoint(BivariateColorPoint selectedPoint);

    public void setExponentP(double exp);

    public void setUseIDW(boolean selected);
}
