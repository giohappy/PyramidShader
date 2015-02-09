/*
 * GridOperator.java
 *
 * Created on January 28, 2006, 2:10 PM
 *
 */

package edu.oregonstate.cartography.grid.operators;

import edu.oregonstate.cartography.grid.Grid;


/**
 * A GridOperator derives a new grid from an existing grid.
 * @author Bernhard Jenny, Institute of Cartography, ETH Zurich.
 */
public interface GridOperator {
    
    /** Returns a descriptive name of this GridOperator
     * @return The name of this GridOperator.
     */
    public String getName();
    
    /**
     * Start operating on the passed GeoGrid.
     * @param grid The grid to operate on.
     * @return A new grid containing the result. The resulting grid may
     * be of a different size than the passed grid.
     */
    public Grid operate (Grid grid);
    
}
