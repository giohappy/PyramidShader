package edu.oregonstate.cartography.grid.operators;

import edu.oregonstate.cartography.app.Vector3D;
import edu.oregonstate.cartography.grid.Grid;
import edu.oregonstate.cartography.gui.ProgressIndicator;
import edu.oregonstate.cartography.gui.bivariate.BivariateColorRenderer;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;

/**
 * This operator colorizes a grid.
 *
 * @author Charles Preppernau and Bernie Jenny, Oregon State University
 */
public class ColorizerOperator extends ThreadedGridOperator {

    /**
     * transparent white for void (NaN) values.
     */
    public static final int VOID_COLOR = 0x00000000;

    /**
     * The type of colored visualization this operator can create.
     */
    public enum ColorVisualization {

        GRAY_SHADING("Gray Shading"),
        EXPOSITION("Exposition Color"),
        BIVARIATE("Bivariate Color"),
        HYPSOMETRIC_SHADING("Hypsometric Color with Shading"),
        HYPSOMETRIC("Hypsometric Color"),
        LOCAL_HYPSOMETRIC_SHADING("Local Hypsometric Color with Shading"),
        LOCAL_HYPSOMETRIC("Local Hypsometric Color"),
        SLOPE("Slope"),
        ASPECT("Aspect"),
        PROFILE_CURVATURE("Profile Curvature"),
        CONTINUOUS("Continuous Tone (for Illuminated Contours)");

        private final String description;

        private ColorVisualization(String s) {
            description = s;
        }

        public boolean isLocal() {
            return this == LOCAL_HYPSOMETRIC
                    || this == LOCAL_HYPSOMETRIC_SHADING;
        }

        public boolean isShading() {
            return this == GRAY_SHADING
                    || this == EXPOSITION
                    || this == HYPSOMETRIC_SHADING
                    || this == LOCAL_HYPSOMETRIC_SHADING;
        }

        public boolean isColored() {
            return this == EXPOSITION
                    || this == BIVARIATE
                    || this == HYPSOMETRIC
                    || this == HYPSOMETRIC_SHADING
                    || this == LOCAL_HYPSOMETRIC
                    || this == LOCAL_HYPSOMETRIC_SHADING
                    || this == SLOPE
                    || this == ASPECT;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    // minimum and maximum value in the elevation model
    private float minElev, maxElev;

    // the position of each color on a relative scale between 0 and 1
    // the lowest elevation is at index 0, the highest elevation at index
    // elevations.length - 1
    private float[] colorPositions;

    // a set of colors for the hypsometric tints
    // each elevation in the elevations array has a corresponding color in the 
    // colors array.
    // store the three color components in separate arrays. This avoid repeated
    // slow calls to color.getRed(), color.getGreen() and color.getBlue()
    private int[] reds;
    private int[] greens;
    private int[] blues;

    // utility variables for accelerating gray shading computation
    private double lx, ly, lz, nz, nz_sq;

    // constant abmient illumination component.
    private double ambientLight;

    // renderer for bivariate color images
    private final BivariateColorRenderer bivariateColorRenderer;

    // colored image output
    private BufferedImage dstImage;

    // the type of visualization created
    private ColorVisualization colorVisualization = ColorVisualization.GRAY_SHADING;

    // progress indicator that needs to be updated periodically and be checked 
    // for user cancellation.
    private final ProgressIndicator progressIndicator;

    /**
     * Creates a new instance
     *
     * @param colorVisualization the type of color that is created by this
     * operator
     * @param bivariateColorRenderer Bivariate color renderer
     * @param progressIndicator Progress indicator that will be periodically
     * updated.
     */
    public ColorizerOperator(ColorVisualization colorVisualization,
            BivariateColorRenderer bivariateColorRenderer,
            ProgressIndicator progressIndicator) {
        this.colorVisualization = colorVisualization;
        this.progressIndicator = progressIndicator;
        this.bivariateColorRenderer = bivariateColorRenderer;
    }

    /**
     * Set the color ramp to use for coloring the grid.
     *
     * @param colors The color definitions
     * @param colorPositions The relative locations of the colors between 0 and
     * 1
     */
    public void setColors(Color[] colors, float[] colorPositions) {
        assert (colors.length == colorPositions.length);

        reds = new int[colors.length];
        greens = new int[colors.length];
        blues = new int[colors.length];
        for (int i = 0; i < colors.length; i++) {
            reds[i] = colors[i].getRed();
            greens[i] = colors[i].getGreen();
            blues[i] = colors[i].getBlue();
        }
        this.colorPositions = Arrays.copyOf(colorPositions, colorPositions.length);
    }

    /**
     * Computes the linearly interpolated color between the knots on the color
     * gradient
     *
     * @param gridValue the elevation or gray scale value of the current cell
     * @param minVal the lowest possible value of gridValue
     * @param maxVal the highest possible value of gridValue
     * @param mult a multiplication factor applied to the interpolated color
     * @return the interpolated color value
     */
    private int getLinearRGB(float gridVal, float minVal, float maxVal, float mult) {

        //normalize the cell elevation
        float nElev = (gridVal - minVal) / (maxVal - minVal);

        int highestID = colorPositions.length - 1;

        //Loop through the elevation values of the gradient knots, starting at 
        //the second from the top.  Stop after we get to the lowest.
        for (int i = colorPositions.length - 2; i >= 0; i -= 1) {

            //Check to see if nElev is above the highest knot
            if (nElev >= colorPositions[highestID]) {

                //If so, use the color of the highest knot and break out of the loop
                return (int) (mult * blues[highestID])
                        | ((int) (mult * greens[highestID])) << 8
                        | ((int) (mult * reds[highestID])) << 16
                        | 0xFF000000;
            }

            //Check to see if nElev is higher than the current knot
            if (nElev >= colorPositions[i]) {

                // if so, get the distance to the knot and normalize it by the 
                // distance between the two surrounding knots.
                float tu = (nElev - colorPositions[i]) / (colorPositions[i + 1] - colorPositions[i]);
                float tl = 1f - tu;
                //Get the rgb values of the upper knot
                float ur = reds[i + 1];
                float ug = greens[i + 1];
                float ub = blues[i + 1];

                //Get the rgb values of the lower knot
                float lr = reds[i];
                float lg = greens[i];
                float lb = blues[i];

                // interpolate between the colors using the normalized distance
                // to the knots as weights
                float r = mult * (tl * lr + tu * ur);
                float g = mult * (tl * lg + tu * ug);
                float b = mult * (tl * lb + tu * ub);
                return (int) b | ((int) g << 8) | ((int) r << 16) | 0xFF000000;
            }
        }

        //There's one case left to deal with; when nElev is below the lowest knot.                
        //Use the color of the lowest knot.
        return (int) (mult * blues[0])
                | ((int) (mult * greens[0]) << 8)
                | ((int) (mult * reds[0]) << 16)
                | 0xFF000000;
    }

    private boolean reportProgress(int startRow, int endRow, int row) {
        if (progressIndicator == null) {
            return true;
        }

        // report progress if this is the first chunk of the image
        // all chunks are the same size, but are rendered in different threads.
        if (startRow == 0) {
            int percentage = Math.round(100f * row / (endRow - startRow - 1f));
            progressIndicator.progress(percentage);
        }

        // stop rendering if the user cancelled
        return !progressIndicator.isCancelled();
    }

    /**
     * Do not call this method. It will throw an UnsupportedOperationException.
     *
     * @param src
     * @param dst
     * @return
     */
    @Override
    public Grid operate(Grid src, Grid dst) {
        throw new UnsupportedOperationException();
    }

    /**
     * Compute the color image.
     *
     * @param grid Grid with (elevation) values.
     * @param image Image to write pixels to. Can be null.
     * @param minElev Lowest elevation in elevationGrid
     * @param maxElev Highest elevation in elevationGrid
     * @param azimuth Azimuth angle of illumination.
     * @param zenith Zenith angle of illumination.
     * @param ambientLight Ambient light added to shading.
     *
     * @param vertExaggeration Vertical exaggeration factor to apply to
     * elevations before shading is computed.
     * @return An image with new pixels.
     */
    public BufferedImage operate(Grid grid,
            BufferedImage image, float minElev, float maxElev, double azimuth,
            double zenith, double ambientLight, float vertExaggeration) {

        // FIXME does not work
        progressIndicator.setMessage("Rendering " + colorVisualization.toString());

        dstImage = image;
        this.minElev = minElev;
        this.maxElev = maxElev;

        // create a light vector
        Vector3D light = new Vector3D(azimuth, zenith);
        lx = light.x;
        ly = light.y;
        lz = light.z;

        // the cell size to calculate the horizontal components of vectors
        double cellSize = grid.getCellSize();
        // convert degrees to meters on a sphere
        if (cellSize < 0.1) {
            cellSize = cellSize / 180 * Math.PI * 6371000;
        }

        // z coordinate of normal vector
        nz = 2 * cellSize / vertExaggeration;
        nz_sq = nz * nz;
        this.ambientLight = ambientLight;

        super.operate(grid, grid);

        // FIXME does not work
        progressIndicator.setMessage("Finished Rendering " + colorVisualization.toString());
        return dstImage;
    }

    private int[] imageBuffer(BufferedImage img) {
        return ((DataBufferInt) (img.getRaster().getDataBuffer())).getData();
    }

    /**
     * Computes a shading value in 0..255 for a normal vector.
     *
     * @param nx X component of normal vector.
     * @param ny Y component of normal vector.
     * @return Gray value between 0 and 255.
     */
    private double shadeNormal(double nx, double ny) {
        // compute the dot product of the normal and the light vector. This
        // gives a value between -1 (surface faces directly away from
        // light) and 1 (surface faces directly toward light)
        final double nL = Math.sqrt(nx * nx + ny * ny + nz_sq);
        final double dotProduct = (nx * lx + ny * ly + nz * lz) / nL;

        // scale dot product from [-1, +1] to a gray value in [0, 255]
        return Math.max(Math.min((dotProduct + 1d + ambientLight) * 127.5, 255.), 0);
    }

    private double shade(float[][] grid, int col, int row, int nCols, int nRows) {
        if (row == 0) {
            // top-left corner
            if (col == 0) {
                final double s = grid[1][0];
                final double e = grid[0][1];
                final double c = grid[0][0];
                return shadeNormal(2 * (e - c), 2 * (s - c));
            }

            // top-right corner
            if (col == nCols - 1) {
                final double s = grid[1][nCols - 1];
                final double w = grid[0][nCols - 2];
                final double c = grid[0][nCols - 1];
                return shadeNormal(2 * (w - c), 2 * (s - c));
            }

            // somewhere in top row
            final double s = grid[1][col];
            final double e = grid[0][col + 1];
            final double c = grid[0][col];
            final double w = grid[0][col - 1];
            return shadeNormal(w - e, 2 * (s - c));
        }

        if (row == nRows - 1) {
            // bottom-left corner
            if (col == 0) {
                final double n = grid[nRows - 2][0];
                final double e = grid[nRows - 1][1];
                final double c = grid[nRows - 1][0];
                return shadeNormal(2 * (c - e), 2 * (c - n));
            }

            // bottom-right corner
            if (col == nCols - 1) {
                final double n = grid[nRows - 2][nCols - 1];
                final double w = grid[nRows - 1][nCols - 2];
                final double c = grid[nRows - 1][nCols - 1];
                return shadeNormal(2 * (w - c), 2 * (c - n));
            }

            // center of bottom row
            final double n = grid[nRows - 2][col];
            final double e = grid[nRows - 1][col + 1];
            final double c = grid[nRows - 1][col];
            final double w = grid[nRows - 1][col - 1];
            return shadeNormal(w - e, 2 * (c - n));
        }

        if (col == 0) {
            final float[] topR = grid[row - 1];
            final float[] ctrR = grid[row];
            final float[] btmR = grid[row + 1];
            return shadeNormal(2 * (ctrR[0] - ctrR[1]), btmR[0] - topR[0]);
        }

        if (col == nCols - 1) {
            final float[] topR = grid[row - 1];
            final float[] ctrR = grid[row];
            final float[] btmR = grid[row + 1];
            return shadeNormal(2 * (ctrR[nCols - 2] - ctrR[nCols - 1]),
                    btmR[nCols - 1] - topR[nCols - 1]);
        }

        // normal vector on vertex
        final float[] centerRow = grid[row];
        final double nx = centerRow[col - 1] - centerRow[col + 1];
        final double ny = grid[row + 1][col] - grid[row - 1][col];
        return shadeNormal(nx, ny);
    }

    private void grayShading(Grid grid, int startRow, int endRow) {
        final float[][] gridArray = grid.getGrid();
        final int nCols = dstImage.getWidth();
        final int nRows = dstImage.getHeight();
        final int[] imageBuffer = imageBuffer(dstImage);

        for (int row = startRow; row < endRow; ++row) {
            if (!reportProgress(startRow, endRow, row)) {
                return;
            }
            for (int col = 0; col < nCols; ++col) {
                final double gray = shade(gridArray, col, row, nCols, nRows);
                if (Double.isNaN(gray)) {
                    imageBuffer[row * nCols + col] = VOID_COLOR;
                } else {
                    final int g = (int) gray;
                    imageBuffer[row * nCols + col] = g | (g << 8) | (g << 16) | 0xFF000000;
                }
            }
        }
    }

    private void hypsometricShading(Grid grid, int startRow, int endRow) {
        final int nCols = grid.getCols();
        final int nRows = grid.getRows();
        final float[][] gr = grid.getGrid();
        final int[] imageBuffer = imageBuffer(dstImage);
        for (int row = startRow; row < endRow; ++row) {
            if (!reportProgress(startRow, endRow, row)) {
                return;
            }
            for (int col = 0; col < nCols; ++col) {
                final double gray = shade(gr, col, row, nCols, nRows);
                if (Double.isNaN(gray)) {
                    imageBuffer[row * nCols + col] = VOID_COLOR;
                } else {
                    // apply a color ramp to the grid value
                    final float v = gr[row][col];
                    // multiply the color with the gray value of the shading
                    final int argb = getLinearRGB(v, minElev, maxElev, (float) (gray / 255d));
                    imageBuffer[row * nCols + col] = argb;
                }
            }
        }
    }

    private void expositionShading(Grid grid, int startRow, int endRow) {
        final float[][] gridArray = grid.getGrid();
        final int nCols = dstImage.getWidth();
        final int nRows = dstImage.getHeight();
        final int[] imageBuffer = imageBuffer(dstImage);
        for (int row = startRow; row < endRow; ++row) {
            if (!reportProgress(startRow, endRow, row)) {
                return;
            }
            for (int col = 0; col < nCols; ++col) {
                final double gray = shade(gridArray, col, row, nCols, nRows);
                if (Double.isNaN(gray)) {
                    imageBuffer[row * nCols + col] = VOID_COLOR;
                } else {
                    // apply a color ramp to the shaded gray value 
                    final int argb = getLinearRGB((float) gray, 0, 255, 1f);
                    imageBuffer[row * nCols + col] = argb;
                }
            }
        }
    }

    private void bivariate(int startRow, int endRow) {
        final int nCols = dstImage.getWidth();
        final int[] imageBuffer = imageBuffer(dstImage);
        for (int row = startRow; row < endRow; ++row) {
            if (!reportProgress(startRow, endRow, row)) {
                return;
            }
            if (bivariateColorRenderer.hasGrids() == false) {
                for (int col = 0; col < nCols; ++col) {
                    imageBuffer[row * nCols + col] = VOID_COLOR;
                }
            } else {
                for (int col = 0; col < nCols; ++col) {
                    int argb = bivariateColorRenderer.renderPixel(col, row);
                    imageBuffer[row * nCols + col] = argb;
                }
            }
        }
    }

    private void hypsometric(Grid grid, int startRow, int endRow) {
        final int nCols = dstImage.getWidth();
        final int[] imageBuffer = imageBuffer(dstImage);
        for (int row = startRow; row < endRow; ++row) {
            if (!reportProgress(startRow, endRow, row)) {
                return;
            }
            float[] gridRow = grid.getGrid()[row];
            for (int col = 0; col < nCols; ++col) {
                final float v = gridRow[col];
                if (Float.isNaN(v)) {
                    imageBuffer[row * nCols + col] = VOID_COLOR;
                } else {
                    final int argb = getLinearRGB(v, minElev, maxElev, 1);
                    imageBuffer[row * nCols + col] = argb;
                }
            }
        }
    }

    private void slope(Grid grid, int startRow, int endRow) {
        final int nCols = dstImage.getWidth();
        final int[] imageBuffer = imageBuffer(dstImage);
        for (int row = startRow; row < endRow; ++row) {
            if (!reportProgress(startRow, endRow, row)) {
                return;
            }
            for (int col = 0; col < nCols; ++col) {
                final float slope = (float) grid.getSlope(col, row);
                if (Float.isNaN(slope)) {
                    imageBuffer[row * nCols + col] = VOID_COLOR;
                } else {
                    final int argb = getLinearRGB(slope, 0, 1, 1);
                    imageBuffer[row * nCols + col] = argb;
                }
            }
        }
    }

    private void aspect(Grid grid, int startRow, int endRow) {
        final int nCols = dstImage.getWidth();
        final int[] imageBuffer = imageBuffer(dstImage);
        for (int row = startRow; row < endRow; ++row) {
            if (!reportProgress(startRow, endRow, row)) {
                return;
            }
            for (int col = 0; col < nCols; ++col) {
                final float aspect = (float) grid.getAspect(col, row);
                if (Float.isNaN(aspect)) {
                    imageBuffer[row * nCols + col] = VOID_COLOR;
                } else {
                    final int argb = getLinearRGB(aspect, (float) -Math.PI, (float) Math.PI, 1);
                    imageBuffer[row * nCols + col] = argb;
                }
            }
        }
    }

    private void profileCurvature(Grid grid, int startRow, int endRow) {
        final int nCols = dstImage.getWidth();
        final int[] imageBuffer = imageBuffer(dstImage);
        for (int row = startRow; row < endRow; ++row) {
            if (!reportProgress(startRow, endRow, row)) {
                return;
            }
            for (int col = 0; col < nCols; ++col) {
                final float profileCurvature = GridProfileCurvatureOperator.getProfileCurvature(grid, col, row, 3);
                if (Float.isNaN(profileCurvature)) {
                    imageBuffer[row * nCols + col] = VOID_COLOR;
                } else {
                    final int argb = getLinearRGB(profileCurvature, 0, 1, 1);
                    imageBuffer[row * nCols + col] = argb;
                }
            }
        }
    }

    /**
     * Compute a chunk of the image.
     *
     * @param grid Grid with elevation values.
     * @param ignore
     * @param startRow First row to compute.
     * @param endRow First row of next chunk.
     */
    @Override
    protected void operate(Grid grid, Grid ignore, int startRow, int endRow) {
        switch (colorVisualization) {
            case GRAY_SHADING:
                grayShading(grid, startRow, endRow);
                break;
            case EXPOSITION:
                expositionShading(grid, startRow, endRow);
                break;
            case BIVARIATE:
                bivariate(startRow, endRow);
                break;
            case HYPSOMETRIC_SHADING:
            case LOCAL_HYPSOMETRIC_SHADING:
                hypsometricShading(grid, startRow, endRow);
                break;
            case HYPSOMETRIC:
            case LOCAL_HYPSOMETRIC:
                hypsometric(grid, startRow, endRow);
                break;
            case SLOPE:
                slope(grid, startRow, endRow);
                break;
            case ASPECT:
                aspect(grid, startRow, endRow);
                break;
            case PROFILE_CURVATURE:
                profileCurvature(grid, startRow, endRow);
                break;
        }
    }

    @Override
    public String getName() {
        return "Colorizer";
    }

}
