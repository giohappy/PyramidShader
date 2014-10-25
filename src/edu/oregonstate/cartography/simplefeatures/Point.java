package edu.oregonstate.cartography.simplefeatures;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

/**
 *
 * @author marstonb
 */
public final class Point extends Geometry {
    private double x;
    private double y;

    public Point(double valueX, double valueY) {
        this.x = valueX;
        this.y = valueY;
    }

    /**
     * @return the x
     */
    public double getX() {
        return x;
    }

    /**
     * @param x the x to set
     */
    public void setX(double x) {
        this.x = x;
    }

    /**
     * @return the y
     */
    public double getY() {
        return y;
    }

    /**
     * @param y the y to set
     */
    public void setY(double y) {
        this.y = y;
    }

    @Override
    public String toString() {
        return "x=\"" + getX() + "\" y=\""+getY() +"\""/* + super.toString()*/;
    }

    @Override
    public void paint(Graphics2D g2d) {
        //Sets color of points
        g2d.setColor(Color.BLUE);
        //Creates a double precision ellipse centered on (6,6)
        Ellipse2D.Double point = new Ellipse2D.Double(x - 3, y - 3, 6, 6);
        //Paints the point
        g2d.fill(point);

    }

    //Returns the bounding box of a 2D rectangle with height and width equal to 0
    @Override
    public Rectangle2D getBoundingBox() {
        Rectangle2D rectangle = new Rectangle2D.Double(x, y, 0, 0);
        return rectangle;
    }

    public boolean isSameLocation(Point p) {
        return x == p.x && y == p.y;
    }
    
    /**
     * Returns the hashcode for this <code>Point2D</code>.
     * From Point2D
     * @return      a hash code for this <code>Point2D</code>.
     */
    @Override
    public int hashCode() {
        long bits = java.lang.Double.doubleToLongBits(getX());
        bits ^= java.lang.Double.doubleToLongBits(getY()) * 31;
        return (((int) bits) ^ ((int) (bits >> 32)));
    }

    /**
     * Determines whether or not two points are equal. Two instances of
     * <code>Point2D</code> are equal if the values of their
     * <code>x</code> and <code>y</code> member fields, representing
     * their position in the coordinate space, are the same.
     * From Point2D
     * @param obj an object to be compared with this <code>Point2D</code>
     * @return <code>true</code> if the object to be compared is
     *         an instance of <code>Point2D</code> and has
     *         the same values; <code>false</code> otherwise.
     * @since 1.2
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Point) {
            Point p2d = (Point) obj;
            return (getX() == p2d.getX()) && (getY() == p2d.getY());
        }
        return super.equals(obj);
    }
}
