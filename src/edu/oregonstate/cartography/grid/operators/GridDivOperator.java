package edu.oregonstate.cartography.grid.operators;

import edu.oregonstate.cartography.grid.Grid;

/**
 * Computes grid1 / (grid2 + k)
 * @author Bernhard Jenny, Cartography and Geovisualization Group, Oregon State University
 */
public class GridDivOperator extends ThreadedGridOperator{
    
    private float k = 1f;
    private Grid grid1, grid2;
    
    public GridDivOperator() {
    }
    
    /**
     * Computes grid1 / (grid2 + k)
     * @param grid1
     * @param grid2
     * @param k
     * @return 
     */
    public Grid operate(Grid grid1, Grid grid2, float k) {
        if (grid1 == null || grid2 == null) {
            throw new IllegalArgumentException();
        }
        
        this.grid1 = grid1;
        this.grid2 = grid2;
        this.k = k;
        return super.operate(grid1);
    }
  
    @Override
    public String getName() {
        return "Division";
    }

    @Override
    protected void operate(Grid src, Grid dst, int startRow, int endRow) {
        int nCols = src.getCols();
        
        float[][] srcGrid1 = grid1.getGrid();
        float[][] srcGrid2 = grid2.getGrid();
        float[][] dstGrid = dst.getGrid();
        
        for (int row = startRow; row < endRow; ++row) {
            float[] srcRow1 = srcGrid1[row];
            float[] srcRow2 = srcGrid2[row];
            float[] dstRow = dstGrid[row];
            for (int col = 0; col < nCols; ++col) {
                float v = srcRow1[col] / (srcRow2[col] + k);
                dstRow[col] = Float.isInfinite(v) ? Float.NaN : v;
            }
        }
    }
}