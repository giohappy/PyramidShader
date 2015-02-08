package edu.oregonstate.cartography.grid.operators;

import edu.oregonstate.cartography.grid.Grid;
import edu.oregonstate.cartography.grid.LaplacianPyramid;

// FIXME filter is square instead of circular
// FIXME should use Gaussian bell curve for weighting values?
// FIXME does not work properly with NaN values
// FIXME does not work with large filter size and small grid

/**
 *
 * @author Bernhard Jenny, Oregon State University
 */
public final class GridStandardDeviationOperator extends ThreadedGridOperator {

    private static final int FILTER_SIZE_SCALE = 16;

    private int levels = 3;

    private LaplacianPyramid laplacianPyramid;

    private GridStandardDeviationOperator() {
    }

    public GridStandardDeviationOperator(int levels, LaplacianPyramid laplacianPyramid) {
        this.levels = levels;
        this.laplacianPyramid = laplacianPyramid;
    }

    private int filterSize() {
        return levels * FILTER_SIZE_SCALE + 1;
    }

    @Override
    public String getName() {
        return "Local Standard Deviation Estimation";
    }

    @Override
    protected void operate(Grid src, Grid dst, int startRow, int endRow) {
        if (src == null) {
            throw new IllegalArgumentException();
        }

        // FIXME make sure filterSize is odd number
        final int filterSize = filterSize();
        final int halfFilterSize = filterSize / 2;

        final int cols = src.getCols();
        final int rows = src.getRows();

        float[][] srcGrid = src.getGrid();
        float[][] dstGrid = dst.getGrid();

        // extract high-pass band from Laplacian pyramid
        float[] weights = laplacianPyramid.createConstantWeights(0);
        for (int i = 0; i < Math.min(levels, weights.length); i++) {
            weights[i] = 1;
        }
        Grid highPassGrid = laplacianPyramid.sumLevels(weights, true);

        // FIXME the following code does not work if the filter size is larger
        // than the grid
        
        // top rows
        for (int row = startRow; row < halfFilterSize; row++) {
            for (int col = 0; col < cols; col++) {
                operateBorder(src, dst, col, row, highPassGrid);
            }
        }
        // bottom rows
        for (int row = rows - halfFilterSize; row < endRow; row++) {
            for (int col = 0; col < cols; col++) {
                operateBorder(src, dst, col, row, highPassGrid);
            }
        }
        
        startRow = Math.max(halfFilterSize, startRow);
        endRow = Math.min(src.getRows() - halfFilterSize, endRow);

        // left columns
        for (int col = 0; col < halfFilterSize; col++) {
            for (int row = startRow; row < endRow; row++) {
                operateBorder(src, dst, col, row, highPassGrid);
            }
        }
        // right columns
        for (int col = cols - halfFilterSize; col < cols; col++) {
            for (int row = startRow; row < endRow; row++) {
                operateBorder(src, dst, col, row, highPassGrid);
            }
        }

        // interior of grid
        // FIXME adjust npts to number of NaNs
        final float npts = filterSize * filterSize;      

        for (int row = startRow; row < endRow; row++) {
            float[] dstRow = dstGrid[row];
            for (int col = halfFilterSize; col < cols - halfFilterSize; col++) {
                float sqDif = 0;
                for (int r = row - halfFilterSize; r <= row + halfFilterSize; r++) {
                    float[] srcRow = srcGrid[r];
                    for (int c = col - halfFilterSize; c <= col + halfFilterSize; c++) {
                        // FIXME avoid function call
                        float dif = highPassGrid.getValue(c, r);
                        // FIXME test for NaN
                        // FIXME apply weighting with Gaussian bell
                        sqDif += dif * dif;
                    }
                }
                float std = (float) Math.sqrt(sqDif / npts);
                dstRow[col] = std;
            }
        }
    }

    private void operateBorder(Grid src, Grid dst, int col, int row, Grid highPassGrid) {

        // make sure filterSize is odd number
        final int filterSize = filterSize();
        final int halfFilterSize = filterSize / 2;

        float[][] srcGrid = src.getGrid();
        float[][] dstGrid = dst.getGrid();

        final int cols = src.getCols();
        final int rows = src.getRows();

        // FIXME adjust npts for NaN values
        int npts = filterSize * filterSize;
        float sqDif = 0;
        for (int r = row - halfFilterSize; r <= row + halfFilterSize; r++) {
            if (r > 0 && r < rows) {
                float[] srcRow = srcGrid[r];
                for (int c = col - halfFilterSize; c <= col + halfFilterSize; c++) {
                    if (c > 0 && c < cols) {
                        final float v = srcRow[c];
                        if (!Float.isNaN(v)) {
                            float dif = highPassGrid.getValue(c, r);
                            sqDif += dif * dif;
                        }
                    }
                }
            }
        }
        float std = (float) Math.sqrt(sqDif / npts);
        dstGrid[row][col] = std;
    }
}
