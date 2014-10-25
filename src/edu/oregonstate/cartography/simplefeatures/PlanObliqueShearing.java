package edu.oregonstate.cartography.simplefeatures;

import edu.oregonstate.cartography.grid.Grid;

/**
 * Applies plan oblique shearing to a set of lines
 * @author Bernhard Jenny, Cartography and Geovisualization Group, Oregon State
 * University
 */
public class PlanObliqueShearing {
    
    final double k;
    
    public PlanObliqueShearing(double inclinationAngleDeg) {
        k = 1d / Math.tan(Math.toRadians(inclinationAngleDeg));
    }
    
    public GeometryCollection shear(GeometryCollection lines, Grid grid) {
        float refLevel = grid.getMinMax()[0];
        GeometryCollection shearedLines = new GeometryCollection();
        int nLines = lines.getNumGeometries();
        for (int lineID = 0; lineID < nLines; lineID++) {
            LineString line = (LineString)lines.getGeometryN(lineID);
            int nPoints = line.getNumPoints();
            LineString shearedLine = new LineString();
            for (int ptID = 0; ptID < nPoints; ptID++) {
                Point pt = line.getPointN(ptID);
                float z = grid.getBilinearInterpol(pt.getX(), pt.getY());
                Point shearedPt = new Point(pt.getX(), pt.getY() + (z - refLevel) * k);
                shearedLine.addPoint(shearedPt);
            }
            if (shearedLine.getNumPoints() > 1) {
                shearedLines.addGeometry(shearedLine);
            }
        }
        return shearedLines;
    }
}
