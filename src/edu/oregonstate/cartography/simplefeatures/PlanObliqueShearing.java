package edu.oregonstate.cartography.simplefeatures;

import edu.oregonstate.cartography.grid.Grid;

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

    public PlanObliqueShearing(double inclinationAngleDeg) {
        k = 1d / Math.tan(Math.toRadians(inclinationAngleDeg));
    }

    /**
     * Shear a line in vertical direction.
     * @param line The line to shear.
     * @param grid Elevation grid.
     * @param refLevel The reference level that is not sheared.
     * @return A sheared line
     */
    private LineString shear(LineString line, Grid grid, float refLevel) {
        int nPoints = line.getNumPoints();
        LineString shearedLine = new LineString();
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
            float z = grid.getBilinearInterpol(x, y);
            Point shearedPt = new Point(x, y + (z - refLevel) * k);
            if (!Double.isNaN(shearedPt.getX()) && !Double.isNaN(shearedPt.getY())) {
                shearedLine.addPoint(shearedPt);
            }
        }
        return shearedLine;
    }

    /**
     * Construct a line along the border of a grid. One point per grid vertex
     * along the upper and lower border of the grid. A single line segment along
     * the two vertical borders.
     * @param grid
     * @return 
     */
    private LineString boundingLine(Grid grid) {
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

    public GeometryCollection shear(GeometryCollection lines, Grid grid) {
        float refLevel = grid.getMinMax()[0];
        GeometryCollection shearedLines = new GeometryCollection();
        int nLines = lines.getNumGeometries();
        for (int lineID = 0; lineID < nLines; lineID++) {
            LineString line = (LineString) lines.getGeometryN(lineID);
            LineString shearedLine = shear(line, grid, refLevel);
            if (shearedLine.getNumPoints() > 1) {
                shearedLines.addGeometry(shearedLine);
            }
        }
        // add sheared line along border of the grid
        shearedLines.addGeometry(shear(boundingLine(grid), grid, refLevel));
        return shearedLines;
    }
}
