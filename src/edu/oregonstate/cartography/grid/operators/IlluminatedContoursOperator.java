package edu.oregonstate.cartography.grid.operators;

import edu.oregonstate.cartography.grid.Grid;
import edu.oregonstate.cartography.gui.SwingWorkerWithProgressIndicator;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import javax.swing.SwingWorker;

/**
 * Computes illuminated contour lines from a digital elevation model.
 *
 * @author Jim Eynard and Bernhard Jenny, Cartography and Geovisualization
 * Group, Oregon State University
 */
public class IlluminatedContoursOperator extends ThreadedGridOperator {

    public final int CONTOURS_TRANSPARENT = -1;
    // a SwingWorker for communicating progress and for checking cancel events
    private SwingWorker progress;
    // this image will receive the computed contour lines
    private BufferedImage image;
    // illuminated and shaded or only shaded contours
    private final boolean illuminated;
    // width of lowest shaded lines
    private final double shadowWidthLow;
    // width of highestshaded lines
    private final double shadowWidthHigh;
    // width of lowest illuminated lines
    private final double illuminatedWidthLow;
    // width of highest illuminated lines
    private final double illuminatedWidthHigh;
    // minimum line width
    private final double minWidth;
    // azimuth of illumination from north in clockwise direction in degrees
    private final double azimuth;
    // contour interval
    private final double interval;
    // a gradient between black and white is applied inside this transition angle
    // in degree
    private final int gradientAngle;
    // standard deviation of Gaussian blur filter applied to grid to create smoothGrid
    private final double aspectGaussBlur;
    // a low-pass version of the source grid. Created with standard deviation
    // of aspectGaussBlur
    private Grid smoothGrid;
    // gray value of illuminated contours
    private final int illluminatedGray;
    // transition angle between illuminated and shaded contour lines, usually 90 degrees
    private final int transitionAngle;
    // pixel buffer to render to
    private int[] imageBuffer;
    // lowest elevation in grid
    private final float gridMin;
    // highest elevation in grid
    private final float gridMax;

    /**
     *
     * @param illuminated
     * @param shadowWidthLow
     * @param shadowWidthHigh
     * @param illuminatedWidthLow
     * @param minWidth
     * @param illuminatedWidthHigh
     * @param azimuth
     * @param interval
     * @param gradientAngle
     * @param illluminatedGray
     * @param aspectGaussBlur
     * @param transitionAngle
     * @param gridMinMax
     */
    public IlluminatedContoursOperator(boolean illuminated,
            double shadowWidthLow,
            double shadowWidthHigh,
            double illuminatedWidthLow,
            double illuminatedWidthHigh,
            double minWidth,
            double azimuth,
            double interval,
            int gradientAngle,
            int illluminatedGray,
            double aspectGaussBlur,
            int transitionAngle,
            float[] gridMinMax) {
        this.illuminated = illuminated;
        this.shadowWidthLow = shadowWidthLow;
        this.shadowWidthHigh = shadowWidthHigh;
        this.illuminatedWidthLow = illuminatedWidthLow;
        this.illuminatedWidthHigh = illuminatedWidthHigh;
        this.minWidth = minWidth;
        this.azimuth = azimuth;
        this.interval = Math.abs(interval);
        this.gradientAngle = gradientAngle;
        this.illluminatedGray = illluminatedGray;
        this.aspectGaussBlur = aspectGaussBlur;
        this.transitionAngle = transitionAngle;
        this.gridMin = gridMinMax[0];
        this.gridMax = gridMinMax[1];
    }

    /**
     * Renders contours to the passed image.
     *
     * @param destinationImage Image must be this.scale times larger than the
     * grid.
     * @param grid Grid with elevation values.
     * @param slopeGrid Grid with slope values.
     * @param progress Progress indicator. Not used when scale is 1.
     */
    public void renderToImage(BufferedImage destinationImage, Grid grid, Grid slopeGrid, SwingWorker progress) {
        if (destinationImage == null) {
            throw new IllegalArgumentException();
        }
        this.image = destinationImage;
        this.progress = progress;
        this.imageBuffer = ((DataBufferInt) (image.getRaster().getDataBuffer())).getData();
        this.smoothGrid = new GridGaussLowPassOperator(aspectGaussBlur).operate(grid);
        super.operate(grid, slopeGrid);
    }

    /**
     * Compute a chunk of the destination grid.
     *
     * @param src The source terrain elevation grid.
     * @param slopeGrid Slope grid.
     * @param startRow The index of the first row of the source grid.
     * @param endRow The index of the first row of the source grid.
     */
    @Override
    public void operate(Grid src, Grid slopeGrid, int startRow, int endRow) {
        startRow = Math.max(1, startRow);
        endRow = Math.min(src.getRows() - 2, endRow);
        int cols = src.getCols();

        int scale = image.getWidth() / src.getCols();
        if (scale == 1) {
            for (int row = startRow; row < endRow; row++) {
                for (int col = 1; col < cols - 1; col++) {
                    illuminatedContours(src, col, row);
                }
            }
        } else {
            // only report progress if this is the first chunk of the image
            // all chunks are the same size, but are rendered in different threads.
            boolean reportProgress = startRow == 1
                    && progress instanceof SwingWorkerWithProgressIndicator;

            for (int row = startRow; row < endRow; row++) {
                // stop rendering if the user canceled
                if (progress != null && progress.isCancelled()) {
                    return;
                }

                // report progress made
                if (reportProgress) {
                    int percentage = Math.round(100f * row / (endRow - startRow));
                    ((SwingWorkerWithProgressIndicator) progress).progress(percentage);
                }

                // destination has different size than source grid.
                for (int col = 1; col < cols - 1; col++) {
                    scaledIlluminatedContours(src, col, row, slopeGrid, scale);
                }
            }
        }
    }

    /**
     * Compute a single grid value with illuminated contours that has the same
     * size as the source grid.
     *
     * @param src The source terrain elevation grid.
     * @param dst The destination grid with illuminated contour lines.
     * @param col The column in the source grid.
     * @param row The row in the source grid.
     */
    private void illuminatedContours(Grid src, int col, int row) {
        double elevation = src.getValue(col, row);
        double smoothAspect = smoothGrid.getAspect(col, row);
        smoothAspect = (smoothAspect + Math.PI) * 180 / Math.PI;
        double slope = src.getSlope(col, row);
        int g = computeGray(elevation, smoothAspect, slope, src.getCellSize());
        if (g != CONTOURS_TRANSPARENT) {
            int argb = (int) g | ((int) g << 8) | ((int) g << 16) | 0xFF000000;
            imageBuffer[row * image.getWidth() + col] = argb;
        }
    }

    /**
     * Compute a grid values corresponding to one cell in the source grid. The
     * destination grid has a different size than the source grid.
     *
     * @param src The source terrain elevation grid.
     * @param col The column in the source grid.
     * @param row The row in the source grid.
     * @param scale The image is this many times larger than the terrain model
     * grid.
     */
    private void scaledIlluminatedContours(Grid src, int col, int row,
            Grid slopeGrid, int scale) {
        final double cellSize = src.getCellSize();
        final double samplingDist = cellSize / scale;
        final double west = src.getWest();
        final double north = src.getNorth();

        // render scale x scale subcells in the destination grid
        for (int r = 0; r < scale; r++) {
            for (int c = 0; c < scale; c++) {
                // convert column and row to geographic coordinates
                double x = west + ((col + (double) c / scale) * cellSize);
                double y = north - ((row + (double) r / scale) * cellSize);
                double elevation = src.getBilinearInterpol(x, y);
                double smoothAspect = smoothGrid.getAspect(x, y, samplingDist);
                smoothAspect = (smoothAspect + Math.PI) * 180 / Math.PI;
                double slopeVal = slopeGrid.getBilinearInterpol(x, y);
                int g = computeGray(elevation, smoothAspect, slopeVal, cellSize);
                if (g != CONTOURS_TRANSPARENT) {
                    int argb = (int) g | ((int) g << 8) | ((int) g << 16) | 0xFF000000;
                    int imageCol = col * scale + c;
                    int imageRow = row * scale + r;
                    imageBuffer[imageRow * image.getWidth() + imageCol] = argb;
                }
            }
        }
    }

    @Override
    public String getName() {
        return "Illuminated Contours";
    }

    /**
     * Returns the smallest angle between two angles
     *
     * @param a angle from east counterclockwise in degrees
     * @param b angle from east counterclockwise in degrees
     * @return The minimum angle between the two angles in degrees
     */
    private static double smallestAngleDiff(double a, double b) {
        final double d = Math.abs(a - b) % 360.;
        return (d > 180.) ? 360. - d : d;
    }

    /**
     * Compute the gray value for the illuminated contour line image
     *
     * @param elevation Elevation of the point.
     * @param aspectDeg Terrain aspect at the point in degrees.
     * @param slopePerc Terrain slope at the point in rise/run [0..1].
     * @param cellSize Size of a grid cell. Same units as elevation parameter.
     * @return Gray value between 0 and 255.
     */
    private int computeGray(double elevation, double aspectDeg, double slopePerc, double cellSize) {
        // convert azimuth angle to geometric angle, from east counterclockwise
        double illumination = 90 - azimuth;
        // calculate minumum angle between illumination angle and aspect
        double angleDiff = smallestAngleDiff(illumination, aspectDeg);
        double angleDiffRad = angleDiff / 180. * Math.PI;
        
        // vary the shadowed and illuminated line widths with elevation
        double w = (gridMax - elevation) / (gridMax - gridMin);
        //double gamma = 2;
        //w = Math.pow(w, 1d / gamma);
        double shadowWidth = w * (shadowWidthLow - shadowWidthHigh) + shadowWidthHigh;

        // compute the line width, which varies with the orientation relative
        // to the illumination direction
        double lineWidth;       
        if (illuminated) {
            //convert to radians
            double transitionAngleRad = transitionAngle / 180. * Math.PI;
            
            if (angleDiff > transitionAngle) {
                //scale angleDiff to range between transitionAngle and 180 degrees
                double m = (Math.PI / 2) / (Math.PI - transitionAngleRad);
                double c = (Math.PI / 2) - m * transitionAngleRad;
                angleDiffRad = angleDiffRad * m + c;
                //modulate with cosine
                lineWidth = shadowWidth * Math.abs(Math.cos(angleDiffRad));
            } else {
                //scale angleDiff to range between 0 and transitionAngle
                angleDiffRad = angleDiffRad / transitionAngleRad * (Math.PI / 2);
                double illuminatedWidth = w * (illuminatedWidthLow - illuminatedWidthHigh) + illuminatedWidthHigh;
                //modulate with cosine
                lineWidth = illuminatedWidth * Math.abs(Math.cos(angleDiffRad));
            }
        } else {
            //for shadowed contours
            //modulate with sine
            lineWidth = shadowWidth * Math.abs(Math.sin(angleDiffRad / 2));
        }

        // make lines minimum width
        lineWidth = Math.max(minWidth, lineWidth);
        // convert to units of z values
        lineWidth *= cellSize;
        
        // compute vertical z distance to closest contour line
        double zDist = Math.abs(elevation) % interval;
        if (zDist > interval / 2) {
            zDist = interval - zDist;
        }

        if (lineWidth * slopePerc > zDist) {
            if (!illuminated || angleDiff >= (transitionAngle + gradientAngle)) {
                // shaded side
                return 0; // black
            } else if (angleDiff <= (transitionAngle - gradientAngle)) {
                // illuminated side
                return illuminated ? illluminatedGray : 0;
            } else {
                // gradient between shaded and illuminated side
                double d = transitionAngle + gradientAngle - angleDiff;
                return (int) (d / (2. * gradientAngle) * 255.);
            }
        }
        return CONTOURS_TRANSPARENT;
    }
}
