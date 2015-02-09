package edu.oregonstate.cartography.grid;

import edu.oregonstate.cartography.grid.operators.GridDiffOperator;
import edu.oregonstate.cartography.grid.operators.GridDivOperator;
import edu.oregonstate.cartography.grid.operators.GridGaussLowPassOperator;
import edu.oregonstate.cartography.grid.operators.GridScaleToRangeOperator;
import edu.oregonstate.cartography.grid.operators.GridStandardDeviationOperator;

/**
 * Local hypsometric tints: A high-pass filtered grid divided by local standard
 * deviation. See:
 * http://cartographicperspectives.org/index.php/journal/article/view/cp74-huffman-patterson/623
 *
 * @author Bernhard Jenny, Cartography and Geovisualization Group, Oregon State
 * University
 */
class LocalGridModel {

    /**
     * un-filtered grid
     */
    private Grid originalGrid;

    /**
     * minimum and maximum values in original grid
     */
    private float[] originalGridMinMax;

    /**
     * Laplacian pyramid of the original grid
     */
    private LaplacianPyramid originalGridLaplacianPyramid;

    /**
     * The high-pass filtered grid.
     */
    private Grid filteredGrid;

    /**
     * The amount of details in highPassGrid. Example values: 0: All values are
     * 0 1: The first level of the Laplacian pyramid is included. 1.2: The first
     * level and 0.2 times level 2 are included.
     */
    private double highPassWeight = 1.5;

    /**
     * High-pass filtered version of the original grid.
     */
    private Grid highPassGrid;

    private int localGridStandardDeviationLevels = 3;

    /**
     * grid with standard deviation values for the original grid.
     */
    private Grid stdGrid;

    public LocalGridModel() {
    }

    public void setGrid(Grid grid, float[] minMax, LaplacianPyramid laplacianPyramid) {
        originalGrid = grid;
        originalGridMinMax = minMax;
        originalGridLaplacianPyramid = laplacianPyramid;
        highPassGrid = null;
        stdGrid = null;
        filteredGrid = null;
    }

    public Grid getFilteredGrid() {
        if (filteredGrid == null) {
            updateFilteredGrid();
        }
        return filteredGrid;
    }

    private void updateHighPassGrid() {
        if (originalGrid == null) {
            return;
        }

        // if highPassWeight < 1: compute Gaussian blur (low-pass) and compute
        // difference to the original grid.
        // if highPassWeight >= 1: combine levels in Lalacian pyramid to simulate
        // Gaussian blur. Laplacian pyramids are faster than Gaussian blur for 
        // larger filter size.
        // Note: if highPassWeight < 1 the Laplacian pyramid cannot be used, as
        // only the most detailed level in the Laplacian pyramid would be scaled
        // by highPassWeight, which corresponds to a vertical scaling, but not a 
        // frequency filter.
        
        if (highPassWeight < 1) {
            // the minimum standard deviation (for highPassWeight = 0)
            double minStdDev = 0.3;
            // the standard deviation for (highPassWeight = 1) such that a smooth
            // transition is created with the Laplacian pyramid low pass filter
            double stdDev_1 = 1.2;
            // compute standard deviation for Gauss low pass filter
            double stdDev = highPassWeight * stdDev_1 + minStdDev;
            GridGaussLowPassOperator op = new GridGaussLowPassOperator(stdDev);
            // System.out.println(op.kernelToString());
            Grid lowPassGrid = op.operate(originalGrid);            
            // high pass grid is the difference between the original grid
            // and the low-pass filtered grid.
            highPassGrid = new GridDiffOperator().operate(originalGrid, lowPassGrid);
        } else {
            // compute weights for constructing high-pass grid from Laplacian pyramid
            float[] w = originalGridLaplacianPyramid.createConstantWeights(0);
            for (int i = 0; i < w.length; i++) {
                if (Math.floor(highPassWeight) == i) {
                    w[i] = (float) (highPassWeight - i);
                } else if (Math.floor(highPassWeight) > i) {
                    w[i] = 1f;
                }
            }
            
            highPassGrid = originalGridLaplacianPyramid.sumLevels(w, true);
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
     * Computes filteredGrid, which is a high-pass filtered grid divided by
     * local standard deviation. Local hypsometric tints by Huffman & Patterson,
     * see
     * http://cartographicperspectives.org/index.php/journal/article/view/cp74-huffman-patterson/623
     */
    private void updateFilteredGrid() {
        if (originalGrid == null) {
            return;
        }

        if (highPassGrid == null) {
            updateHighPassGrid();
        }

        if (stdGrid == null) {
            updateStdGrid();
        }

        filteredGrid = new GridDivOperator().operate(highPassGrid, stdGrid, 1f);
        new GridScaleToRangeOperator(originalGridMinMax).operate(filteredGrid, filteredGrid);
    }

    /**
     * @return the highPassWeight
     */
    public double getHighPassWeight() {
        return highPassWeight;
    }

    /**
     * @param highPassWeight the highPassWeight to set
     */
    public void setHighPassWeight(double highPassWeight) {
        if (highPassWeight >= 0);
        this.highPassWeight = highPassWeight;
        updateHighPassGrid();
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
        assert (levels > 0);
        this.localGridStandardDeviationLevels = levels;
        updateStdGrid();
        updateFilteredGrid();
    }
}
