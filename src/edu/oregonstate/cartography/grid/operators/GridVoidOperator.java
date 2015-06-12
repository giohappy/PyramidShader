package edu.oregonstate.cartography.grid.operators;

import edu.oregonstate.cartography.grid.Grid;

/**
 * Replaces one value with NaN.
 * @author Bernhard Jenny, Oregon State University
 */
public class GridVoidOperator extends ThreadedGridOperator {
    
    private float v;

    public GridVoidOperator() {
        this.v = 0;
    }
    
    public GridVoidOperator(float v) {
        this.v = v;
    }
   
    @Override
    public void operate(Grid src, Grid dst, int startRow, int endRow) {
        float[][] srcGrid = src.getGrid();
        float[][] dstGrid = dst.getGrid();
        final int nCols = src.getCols();
        for (int row = startRow; row < endRow; ++row) {
            float[] srcRow = srcGrid[row];
            float[] dstRow = dstGrid[row];
            for (int col = 0; col < nCols; ++col) {
                dstRow[col] = srcRow[col] == v ? Float.NaN : srcRow[col];
            }
        }
    }

    @Override
    public String getName() {
        return "Void";
    }

}
