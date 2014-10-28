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
     * Compute a shading for a chunk of the grid.
     *
     * @param src Source grid
     * @param dst Destination grid
     * @param startRow First row.
     * @param endRow First row of next chunk.
     */
    @Override
    protected void operate(Grid src, Grid dst, int startRow, int endRow) {
        int nCols = src.getCols();
        int nRows = src.getRows();

        // create a light vector
        Vector3D light = new Vector3D(illuminationAzimuth, illuminationZenith);
        final double lx = light.x;
        final double ly = light.y;
        final double lz = light.z;

        // the cell size to calculate the horizontal components of vectors
        double cellSize = src.getCellSize();
        // convert degrees to meters on a sphere
        if (cellSize < 0.1) {
            cellSize = cellSize / 180 * Math.PI * 6371000;
        }

        // z coordinate of normal vector
        final double nz = 2 * cellSize / vertExaggeration;
        final double nz_sq = nz * nz;

        final float[][] dstGrid = dst.getGrid();
        final float[][] srcGrid = src.getGrid();

        // top row
        if (startRow == 0) {
            for (int col = 1; col < nCols - 1; col++) {
                final double s = srcGrid[1][col];
                final double e = srcGrid[0][col + 1];
                final double c = srcGrid[0][col];
                final double w = srcGrid[0][col - 1];
                final double nx = w - e;
                final double ny = 2 * (s - c);
                final double nL = Math.sqrt(nx * nx + ny * ny + nz_sq);
                final double dotProduct = (nx * lx + ny * ly + nz * lz) / nL;
                dstGrid[0][col] = (float) ((dotProduct + 1) / 2 * 255.0D);
            }
            // top-left corner
            {
                final double s = srcGrid[1][0];
                final double e = srcGrid[0][1];
                final double c = srcGrid[0][0];
                final double nx = 2 * (e - c);
                final double ny = 2 * (s - c);
                final double nL = Math.sqrt(nx * nx + ny * ny + nz_sq);
                final double dotProduct = (nx * lx + ny * ly + nz * lz) / nL;
                dstGrid[0][0] = (float) ((dotProduct + 1) / 2 * 255.0D);
            }
            // top-right corner
            {
                final double s = srcGrid[1][nCols - 1];
                final double w = srcGrid[0][nCols - 2];
                final double c = srcGrid[0][nCols - 1];
                final double nx = 2 * (w - c);
                final double ny = 2 * (s - c);
                final double nL = Math.sqrt(nx * nx + ny * ny + nz_sq);
                final double dotProduct = (nx * lx + ny * ly + nz * lz) / nL;
                dstGrid[0][nCols - 1] = (float) ((dotProduct + 1) / 2 * 255.0D);
            }
        }
        startRow = Math.max(1, startRow);

        // bottom row
        if (endRow == nRows) {
            for (int col = 1; col < nCols - 1; col++) {
                final double n = srcGrid[nRows - 2][col];
                final double e = srcGrid[nRows - 1][col + 1];
                final double c = srcGrid[nRows - 1][col];
                final double w = srcGrid[nRows - 1][col - 1];
                final double nx = w - e;
                final double ny = 2 * (c - n);
                final double nL = Math.sqrt(nx * nx + ny * ny + nz_sq);
                final double dotProduct = (nx * lx + ny * ly + nz * lz) / nL;
                dstGrid[nRows - 1][col] = (float) ((dotProduct + 1) / 2 * 255.0D);
            }
            // bottom-left corner
            {
                final double n = srcGrid[nRows - 2][0];
                final double e = srcGrid[nRows - 1][1];
                final double c = srcGrid[nRows - 1][0];
                final double nx = 2 * (c - e);
                final double ny = 2 * (c - n);
                final double nL = Math.sqrt(nx * nx + ny * ny + nz_sq);
                final double dotProduct = (nx * lx + ny * ly + nz * lz) / nL;
                dstGrid[nRows - 1][0] = (float) ((dotProduct + 1) / 2 * 255.0D);
            }
            // bottom-right corner
            {
                final double n = srcGrid[nRows - 2][nCols - 1];
                final double w = srcGrid[nRows - 1][nCols - 2];
                final double c = srcGrid[nRows - 1][nCols - 1];
                final double nx = 2 * (w - c);
                final double ny = 2 * (c - n);
                final double nL = Math.sqrt(nx * nx + ny * ny + nz_sq);
                final double dotProduct = (nx * lx + ny * ly + nz * lz) / nL;
                dstGrid[nRows - 1][nCols - 1] = (float) ((dotProduct + 1) / 2 * 255.0D);
            }
        }
        endRow = Math.min(endRow, nRows - 1);

        // left column
        for (int row = startRow; row < endRow; ++row) {
            final double s = srcGrid[row + 1][0];
            final double e = srcGrid[row][0 + 1];
            final double c = srcGrid[row][0];
            final double n = srcGrid[row - 1][0];
            final double nx = 2 * (c - e);
            final double ny = s - n;
            final double nL = Math.sqrt(nx * nx + ny * ny + nz_sq);
            final double dotProduct = (nx * lx + ny * ly + nz * lz) / nL;
            dstGrid[row][0] = (float) ((dotProduct + 1) / 2 * 255.0D);
        }

        // right column
        for (int row = startRow; row < endRow; ++row) {
            final double s = srcGrid[row + 1][nCols - 1];
            final double c = srcGrid[row][nCols - 1];
            final double n = srcGrid[row - 1][nCols - 1];
            final double w = srcGrid[row][nCols - 2];
            final double nx = 2 * (w - c);
            final double ny = s - n;
            final double nL = Math.sqrt(nx * nx + ny * ny + nz_sq);
            final double dotProduct = (nx * lx + ny * ly + nz * lz) / nL;
            dstGrid[row][nCols - 1] = (float) ((dotProduct + 1) / 2 * 255.0D);
        }

        // interior of grid
        for (int row = startRow; row < endRow; ++row) {
            final float[] dstRow = dstGrid[row];
            final float[] topRow = srcGrid[row - 1];
            final float[] centralRow = srcGrid[row];
            final float[] bottomRow = srcGrid[row + 1];
            for (int col = 1; col < nCols - 1; col++) {
                // get height values of four neighboring points
                final double s = bottomRow[col];
                final double e = centralRow[col + 1];
                final double n = topRow[col];
                final double w = centralRow[col - 1];

                // normal vector on vertex
                final double nx = w - e;
                final double ny = s - n;
                final double nL = Math.sqrt(nx * nx + ny * ny + nz_sq);

                // compute the dot product of the normal and the light vector. This
                // gives a value between -1 (surface faces directly away from
                // light) and 1 (surface faces directly toward light)
                final double dotProduct = (nx * lx + ny * ly + nz * lz) / nL;

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
