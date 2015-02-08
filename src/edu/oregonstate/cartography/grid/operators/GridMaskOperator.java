/*
 * GridMaskOperator.java
 *
 * Created on February 7, 2015
 *
 */
package edu.oregonstate.cartography.grid.operators;

import edu.oregonstate.cartography.grid.Grid;

/**
 * Copies NaN values from a source grid to a destination grid.
 *
 * @author Bernhard Jenny, Oregon State University
 */
public class GridMaskOperator extends ThreadedGridOperator {

    @Override
    public void operate(Grid src, Grid dst, int startRow, int endRow) {

        float[][] srcGrid = src.getGrid();
        float[][] dstGrid = dst.getGrid();
        final int nCols = src.getCols();

        for (int row = startRow; row < endRow; ++row) {
            float[] srcRow = srcGrid[row];
            float[] dstRow = dstGrid[row];
            for (int col = 0; col < nCols; ++col) {
                if (Float.isNaN(srcRow[col])) {
                    dstRow[col] = Float.NaN;
                }
            }
        }
    }

    @Override
    public String getName() {
        return "Mask";
    }
}
