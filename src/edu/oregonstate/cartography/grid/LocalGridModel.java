package edu.oregonstate.cartography.grid;

import edu.oregonstate.cartography.grid.operators.GridDiffDivOperator;
import edu.oregonstate.cartography.grid.operators.GridGaussLowPassOperator;
import edu.oregonstate.cartography.grid.operators.GridScaleToRangeOperator;
import edu.oregonstate.cartography.grid.operators.GridStandardDeviationOperator;

/**
 * a high-pass filtered grid divided by local standard deviation used for local
 * hypsometric tints. See:
 * http://cartographicperspectives.org/index.php/journal/article/view/cp74-huffman-patterson/623
 *
 * @author Bernhard Jenny, Cartography and Geovisualization Group, Oregon State
 * University
 */
class LocalGridModel {

    // FIXME add comments

    private Grid filteredGrid;
    private double localGridLowPassStd = 11;
    private int localGridStandardDeviationLevels = 3;
    private Grid originalGrid;
    private float[] originalGridMinMax;
    private LaplacianPyramid originalGridLaplacianPyramid;
    private Grid lowPassGrid;
    private Grid stdGrid;
    

    public LocalGridModel() {
    }

    public void setGrid(Grid grid, float[] minMax, LaplacianPyramid laplacianPyramid) {
        originalGrid = grid;
        originalGridMinMax = minMax;
        originalGridLaplacianPyramid = laplacianPyramid;
    }

    public Grid getFilteredGrid() {
        if (filteredGrid == null) {
            updateFilteredGrid();
        }
        return filteredGrid;
    }

    private void updateLowPassGrid() {
        if (originalGrid != null) {
            //long startTime = System.nanoTime();
            //System.out.println("low pass: start");
            lowPassGrid = new GridGaussLowPassOperator(localGridLowPassStd).operate(originalGrid);
            //System.out.println("low pass: end " + (System.nanoTime() - startTime) / 1000 / 1000 + "ms");
        }
    }

    private void updateStdGrid() {
        if (originalGrid != null) {
            //long startTime = System.nanoTime();
            //System.out.println("std dev: start");
            stdGrid = new GridStandardDeviationOperator(localGridStandardDeviationLevels, originalGridLaplacianPyramid).operate(originalGrid);
            //System.out.println("std dev: end " + (System.nanoTime() - startTime) / 1000 / 1000 + "ms");
        }
    }

    /**
     * re-computes localGrid, which is a high-pass filtered grid divided by
     * local standard deviation. localGrid is used for local hypsometric tints
     * as introduced by Huffman & Patterson, see
     * http://cartographicperspectives.org/index.php/journal/article/view/cp74-huffman-patterson/623
     */
    private void updateFilteredGrid() {
        if (originalGrid == null) {
            return;
        }
        if (lowPassGrid == null) {
            updateLowPassGrid();
        }
        if (stdGrid == null) {
            updateStdGrid();
        }
        //long startTime = System.nanoTime();
        //System.out.println("local grid: start");
        filteredGrid = new GridDiffDivOperator().operate(originalGrid, lowPassGrid, stdGrid, 1f);
        new GridScaleToRangeOperator(originalGridMinMax).operate(filteredGrid, filteredGrid);
        //System.out.println("local grid: end " + (System.nanoTime() - startTime) / 1000 / 1000 + "ms");
    }

    /**
     * @return the localGridLowPassStd
     */
    public double getLocalGridLowPassStd() {
        return localGridLowPassStd;
    }

    /**
     * @param localGridLowPassStd the localGridLowPassStd to set
     */
    public void setLocalGridLowPassStd(double localGridLowPassStd) {
        this.localGridLowPassStd = localGridLowPassStd;
        updateLowPassGrid();
        updateFilteredGrid();
    }

    /**
     * @return the localGridStandardDeviationLevels
     */
    public int getLocalGridStandardDeviationLevels() {
        return localGridStandardDeviationLevels;
    }

    /**
     * @param localGridStandardDeviationFilterSize the
     * localGridStandardDeviationFilterSize to set
     */
    public void setLocalGridStandardDeviationLevels(int levels) {
        this.localGridStandardDeviationLevels = levels;
        updateStdGrid();
        updateFilteredGrid();
    }

}
