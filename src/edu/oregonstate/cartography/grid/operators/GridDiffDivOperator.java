package edu.oregonstate.cartography.grid.operators;

import edu.oregonstate.cartography.grid.Grid;

/**
 * Computes (grid1 - grid2) / (grid3 + k)
 * @author Bernhard Jenny, Cartography and Geovisualization Group, Oregon State University
 */
public class GridDiffDivOperator extends ThreadedGridOperator{
    
    private float k = 1f;
    private Grid grid1, grid2, grid3;
    
    public GridDiffDivOperator() {
    }
    
    /**
     * Computes (grid1 - grid2) / (grid3 + k)
     * @param grid1
     * @param grid2
     * @param grid3
     * @param k
     * @return 
     */
    public Grid operate(Grid grid1, Grid grid2, Grid grid3, float k) {
        if (grid1 == null || grid2 == null || grid3 == null) {
            throw new IllegalArgumentException();
        }
        
        this.grid1 = grid1;
        this.grid2 = grid2;
        this.grid3 = grid3;
        this.k = k;
        return super.operate(grid1);
    }
  
    @Override
    public String getName() {
        return "Difference & Division";
    }

    @Override
    protected void operate(Grid src, Grid dst, int startRow, int endRow) {
        int nCols = src.getCols();
        
        float[][] srcGrid1 = grid1.getGrid();
        float[][] srcGrid2 = grid2.getGrid();
        float[][] srcGrid3 = grid3.getGrid();
        float[][] dstGrid = dst.getGrid();
        
        for (int row = startRow; row < endRow; ++row) {
            float[] srcRow1 = srcGrid1[row];
            float[] srcRow2 = srcGrid2[row];
            float[] srcRow3 = srcGrid3[row];
            float[] dstRow = dstGrid[row];
            for (int col = 0; col < nCols; ++col) {
                float v = (srcRow1[col] - srcRow2[col]) / (srcRow3[col] + k);
                dstRow[col] = Float.isInfinite(v) ? Float.NaN : v;
            }
        }
    }
}