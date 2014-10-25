/*
 * ShapeGeometryImporter.java
 *
 * Created on June 9, 2006, 1:51 PM
 *
 */
package edu.oregonstate.cartography.geometryimport;

import edu.oregonstate.cartography.simplefeatures.GeometryCollection;
import edu.oregonstate.cartography.simplefeatures.LineString;
import edu.oregonstate.cartography.simplefeatures.Point;
import java.io.*;

/**
 * An importer for ESRI shape files. This importer only reads geometry from .shp
 * files.
 *
 * @author Bernhard Jenny, Institute of Cartography, ETH Zurich.
 */
public class ShapeGeometryImporter implements GeometryCollectionImporter {

    /**
     * Identifiers for different shape types.
     */
    private static final int NULLSHAPE = 0;
    private static final int POINT = 1;
    private static final int POLYLINE = 3;
    private static final int POLYGON = 5;
    private static final int MULTIPOINT = 8;
    private static final int POINTZ = 11;
    private static final int POLYLINEZ = 13;
    private static final int POLYGONZ = 15;
    private static final int MULTIPOINTZ = 18;
    private static final int POINTM = 21;
    private static final int POLYLINEM = 23;
    private static final int POLYGONM = 25;
    private static final int MULTIPOINTM = 28;
    private static final int MULTIPATCH = 31;   // not supported yet
    /**
     * ESRI shapefile magic code at the beginning of the .shp file.
     */
    private static final int FILE_CODE = 9994;

    /**
     * Creates a new instance of ShapeGeometryImporter
     */
    public ShapeGeometryImporter() {
    }

    protected String findDataFile(String path) {

        if (path == null || path.length() < 5) {
            return null;
        }

        String dataFileExtension = this.getLowerCaseDataFileExtension();
        String lowerCaseFilePath = path.toLowerCase();
        if (lowerCaseFilePath.endsWith("." + dataFileExtension)) {
            return path;
        }

        final boolean is_shp_sibling
                = lowerCaseFilePath.endsWith(".dbf")
                || lowerCaseFilePath.endsWith(".prj")
                || lowerCaseFilePath.endsWith(".sbn")
                || lowerCaseFilePath.endsWith(".sbx")
                || lowerCaseFilePath.endsWith(".shx");

        if (!is_shp_sibling) {
            return null;
        }

        path = replaceExtension(path, dataFileExtension);
        if (new File(path).exists()) {
            return path;
        }
        path = replaceExtension(path, dataFileExtension.toUpperCase());
        return new File(path).exists() ? path : null;
    }

    protected String getLowerCaseDataFileExtension() {
        return "shp";
    }

    private String findSHXFilePath(String path) {
        if (path == null || path.length() < 5) {
            return null;
        }
        path = replaceExtension(path, "shx");
        if (!new File(path).exists()) {
            path = replaceExtension(path, "SHX");
        }
        return new File(path).exists() ? path : null;
    }

    /**
     * Imports the geometry of a shapefile.
     *
     * @param path The path to the shapefile
     * @return A GeometryCollection with all imported features.
     * @throws IOException
     */
    @Override
    public GeometryCollection importData(String path) throws IOException {
        MixedEndianDataInputStream is = null;
        try {
            path = this.findDataFile(path);
            if (path == null) {
                return null;
            }
            GeometryCollection geometryCollection = new GeometryCollection();

            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(path));
            is = new MixedEndianDataInputStream(bis);
            // magic code is 9994
            if (is.readInt() != FILE_CODE) {
                throw new IOException("File is not an ESRI Shape file.");
            }
            is.skipBytes(5 * 4);
            int fileLength = is.readInt() * 2;
            // read version and shape type
            int version = is.readLittleEndianInt();
            int shapeType = is.readLittleEndianInt();
            // skip bounding box and four double values
            is.skipBytes(8 * 8);
            // Read all features stored in records. The shp file does not contain
            // the number of records present in the file. The shx file can be 
            // used to extract this information.
            // If the shx file cannot be found, -1 is returned.
            final int recordCount = this.readRecordCountFromSHXFile(path);
            final int[] recOffsets = readSHXFile(path);
            // Read until all records specified in the shx file are
            // imported or until the end of file is reached and an EOFException
            // is thrown.
            int currentRecord = 0;
            int currentPos = 100;
            try {
                while (true) {
                    // move to beginning of record
                    is.skipBytes(recOffsets[currentRecord] - currentPos);
                    currentPos = recOffsets[currentRecord];
                    currentPos += readRecord(is, geometryCollection);
                    if (++currentRecord == recordCount) {
                        break;
                    }
                }
            } catch (EOFException e) {
                // EOFException indicates that all records have been read.
            }
            return geometryCollection;
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    public String getImporterName() {
        return "Shape Importer";
    }

    private int readRecord(MixedEndianDataInputStream is,
            GeometryCollection geometryCollection)
            throws IOException {

        final int recordNumber = is.readInt();
        final int contentLength = is.readInt() * 2;
        // content is at least one int (i.e. the ShapeType)
        if (contentLength < 4) {
            throw new EOFException("Negative record length");
        }

        final int shapeType = is.readLittleEndianInt();
        int recordBytesRead = 8 + 4; // header is 8 bytes, shapeType is 4 bytes

        switch (shapeType) {
            case NULLSHAPE:
                break;
            case POINT:
            case POINTZ:
            case POINTM:
                recordBytesRead += readPoint(is, geometryCollection, recordNumber);
                break;
            case MULTIPOINT:
            case MULTIPOINTZ:
            case MULTIPOINTM:
                recordBytesRead += readMultipoint(is, geometryCollection, recordNumber);
                break;
            case POLYLINE:
            case POLYLINEZ:
            case POLYLINEM:
                recordBytesRead += readPolyline(is, geometryCollection, recordNumber);
                break;
            case POLYGON:
            case POLYGONZ:
            case POLYGONM:
                recordBytesRead += readPolygon(is, geometryCollection, recordNumber);
                break;
            case MULTIPATCH:
                throw new IOException("Multipatch Shape files are not supported.");
            default:
                throw new IOException("Shapefile contains unsupported "
                        + "geometry type: " + shapeType);
        }

        return recordBytesRead;
    }

    private int readPoint(MixedEndianDataInputStream is,
            GeometryCollection geometryCollection, int recordID)
            throws IOException {

        double x = is.readLittleEndianDouble();
        double y = is.readLittleEndianDouble();
        Point point = new Point(x, y);
        geometryCollection.addGeometry(point);
        return 2 * 8;

    }

    private int readMultipoint(MixedEndianDataInputStream is,
            GeometryCollection geometryCollection, int recordID)
            throws IOException {

        is.skipBytes(4 * 8); // skip bounding box
        final int numPoints = is.readLittleEndianInt();
        for (int ptID = 0; ptID < numPoints; ptID++) {
            readPoint(is, geometryCollection, recordID);
        }
        return 4 * 8 + 4 + numPoints * 2 * 8;

    }

    private int readPolyline(MixedEndianDataInputStream is,
            GeometryCollection geometryCollection,
            int recordID) throws IOException {

        is.skipBytes(4 * 8); // skip bounding box
        final int numParts = is.readLittleEndianInt();
        final int numPoints = is.readLittleEndianInt();

        // read indices into point array
        int[] pointIds = new int[numParts];
        for (int partID = 0; partID < numParts; partID++) {
            pointIds[partID] = is.readLittleEndianInt();
        }

        // read point array
        double[] x = new double[numPoints];
        double[] y = new double[numPoints];
        for (int ptID = 0; ptID < numPoints; ptID++) {
            x[ptID] = is.readLittleEndianDouble();
            y[ptID] = is.readLittleEndianDouble();
        }

        for (int partID = 0; partID < 1/* FIXME numParts*/; partID++) {

            LineString line = new LineString();
            geometryCollection.addGeometry(line);

            int firstPtID = pointIds[partID];
            int lastPtID = partID + 1 < numParts ? pointIds[partID + 1] : numPoints;

            // part must have at least two points
            if ((lastPtID - firstPtID) < 2) {
                continue;
            }

            for (int ptID = firstPtID; ptID < lastPtID; ptID++) {
                line.addPoint(new Point(x[ptID], y[ptID]));
            }
        }

        return 4 * 8 + 4 + 4 + numParts * 4 + numPoints * 2 * 8;
    }

    private int readPolygon(MixedEndianDataInputStream is,
            GeometryCollection geometryCollection,
            int recordID) throws IOException {

        is.skipBytes(4 * 8); // skip bounding box

        final int numParts = is.readLittleEndianInt();
        final int numPoints = is.readLittleEndianInt();

        // read indices into point array
        int[] pointIds = new int[numParts];
        for (int partID = 0; partID < numParts; partID++) {
            pointIds[partID] = is.readLittleEndianInt();
        }

        // read point array
        double[] x = new double[numPoints];
        double[] y = new double[numPoints];
        for (int ptID = 0; ptID < numPoints; ptID++) {
            x[ptID] = is.readLittleEndianDouble();
            y[ptID] = is.readLittleEndianDouble();
        }

        LineString line = new LineString();

        // add sections
        for (int partID = 0; partID < numParts; partID++) {
            int firstPtID = pointIds[partID];
            int lastPtID = partID + 1 < numParts ? pointIds[partID + 1] : numPoints - 1;

            // part must have at least two points
            if ((lastPtID - firstPtID) < 2) {
                continue;
            }

            for (int ptID = firstPtID + 1; ptID < lastPtID; ptID++) {
                line.addPoint(new Point(x[ptID], y[ptID]));
            }

            // close polygon when there are more than 2 points
            if (line.getNumPoints() > 2) {
                // FIXME line.close();
            }
        }

        geometryCollection.addGeometry(line);
        return 4 * 8 + 4 + 4 + numParts * 4 + numPoints * 2 * 8;
    }

    /**
     * Reads the number of records from the shx file.
     *
     * @param path The path of the data shape file.
     * @return The number of records or -1 if the shx file cannot be found.
     */
    private int readRecordCountFromSHXFile(String path) {

        MixedEndianDataInputStream is = null;
        try {
            String shxPath = findSHXFilePath(path);
            if (shxPath == null) {
                return -1;
            }
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(shxPath));
            is = new MixedEndianDataInputStream(bis);
            is.skipBytes(24);
            final int fileLength = is.readInt() * 2;
            final int recordsCount = (fileLength - 100) / 8;
            return recordsCount;
        } catch (java.io.IOException e) {
            return -1;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (java.io.IOException e) {
                }
            }
        }

    }

    private int[] readSHXFile(String path) {

        MixedEndianDataInputStream is = null;
        try {
            String shxPath = findSHXFilePath(path);
            if (shxPath == null) {
                return null;
            }
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(shxPath));
            is = new MixedEndianDataInputStream(bis);
            is.skipBytes(24);
            final int fileLength = is.readInt() * 2;
            final int recordsCount = (fileLength - 100) / 8;
            is.skipBytes(72);
            int[] recOffsets = new int[recordsCount];

            for (int i = 0; i < recordsCount; i++) {
                recOffsets[i] = is.readInt() * 2;
                // skip length
                int length = is.readInt() * 2;
                //System.out.println("Shape " + i);
                //System.out.println("Offset: " + recOffsets[i]);
                //System.out.println("Content Length: " + length);
            }

            return recOffsets;
        } catch (java.io.IOException e) {
            return null;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (java.io.IOException e) {
                }
            }
        }

    }

    /**
     * Change the extension of a file path. The extension is what follows the
     * last dot '.' in the path. If no dot exists in the path, the passed
     * extension is simply appended without replacing anything.
     *
     * @param filePath The path of the file with the extension to replace.
     * @param newExtension The new extension for the file, e.g. "tif".
     * @return A new path to a file. The file may not actually exist on the hard
     * disk.
     */
    public static String replaceExtension(String filePath, String newExtension) {
        final int dotIndex = filePath.lastIndexOf('.');
        if (dotIndex == -1) {
            return filePath + "." + newExtension;
        }
        return filePath.substring(0, dotIndex + 1) + newExtension;
    }
}
