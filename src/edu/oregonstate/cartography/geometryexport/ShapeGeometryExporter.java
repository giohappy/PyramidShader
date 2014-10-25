/*
 * ShapeGeometryExporter.java
 *
 * Created on March 28, 2007, 10:23 PM
 *
 */
package edu.oregonstate.cartography.geometryexport;

import edu.oregonstate.cartography.simplefeatures.Geometry;
import edu.oregonstate.cartography.simplefeatures.GeometryCollection;
import edu.oregonstate.cartography.simplefeatures.LineString;
import edu.oregonstate.cartography.simplefeatures.Point;
import java.awt.geom.Rectangle2D;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 * Exports a GeoSet to .shp and .shx files. Does not create a .dbf file.
 *
 * @author Bernhard Jenny, Institute of Cartography, ETH Zurich
 */
public class ShapeGeometryExporter {

    /**
     * Write points.
     */
    public static final int POINT_SHAPE_TYPE = 1;
    /**
     * Write open polylines.
     */
    public static final int POLYLINE_SHAPE_TYPE = 3;
    /**
     * Write closed polygons.
     */
    public static final int POLYGON_SHAPE_TYPE = 5;
    /**
     * This type of shapes will be exported.
     */
    private int shapeType = POLYLINE_SHAPE_TYPE;
    /**
     * Count the exported shapes, start counting at 1. This is needed to
     * sequentially number the records written to the file.
     */
    private int recordCounter = 1;
    /**
     * Store the beginning of each record in this array. This is an array of
     * offsets in bytes counted from the end of the file header. This
     * information is needed to generate the shx file, which is required by the
     * specification.
     */
    private final ArrayList shxRecords = new ArrayList();

    /**
     * Creates a new instance of ShapeGeometryExporter
     */
    public ShapeGeometryExporter() {
    }

    public String getFileFormatName() {
        return "Shape";
    }

    public String getFileExtension() {
        return "shp";
    }

    protected void write(GeometryCollection geoSet, OutputStream outputStream) throws IOException {

        this.recordCounter = 1;
        this.shxRecords.clear();

        // Accumulate the data in a ByteArrayOutputStream.
        // This allows for finding the size of the resulting file.
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        BufferedOutputStream buffOs = new BufferedOutputStream(byteArrayOutputStream);
        MixedEndianDataOutputStream geom = new MixedEndianDataOutputStream(buffOs);
        writeGeometryCollection(geom, geoSet);
        // add total size of geometry to shx records
        this.shxRecords.add(geom.size());

        // Close the ByteArrayOutputStream.
        // This is not closing the destination outputStream
        geom.close();

        // write file header
        MixedEndianDataOutputStream head = new MixedEndianDataOutputStream(outputStream);
        this.writeHeader(geoSet, head, geom.size());

        // copy the geometry to the outputStream
        byteArrayOutputStream.writeTo(outputStream);
    }

    /**
     * Writes the file header.
     *
     * @param geoSet The GeoSet to export.
     * @param mos The stream to write to.
     * @dataSize The header contains a file length field. dataSize is in bytes,
     * not including the header size.
     */
    private void writeHeader(GeometryCollection geoSet,
            MixedEndianDataOutputStream mos,
            int dataSize)
            throws IOException {

        Rectangle2D bbox = geoSet.getBoundingBox();

        mos.writeInt(9994);                 // file code
        for (int i = 0; i < 5; i++) // unused
        {
            mos.writeInt(0);
        }
        mos.writeInt(dataSize / 2 + 50);    // file length

        mos.writeLittleEndianInt(1000);     // version
        mos.writeLittleEndianInt(this.shapeType);       // shape type
        mos.writeLittleEndianDouble(bbox.getMinX());    // xmin
        mos.writeLittleEndianDouble(bbox.getMinY());    // ymin
        mos.writeLittleEndianDouble(bbox.getMaxX());    // xmax
        mos.writeLittleEndianDouble(bbox.getMaxY());    // ymax
        mos.writeLittleEndianDouble(0);     // zmin
        mos.writeLittleEndianDouble(0);     // zmax
        mos.writeLittleEndianDouble(0);     // mmin
        mos.writeLittleEndianDouble(0);     // mmax

    }

    /**
     * Writes a record header. Assigns a unique id to the new record.
     *
     * @param mos The destination stream.
     * @param length the length of the record content in bytes.
     */
    private void writeRecordHeader(MixedEndianDataOutputStream mos, int length)
            throws IOException {
        mos.writeInt(recordCounter++);      // record number, starting at 1
        mos.writeInt(length / 2);           // content length in 16 bit words
    }

    /**
     * Writes a GeoSet to a stream.
     */
    private void writeGeometryCollection(MixedEndianDataOutputStream mos, GeometryCollection geoSet)
            throws IOException {

        final int numberOfChildren = geoSet.getNumGeometries();
        for (int i = 0; i < numberOfChildren; i++) {
            Geometry geoObject = geoSet.getGeometryN(i);

            if (geoObject instanceof LineString
                    && (this.shapeType == POLYGON_SHAPE_TYPE
                    || this.shapeType == POLYLINE_SHAPE_TYPE)) {
                LineString geoPath = (LineString) geoObject;
                if (geoPath.getNumPoints() == 0) {
                    continue;
                }
                this.shxRecords.add(new Integer(mos.size()));
                writePolyline(mos, geoPath);
            } else if (geoObject instanceof Point
                    && this.shapeType == POINT_SHAPE_TYPE) {
                    // FIXME                
                //this.shxRecords.add(new Integer(mos.size()));
                //writePoint(mos, (Point) geoObject);
            } else if (geoObject instanceof GeometryCollection) {
                this.writeGeometryCollection(mos, (GeometryCollection) geoObject);
            }
        }
    }

    /**
     * Writes a path to a stream.
     */
    private void writePolyline(MixedEndianDataOutputStream mos, LineString geoPath)
            throws IOException {

        Rectangle2D bbox = geoPath.getBoundingBox();
        final double xmin = bbox.getMinX();
        final double xmax = bbox.getMaxX();
        final double ymin = bbox.getMinY();
        final double ymax = bbox.getMaxY();

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        LittleEndianOutputStream los = new LittleEndianOutputStream(bos);
        los.writeInt(shapeType);  // polyline or polygon
        los.writeDouble(xmin);              // xmin
        los.writeDouble(ymin);              // ymin
        los.writeDouble(xmax);              // xmax
        los.writeDouble(ymax);              // ymax

        final int partsCount = 1; // geoPath.getCompoundCount();
        los.writeInt(partsCount);           // number of parts

        final int pointsCount = geoPath.getNumPoints();
        los.writeInt(pointsCount);          // number of points

        int pointsCounter = 0;
        los.writeInt(pointsCounter);
        
        // write the path geometry
        for (int i = 0; i < pointsCount; i++) {
            Point p = geoPath.getPointN(i);
            los.writeDouble(p.getX());
            los.writeDouble(p.getY());
        }

        bos.flush();
        bos.close(); // close the local ByteArrayOutputStream
        this.writeRecordHeader(mos, los.size());
        mos.write(bos.toByteArray());
    }

    /**
     * Returns the number of records written to the geometry file.
     *
     * @return The number of features written so far.
     */
    public int getWrittenRecordCount() {
        return this.recordCounter - 1;
    }

    /**
     * Writes a SHX file to the passed stream.
     *
     * @param shxOutputStream The stream to write to. This stream is not closed.
     * @param geoSet The GeoSet that is exported.
     */
    public void writeSHXFile(OutputStream shxOutputStream, GeometryCollection geoSet)
            throws IOException {

        MixedEndianDataOutputStream mos = new MixedEndianDataOutputStream(shxOutputStream);

        // write the file header
        final int dataSize = (this.shxRecords.size() - 1) * 8;
        this.writeHeader(geoSet, mos, dataSize);

        // write the records
        final int recordsCount = this.shxRecords.size();
        for (int i = 0; i < recordsCount - 1; i++) {
            final int offset = ((Integer) this.shxRecords.get(i)).intValue();
            final int nextOffset = ((Integer) this.shxRecords.get(i + 1)).intValue();
            final int contentLength = nextOffset - offset;
            mos.writeInt(offset / 2 + 50);  // + 50 for the file header
            mos.writeInt(contentLength / 2 - 4);
        }

        mos.flush();
    }

    public int getShapeType() {
        return shapeType;
    }

    /**
     * Set the type of shape file that will be generated. Valid values are
     * POINT_SHAPE_TYPE, POLYLINE_SHAPE_TYPE, and POLYGON_SHAPE_TYPE. The
     * default value is POLYLINE_SHAPE_TYPE. use setShapeTypeFromFirstGeoObject
     * to automatically determine the type of shape file based on the first
     * GeoObject in a GeoSet.
     */
    public void setShapeType(int shapeType) {
        if (shapeType != POINT_SHAPE_TYPE
                && shapeType != POLYLINE_SHAPE_TYPE
                && shapeType != POLYGON_SHAPE_TYPE) {
            throw new IllegalArgumentException("invalid shape type");
        }

        this.shapeType = shapeType;
    }

}
