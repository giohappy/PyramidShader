/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.oregonstate.cartography.simplefeatures;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

/**
 *
 * @author marstonb
 */
public class GeometryCollection extends Geometry {

    //Stores Geometry objects in an ArrayList called geometryList
    ArrayList<Geometry> geometryList = new ArrayList<Geometry>();
    private Graphics2D g2g;

    //Returns the number of Geometry objects currently in GeometryCollection
    public int getNumGeometries() {
        return geometryList.size();
    }

    //Returns the Geometry object at a specified index
    public Geometry getGeometryN(int index) {
        return geometryList.get(index);
    }

    //Append a passed Geometry object to list of existing Geometry objects
    public void addGeometry(Geometry addedGeometry) {
        geometryList.add(addedGeometry);
    }

    //Calls the toString method of each of its Geometry objects and 
    //concatenate the returned strings.
    @Override
    public String toString() {
        String geometryDesc = "";
        //Iterates over all objects in the geometryList ArrayList
        for (Geometry geometry : geometryList) {
            // the variable point is a reference to a point in the variable arrayList
            String desc = geometry.toString();
            // append desc to a String object that is created outside of this for loop
            geometryDesc += desc + "\n";
        }
        return geometryDesc;
    }

    @Override
    public void paint(Graphics2D g2d) {
        //Calls the paint() method for each feature
        for (Geometry geometry : geometryList) {
            //Paints feature
            geometry.paint(g2d);
        }
    }

    @Override
    public Rectangle2D getBoundingBox() {
        if (geometryList.size() < 1) {
            return null;
        }
        Geometry firstGeom = getGeometryN(0);
        Rectangle2D bb = firstGeom.getBoundingBox();

        for (Geometry geometry : geometryList) {
            bb = bb.createUnion(geometry.getBoundingBox());
        }
        return bb;
    }

    public Geometry[] toArray() {
        ArrayList<Geometry> geometries = new ArrayList<>();
        for (int i = 0; i < getNumGeometries(); i++) {
            Geometry geometry = getGeometryN(i);
            geometries.add(geometry);
        }
        return geometries.toArray(new Geometry[getNumGeometries()]);
    }
}
