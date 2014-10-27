package edu.oregonstate.cartography.grid.operators;

import edu.oregonstate.cartography.app.Vector3D;
import edu.oregonstate.cartography.grid.Grid;

/**
 * This operator computes shaded relief from a terrain model.
 *
 * @author Charles Preppernau and Bernie Jenny, Oregon State University
 */
public class ShaderOperator extends ThreadedGridOperator {

    /**
     * Vertical exaggeration factor applied to terrain values before computing a
     * shading value.
     */
    private double vertExaggeration = 1;

    /**
     * Azimuth of the light. Counted from north in counter-clock-wise direction.
     * Between 0 and 360 degrees.
     */
    private int illuminationAzimuth = 315;

    /**
     * The vertical angle of the light direction from the zenith towards the
     * horizon. Between 0 and 90 degrees.
     */
    private int illuminationZenith = 45;

    /**
     * Creates a new instance
     */
    public ShaderOperator() {
    }

    /**
     * Compute a normal vector for a point on a digital elevation model. The
     * length of the normal vector is 1.
     *
     * @param col The horizontal coordinate at which a normal must be computed.
     * @param row The vertical coordinate at which a normal must be computed.
     * @param g Grid with elevation values
     * @param n The vector that will receive the resulting normal. Invalid upon
     * start, only used to avoid creation of a new Vector3D object.
     * @param cellSize The cell size in meters. Should not be in degrees.
     * <B>Important: row is counted from top to bottom.</B>
     */
    private void computeTerrainNormal(int col, int row, float[][] g, Vector3D n, double cellSize) {
        int nRows = g.length;
        int nCols = g[0].length;

        // get height values of four neighboring points
        final float[] centralRow = g[row];
        final double s = vertExaggeration * cellSize;
        final double elevCenter = centralRow[col];
        final double elevS, elevE, elevN, elevW;
        if (row == nRows - 1) {
            // bottom border: use inverted upper neighbor
            elevS = -(g[row - 1][col] - elevCenter) * s;
        } else {
            elevS = (g[row + 1][col] - elevCenter) * s;
        }

        if (col == nCols - 1) {
            // right border: use inverted left neighbor
            elevE = -(centralRow[col - 1] - elevCenter) * s;
        } else {
            elevE = (centralRow[col + 1] - elevCenter) * s;
        }

        if (row == 0) {
            // top border: use inverted lower neighbor
            elevN = -elevS;
        } else {
            elevN = (g[row - 1][col] - elevCenter) * s;
        }

        if (col == 0) {
            // left border: use inverted right neighbor
            elevW = -elevE;
        } else {
            elevW = (centralRow[col - 1] - elevCenter) * s;
        }

        final double x = elevW - elevE;
        final double y = elevS - elevN;
        final double z = 2 * cellSize * cellSize;

        // normalize and return vector
        final double length = Math.sqrt(x * x + y * y + z * z);
        n.x = x / length;
        n.y = y / length;
        n.z = z / length;
    }

    /**
     * Compute a shading for a chunk of the grid.
     *
     * @param src Source grid
     * @param dst Destination grid
     * @param startRow First row.
     * @param endRow First row of next chunk.
     */
    @Override
    protected void operate(Grid src, Grid dst, int startRow, int endRow) {
        int cols = src.getCols();

        // create a light vector
        Vector3D light = new Vector3D(illuminationAzimuth, illuminationZenith);

        // create a normal vector, and re-use it for every pixel
        Vector3D n = new Vector3D(0, 0, 0);

        // the cell size to calculate the horizontal components of vectors
        double cellSize = src.getCellSize();
        // convert degrees to meters on a sphere
        if (cellSize < 0.1) {
            cellSize = cellSize / 180 * Math.PI * 6371000;
        }

        // Loop through each grid cell
        float[][] dstGrid = dst.getGrid();
        float[][] srcGrid = src.getGrid();
        for (int row = startRow; row < endRow; ++row) {
            float[] dstRow = dstGrid[row];
            for (int col = 0; col < cols; col++) {
                // compute the normal of the cell
                computeTerrainNormal(col, row, srcGrid, n, cellSize);

                // compute the dot product of the normal and the light vector. This
                // gives a value between -1 (surface faces directly away from
                // light) and 1 (surface faces directly toward light)
                double dotProduct = n.dotProduct(light);

                // scale dot product from [-1, +1] to a gray value in [0, 255]
                dstRow[col] = (float) ((dotProduct + 1) / 2 * 255.0D);
            }
        }
    }

    @Override
    public String getName() {
        return "Shading";
    }

    /**
     * @param illuminationAzimuth Counted from north in counter-clock-wise
     * direction. Between 0 and 360 degrees.
     */
    public void setIlluminationAzimuth(int illuminationAzimuth) {
        this.illuminationAzimuth = illuminationAzimuth;
    }

    /**
     * @param illuminationZenith The vertical angle of the light direction from
     * the zenith towards the horizon. Between 0 and 90 degrees.
     */
    public void setIlluminationZenith(int illuminationZenith) {
        this.illuminationZenith = illuminationZenith;
    }

    /**
     *
     * @param ve Vertical exaggeration factor applied to terrain values before
     * computing a shading value.
     */
    public void setVerticalExaggeration(double ve) {
        this.vertExaggeration = ve;
    }
}
