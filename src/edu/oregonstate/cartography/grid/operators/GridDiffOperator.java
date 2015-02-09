package edu.oregonstate.cartography.grid.operators;

import edu.oregonstate.cartography.grid.Grid;

/**
 * Computes dst = src - dst
 * @author Bernhard Jenny, Cartography and Geovisualization Group, Oregon State University
 */
public class GridDiffOperator extends ThreadedGridOperator{
    
    
    public GridDiffOperator() {
    }
    
    @Override
    public String getName() {
        return "Difference";
    }

    @Override
    protected void operate(Grid src, Grid dst, int startRow, int endRow) {
        int nCols = src.getCols();
        
        float[][] srcGrid1 = src.getGrid();
        float[][] srcGrid2 = dst.getGrid();
        float[][] dstGrid = dst.getGrid();
        
        for (int row = startRow; row < endRow; ++row) {
            float[] srcRow1 = srcGrid1[row];
            float[] srcRow2 = srcGrid2[row];
            float[] dstRow = dstGrid[row];
            for (int col = 0; col < nCols; ++col) {
                float v = srcRow1[col] - srcRow2[col];
                dstRow[col] = Float.isInfinite(v) ? Float.NaN : v;
            }
        }
    }
}