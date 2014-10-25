package edu.oregonstate.cartography.grid.operators;

import edu.oregonstate.cartography.grid.Grid;

/**
 *
 * @author Bernhard Jenny, Cartography and Geovisualization Group, Oregon State
 * University
 */
public class PlanObliqueOperator implements GridOperator {

    private final double k;
    
    /**
     * the elevation that will not shift, usually the minimum elevation in the grid.
     */
    private final float refElevation;

    public PlanObliqueOperator(double inclinationAngleDeg, float refElevation) {
        k = 1d / Math.tan(Math.toRadians(inclinationAngleDeg));
        this.refElevation = refElevation;
    }

    @Override
    public Grid operate(Grid grid) {
        // number of rows
        int nRows = grid.getRows();
        // number of columns
        int nCols = grid.getCols();
        // y coordinate of northern most vertices in grid
        double north = grid.getNorth();
        // grid cell size
        double cellSize = grid.getCellSize();

        // init sheared grid
        Grid shearedGrid = new Grid(nCols, nRows, grid.getCellSize());
        shearedGrid.setSouth(grid.getSouth());
        shearedGrid.setWest(grid.getWest());

        // fill sheared grid: iteerate over all columns
        for (int col = 0; col < nCols; col++) {

            // store the sheared y coordinate and the elevation of the grid 
            // vertex below the current vertex
            double prevShearedY = 0;
            double prevZ = Double.NaN;

            // keep track of the last vertext of the source grid that has been sheared.
            // this accelerates the algorithm, because we don't need to start 
            // each search at the lower grid border, but can instead continue
            // searching at the last found vertex.
            int prevRow = grid.getRows() - 1;

            // iterate over all rows, from bottom to top
            for (int row = nRows - 1; row >= 0; row--) {

                // the vertical y coordinate where an elevation value is needed
                double targetY = north - row * cellSize;

                // initialize z value at targetY
                double interpolatedZ = Double.NaN;

                // vertically traverse the column towards upper border, starting 
                // at the last visited vertex
                for (int r = prevRow; r >= 0; r--) {
                    // the elevation for the current vertex
                    float z = grid.getValue(col, r);

                    // shear the y coordinate
                    double shearedY = north - r * cellSize + (z - refElevation) * k;

                    // if the sheared y coordinate is vertically higher than the 
                    // y coordinate where an elevation value is needed, we have 
                    // found the next upper vertex
                    if (shearedY > targetY) {
                        // linearly interpolate the elevations of the vertices 
                        // that are vertically above and below
                        double w = (targetY - prevShearedY) / (shearedY - prevShearedY);
                        interpolatedZ = w * z + (1d - w) * prevZ;
                        break;
                    }

                    // the next target vertex might again fall between the same
                    // pair of vertices, so only update prevRow now.
                    prevRow = r;

                    // store the sheared y coordinate and the elevation if the current
                    // vertex is not occluded by a previously sheared vertex
                    if (shearedY >= prevShearedY) {
                        prevShearedY = shearedY;
                        prevZ = z;
                    }
                }

                // store the found z value in the grid
                shearedGrid.setValue(interpolatedZ, col, row);
            }
        }
        return shearedGrid;
    }

    @Override
    public String getName() {
        return "Plan oblique relief";
    }

}
