package edu.oregonstate.cartography.grid.operators;

import edu.oregonstate.cartography.grid.Grid;
import edu.oregonstate.cartography.grid.LaplacianPyramid;

// FIXME filter is square instead of circular

/**
 *
 * @author Bernhard Jenny, Oregon State University
 */
public final class GridStandardDeviationOperator implements /*Threaded*/GridOperator {

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
    public Grid operate(Grid grid) {
        if (grid == null) {
            throw new IllegalArgumentException();
        }
        
        // make sure filterSize is odd number
        final int filterSize = filterSize();
        final int halfFilterSize = filterSize / 2;

        // create the new GeoGrid
        final int rows = grid.getRows();
        final int cols = grid.getCols();
        final double meshSize = grid.getCellSize();
        Grid newGrid = new Grid(cols, rows, meshSize);
        newGrid.setWest(grid.getWest());
        newGrid.setSouth(grid.getSouth());

        float[][] srcGrid = grid.getGrid();
        float[][] dstGrid = newGrid.getGrid();

        // extract high-pass band from Laplacian pyramid
        float[] weights = laplacianPyramid.createConstantWeights(0);
        for (int i = 0; i < Math.min(levels, weights.length); i++) {
            weights[i] = 1;
        }
        Grid highPassGrid = laplacianPyramid.sumLevels(weights, true);
        
        // top rows
        for (int row = 0; row < halfFilterSize; row++) {
            for (int col = 0; col < cols; col++) {
                operateBorder(grid, newGrid, col, row, highPassGrid);
            }
        }
        // bottom rows
        for (int row = rows - halfFilterSize; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                operateBorder(grid, newGrid, col, row, highPassGrid);
            }
        }
        // left columns
        for (int col = 0; col < halfFilterSize; col++) {
            for (int row = halfFilterSize; row < rows - halfFilterSize; row++) {
                operateBorder(grid, newGrid, col, row, highPassGrid);
            }
        }
        // right columns
        for (int col = cols - halfFilterSize; col < cols; col++) {
            for (int row = halfFilterSize; row < rows - halfFilterSize; row++) {
                operateBorder(grid, newGrid, col, row, highPassGrid);
            }
        }

        // interior of grid
        // FIXME adjust npts to number of NaNs
        final float npts = filterSize * filterSize;
        for (int row = halfFilterSize; row < rows - halfFilterSize; row++) {
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
        return newGrid;
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
/*
    //@Override
    protected void operate(Grid src, Grid dst, int startRow, int endRow) {
        if (src == null) {
            throw new IllegalArgumentException();
        }

        // make sure filterSize is odd number
        if (filterSize % 2 != 1) {
            throw new IllegalStateException("filter size must be odd number");
        }
        final int halfFilterSize = filterSize / 2;

        float[][] srcGrid = src.getGrid();
        float[][] dstGrid = dst.getGrid();
        final int nCols = src.getCols();
        final int nRows = src.getRows();

        // top rows
        for (int row = startRow; row < halfFilterSize; row++) {
            for (int col = 0; col < nCols; col++) {
                operateBorder(src, dst, col, row);
            }
        }
        // bottom rows
        for (int row = nRows - halfFilterSize; row < endRow; row++) {
            for (int col = 0; col < nCols; col++) {
                operateBorder(src, dst, col, row);
            }
        }
        // left columns
        for (int col = 0; col < halfFilterSize; col++) {
            for (int row = startRow; row < endRow; row++) {
                operateBorder(src, dst, col, row);
            }
        }
        // right columns
        for (int col = nCols - halfFilterSize; col < nCols; col++) {
            for (int row = startRow; row < endRow; row++) {
                operateBorder(src, dst, col, row);
            }
        }

        startRow = Math.max(halfFilterSize, startRow);
        endRow = Math.min(src.getRows() - halfFilterSize, endRow);

        for (int row = startRow; row < endRow; row++) {
            float[] dstRow = dstGrid[row];
            for (int col = halfFilterSize; col < nCols - halfFilterSize; col++) {
                // compute local mean value
                int npts = 0;
                double tot = 0;
                for (int r = row - halfFilterSize; r <= row + halfFilterSize; r++) {
                    float[] srcRow = srcGrid[r];
                    for (int c = col - halfFilterSize; c <= col + halfFilterSize; c++) {
                        final float v = srcRow[c];
                        if (!Float.isNaN(v)) {
                            tot += v;
                            npts++;
                        }
                    }
                }
                final double mean = tot / npts;

                // compute local differences to the mean value and sum the square 
                // of the differences
                double sqDif = 0;
                for (int r = row - halfFilterSize; r <= row + halfFilterSize; r++) {
                    float[] srcRow = srcGrid[r];
                    for (int c = col - halfFilterSize; c <= col + halfFilterSize; c++) {
                        final float v = srcRow[c];
                        if (!Float.isNaN(v)) {
                            final double dif = mean - v;
                            sqDif += dif * dif;
                        }
                    }
                }

                // compute standard deviation
                dstRow[col] = (float) Math.sqrt(sqDif / npts);
            }
        }

    }*/
}
