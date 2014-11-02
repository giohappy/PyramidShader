package edu.oregonstate.cartography.simplefeatures;

import edu.oregonstate.cartography.grid.Grid;
import edu.oregonstate.cartography.grid.operators.PlanObliqueOperator;
import java.util.ArrayList;

/**
 * Applies plan oblique shearing to a set of lines
 *
 * @author Bernhard Jenny, Cartography and Geovisualization Group, Oregon State
 * University
 */
public class PlanObliqueShearing {

    /**
     * shearing factor
     */
    final double k;

    /**
     * elevation model
     */
    final Grid grid;

    /**
     * sheared elevation model
     */
    final Grid shearedGrid;

    /**
     * reference elevation that will not be sheared. Usually the lowest
     * elevation in the elevation model.
     */
    final float refElevation;
    
    /**
     * if true occluded line segments are clipped
     */
    final boolean clipOccluded;

    /**
     * Constructor
     * @param inclinationAngleDeg Shearing angle in degrees.
     * @param grid Elevation model to shear.
     * @param refElevation Elevation which is not sheared. Usually the lowest
     * elevation in the elevation model.
     * @param clipOccluded If true occluded line segments are clipped
     */
    public PlanObliqueShearing(double inclinationAngleDeg, Grid grid, 
            float refElevation, boolean clipOccluded) {
        k = 1d / Math.tan(Math.toRadians(inclinationAngleDeg));
        this.grid = grid;
        this.refElevation = refElevation;
        PlanObliqueOperator op = new PlanObliqueOperator(inclinationAngleDeg, refElevation);
        shearedGrid = op.operate(grid);
        this.clipOccluded = clipOccluded;
    }

    /**
     * Returns true if the location at x/y in the original terrain model is 
     * occluded in the sheared terrain.
     * @param x Horizontal location
     * @param y Vertical location
     * @return True if x/y is occluded
     */
    public boolean isOccluded(double x, double y) {
        // tolerance to avoid z-fighting
        final double TOL = grid.getCellSize() / 2;

        float z = grid.getBilinearInterpol(x, y);
        double shearedY = y + (z - refElevation) * k;
        return shearedGrid.getBilinearInterpol(x, shearedY) > z + TOL;
    }

    /**
     * Shear a line in vertical direction.
     *
     * @param line The line to shear.
     * @return Sheared lines
     */
    private ArrayList<LineString> shear(LineString line) {
        int nPoints = line.getNumPoints();
        LineString shearedLine = new LineString();
        ArrayList<LineString> sheared = new ArrayList();

        for (int ptID = 0; ptID < nPoints; ptID++) {
            Point pt = line.getPointN(ptID);
            double x = pt.getX();
            double y = pt.getY();
            if (x < grid.getWest()) {
                x = grid.getWest();
            }
            if (x > grid.getEast()) {
                x = grid.getEast();
            }
            if (y > grid.getNorth()) {
                y = grid.getNorth();
            }
            if (y < grid.getSouth()) {
                y = grid.getSouth();
            }
            if (clipOccluded && isOccluded(x, y)) {
                if (shearedLine.getNumPoints() > 1) {
                    sheared.add(shearedLine);
                }
                if (shearedLine.getNumPoints() != 0) {
                    shearedLine = new LineString();
                }
            } else {
                float z = grid.getBilinearInterpol(x, y);
                Point shearedPt = new Point(x, y + (z - refElevation) * k);
                if (!Double.isNaN(shearedPt.getX()) && !Double.isNaN(shearedPt.getY())) {
                    shearedLine.addPoint(shearedPt);
                }
            }
        }
        if (shearedLine.getNumPoints() > 1) {
            sheared.add(shearedLine);
        }
        return sheared;
    }

    /**
     * Construct a line along the border of a grid. One point per grid vertex
     * along the upper and lower border of the grid. A single line segment along
     * the two vertical borders.
     *
     * @param grid
     * @return
     */
    private LineString boundingLine() {
        double cellSize = grid.getCellSize();
        double west = grid.getWest();
        LineString line = new LineString();
        int nCols = grid.getCols();
        // lower border from left to right
        for (int c = 0; c < nCols; c++) {
            double x = west + c * cellSize;
            line.addPoint(new Point(x, grid.getSouth() + cellSize));
        }
        // vertical line from lower right to upper right corner
        line.addPoint(new Point(grid.getEast(), grid.getNorth() - cellSize));
        // upper border from right to left
        for (int c = nCols - 1; c >= 0; c--) {
            double x = west + c * cellSize;
            line.addPoint(new Point(x, grid.getNorth() - cellSize));
        }
        // vertical line from upper left to lower left corner
        line.addPoint(new Point(grid.getWest(), grid.getSouth() + cellSize));
        return line;
    }

    public GeometryCollection shear(GeometryCollection lines) {
        GeometryCollection shearedLines = new GeometryCollection();
        int nLines = lines.getNumGeometries();
        for (int lineID = 0; lineID < nLines; lineID++) {
            LineString line = (LineString) lines.getGeometryN(lineID);
            ArrayList<LineString> sheared = shear(line);
            for (LineString l : sheared) {
                shearedLines.addGeometry(l);
            }
        }
        // add sheared line along border of the grid
        shearedLines.addGeometry(shear(boundingLine()).get(0));
        return shearedLines;
    }
}
