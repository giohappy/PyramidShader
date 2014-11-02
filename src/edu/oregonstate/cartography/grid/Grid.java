/*
 * Grid.java
 *
 * Created on November 21, 2006, 4:28 PM
 *
 */
package edu.oregonstate.cartography.grid;

import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;

/**
 * The Grid class models regularly spaced values, for example a digital
 * elevation model.
 *
 * @author Bernhard Jenny, Institute of Cartography, ETH Zurich.
 */
public final class Grid {

    /**
     * The size between two neighboring columns or rows.
     */
    private double cellSize;
    /**
     * The grid values.
     */
    private float[][] grid;

    /**
     * horizontal coordinate of west border
     */
    private double west = 0;

    /**
     * vertical coordinate of south border
     */
    private double south = 0;

    /**
     * Copy constructor
     *
     * @param template
     */
    public Grid(Grid template) {
        this(template.getCols(), template.getRows(), template.getCellSize());
        west = template.getWest();
        south = template.getSouth();

        // deep clone grid array
        int nRows = template.getRows();
        int nCols = template.getCols();
        for (int row = 0; row < nRows; row++) {
            System.arraycopy(template.grid[row], 0, grid[row], 0, nCols);
        }
    }

    /**
     * Creates a new instance of Grid.
     *
     * @param cols The number of vertical columns in the new grid.
     * @param rows The number of horizontal rows in the new grid.
     */
    public Grid(int cols, int rows) {
        // the grid must contain at least 2 x 2 cells.
        if (cols < 3 || rows < 3) {
            throw new IllegalArgumentException("Not enough data points.");
        }

        this.cellSize = 1;
        this.grid = new float[rows][cols];
    }

    /**
     * Creates a new instance of Grid.
     *
     * @param cols The number of vertical columns in the new grid.
     * @param rows The number of horizontal rows in the new grid.
     * @param cellSize The size between two rows or columns.
     */
    public Grid(int cols, int rows, double cellSize) {
        this(cols, rows);

        // the grid must contain at least 2 x 2 cells.
        if (cols < 3 || rows < 3) {
            throw new IllegalArgumentException("Not enough data points.");
        }

        if (cellSize <= 0) {
            throw new IllegalArgumentException("Negative cell size");
        }
        this.cellSize = cellSize;
    }

    /**
     * Returns the value at a specific position in the grid.
     *
     * @param col The vertical column for which a value is returned.
     * @param row The horizontal row for which a value is returned.
     * @return The value at the specified position.
     */
    public final float getValue(int col, int row) {
        return grid[row][col];
    }

    /**
     * Sets a value in the grid.
     *
     * @param value The value to store in the grid.
     * @param col The vertical column for which a value is set.
     * @param row The horizontal row for which a value is set.
     */
    public final void setValue(float value, int col, int row) {
        grid[row][col] = value;
    }

    /**
     * Sets a value in the grid.
     *
     * @param value The value to store in the grid. The value is cast to a
     * float.
     * @param col The vertical column for which a value must be set.
     * @param row The horizontal row for which a value must be set.
     */
    public void setValue(double value, int col, int row) {
        grid[row][col] = (float) value;
    }

    private float verticalBilinearInterpol(double y, int topRow, int col) {
        // top value
        float topH = grid[topRow][col];
        // bottom value
        float bottomH = grid[topRow + 1][col];
        double w = (getNorth() - y) / cellSize;
        return (float) (w * (topH - bottomH) + bottomH);
    }

    /**
     * Bilinear interpolation. See
     * http://www.geovista.psu.edu/sites/geocomp99/Gc99/082/gc_082.htm "What's
     * the point? Interpolation and extrapolation with a regular grid DEM"
     *
     * @param x horizontal coordinate
     * @param y vertical coordinate
     * @return interpolated value
     */
    public final float getBilinearInterpol(double x, double y) {

        float h1, h2, h3, h4;
        final int rows = grid.length;
        final int cols = grid[0].length;
        final double north = south + (rows - 1) * cellSize;

        final double dx = (x - west) / cellSize;

        // column and row of the top left corner
        final int col = (int) (dx);
        final int row = (int) ((north - y) / cellSize);

        if (col < 0 || col > cols - 1 || row < 0 || row > rows - 1) {
            return Float.NaN;
        }
        
        // point on right border
        if (col == cols - 1) {
            // point on lower right corner
            if (row == rows - 1) {
                return grid[rows - 1][cols - 1];
            }
            return verticalBilinearInterpol(y, row, col);
        }
        
        // point on lower border
        if (row == rows - 1) {
            float leftH = grid[row][col];
            float rightH = grid[row][col + 1];
            return (float)(dx * (rightH - leftH) + leftH);
        }
        
        final double relX = dx - col;
        final double relY = (y - south) / cellSize - rows + row + 2;

        // value at bottom left corner
        h1 = grid[row + 1][col];
        // value at bottom right corner
        h2 = grid[row + 1][col + 1];

        // value at top left corner
        h3 = grid[row][col];

        // value at top right corner
        h4 = grid[row][col + 1];

        // assume all values are valid
        return (float) (h1 + (h2 - h1) * relX + (h3 - h1) * relY + (h1 - h2 - h3 + h4) * relX * relY);
    }

    /**
     * Returns the minimum and the maximum value in the grid.
     *
     * @return Returns an array with two elements. The first element is the
     * minimum value in the grid, the second value is the maximum value in the
     * grid.
     */
    public float[] getMinMax() {
        int cols = this.getCols();
        int rows = this.getRows();
        float min = Float.MAX_VALUE;
        float max = -Float.MAX_VALUE;
        for (int r = 0; r < rows; ++r) {
            for (int c = 0; c < cols; ++c) {
                if (grid[r][c] < min) {
                    min = grid[r][c];
                }
                if (grid[r][c] > max) {
                    max = grid[r][c];
                }
            }
        }
        return new float[]{min, max};
    }

    /**
     * Returns the number of columns in the grid.
     *
     * @return The number of columns in the grid.
     */
    public int getCols() {
        return grid[0].length;
    }

    /**
     * Returns the number of rows in the grid.
     *
     * @return The number of rows in the grid.
     */
    public int getRows() {
        return grid.length;
    }

    /**
     * Returns the distance between two neighboring rows or columns-
     *
     * @return The distance between two rows or columns.
     */
    public double getCellSize() {
        return cellSize;
    }

    public float[][] getGrid() {
        return grid;
    }

    /**
     * Returns true if the passed grid has the same number of columns and rows
     * and has the same cell size.
     *
     * @param grid
     * @return
     */
    public boolean isIdenticalInSize(Grid grid) {
        if (grid == null) {
            return false;
        }
        return getCols() == grid.getCols()
                && getRows() == grid.getRows()
                && getCellSize() == grid.getCellSize();

    }

    public double getAspect(int col, int row) {
        final double w = getValue(col - 1, row);
        final double e = getValue(col + 1, row);
        final double s = getValue(col, row + 1);
        final double n = getValue(col, row - 1);
        return Math.atan2(n - s, e - w);
    }

    /**
     * Returns aspect angle in radians for a provided position.
     *
     * @param x
     * @param y
     * @param samplingDist
     * @return Angle in radians in counter-clockwise direction. East is 0.
     */
    public double getAspect(double x, double y, double samplingDist) {
        final double w = getBilinearInterpol(x - samplingDist, y);
        final double e = getBilinearInterpol(x + samplingDist, y);
        final double s = getBilinearInterpol(x, y - samplingDist);
        final double n = getBilinearInterpol(x, y + samplingDist);
        return Math.atan2(n - s, e - w);
    }

    /**
     * Returns the slope for col in [1, cols-2] and row in [1, rows - 2]
     * http://help.arcgis.com/en/arcgisdesktop/10.0/help../index.html#/How_Slope_works/009z000000vz000000/
     *
     * @param col
     * @param row
     * @return
     */
    public double getSlopeInsideGrid(int col, int row) {
        double a = getValue(col - 1, row - 1);
        double b = getValue(col, row - 1);
        double c = getValue(col + 1, row - 1);
        double d = getValue(col - 1, row);

        double f = getValue(col + 1, row);
        double g = getValue(col - 1, row + 1);
        double h = getValue(col, row + 1);
        double i = getValue(col + 1, row + 1);

        //parameters used in the slope calculation
        double dZdX = ((c + (2 * f) + i) - (a + (2 * d) + g)) / (8 * cellSize);
        double dZdY = ((g + (2 * h) + i) - (a + (2 * b) + c)) / (8 * cellSize);
        return Math.sqrt((dZdX * dZdX) + (dZdY * dZdY));
    }

    // http://help.arcgis.com/en/arcgisdesktop/10.0/help../index.html#/How_Slope_works/009z000000vz000000/
    public double getSlope(int col, int row) {
        final double a, b, c, d, f, g, h, i;
        final int cols = grid[0].length;
        final int rows = grid.length;

        final int colLeft = col > 0 ? col - 1 : 0;
        final int colRight = col < cols - 1 ? col + 1 : cols - 1;
        final int rowTop = row > 0 ? row - 1 : 0;
        final int rowBottom = row < rows - 1 ? row + 1 : rows - 1;

        a = getValue(colLeft, rowTop);
        b = getValue(col, rowTop);
        c = getValue(colRight, rowTop);
        d = getValue(colLeft, row);

        f = getValue(colRight, row);
        g = getValue(colLeft, rowBottom);
        h = getValue(col, rowBottom);
        i = getValue(colRight, rowBottom);

        //parameters used in the slope calculation
        double dZdX = ((c + (2 * f) + i) - (a + (2 * d) + g)) / (8 * cellSize);
        double dZdY = ((g + (2 * h) + i) - (a + (2 * b) + c)) / (8 * cellSize);
        return Math.sqrt((dZdX * dZdX) + (dZdY * dZdY));
    }

    @Override
    public String toString() {
        float[] minMax = getMinMax();
        return "Grid: rows:" + getRows() + " cols:" + getCols() + " range:" + minMax[0] + " to " + minMax[1];
    }

    /**
     * Returns a descriptive text for GUI display.
     *
     * @param newLine The line separator. Could be \n, <br> or null.
     * @return
     */
    public String getDescription(String newLine) {
        if (newLine == null) {
            newLine = System.getProperty("line.separator");
        }
        DecimalFormat f = new DecimalFormat(cellSize < 1 ? "#,##0.0#####" : "#,##0.0");
        DecimalFormat intFormat = new DecimalFormat("#,###");
        StringBuilder sb = new StringBuilder();
        sb.append("Dimension: ");
        sb.append(intFormat.format(getCols()));
        sb.append("\u2006\u00D7\u2006"); // multiplication sign surrounded by small spaces
        sb.append(intFormat.format(getRows()));
        sb.append(newLine);
        sb.append("Cell size: ");
        sb.append(f.format(cellSize));
        sb.append(newLine);
        sb.append("West: ");
        sb.append(f.format(getWest()));
        sb.append(newLine);
        sb.append("East: ");
        sb.append(f.format(getWest() + (getCols() - 1) * getCellSize()));
        sb.append(newLine);
        sb.append("South: ");
        sb.append(f.format(getSouth()));
        sb.append(newLine);
        sb.append("North: ");
        sb.append(f.format(getSouth() + (getRows() - 1) * getCellSize()));
        return sb.toString();
    }

    /**
     * Returns a descriptive text for GUI display, including min and max values.
     *
     * @param newLine The line separator. Could be \n, <br> or null.
     * @return
     */
    public String getDescriptionWithStatistics(String newLine) {
        if (newLine == null) {
            newLine = System.getProperty("line.separator");
        }
        DecimalFormat f = new DecimalFormat("#,##0.######");
        StringBuilder sb = new StringBuilder(this.getDescription(newLine));
        float[] minMax = getMinMax();
        sb.append(newLine);
        sb.append("Minimum value: ");
        sb.append(f.format(minMax[0]));
        sb.append(newLine);
        sb.append("Maximum value: ");
        sb.append(f.format(minMax[1]));
        return sb.toString();
    }

    /**
     * @return the west
     */
    public double getWest() {
        return west;
    }

    /**
     * @param west the west to set
     */
    public void setWest(double west) {
        this.west = west;
    }

    /**
     * @return the south
     */
    public double getSouth() {
        return south;
    }

    /**
     * @param south the south to set
     */
    public void setSouth(double south) {
        this.south = south;
    }

    /**
     * The northern border of this grid
     *
     * @return
     */
    public double getNorth() {
        return getSouth() + (getRows() - 1) * getCellSize();
    }

    /**
     * The eastern border of the grid
     *
     * @return
     */
    public double getEast() {
        return getWest() + (getCols() - 1) * getCellSize();
    }

    /**
     * Returns a bounding box for this grid.
     * @return The bounding box.
     */
    public Rectangle2D getBoundingBox() {
        return new Rectangle2D.Double(getWest(), getSouth(), 
                getEast() - getWest(), getNorth() - getSouth());
    }
    
    /**
     * Returns true if the grid has non-zero dimensions and non-NaN position.
     *
     * @return
     */
    public boolean isWellFormed() {
        return getCols() > 0
                && getRows() > 0
                && getCellSize() > 0
                && !Double.isNaN(getWest())
                && !Double.isNaN(getNorth());
    }

}
