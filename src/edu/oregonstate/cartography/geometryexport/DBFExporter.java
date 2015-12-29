/*
 * DBFExporter.java
 *
 * Created on April 13, 2007, 2:18 PM
 *
 */
package edu.oregonstate.cartography.geometryexport;

import edu.oregonstate.cartography.simplefeatures.Geometry;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Exporter for the DBF file format.
 *
 * @author Bernhard Jenny, Institute of Cartography, ETH Zurich.
 */
public class DBFExporter {

    private static final int STRING_LENGTH = 64;
    private static final int NUMBER_LENGTH = 20;    // F n=1..20
    private static final int NUMBER_DECIMALS = 8;

    /**
     * Creates a new instance of DBFExporter
     */
    public DBFExporter() {
    }

    public void exportTable(OutputStream outputStream, Geometry[] geometries, String attributeName)
            throws IOException {

        try (BufferedOutputStream bos = new BufferedOutputStream(outputStream)) {
            LittleEndianOutputStream dos = new LittleEndianOutputStream(bos);

            // dbf flag
            dos.write(0x03);

            // current date
            Calendar cal = GregorianCalendar.getInstance();
            int year = cal.get(Calendar.YEAR);             // 2002
            int month = cal.get(Calendar.MONTH);           // 0=Jan, 1=Feb, ...
            int day = cal.get(Calendar.DAY_OF_MONTH);      // 1...
            dos.write(year - 1900);
            dos.write(month);
            dos.write(day);

            // number of records
            dos.writeInt(geometries.length);

            // header size
            int columnsCount = 1; // table.getColumnCount();
            if (attributeName != null) {
                columnsCount++;
            }
            dos.writeShort(32 + columnsCount * 32 + 1);

            // record size
            dos.writeShort(computeRecordSize(null));

            // reserved value, fill with 0
            dos.writeShort(0);

            // transaction byte
            dos.write(0);

            // encription byte
            dos.write(0);

            // multi user environment use
            for (int i = 0; i < 13; i++) {
                dos.write(0);
            }

            // codepage / language driver
            // ESRI shape files use code 0x57 to indicate that
            // data is written in ANSI (whatever that means).
            // http://www.esricanada.com/english/support/faqs/arcview/avfaq21.asp
            dos.write(0x57);

            // two reserved bytes
            dos.writeShort(0);

            this.writeFieldDescriptors(dos, attributeName);

            // header record terminator
            dos.write(0x0D);

            this.writeRecords(dos, geometries, attributeName);
        }
    }

    private int computeRecordSize(String attributeName) throws IOException {
        int recordSize = 1; // 1 for deletion flag
        int columnsCount = 1; // table.getColumnCount();
        if (attributeName != null) {
            columnsCount++;
        }
        for (int i = 0; i < columnsCount; i++) {
            /*TableColumn tc = table.getColumn(i);
            
             if (table.isStringColumn(i)) {
             recordSize += STRING_LENGTH;
             } else if (table.isDoubleColumn(i)) {
             recordSize += NUMBER_LENGTH;
             } else {
             throw new IOException("DBF export: unsupported type");
             }*/
            recordSize += NUMBER_LENGTH;
        }
        return recordSize;
    }

    private void writeString(LittleEndianOutputStream dos, String str, int length)
            throws IOException {
        byte[] b = str.getBytes("ISO-8859-1");
        dos.write(b, 0, Math.min(length, b.length));
        for (int c = b.length; c < length; c++) {
            dos.write(0);
        }
    }

    private void writeFieldDescriptors(LittleEndianOutputStream dos, String attributeName)
            throws IOException {

        int columnsCount = 1; // table.getColumnCount();
        if (attributeName != null) {
            columnsCount++;
        }
        for (int i = 0; i < columnsCount; i++) {
            //TableColumn tc = table.getColumn(i);

            // write column title, 10 chars, plus terminating 0.
            String title = "attr" + i; // tc.getHeaderValue().toString().trim();
            this.writeString(dos, title, 10);
            dos.write(0x0);

            // write field type
            /*if (table.isStringColumn(i)) {
             dos.write('C');
             dos.writeInt(0);            // field address (ignored)
             dos.write(STRING_LENGTH);   // field length
             dos.write(0);               // decimal count not used
             } else if (table.isDoubleColumn(i)) {*/
            dos.write('F');
            dos.writeInt(0);            // field address (ignored)
            dos.write(NUMBER_LENGTH);   // field length
            dos.write(NUMBER_DECIMALS); // decimal count
           /* } else {
             throw new IOException("DBF export: unsupported type");
             }*/

            // 14 reserved or unusued bytes
            for (int c = 0; c < 14; c++) {
                dos.write(0);
            }
        }
    }

    /**
     * Convert a double value to a string with a specified width, number of
     * digits after the decimal point, and preceeding white spaces.
     *
     * @param value The value to convert.
     * @param width The total width of the string (= the number of characters).
     * @param decimals The number of digits after the decimal point.
     * @param leadingSpaces The number of leading spaces. Usually 0.
     * @return The value converted to a string.
     */
    public static String format(double value, int width, int decimals, int leadingSpaces) {
        StringBuilder str = new StringBuilder(width);
        width += 1 + leadingSpaces;
        DecimalFormat formatter = new DecimalFormat();
        formatter.setMaximumFractionDigits(decimals);
        formatter.setMinimumFractionDigits(decimals);
        String s = formatter.format(value); // format the number
        int padding = Math.max(leadingSpaces, width - s.length()); // insert leadingSpaces
        for (int k = 0; k < padding; k++) {
            str.append(' ');
        }
        str.append(s);
        return str.toString();
    }

    private void writeRecords(LittleEndianOutputStream dos, Geometry[] geometries, String attributeName)
            throws IOException {

        int rowsCount = geometries.length; // table.getRowCount();
        for (int row = 0; row < rowsCount; row++) {
            // write deleted flag
            dos.write(' ');

            String nbrStr = format(row, NUMBER_LENGTH, NUMBER_DECIMALS, 0);
            this.writeString(dos, nbrStr, NUMBER_LENGTH);
            
            Number attr = geometries[row].getAttribute(attributeName);
            if (attr != null) {
                nbrStr = format(attr.doubleValue(), NUMBER_LENGTH, NUMBER_DECIMALS, 0);
            } else {
                nbrStr = format(0, NUMBER_LENGTH, NUMBER_DECIMALS, 0);
            }
            this.writeString(dos, nbrStr, NUMBER_LENGTH);
        }
    }
}
