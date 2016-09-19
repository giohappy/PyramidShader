package edu.oregonstate.cartography.grid;

import edu.oregonstate.cartography.gui.bivariate.BivariateColorPoint;
import edu.oregonstate.cartography.gui.bivariate.ColorLUTInterface;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;

/**
 * Two-dimensional color look-up table.
 * 
 * @author Bernhard Jenny, School of Science - Geospatial Science, RMIT University, Melbourne
 */
public final class ColorLUT implements ColorLUTInterface {

    /**
     * size of look-up table
     */
    public static final int LUT_SIZE = 256;
    
    /**
     * color reference points
     */
    private final ArrayList<BivariateColorPoint> points = new ArrayList<>();
    
    /**
     * exponent value for interpolation
     */
    private double exponentP = 1.3;
    
    /**
     * Use inverse distance weighting or Gaussian bell curve weighting
     */
    private boolean useIDW = false;
    
    /**
     * cached look-up values
     */
    private int[][] lut = null;

    public ColorLUT() {
        initPoints();
    }

    // FIXME hard coded color points for the moment
    protected final void initPoints() {
        //Assign point x, y values
        //Set their r, g, b values (0-255)
        //Set grid values (normalized 0-1)

        BivariateColorPoint point1 = new BivariateColorPoint();
        point1.setR(255);
        point1.setG(0);
        point1.setB(0);
        point1.setAttribute1(0);
        point1.setAttribute2(0);

        BivariateColorPoint point2 = new BivariateColorPoint();
        point2.setR(0);
        point2.setG(255);
        point2.setB(0);
        point2.setAttribute1(1);
        point2.setAttribute2(0);
       
        BivariateColorPoint point3 = new BivariateColorPoint();
        point3.setR(0);
        point3.setG(0);
        point3.setB(255);
        point3.setAttribute1(1);
        point3.setAttribute2(1);

        BivariateColorPoint point4 = new BivariateColorPoint();
        point4.setR(0);
        point4.setG(0);
        point4.setB(255);
        point4.setAttribute1(0);
        point4.setAttribute2(1);

        addPoint(point1);
        addPoint(point2);
        addPoint(point3);
        addPoint(point4);
    }
    
     /**
     * Returns a color for a cell in the grid
     * @param shade gray value between 0 and 1
     * @param relativeElevation relative elevation between 0 and 1
     * @return the color
     */
    public final int renderPixel(double shade, double relativeElevation) {
        assert (shade >= 0d && shade <= 1d);
        assert (relativeElevation >= 0d && relativeElevation <= 1d);
        
        int lutCol = (int) Math.round((ColorLUT.LUT_SIZE - 1) * shade);
        int lutRow = (int) Math.round((ColorLUT.LUT_SIZE - 1) * relativeElevation);
        return lut[lutRow][lutCol];
    }
    
    /**
     * Renders an image with all possible colors.
     *
     * @param width Width of the image
     * @param height Height of the image
     * @return The new image.
     */
    @Override
    public BufferedImage getDiagramImage(int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int[] imageBuffer = ((DataBufferInt) (img.getRaster().getDataBuffer())).getData();
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                double x = c / (width - 1d);
                double y = 1d - r / (height - 1d);
                int lutCol = (int) Math.round(x * (ColorLUT.LUT_SIZE - 1));
                int lutRow = (int) Math.round(y * (ColorLUT.LUT_SIZE - 1));
                imageBuffer[r * width + c] = lut[lutRow][lutCol];
            }
        }
        return img;
    }
    
    /**
     * Updates the color look-up table. Needs to be called after any point or
     * the exponent change.
     */
    @Override
    public void colorPointsChanged() {
        lut = new int[LUT_SIZE][LUT_SIZE];
        for (int r = 0; r < LUT_SIZE; r++) {
            double y = r / (LUT_SIZE - 1d);
            for (int c = 0; c < LUT_SIZE; c++) {
                double x = c / (LUT_SIZE - 1d);
                lut[r][c] = interpolateValue(x, y);
            }
        }
    }
    
    @Override
    public int interpolateValue(double attr1AtPixel, double attr2AtPixel) {
        double wTot = 0;
        double weightedSumR = 0;
        double weightedSumG = 0;
        double weightedSumB = 0;
        /* loop over all points. For each point, compute distance */
        for (BivariateColorPoint point : points) {
            double attr1Point = point.getAttribute1();
            double attr2Point = point.getAttribute2();
            double d1 = attr1Point - attr1AtPixel;
            double d2 = attr2Point - attr2AtPixel;
            double distance = Math.sqrt(d1 * d1 + d2 * d2);
            double w = weight(distance);
            weightedSumR += point.getR() * w;
            weightedSumG += point.getG() * w;
            weightedSumB += point.getB() * w;
            wTot += w;
        }
        weightedSumR = Math.min(255, Math.max(0, weightedSumR / wTot));
        weightedSumG = Math.min(255, Math.max(0, weightedSumG / wTot));
        weightedSumB = Math.min(255, Math.max(0, weightedSumB / wTot));
        //Encode r, g, & b values into a single int value using shifting
        return ((int) weightedSumB) | (((int) weightedSumG) << 8) | (((int) weightedSumR) << 16) | (255 << 24);
    }

    /**
     * @return the points
     */
    public ArrayList<BivariateColorPoint> getPoints() {
        return points;
    }

    public void addPoint(BivariateColorPoint p) {
        points.add(p);
        colorPointsChanged();
    }

    public void removePoint(BivariateColorPoint selectedPoint) {
        points.remove(selectedPoint);
        colorPointsChanged();
    }

    protected double gaussianWeight(double d) {
        double K = exponentP / 10000 /*0.0002*/ * 255 * 255 / 3;
        return Math.exp(-K * d * d);
    }

    protected double inverseDistanceWeight(double d) {
        return 1. / Math.pow(d, exponentP);
    }

    protected double weight(double d) {
        return useIDW ? inverseDistanceWeight(d) : gaussianWeight(d);
    }

    public int getLUTColor(int lutCol, int lutRow) {
        return lut[LUT_SIZE - 1 - lutRow][lutCol];
    }

    /**
     * @return the exponentP
     */
    public double getExponentP() {
        return exponentP;
    }

    /**
     * @param exponentP the exponentP to set
     */
    public void setExponentP(double exponentP) {
        this.exponentP = exponentP;
        colorPointsChanged();
    }

    /**
     * @return the useIDW
     */
    public boolean isUseIDW() {
        return useIDW;
    }

    /**
     * @param useIDW the useIDW to set
     */
    public void setUseIDW(boolean useIDW) {
        this.useIDW = useIDW;
        colorPointsChanged();
    }

    @Override
    public String getWarning() {
        return null;
    }

}
