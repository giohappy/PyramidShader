/*
 * Table.java
 *
 * Created on February 15, 2006, 9:33 PM
 *
 */

package edu.oregonstate.cartography.geometryimport;

import java.io.*;
import java.util.*;
import javax.swing.event.TableModelListener;
import javax.swing.table.*;

/**
 *
 * @author jenny
 */
public class Table extends DefaultTableModel 
        implements java.io.Serializable {
    
    private String name;
    private final TableColumnModel tableColumnModel;
    private ArrayList geometries;
    
    /**
     * Store the name of the character set used by the source. Strings are 
     * stored in this table as standard UTF Java strings. The name of the 
     * source character set is used when the encoding must be changed at a later date.
     */
    private String sourceCharsetName;
    
    public Table(String sourceCharsetName) {
        this.name = "unnamed";
        this.tableColumnModel = new DefaultTableColumnModel();
        this.geometries = new ArrayList(1);
        this.sourceCharsetName = sourceCharsetName;
    }
    
    /**
     * Serializable.
     */
    private void writeObject(ObjectOutputStream outputStream) throws IOException {
        
        // don't serialize the attached TableModelModelListeners. Serializing
        // them is not possible. Temporarily remove them, serialize, and add 
        // them again.
        TableModelListener[] tml = this.getTableModelListeners();
        for(int i=0;i<tml.length;++i)
            this.removeTableModelListener(tml[i]);
        outputStream.defaultWriteObject();
        for(int i=0;i<tml.length;++i) 
            this.addTableModelListener(tml[i]);
    } 
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public TableColumn getColumn(int id) {
        return this.tableColumnModel.getColumn(id);
    }
    
    public int getNbrColumns() {
        return this.tableColumnModel.getColumnCount();
    }
    
    public double[] getMinMax (int colID) {
        return this.getMinMax(colID, false, false);
    }
    
    public double[] getMinMax (int colID, boolean absolute, boolean notZero) {
        try {
            double min = Double.MAX_VALUE;
            double max = -Double.MAX_VALUE;
            
            final int nRows = this.getRowCount();
            for (int i = 0; i < nRows; i++) {
                double d = ((Double)this.getValueAt(i, colID)).doubleValue();
                if (absolute)
                    d = Math.abs(d);
                if (d == 0. && notZero)
                    continue;
                if (d < min)
                    min = d;
                if (d > max)
                    max = d;
            }
            return new double[] {min, max};
        } catch (Exception exc) {
            return null;
        }
    }
    
    @Override
    public String toString() {
        return this.getName();
    }
    
    public void addColumn(String name) {
        super.addColumn(name);
        TableColumn tableColumn = new DisplayableTableColumn(this.getNbrColumns());
        tableColumn.setHeaderValue(name);
        this.tableColumnModel.addColumn(tableColumn);
    }

    public ArrayList getGeometries() {
        return geometries;
    }

    public void setGeometries(ArrayList geometries) {
        this.geometries = geometries;
    }
    
    public int findRowWithValue (Object value, int columnID) {
        final int nbrRows = this.getRowCount();
        for (int i = 0; i < nbrRows; i++) {
            Object tableValue = this.getValueAt(i, columnID);
            if (value.equals(tableValue))
                return i;
        }
        return -1;
    }
    
    /**
     * Returns the Class object of the first cell in specified column in the 
     * table model. Unless this method is overridden, all values are assumed 
     * to be of the returned Class.
     * @param columnIndex The id of the column.
     * @return A Class object. All values in this column are of this Class.
     */
    public Class getColumnClass(int columnIndex) {
        Object o = getValueAt(0, columnIndex);
        if (o == null) {
            return Object.class;
        } else {
            return o.getClass();
        }
    }
    
    public boolean isStringColumn (int columnIndex) {
            Class cl = this.getColumnClass(columnIndex);
            return (String.class.isAssignableFrom(cl));
    }
    
    public boolean isDoubleColumn (int columnIndex) {
            Class cl = this.getColumnClass(columnIndex);
            return (Double.class.isAssignableFrom(cl));
    }
    
    /**
     * Returns the name of the characters set that was used to encode the source
     * data of this table.
     * @return A name, e.g. MacRoman or UTF-16.
     */
    public String getEncodingName() {
        return this.sourceCharsetName;
    }
    
    /**
     * Change the character encoding of all Strings stored in this table.
     * This could be optimized for speed!
     * @param charsetName The name of the new encoding. This name will be stored
     * in this object. E.g. MacRoman or UTF-16.
     * @throws java.io.UnsupportedEncodingException
     */
    public void changeEncoding(String charsetName) 
    throws UnsupportedEncodingException{
        
        // if the source encoding is not known, assume the default encoding
        // of this VM was used.
        if (this.sourceCharsetName == null)
            this.sourceCharsetName = System.getProperty("file.encoding");
        
        final int nCols = this.getNbrColumns();
        final int nRows = this.getRowCount();
        
        for (int c = 0; c < nCols; c++) {
            Class cl = this.getColumnClass(c);
            
            // search for columns of Strings
            if (String.class.isAssignableFrom(cl) == false) {
                continue;
            }
            
            // change each string back to the original encoding, and then
            // forward to the new encoding
            for (int r = 0; r < nRows; r++) {
                String str = (String) this.getValueAt(r, c);
                byte[] bytes = str.getBytes(this.sourceCharsetName);
                String newStr = new String(bytes, charsetName);
                this.setValueAt(newStr, r, c);
            }
        }
        
        // store the name of the character set encoding
        this.sourceCharsetName = charsetName;
    }
}
