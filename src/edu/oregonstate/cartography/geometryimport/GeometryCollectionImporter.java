package edu.oregonstate.cartography.geometryimport;

import edu.oregonstate.cartography.simplefeatures.GeometryCollection;
import java.io.IOException;

/**
 *
 * @author Bernhard Jenny, Cartography and Geovisualization Group, Oregon State
 * University
 */
public interface GeometryCollectionImporter {

    GeometryCollection importData(String path) throws IOException;
}
