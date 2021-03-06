package edu.oregonstate.cartography.gui.bivariate;

import ij.process.ColorSpaceConverter;
import java.awt.Color;

/**
 *
 * @author Jane Darbyshire and Bernie Jenny, Oregon State University
 */
public class BivariateColorPoint {

    // RGB color values for the point (0-255)
    private int r;
    private int g;
    private int b;

    // Lab color (identical to RGB)
    private double labL;
    private double labA;
    private double labB;

    // first attribute
    private double attribute1; //ex: precipitation

    // second attribute
    private double attribute2; //ex: elevation

    // longitude of the point
    private double lon = Double.NaN;

    // latitude of the point
    private double lat = Double.NaN;

    /**
     * Returns a string description with lon/lat, RGB, and the two attributes.
     *
     * @return The description.
     */
    @Override
    public String toString() {
        return lon + " " + lat + " " + r + " " + g + " " + b + " " + attribute1 + " " + attribute2;
    }

    /**
     * Returns the current RGB color as a Color object.
     *
     * @return a new color object
     */
    public Color getColor() {
        return new Color(r, g, b);
    }

    /**
     * The the RGB color.
     *
     * @param rgb
     */
    public void setColor(Color rgb) {
        setColor(rgb.getRed(), rgb.getGreen(), rgb.getBlue());
    }

    /**
     * Set red, green and blue color component.
     *
     * @param r red
     * @param g green
     * @param b blue
     */
    public void setColor(int r, int g, int b) {
        this.r = r;
        this.g = g;
        this.b = b;

        ColorSpaceConverter colorConverter = new ColorSpaceConverter();
        double[] lab = colorConverter.RGBtoLAB(new int[]{r, g, b});
        labL = lab[0];
        labA = lab[1];
        labB = lab[2];
    }

    /**
     * Returns the red value.
     *
     * @return the red value
     */
    public int getR() {
        return r;
    }

    /**
     * Returns the green value
     *
     * @return the green value
     */
    public int getG() {
        return g;
    }

    /**
     * Returns the blue value.
     *
     * @return the b
     */
    public int getB() {
        return b;
    }

    /**
     * Returns the Lab L component.
     *
     * @return the Lab L component
     */
    public double getLabL() {
        return labL;
    }

    /**
     * Returns the Lab A component.
     *
     * @return the Lab A component
     */
    public double getLabA() {
        return labA;
    }

    /**
     * Returns the Lab B component.
     *
     * @return the Lab B component
     */
    public double getLabB() {
        return labB;
    }

    /**
     * @return the attribute1
     */
    public double getAttribute1() {
        return attribute1;
    }

    /**
     * @param attribute1 the attribute1 to set
     */
    public void setAttribute1(double attribute1) {
        this.attribute1 = attribute1;
    }

    /**
     * @return the attribute2
     */
    public double getAttribute2() {
        return attribute2;
    }

    /**
     * @param attribute2 the attribute2 to set
     */
    public void setAttribute2(double attribute2) {
        this.attribute2 = attribute2;
    }

    /**
     * Set longitude and latitude of the point.
     *
     * @param lon Longitude
     * @param lat Latitude
     */
    public void setLonLat(double lon, double lat) {
        this.lon = lon;
        this.lat = lat;
    }

    /**
     * @return the longitude
     */
    public double getLon() {
        return lon;
    }

    /**
     * @return the latitude
     */
    public double getLat() {
        return lat;
    }

    /**
     * Returns true if the longitude and latitude values are defined.
     *
     * @return True if longitude and latitude are defined, false otherwise.
     */
    public boolean isLonLatDefined() {
        return !(Double.isNaN(lon) || Double.isNaN(lat));
    }
}
