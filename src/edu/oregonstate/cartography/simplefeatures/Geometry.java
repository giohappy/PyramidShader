package edu.oregonstate.cartography.simplefeatures;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;

/**
 *
 * @author marstonb
 */
public abstract class Geometry {

    HashMap<String, Number> attributes = new HashMap<>(5);

    public void setAttribute(String name, Number value) {
        attributes.put(name, value);
    }

    public Number getAttribute(String name) {
        return attributes.get(name);
    }
    
    public boolean hasAttribute(String name) {
        return attributes.containsKey(name);
    }

    //paint method for each feature class
    public abstract void paint(Graphics2D g2d);

    //Returns a bounding box
    public abstract Rectangle2D getBoundingBox();

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (String key : attributes.keySet()) {
            sb.append(key);
            sb.append(" ");
            sb.append(attributes.get(key));
            sb.append("\n");
        }
        return sb.toString();
    }
}
