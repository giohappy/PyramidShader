package edu.oregonstate.cartography.simplefeatures;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

public class LineString extends Geometry {

    //Stores Point objects in an ArrayList called linePoints
    ArrayList<Point> points = new ArrayList<>();

    //Returns the number of points
    public int getNumPoints() {
        return points.size();
    }

    //Returns point at specified index
    public Point getPointN(int index) {
        return points.get(index);
    }
    
    public Point getFirstPoint() {
        return points.isEmpty() ? null : points.get(0);
    }
    
    public Point getLastPoint() {
        return points.isEmpty() ? null : points.get(points.size() - 1);
    }

    //Points of line field
    public ArrayList<Point> getPoints() {
        return points;
    }

    /**
     * Returns the length of this line.
     *
     * @return
     */
    public double getLength() {
        int nPoints = getNumPoints();
        if (nPoints < 2) {
            return 0;
        }
        double length = 0;
        Point p1 = getPointN(0);
        for (int i = 1; i < nPoints; i++) {
            Point p2 = getPointN(i);
            double dx = p1.getX() - p2.getX();
            double dy = p1.getY() - p2.getY();
            length += Math.sqrt(dx * dx + dy * dy);
            p1 = p2;
        }
        return length;
    }

    //Tests if first and last point are identical and returns true if they are
    //If true, line is closed
    public boolean isClosed() {
        Point point1 = points.get(0);
        Point point2 = points.get(points.size() - 1);
        return (point1.getX() == point2.getX() && point1.getY() == point2.getY());
    }

    //Appends a passed point to list of existing points
    public void addPoint(Point addedPoint) {
        points.add(addedPoint);
    }

    public void addPointIfDifferentFromLast(Point p) {
        int nbrPoints = getNumPoints();
        if (nbrPoints == 0) {
            this.addPoint(p);
        } else {
            Point lastPoint = getPointN(nbrPoints - 1);
            if (!lastPoint.isSameLocation(p)) {
                points.add(p);
            }
        }
    }
    
    //Calls the toString method of each point and concatenates returned strings 
    //to a single string and returns the concatenated string
    @Override
    public String toString() {
        String lineDesc = /*"Attributes:\n" + super.toString() + */"Line:" + "\n";
        for (Point point : points) {
            String desc = point.toString();
            lineDesc += desc + "\n";
        }
        return lineDesc;
    }

    @Override
    public void paint(Graphics2D g2d) {
        //Sets color of lines
        g2d.setColor(Color.green);
        //Creates a GeneralPath shape
        GeneralPath.Double path = new GeneralPath.Double();

        //Retrieves the first point
        Point firstPoint = getPointN(0);
        //Pass x and y coordinates of the first point in path.moveTo()
        path.moveTo(firstPoint.getX(), firstPoint.getY());

        //Starts at index 1 and iterates over all remaining points, then 
        //calls path.lineTo
        for (int i = 1; i < points.size(); i++) {
            Point nextPoint = getPointN(i);
            path.lineTo(nextPoint.getX(), nextPoint.getY());

        }

        //Paints line
        g2d.draw(path);
    }

    @Override
    public Rectangle2D getBoundingBox() {

        //Returns null if linePoints ArrayList does not have any points
        if (points.size() < 1) {
            return null;
        }

        //Retrieves bounding box of first point of line
        Point firstPoint = getPointN(0);
        //Assign bounding box to variable
        Rectangle2D bb = firstPoint.getBoundingBox();
        //Iterate over remaining points and retrieve bounding boxes
        for (Point point : points) {
            //creates new bounding box that includes previous points and current point
            bb = bb.createUnion(point.getBoundingBox());
        }
        return bb;
    }

    public LineString densify(double maxSegmentLength) {
        LineString denseLine = new LineString();
        if (getNumPoints() == 0) {
            return denseLine;
        }

        // copy first point
        Point p1 = getPointN(0);
        denseLine.addPoint(p1);

        // densify line segment
        for (int i = 1; i < getNumPoints(); i++) {
            Point p2 = getPointN(i);
            double x1 = p1.getX();
            double y1 = p1.getY();
            double x2 = p2.getX();
            double y2 = p2.getY();
            double segmentLength = Math.hypot(x1 - x2, y1 - y2);
            if (segmentLength > maxSegmentLength) {
                double dx = (x2 - x1) / segmentLength;
                double dy = (y2 - y1) / segmentLength;
                int nbrIntermediatePoints = (int) (segmentLength / maxSegmentLength);
                for (int j = 0; j < nbrIntermediatePoints; j++) {
                    double x = x1 + j * dx * maxSegmentLength;
                    double y = y1 + j * dy * maxSegmentLength;
                    denseLine.addPoint(new Point(x, y));
                }
            }
            // add last point
            denseLine.addPoint(p2);
            p1 = p2;
        }
        return denseLine;
    }

    public void smoothPointAttribute(int filterSize, String sourceAttributeName, 
            String targetAttributeName) {
        
        if (filterSize % 2 != 1 || filterSize <= 0) {
            throw new IllegalArgumentException("filter size must be positive odd number");
        }
        
        if (getNumPoints() == 0 || filterSize == 1) {
            return;
        }
        
        int halfFilterSize = filterSize / 2;
        int nbrPoints = getNumPoints();
        for (int i = 0; i < nbrPoints; i++) {
            double total = 0;
            int nbrPointsInFilter = 0;
            for (int j = -halfFilterSize; j <= halfFilterSize; j++) {
                int ptID = i + j;
                if (ptID >= 0 && ptID < nbrPoints) {
                    Point pt = getPointN(ptID);
                    total += pt.getAttribute(sourceAttributeName).doubleValue();
                    nbrPointsInFilter++;
                }
            }
            double smoothValue = total / nbrPointsInFilter;
            getPointN(i).setAttribute(targetAttributeName, smoothValue);
        }
    }
    /*
    public static void main (String[]args) {
        LineString line = new LineString();
        for (int i = 0; i < 10; i++) {
            Point p = new Point(i, i);
            p.setAttribute("in", i*i);
            line.addPoint(p);
        }
        line.smoothPointAttribute(5, "in", "out");
        System.out.println(line);
    }
    */
    
    /**
     * Invert order of points.
     */
    public void invert() {
        int nPoints = getNumPoints();
        for (int i = 0; i < nPoints / 2; i++) {
            Point p1 = getPointN(i);
            Point p2 = getPointN(nPoints - i - 1);
            points.set(nPoints - i - 1, p1);
            points.set(i, p2);
        }
    }

    /**
     * Append line
     *
     * @param l2
     */
    public void append(LineString l2) {
        if (l2 == null || l2.getNumPoints() == 0) {
            return;
        }
        int nPoints = l2.getNumPoints();
        for (int i = 0; i < nPoints; i++) {
            Point p = l2.getPointN(i);
            addPointIfDifferentFromLast(p);
        }
    }
    
    /**
     * Joins two lines. Inverts order of points in lines if necessary. Does not
     * modify passed lines.
     *
     * @param l1
     * @param l2
     * @param centralPt
     * @return
     */
    public static LineString concatenateLines(LineString l1, LineString l2, Point centralPt) {
        if (l1.getFirstPoint().isSameLocation(centralPt)) {
            l1.invert();
        }
        if (!l2.getFirstPoint().isSameLocation(centralPt)) {
            l2.invert();
        }
        LineString combinedLine = new LineString();
        combinedLine.append(l1);
        combinedLine.append(l2);
        return combinedLine;
    }
}
