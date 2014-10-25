/*
 * ShapeExporter.java
 *
 * Created on April 13, 2007, 3:41 PM
 *
 */

package edu.oregonstate.cartography.geometryexport;

import edu.oregonstate.cartography.simplefeatures.GeometryCollection;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 *
 * @author Bernhard Jenny, Institute of Cartography, ETH Zurich.
 */
public class ShapeExporter {
    
    private ShapeGeometryExporter shapeGeometryExporter = new ShapeGeometryExporter();
    
    private boolean shapeTypeSet = false;
    
    /** Creates a new instance of ShapeExporter */
    public ShapeExporter() {
    }
    
    public String getFileFormatName() {
        return "Shape";
    }
    
    public String getFileExtension() {
        return "shp";
    }
    
     /**
     * Exports a GeoSet to an output stream. Derived classes can overwrite this
     * method to initialize themselves. However, the exporting should be done in
     * write().
     * @param geoSet The GeoSet to export.
     * @param outputStream The destination stream that will receive the result. This
     * stream is not closed by export() - it is the responsibility of the caller
     * to close it.
     */
    public void export (GeometryCollection geoSet, OutputStream outputStream)
    throws IOException {
        
        if (geoSet == null || outputStream == null)
            throw new IllegalArgumentException();
        this.write(geoSet, outputStream);
        
    }
    
    /**
     * Export a GeoSet to a file.
     * @param geoSet The GeoSet to export.
     * @param filePath A path to a file that will receive the result. If the file
     * already exists, its content is completely overwritten. If the file does 
     * not exist, a new file is created.
     */
    public final void export (GeometryCollection geoSet, String filePath) throws IOException {
        
        if (geoSet == null || filePath == null)
            throw new IllegalArgumentException();
        
        OutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(filePath);
            this.export(geoSet, outputStream);
        } finally {
            if (outputStream != null)
                outputStream.close();
        }
        
    }
    
    protected void write(GeometryCollection geoSet, OutputStream outputStream)
    throws IOException {
        
        if (!this.shapeTypeSet)
            shapeGeometryExporter.setShapeType(ShapeGeometryExporter.POLYLINE_SHAPE_TYPE);
        shapeGeometryExporter.write(geoSet, outputStream);
    }
    
    public int getFeatureCount() {
        return this.shapeGeometryExporter.getWrittenRecordCount();
    }
    
    public void exportTableForGeometry(String geometryFilePath,
           GeometryCollection geometryCollection, String attributeName) throws IOException {
        
        
        FileOutputStream dbfOutputStream = null;
        FileOutputStream shxOutputStream = null;
        
        try {
            String dbfPath = FileUtils.replaceExtension(geometryFilePath, "dbf");
            dbfOutputStream = new FileOutputStream(dbfPath);
            new DBFExporter().exportTable(dbfOutputStream, geometryCollection.toArray(), 
                    attributeName);
            
            String shxPath = FileUtils.replaceExtension(geometryFilePath, "shx");
            shxOutputStream = new FileOutputStream(shxPath);
            shapeGeometryExporter.writeSHXFile(shxOutputStream, geometryCollection);
            
        } finally {
            if (dbfOutputStream != null)
                dbfOutputStream.close();
            if (shxOutputStream != null)
                shxOutputStream.close();
        }
    }
    
    

    /**
     * Set the type of shape file that will be generated. Valid values are 
     * POINT_SHAPE_TYPE, POLYLINE_SHAPE_TYPE, and POLYGON_SHAPE_TYPE.
     * The default value is POLYLINE_SHAPE_TYPE.
     */
    public void setShapeType(int shapeType) {
        
        this.shapeTypeSet = true;
        this.shapeGeometryExporter.setShapeType(shapeType);
        
    }
   
}
