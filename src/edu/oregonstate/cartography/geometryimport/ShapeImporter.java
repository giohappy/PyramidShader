package edu.oregonstate.cartography.geometryimport;

import edu.oregonstate.cartography.simplefeatures.GeometryCollection;
import java.io.IOException;
import javax.swing.table.TableColumn;

/**
 *
 * @author Bernhard Jenny, Cartography and Geovisualization Group, Oregon State
 */
public class ShapeImporter extends ShapeGeometryImporter {

    @Override
    public GeometryCollection importData(String path) throws IOException {
        GeometryCollection geometryCollection = super.importData(path);
        DBFShapeImporter dbfImporter = new DBFShapeImporter();
        Table table = dbfImporter.read(path);
        int nbrFeatures = geometryCollection.getNumGeometries();
        int nbrAttributes = table.getColumnCount();
        for (int featureIdx = 0; featureIdx < nbrFeatures; featureIdx++) {
            for (int attributeIdx = 0; attributeIdx < nbrAttributes; attributeIdx++) {
                if (table.isDoubleColumn(attributeIdx)) {
                    TableColumn tc = table.getColumn(attributeIdx);
                    String key = tc.getHeaderValue().toString().trim();
                    Double value = (Double)table.getValueAt(featureIdx, attributeIdx);
                    geometryCollection.getGeometryN(featureIdx).setAttribute(key, value);
                }
            }
        }
        return geometryCollection;
    }
}
