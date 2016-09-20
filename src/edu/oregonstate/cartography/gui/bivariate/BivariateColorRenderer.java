package edu.oregonstate.cartography.gui.bivariate;

import edu.oregonstate.cartography.grid.Grid;
import edu.oregonstate.cartography.grid.operators.ColorizerOperator;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;

/**
 *
 * @author Jane Darbyshire and Bernie Jenny, Oregon State University
 */
public class BivariateColorRenderer implements ColorLUTInterface {

    public static final int LUT_SIZE = 256;

    private final ArrayList<BivariateColorPoint> points = new ArrayList<>();
    private double exponentP = 1.3;
    private boolean useIDW = false;

    private Grid attribute1Grid = null;
    private Grid attribute2Grid = null;
    private float[] attribute2MinMax = null;
    private float[] attribute1MinMax = null;

    private int[][] lut = null;

    public BivariateColorRenderer() {
        initPoints();
        updateLUT();
    }

    /**
     * Updates the color look-up table. Needs to be called after any point or
     * the exponent change.
     */
    private void updateLUT() {
        lut = new int[LUT_SIZE][LUT_SIZE];
        for (int r = 0; r < LUT_SIZE; r++) {
            double y = r / (LUT_SIZE - 1d);
            for (int c = 0; c < LUT_SIZE; c++) {
                double x = c / (LUT_SIZE - 1d);
                lut[r][c] = interpolateColor(x, y);
            }
        }
    }

    public void colorPointsChanged() {
        updateLUT();
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
        updateLUT();
    }

    /**
     * @return the points
     */
    public ArrayList<BivariateColorPoint> getPoints() {
        return points;
    }

    public void addPoint(BivariateColorPoint p) {
        points.add(p);
        updateLUT();
    }

    public void removePoint(BivariateColorPoint selectedPoint) {
        points.remove(selectedPoint);
        updateLUT();
    }

    public final int renderPixel(int col, int row) {
        double attr1AtPixel = attribute1Grid.getValue(col, row);
        // scale to 0..1
        attr1AtPixel = (attr1AtPixel - attribute1MinMax[0])
                / (attribute1MinMax[1] - attribute1MinMax[0]);

        double attr2AtPixel = attribute2Grid.getValue(col, row);
        // scale to 0..1 and invert vertical axis
        attr2AtPixel = (attr2AtPixel - attribute2MinMax[0])
                / (attribute2MinMax[1] - attribute2MinMax[0]);

        if (Double.isNaN(attr1AtPixel) || Double.isNaN(attr2AtPixel)) {
            return ColorizerOperator.VOID_COLOR;
        }

        int lutCol = (int) Math.round(attr1AtPixel * (LUT_SIZE - 1));
        int lutRow = (int) Math.round(attr2AtPixel * (LUT_SIZE - 1));
        return lut[lutRow][lutCol];
    }

    public int getLUTColor(int lutCol, int lutRow) {
        return lut[LUT_SIZE - 1 - lutRow][lutCol];
    }
    
    /**
     * Returns the coordinates of the LUT cell that is closest to the passed 
     * location. The location is in percentage, relative to attribute grid 1.
     * @param xPerc Horizontal location in percentage.
     * @param yPerc Vertical location in percentage, from top to bottom.
     * @return 
     */
    public Point getLUTCoordinates(double xPerc, double yPerc) {
        int col = (int) Math.round(attribute1Grid.getCols() * xPerc / 100d);
        int row = (int) Math.round(attribute1Grid.getRows() * yPerc / 100d);
        
        double attr1AtPixel = attribute1Grid.getValue(col, row);
        // scale to 0..1
        attr1AtPixel = (attr1AtPixel - attribute1MinMax[0])
                / (attribute1MinMax[1] - attribute1MinMax[0]);

        double attr2AtPixel = attribute2Grid.getValue(col, row);
        // scale to 0..1 and invert vertical axis
        attr2AtPixel = (attr2AtPixel - attribute2MinMax[0])
                / (attribute2MinMax[1] - attribute2MinMax[0]);

        if (Double.isNaN(attr1AtPixel) || Double.isNaN(attr2AtPixel)) {
            return null;
        }

        int lutCol = (int) Math.round(attr1AtPixel * (LUT_SIZE - 1));
        int lutRow = LUT_SIZE - 1 - (int) Math.round(attr2AtPixel * (LUT_SIZE - 1));
        return new Point(lutCol, lutRow);
    }

    public void renderImage(BufferedImage img, Grid attribute1Grid, Grid attribute2Grid) {
        int cols = img.getWidth();
        int rows = img.getHeight();
        int[] imageBuffer = ((DataBufferInt) (img.getRaster().getDataBuffer())).getData();

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                double attr1AtPixel = attribute1Grid.getValue(col, row);
                double attr2AtPixel = attribute2Grid.getValue(col, row);
                //int color = interpolateValue(attr1AtPixel, attr2AtPixel);
                int lutCol = (int) Math.round(attr1AtPixel * (LUT_SIZE - 1));
                int lutRow = (int) Math.round(attr2AtPixel * (LUT_SIZE - 1));
                imageBuffer[row * cols + col] = lut[lutRow][lutCol];
            }
        }
    }

    private double gaussianWeight(double d) {
        double K = exponentP / 10000 /*0.0002*/ * 255 * 255 / 3;
        return Math.exp(-K * d * d);
    }

    private double inverseDistanceWeight(double d) {
        return 1. / Math.pow(d, exponentP);
    }

    public int interpolateColor(double attr1AtPixel, double attr2AtPixel) {

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

            double w = useIDW ? inverseDistanceWeight(distance) : gaussianWeight(distance);
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

    public String getColorPointsString() {
        StringBuilder sb = new StringBuilder();
        for (BivariateColorPoint point : points) {
            if (point.isLonLatDefined()) {
                sb.append(point.getLon());
                sb.append(", ");
                sb.append(point.getLat());
                sb.append(", 0x");
                sb.append(Integer.toHexString(point.getColor().getRGB()));
                sb.append(", ");
            }
        }
        // remove last coma and trailing empty space
        String str = sb.toString();
        if (str.length() >= 2) {
            str = str.substring(0, str.length() - 2);
        }
        return str;
    }

    // FIXME hard coded color points for the moment
    private void initPoints() {
        //Assign point x, y values
        //Set their r, g, b values (0-255)
        //Set grid values (normalized 0-1)

        //Point 1: 0 elevation and .01 precip = brown
        BivariateColorPoint point1 = new BivariateColorPoint();
        point1.setColor(131, 116, 96);
        //Set precipitation grid value
        point1.setAttribute1(0.0);
        //Set elevation grid value
        point1.setAttribute2(0.0);

        //Point 2: 0.0 elevation and 1.0 precip = green
        BivariateColorPoint point2 = new BivariateColorPoint();
        point2.setColor(0, 100, 0);
        //Set precipitation grid value
        point2.setAttribute1(1.0);
        //Set elevation grid value
        point2.setAttribute2(0.0);

        //Point 3: 1 elevation and 1 precip = white
        BivariateColorPoint point3 = new BivariateColorPoint();
        point3.setColor(255, 255, 255);
        //Set precipitation grid value
        point3.setAttribute1(1.0);
        //Set elevation grid value
        point3.setAttribute2(1.0);

        //Point 4: 1 elevation and 0 precip = best color?
        BivariateColorPoint point4 = new BivariateColorPoint();
        point4.setColor(0, 0, 255);
        //Set precipitation grid value
        point4.setAttribute1(0);
        //Set elevation grid value
        point4.setAttribute2(1);

        points.add(point1);
        points.add(point2);
        points.add(point3);
        points.add(point4);
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
                int lutCol = (int) Math.round(x * (LUT_SIZE - 1));
                int lutRow = (int) Math.round(y * (LUT_SIZE - 1));
                imageBuffer[r * width + c] = lut[lutRow][lutCol];
            }
        }
        return img;
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
        updateLUT();
    }

    /**
     * @return the attribute1Grid
     */
    public Grid getAttribute1Grid() {
        return attribute1Grid;
    }

    /**
     * @param attribute1Grid the attribute1Grid to set
     */
    public void setAttribute1Grid(Grid attribute1Grid) {
        this.attribute1Grid = attribute1Grid;
        this.attribute1MinMax = attribute1Grid.getMinMax();
    }

    /**
     * @return the attribute2Grid
     */
    public Grid getAttribute2Grid() {
        return attribute2Grid;
    }

    /**
     * @param attribute2Grid the attribute2Grid to set
     */
    public void setAttribute2Grid(Grid attribute2Grid) {
        this.attribute2Grid = attribute2Grid;
        this.attribute2MinMax = attribute2Grid.getMinMax();
    }

    public boolean hasGrids() {
        return attribute1Grid != null && attribute2Grid != null;
    }

    /**
     * @return the attribute1MinMax
     */
    public float[] getAttribute1MinMax() {
        return attribute1MinMax;
    }

    /**
     * @return the attribute2MinMax
     */
    public float[] getAttribute2MinMax() {
        return attribute2MinMax;
    }

    @Override
    public String getWarning() {
        String msg = null;
        if (getAttribute1Grid() == null && getAttribute2Grid() == null) {
            msg = "Select two grids.";
        } else if (getAttribute1Grid() == null) {
            msg = "Horizontal grid missing.";
        } else if (getAttribute2Grid() == null) {
            msg = "Vertical grid missing.";
        }
        return msg;
    }

}
