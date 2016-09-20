package edu.oregonstate.cartography.grid;

import edu.oregonstate.cartography.gui.bivariate.BivariateColorPoint;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import edu.oregonstate.cartography.gui.bivariate.ColorLUTInterface;
import ij.process.ColorSpaceConverter;

/**
 * Renderer using a two-dimensional color look-up table.
 *
 * @author Bernhard Jenny, School of Science - Geospatial Science, RMIT
 * University, Melbourne
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
    private double exponentP = 2;

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

    /**
     * Assign default color reference points.
     */
    protected final void initPoints() {
        addPoint(231, 252, 253, 0.5, 0.0);
        addPoint(226, 254, 255, 1.0, 0.0);
        addPoint(252, 252, 252, 1.0, 0.35);        
        addPoint(247, 255, 233, 1.0, 0.65);
        addPoint(255, 255, 192, 0.95, 0.8);
        addPoint(255, 255, 192, 0.9, 0.9);
        addPoint(196, 207, 255, 0.7, 0.6);
        addPoint(056, 125, 254, 0.7, 0.9);
    }

    private void addPoint(int r, int g, int b, double x, double y) {
        BivariateColorPoint p = new BivariateColorPoint();
        p.setColor(r, g, b);
        p.setAttribute1(x);
        p.setAttribute2(y);
        addPoint(p);
    }
    
    /**
     * Returns a color for a gray scale shaded value and an elevation.
     *
     * @param shade gray value between 0 and 1. An index along the horizontal
     * axis of the look-up table.
     * @param relativeElevation relative elevation between 0 and 1. An index
     * along the vertical axis of the look-up table.
     * @return the RGB color
     */
    public final int getColor(double shade, double relativeElevation) {
        assert (shade >= 0d && shade <= 1d);
        assert (relativeElevation >= 0d && relativeElevation <= 1d);

        int lutCol = (int) Math.round((ColorLUT.LUT_SIZE - 1) * shade);
        int lutRow = (int) Math.round((ColorLUT.LUT_SIZE - 1) * relativeElevation);
        return lut[lutRow][lutCol];
    }

    /**
     * Renders an image with all possible colors. The image can be used to
     * display the color look-up table.
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
     * Updates the cached values of the color look-up table. Needs to be called
     * after a point or any attribute that changes the colors in the look-up
     * table has been altered.
     */
    @Override
    public void colorPointsChanged() {
        lut = new int[LUT_SIZE][LUT_SIZE];
        for (int r = 0; r < LUT_SIZE; r++) {
            double y = r / (LUT_SIZE - 1d);
            for (int c = 0; c < LUT_SIZE; c++) {
                double x = c / (LUT_SIZE - 1d);
                lut[r][c] = interpolateColor(x, y);
            }
        }
    }

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
    @Override
    public int interpolateColor(double h, double v) {

        final boolean useLAB = true;

        ColorSpaceConverter colorConverter = new ColorSpaceConverter();

        double wTot = 0;
        double weightedSumR = 0;
        double weightedSumG = 0;
        double weightedSumB = 0;
        double weightedSumLabL = 0;
        double weightedSumLabA = 0;
        double weightedSumLabB = 0;

        for (BivariateColorPoint point : points) {
            double attr1Point = point.getAttribute1();
            double attr2Point = point.getAttribute2();

            // distance in the color look-up table.
            double d1 = attr1Point - h;
            double d2 = attr2Point - v;
            double d = Math.sqrt(d1 * d1 + d2 * d2);

            // IDW or Gaussian weigh
            double w = useIDW ? inverseDistanceWeight(d) : gaussianWeight(d);

            if (useLAB) {
                // Lab mix
                // mixing with Lab tends to create slightly more saturated colors than with RGB
                weightedSumLabL += point.getLabL() * w;
                weightedSumLabA += point.getLabA() * w;
                weightedSumLabB += point.getLabB() * w;
            } else {
                // RGB mix
                weightedSumR += point.getR() * w;
                weightedSumG += point.getG() * w;
                weightedSumB += point.getB() * w;
            }
            wTot += w;
        }

        if (useLAB) {
            double L = Math.min(ColorSpaceConverter.LAB_MAX_L, Math.max(ColorSpaceConverter.LAB_MIN_L, weightedSumLabL / wTot));
            double a = Math.min(ColorSpaceConverter.LAB_MAX_A, Math.max(ColorSpaceConverter.LAB_MIN_A, weightedSumLabA / wTot));
            double b = Math.min(ColorSpaceConverter.LAB_MAX_B, Math.max(ColorSpaceConverter.LAB_MIN_B, weightedSumLabB / wTot));
            int[] rgb = colorConverter.LABtoRGB(L, a, b);
            return rgb[2] | (rgb[1] << 8) | (rgb[0] << 16) | 0xFF000000;
        } else {
            int r = (int) Math.min(255, Math.max(0, weightedSumR / wTot));
            int g = (int) Math.min(255, Math.max(0, weightedSumG / wTot));
            int b = (int) Math.min(255, Math.max(0, weightedSumB / wTot));
            return b | (g << 8) | (r << 16) | 0xFF000000;
        }

    }

    /**
     * Returns the color reference points.
     *
     * @return the points
     */
    @Override
    public ArrayList<BivariateColorPoint> getPoints() {
        return points;
    }

    /**
     * Add a color reference point.
     *
     * @param p color reference point to add.
     */
    @Override
    public void addPoint(BivariateColorPoint p) {
        points.add(p);
        colorPointsChanged();
    }

    /**
     * Remove a point from the color reference points. Will not remove the point
     * if it is the only point.
     *
     * @param point point to remove.
     */
    @Override
    public void removePoint(BivariateColorPoint point) {
        if (points.size() > 1) {
            points.remove(point);
            colorPointsChanged();
        }
    }

    /**
     * Gaussian bell curve.
     *
     * @param d horizontal value for which a vertical Gaussian bell curve value
     * is computed.
     * @return the Gaussian bell curve value
     */
    private double gaussianWeight(double d) {
        // empirical scaling of exponent
        // this makes it possible to use the same exponentP for IDW and Gaussian curve weighting
        double K = exponentP / 10000 * LUT_SIZE * LUT_SIZE;
        return Math.exp(-K * d * d);
    }

    /**
     * Inverse distance weight after Shepard.
     *
     * @param d distance for which a weight is computed.
     * @return the weight for d
     */
    private double inverseDistanceWeight(double d) {
        return 1. / Math.pow(d, exponentP);
    }

    /**
     * Returns the exponent for weight calculations.
     *
     * @return the exponent
     */
    @Override
    public double getExponentP() {
        return exponentP;
    }

    /**
     * Set the exponent for weight calculations.
     *
     * @param exponentP the exponent
     */
    @Override
    public void setExponentP(double exponentP) {
        this.exponentP = exponentP;
        colorPointsChanged();
    }

    /**
     * Returns true if inverse distance weighting is used. Returns true if a
     * Gaussian bell curve is used for weighting.
     *
     * @return true=IDW, false=Gauss
     */
    @Override
    public boolean isUseIDW() {
        return useIDW;
    }

    /**
     * Set whether inverse distance weighting or a Gaussian bell curve is used
     * for weighting.
     *
     * @param useIDW true=IDW, false=Gauss
     */
    @Override
    public void setUseIDW(boolean useIDW) {
        this.useIDW = useIDW;
        colorPointsChanged();
    }

    /**
     * Returns a warning string that can be displayed to the user.
     *
     * @return a string warning the user about some illegal condition.
     */
    @Override
    public String getWarning() {
        return null;
    }
}
