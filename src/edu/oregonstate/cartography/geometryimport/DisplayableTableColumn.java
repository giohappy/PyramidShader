/*
 * DisplayableTableColumn.java
 *
 * Created on June 15, 2006, 5:34 PM
 *
 */

package edu.oregonstate.cartography.geometryimport;

import javax.swing.table.TableColumn;
/**
 * A TableColumn that overwrites toString to return the header value.
 * @author jenny
 */
public class DisplayableTableColumn extends TableColumn {
    
    /**
     * Creates a new instance of DisplayableTableColumn
     * @param modelIndex
     */
    public DisplayableTableColumn(int modelIndex) {
        super(modelIndex);
    }
       
    @Override
    public String toString() {
        return this.getHeaderValue().toString();
    }
}
