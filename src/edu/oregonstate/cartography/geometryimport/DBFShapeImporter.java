/*
 * DBFShapeImporter.java
 *
 * Created on July 6, 2006, 2:28 PM
 *
 */

package edu.oregonstate.cartography.geometryimport;

import java.io.File;


/**
 * Extends DBFImporter to add support for finding a dbf file that is part of 
 * a ESRI shape file set.
 * @author jenny
 */
public class DBFShapeImporter extends DBFImporter {
    
    /** Creates a new instance of DBFShapeImporter */
    public DBFShapeImporter() {
    }
    
    private String findDbfFileSibling(String filePath) {
        
        if (filePath == null || filePath.length() < 5)
            return null;
        
        String lowerCaseFilePath = filePath.toLowerCase();
        if (lowerCaseFilePath.endsWith(".dbf"))
            return filePath;
        
        final boolean is_dbf_sibling =
                lowerCaseFilePath.endsWith(".shp") ||
                lowerCaseFilePath.endsWith(".prj") ||
                lowerCaseFilePath.endsWith(".sbn") ||
                lowerCaseFilePath.endsWith(".sbx") ||
                lowerCaseFilePath.endsWith(".shx");
        
        if (!is_dbf_sibling)
            return null;
        
        filePath = ShapeGeometryImporter.replaceExtension(filePath, "dbf");
        if (!new File(filePath).exists())
            filePath = ShapeGeometryImporter.replaceExtension(filePath, "DBF");
        return new File(filePath).exists() ? filePath : null;
    }
    
    public Table read(String filePath) throws java.io.IOException {
        return super.read(findDbfFileSibling(filePath));
    }
}
