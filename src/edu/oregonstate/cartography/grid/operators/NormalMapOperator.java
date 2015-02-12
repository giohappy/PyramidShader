package edu.oregonstate.cartography.grid.operators;

import edu.oregonstate.cartography.grid.Grid;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

/**
 * This operator computes a normal map.
 *
 * @author Bernie Jenny, Oregon State University
 */
public class NormalMapOperator extends ThreadedGridOperator {

    /**
     * Red, green or blue color channel.
     */
    public enum Channel {

        R, G, B
    };

    /**
     * The color channel to store the x component of the normal vector. Default is red.
     */
    private Channel xChannel = Channel.R;
    
    /**
     * The color channel to store the y component of the normal vector. Default is green.
     */
    private Channel yChannel = Channel.G;
    
    /**
     * The color channel to store the z component of the normal vector. Default is blue.
     */
    private Channel zChannel = Channel.B;

    /**
     * If true, the x component of the normal vector is multiplied with -1.
     */
    private boolean invertX = false;
    
    /**
     * If true, the y component of the normal vector is multiplied with -1.
     */
    private boolean invertY = false;
    
    /**
     * If true, the z component of the normal vector is multiplied with -1.
     */
    private boolean invertZ = false;

    /**
     * Vertical exaggeration applied to terrain before the normal vector is computed.
     */
    private float vertExaggeration = 1;

    // colored image output
    private BufferedImage dstImage;

    /**
     * Creates a new instance with x normal components on red channel, y on
     * green channel, and z on blue channel.
     */
    public NormalMapOperator() {
    }

    /**
     * Creates a new instance with configurable channel assignments.
     *
     * @param xChannel The channel for storing the x component of normals.
     * @param yChannel The channel for storing the y component of normals.
     * @param zChannel The channel for storing the z component of normals.
     * @param invertX True if the x component of the normal is to be inverted.
     * @param invertY True if the y component of the normal is to be inverted.
     * @param invertZ True if the z component of the normal is to be inverted.
     * @param vertExaggeration Vertical exaggeration factor for terrain.
     */
    public NormalMapOperator(Channel xChannel, Channel yChannel, Channel zChannel,
            boolean invertX, boolean invertY, boolean invertZ, float vertExaggeration) {
        this.xChannel = xChannel;
        this.yChannel = yChannel;
        this.zChannel = zChannel;
        this.invertX = invertX;
        this.invertY = invertY;
        this.invertZ = invertZ;
        this.vertExaggeration = vertExaggeration;
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
     * @param vertExaggeration Vertical exaggeration factor for terrain.
     * @return An image with new pixels. image.
     */
    public BufferedImage operate(Grid grid, BufferedImage image, float vertExaggeration) {
        dstImage = image;
        super.operate(grid, grid);
        return dstImage;
    }

    /**
     * Shifts a color value by 16 or 8 bits.
     * @param ch The destination color channel.
     * @param color The color value to shift.
     * @return An integer with the shifted color value.
     */
    private int shift(Channel ch, int color) {
        if (ch == Channel.R) {
            return color << 16;
        }
        if (ch == Channel.G) {
            return color << 8;
        }
        return color;
    }

    /**
     * Computes a color-coded normal vector.
     *
     * @param nx X component of the normal vector.
     * @param ny Y component of the normal vector.
     * @param nz Z component of the normal vector.
     * @return ARGB color with encoded normal vector. The alpha component is always 255.
     */
    private int normalARGB(double nx, double ny, double nz) {
        final double nL = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (Double.isNaN(nL)) {
            return 0xFF0000FF; // should this be a vector with 0 length?
        }

        if (invertX) {
            nx *= -1;
        }
        if (invertY) {
            ny *= -1;
        }
        if (invertZ) {
            nz *= -1;
        }

        final int x = (int) (Math.round((nx / nL + 1d) / 2d * 255d));
        final int y = (int) (Math.round((ny / nL + 1d) / 2d * 255d));
        final int z = (int) (Math.round((nz / nL + 1d) / 2d * 255d));
        return 0xFF000000 | shift(xChannel, x) | shift(yChannel, y) | shift(zChannel, z);
    }

    /**
     * Computes a normal vector and encodes the vector in an ARGB color. Uses 4
     * neighboring cells to compute the normal vector.
     * @param grid The grid with elevation values.
     * @param col Column of cell.
     * @param row Row of cell.
     * @param nCols Number of columns in grid.
     * @param nRows Number of rows in grid.
     * @param nz Z component of the normal vector.
     * @return ARGB color with encoded normal vector. The alpha component is always 255.
     */
    private int normalARGB_4Neighbors(float[][] grid, int col, int row, int nCols, int nRows, double nz) {
        if (row == 0) {
            // top-left corner
            if (col == 0) {
                final double s = grid[1][0];
                final double e = grid[0][1];
                final double c = grid[0][0];
                return normalARGB(2 * (e - c), 2 * (s - c), nz);
            }

            // top-right corner
            if (col == nCols - 1) {
                final double s = grid[1][nCols - 1];
                final double w = grid[0][nCols - 2];
                final double c = grid[0][nCols - 1];
                return normalARGB(2 * (w - c), 2 * (s - c), nz);
            }

            // somewhere in top row
            final double s = grid[1][col];
            final double e = grid[0][col + 1];
            final double c = grid[0][col];
            final double w = grid[0][col - 1];
            return normalARGB(w - e, 2 * (s - c), nz);
        }

        if (row == nRows - 1) {
            // bottom-left corner
            if (col == 0) {
                final double n = grid[nRows - 2][0];
                final double e = grid[nRows - 1][1];
                final double c = grid[nRows - 1][0];
                return normalARGB(2 * (c - e), 2 * (c - n), nz);
            }

            // bottom-right corner
            if (col == nCols - 1) {
                final double n = grid[nRows - 2][nCols - 1];
                final double w = grid[nRows - 1][nCols - 2];
                final double c = grid[nRows - 1][nCols - 1];
                return normalARGB(2 * (w - c), 2 * (c - n), nz);
            }

            // center of bottom row
            final double n = grid[nRows - 2][col];
            final double e = grid[nRows - 1][col + 1];
            final double c = grid[nRows - 1][col];
            final double w = grid[nRows - 1][col - 1];
            return normalARGB(w - e, 2 * (c - n), nz);
        }

        if (col == 0) {
            final float[] topR = grid[row - 1];
            final float[] ctrR = grid[row];
            final float[] btmR = grid[row + 1];
            return normalARGB(2 * (ctrR[0] - ctrR[1]), btmR[0] - topR[0], nz);
        }

        if (col == nCols - 1) {
            final float[] topR = grid[row - 1];
            final float[] ctrR = grid[row];
            final float[] btmR = grid[row + 1];
            return normalARGB(2 * (ctrR[nCols - 2] - ctrR[nCols - 1]),
                    btmR[nCols - 1] - topR[nCols - 1], nz);
        }

        // normal vector on vertex
        final float[] centerRow = grid[row];
        final double nx = centerRow[col - 1] - centerRow[col + 1];
        final double ny = grid[row + 1][col] - grid[row - 1][col];
        return normalARGB(nx, ny, nz);
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
        float[][] g = grid.getGrid();
        final int nCols = dstImage.getWidth();
        final int nRows = dstImage.getHeight();
        final int[] imageBuffer = ((DataBufferInt) (dstImage.getRaster().getDataBuffer())).getData();
        
        // the cell size to calculate the horizontal components of vectors
        double cellSize = grid.getCellSize();
        // convert degrees to meters on a sphere
        if (cellSize < 0.1) {
            cellSize = cellSize / 180 * Math.PI * 6371000;
        }

        // z coordinate of normal vector
        double nz = 2 * cellSize / vertExaggeration;
        
        for (int row = startRow; row < endRow; ++row) {
            for (int col = 0; col < nCols; ++col) {
                imageBuffer[row * nCols + col] = normalARGB_4Neighbors(g, col, row, nCols, nRows, nz);
            }
        }
    }

    @Override
    public String getName() {
        return "Normal Map";
    }

}
