package edu.oregonstate.cartography.grid;

import static edu.oregonstate.cartography.grid.Model.ForegroundVisualization.ILLUMINATED_CONTOURS;
import edu.oregonstate.cartography.grid.operators.ColorizerOperator;
import edu.oregonstate.cartography.grid.operators.ColorizerOperator.ColorVisualization;
import edu.oregonstate.cartography.grid.operators.GridAddOperator;
import edu.oregonstate.cartography.grid.operators.GridCopyOperator;
import edu.oregonstate.cartography.grid.operators.GridMaskOperator;
import edu.oregonstate.cartography.grid.operators.GridScaleOperator;
import edu.oregonstate.cartography.grid.operators.GridScaleToRangeOperator;
import edu.oregonstate.cartography.grid.operators.GridSlopeOperator;
import edu.oregonstate.cartography.grid.operators.GridVoidOperator;
import edu.oregonstate.cartography.grid.operators.IlluminatedContoursOperator;
import edu.oregonstate.cartography.grid.operators.PlanObliqueOperator;
import edu.oregonstate.cartography.gui.bivariate.BivariateColorRenderer;
import edu.oregonstate.cartography.gui.ProgressIndicator;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

/**
 *
 * @author Bernhard Jenny, Cartography and Geovisualization Group, Oregon State
 * University
 */
public class Model implements Cloneable {

    public class ColorRamp {

        /**
         *
         * @param name
         * @param colors
         * @param colorPositions
         */
        public ColorRamp(String name, Color[] colors, float[] colorPositions) {
            this.name = name;
            this.colors = colors;
            this.colorPositions = colorPositions;
        }

        /**
         * Name of color ramp
         */
        public String name;

        /**
         * color definitions
         */
        public Color[] colors;

        /**
         * relative positions between 0 and 1
         */
        public float[] colorPositions;
    }

    public final ArrayList<ColorRamp> predefinedColorRamps;

    public enum ForegroundVisualization {

        NONE, ILLUMINATED_CONTOURS, SHADED_CONTOURS
    }

    /**
     * original grid
     */
    private Grid grid;

    /**
     * minimum and maximum value of original grid
     */
    private float[] gridMinMax;

    /**
     * Laplacian pyramid of original grid
     */
    public LaplacianPyramid laplacianPyramid;

    /**
     * modified grid composed of summed pyramids
     */
    private Grid generalizedGrid;

    /**
     * slope values of generalized grid.
     */
    private Grid generalizedSlopeGrid;

    /**
     * the number of levels of the Laplacian pyramid that are filtered
     */
    public int generalizationMaxLevels = 2;

    /**
     * amount of filtering applied to generalizationMaxLevels pyramid levels.
     * Between -1 and +1. With -1 the weight for all generalizationMaxLevels is
     * 1 (hence no generalization). With +1 the weigh for all
     * generalizationMaxLevels is 0.
     */
    private double generalizationDetails = -1;

    /**
     * illumination azimuth
     */
    public int azimuth = 315;

    /**
     * illumination zenith
     */
    public int zenith = 45;

    /**
     * ambient illumination component. Usually between -0.5 and +0.5
     */
    public double ambientLight = 0;

    /**
     * vertical exaggeration applied when shading
     */
    public float shadingVerticalExaggeration = 1;

    /**
     * terrain coloring in the background (shading, hypsometric tints, etc)
     */
    public ColorVisualization backgroundVisualization = ColorVisualization.GRAY_SHADING;

    /**
     * terrain visualization in the foreground (contours)
     */
    public ForegroundVisualization foregroundVisualization = ForegroundVisualization.NONE;

    /**
     * color definitions with relative elevation values
     */
    public ColorRamp colorRamp;

    /**
     * color applied when the entire background is filled with a single color
     */
    public Color solidColor = Color.LIGHT_GRAY;

    /**
     * color for illuminated contour lines. Default is white
     */
    public int contoursIlluminatedColor = 0x00FFFFFF;

    /**
     * color for shaded contour lines. Default is black.
     */
    public int contoursShadowedColor = 0x00000000;

    /**
     * contour interval
     */
    public double contoursInterval = 200;

    /**
     * line width of illuminated contours (relative to cell size) at lowest
     * elevation
     */
    public double contoursIlluminatedWidthLow = 1;

    /**
     * line width of illuminated contours (relative to cell size) at highest
     * elevation
     */
    public double contoursIlluminatedWidthHigh = 1;

    /**
     * line width of shaded contours (relative to cell size) at lowest elevation
     */
    public double contoursShadowWidthLow = 1;

    /**
     * line width of shaded contours (relative to cell size) at highest
     * elevation
     */
    public double contoursShadowWidthHigh = 1;

    /**
     * contour line widths are never smaller than this value (relative to cell
     * size)
     */
    public double contoursMinWidth = 0.2;

    /**
     * minimum distance between contour lines (relative to cell size)
     */
    public double contoursMinDist = 0;

    /**
     * contour gray values are smoothly interpolated between illuminated and
     * shaded slope. This angle defines the range of interpolation.
     */
    public int contoursGradientAngle = 0;

    /**
     * standard deviation of Gaussian blur filter to despeckle contour lines
     */
    public double contoursAspectGaussBlur;

    /**
     * transition angle between illuminated and shaded contour lines
     */
    public int contoursTransitionAngle = 90;

    /**
     * inclination angle for plan oblique relief orthogonal is 90 degrees
     */
    public int planObliqueAngle = 90;

    /**
     * localGridModel encapsulates the settings and cashed intermediate results
     * for computing a locally filtered grid for local hypsometric tinting.
     */
    private final LocalGridModel localGridModel = new LocalGridModel();

    /**
     * Contains references to 2 grids for creating a bivariate color scheme.
     */
    protected final BivariateColorRenderer bivariateColorRender = new BivariateColorRenderer();

    public Model() {
        predefinedColorRamps = new ArrayList<>();

        float[] pos = new float[]{0.0F, 1.0F};
        Color[] col = new Color[]{
            Color.BLACK,
            Color.WHITE};
        predefinedColorRamps.add(new ColorRamp("Black-White", col, pos));

        col = new Color[]{
            Color.GRAY,
            Color.WHITE};
        predefinedColorRamps.add(new ColorRamp("Soft Gray", col, pos));

        pos = new float[]{0.5F, 1.0F};
        col = new Color[]{
            Color.BLACK,
            Color.WHITE};
        predefinedColorRamps.add(new ColorRamp("Hard Gray", col, pos));

        pos = new float[]{0.0F, 0.56F, 0.81F, 0.93F, 1.0F};
        col = new Color[]{
            Color.decode("#6d7ea1"),
            Color.decode("#97a3ba"),
            Color.decode("#bcbcbc"),
            Color.decode("#dedace"),
            Color.decode("#e8e8e8")};
        predefinedColorRamps.add(new ColorRamp("Natural Light (Exposition)", col, pos));

        pos = new float[]{0.0F, 0.42F, 0.73F, 0.88F, 1.0F};
        col = new Color[]{
            Color.decode("#526b75"),
            Color.decode("#6a8e82"),
            Color.decode("#a6b4a9"),
            Color.decode("#e2d4ac"),
            Color.decode("#f7f3b1")};
        predefinedColorRamps.add(new ColorRamp("Swiss Style (Exposition)", col, pos));

        pos = new float[]{0, 0.08f, 0.24f, 0.43f, 0.69f, 0.89f};
        col = new Color[]{
            new Color(120, 181, 141),
            new Color(124, 172, 104),
            new Color(190, 194, 107),
            new Color(212, 218, 170),
            new Color(225, 246, 244),
            new Color(255, 255, 255)
        };
        predefinedColorRamps.add(new ColorRamp("Hypsometric", col, pos));

        colorRamp = predefinedColorRamps.get(0);
    }

    /**
     * Use one of the named color ramps. If an invalid name is passed, the color
     * ramp does not change.
     *
     * @param name The name of the ColorRamp to use.
     */
    public void selectColorRamp(String name) {
        for (ColorRamp cr : predefinedColorRamps) {
            if (cr.name.equals(name)) {
                colorRamp = cr;
                break;
            }
        }
    }

    /**
     * Computes the weight for one level of the Laplacian pyramid.
     *
     * @param pyramidLevel the pyramid level. The level with the highest
     * frequencies has a value of 0.
     * @return the weight for that pyramid level between 0 and 1
     */
    private float getPyramidLevelWeight(int pyramidLevel) {
        if (pyramidLevel >= generalizationMaxLevels || generalizationMaxLevels <= 0) {
            return 1;
        }

        // return (float) (1 / Math.pow(base, maxLevels - pyramidLevel));
        // simplified:
        // return (float) (Math.pow(base, pyramidLevel - maxLevels));
        if (generalizationDetails == 1d) {
            return 0;
        }

        double m, c;
        if (generalizationDetails > 0) {
            // a line of the form y = mx + c
            // the line is crossing the positive horizontal x axis 
            // at generalizationDetails * generalizationMaxLevels
            m = 1 / (generalizationMaxLevels * (1 - generalizationDetails));
            c = generalizationDetails / (generalizationDetails - 1);
        } else {
            // a line of the form y = mx + c
            // the line is crossing the positive vertical y axis at c = -b
            c = -generalizationDetails;
            m = (1 + generalizationDetails) / generalizationMaxLevels;
        }
        double w = m * pyramidLevel + c;

        // clamp weight to [0..1]
        return (float) Math.min(Math.max(0d, w), 1d);
    }

    /**
     * re-computes generalized grid. Call this method whenever the
     * generalization parameters or the Laplacian pyramid have changed.
     */
    public void updateGeneralizedGrid() {
        if (laplacianPyramid == null) {
            return;
        }

        //long start = System.nanoTime();
        if (isGeneralizing()) {
            // compute weights for summing levels in Laplacian pyramid
            float[] w = new float[laplacianPyramid.getLevels().length];
            for (int i = 0; i < w.length; i++) {
                w[i] = getPyramidLevelWeight(i);
            }

            // sum the Laplacian pyramids
            generalizedGrid = laplacianPyramid.sumLevels(w, true);

            // copy NaN values from original grid
            new GridMaskOperator().operate(grid, generalizedGrid);

            // scale the minimum and maximum values of the output generalized grid to 
            // the same range as the input grid.
            new GridScaleToRangeOperator(gridMinMax).operate(generalizedGrid, generalizedGrid);
        } else {
            generalizedGrid = new GridCopyOperator().operate(grid);
        }
        generalizedSlopeGrid = new GridSlopeOperator().operate(generalizedGrid);
        //System.out.println((System.nanoTime() - start) / 1000 / 1000 + "ms");
    }

    /**
     * Creates a new BufferedImage
     *
     * @param scale Scale factor by which the created image will be larger than
     * the generalized grid.
     * @return A new image or null
     */
    public BufferedImage createDestinationImage(int scale) {
        if (backgroundVisualization == ColorVisualization.BIVARIATE) {
            Grid grid1 = bivariateColorRender.getAttribute1Grid();
            if (grid1 != null) {
                return new BufferedImage(grid1.getCols(), grid1.getRows(), BufferedImage.TYPE_INT_ARGB);
            }
        }

        if (generalizedGrid == null) {
            return null;
        }

        //Get the number of columns and rows in the DEM
        int cols = generalizedGrid.getCols() * scale;
        int rows = generalizedGrid.getRows() * scale;

        return new BufferedImage(cols, rows, BufferedImage.TYPE_INT_ARGB);
    }

    /**
     * Render background image, such as shading or hypsometric tinting.
     *
     * @param destinationImage The background image will be rendered to this
     * image.
     * @param progressIndicator
     * @return
     */
    public BufferedImage renderBackgroundImage(BufferedImage destinationImage,
            ProgressIndicator progressIndicator) {
        if (destinationImage == null) {
            return null;
        }

        // background visualization
        if (backgroundVisualization == ColorVisualization.CONTINUOUS) {
            // fill image with single color
            Graphics2D graphics = (Graphics2D) destinationImage.getGraphics();
            graphics.setColor(solidColor);
            graphics.fillRect(0, 0, destinationImage.getWidth(), destinationImage.getHeight());
            graphics.dispose();
        } else {
            Grid planObliqueGeneralizedGrid = generalizedGrid;
            if (generalizedGrid != null) {
                PlanObliqueOperator planObliqueOp = new PlanObliqueOperator(planObliqueAngle, gridMinMax[0]);
                if (planObliqueAngle != 90) {
                    planObliqueGeneralizedGrid = planObliqueOp.operate(generalizedGrid);
                }
            }

            // coloring and shading
            ColorizerOperator colorizer = new ColorizerOperator(backgroundVisualization,
                    bivariateColorRender, progressIndicator);
            colorizer.setColors(colorRamp.colors, colorRamp.colorPositions);

            final Grid g;
            final float min;
            final float max;
            if (backgroundVisualization == ColorVisualization.BIVARIATE) {
                g = getBivariateColorRender().getAttribute1Grid();
                float[] minMax = getBivariateColorRender().getAttribute1MinMax();
                if (minMax == null) {
                    return null;
                }
                min = minMax[0];
                max = minMax[1];
            } else {
                min = gridMinMax[0];
                max = gridMinMax[1];
                if (backgroundVisualization.isLocal()) {
                    g = localGridModel.getFilteredGrid();
                } else {
                    g = planObliqueGeneralizedGrid;
                }
            }
            colorizer.operate(g, destinationImage,
                    min, max,
                    azimuth, zenith, ambientLight, shadingVerticalExaggeration);
        }
        return destinationImage;
    }

    /**
     * Render foreground visualization: illuminated contours
     *
     * @param destinationImage The foreground image will be rendered to this
     * image.
     * @param progressIndicator
     * @return
     */
    public BufferedImage renderForegroundImage(BufferedImage destinationImage,
            ProgressIndicator progressIndicator) {
        if (isRenderingForeground()) {
            boolean illuminated = (foregroundVisualization == ILLUMINATED_CONTOURS);
            IlluminatedContoursOperator op = setupIlluminatedContoursOperator(illuminated);
            op.renderToImage(destinationImage, generalizedGrid,
                    generalizedSlopeGrid, progressIndicator);
        }
        return destinationImage;
    }

    /**
     * Set the elevation grid.
     *
     * @param grid The new grid.
     */
    public void setGrid(Grid grid) {
        this.grid = grid;

        // find minimum and maximum values in grid
        gridMinMax = grid.getMinMax();

        // create a Gaussian pyramids
        GaussianPyramid gaussianPyramid = new GaussianPyramid(grid);

        // create the Laplacian pyramid 
        laplacianPyramid = new LaplacianPyramid();
        laplacianPyramid.createPyramid(gaussianPyramid.getPyramid());

        updateGeneralizedGrid();

        localGridModel.setGrid(generalizedGrid, gridMinMax, laplacianPyramid);
    }

    /**
     * Returns the original ungeneralized grid.
     *
     * @return The ungeneralized grid.
     */
    public Grid getGrid() {
        return grid;
    }

    /**
     * Returns the generalized grid.
     *
     * @return the generalizedGrid
     */
    public Grid getGeneralizedGrid() {
        return generalizedGrid;
    }

    /**
     * Returns the width and height of the un-scaled rendered image.
     * @return 
     */
    public Dimension getGridDimensionForDisplay() {
        final Grid g;
        if (backgroundVisualization == ColorVisualization.BIVARIATE
                && bivariateColorRender.hasGrids()) {
            g = bivariateColorRender.getAttribute1Grid();
        } else {
            g = grid;
        }
        return g == null ? null : new Dimension(g.getCols(), g.getRows());
    }
    
    /**
     * Returns the locally filtered grid.
     *
     * @return the locally filtered grid.
     */
    public Grid getLocalGrid() {
        return localGridModel.getFilteredGrid();
    }

    /**
     * Returns a grid with slope values for the generalized grid.
     *
     * @return The generalized grid.
     */
    public Grid getGeneralizedSlopeGrid() {
        return generalizedSlopeGrid;
    }

    /**
     * Initializes an illuminatedIlluminatedContoursOperator with the current
     * model settings.
     *
     * @param illuminated If true illuminated contours are created, otherwise
     * shaded contours are created.
     * @return
     */
    public IlluminatedContoursOperator setupIlluminatedContoursOperator(
            boolean illuminated) {
        return new IlluminatedContoursOperator(
                illuminated,
                contoursIlluminatedColor,
                contoursShadowedColor,
                contoursShadowWidthLow,
                contoursShadowWidthHigh,
                contoursIlluminatedWidthLow,
                contoursIlluminatedWidthHigh,
                contoursMinWidth,
                contoursMinDist,
                azimuth,
                contoursInterval,
                contoursGradientAngle,
                contoursAspectGaussBlur,
                contoursTransitionAngle,
                gridMinMax);
    }

    /**
     * @return the generalizationDetails
     */
    public double getGeneralizationDetails() {
        return generalizationDetails;
    }

    /**
     * Returns true if the grid is being generalized.
     *
     * @return
     */
    public boolean isGeneralizing() {
        return generalizationDetails > -1d;
    }

    /**
     * Returns true when contour lines need to be rendered in the foreground
     *
     * @return
     */
    public boolean isRenderingForeground() {
        return foregroundVisualization != ForegroundVisualization.NONE;
    }

    /**
     * @param generalizationDetails the generalizationDetails to set
     */
    public void setGeneralizationDetails(double generalizationDetails) {
        if (generalizationDetails < -1 || generalizationDetails > 1) {
            throw new IllegalArgumentException();
        }
        this.generalizationDetails = generalizationDetails;
    }

    public double getLocalGridHighPassWeight() {
        return localGridModel.getHighPassWeight();
    }

    public int getLocalGridStandardDeviationLevels() {
        return localGridModel.getLocalGridStandardDeviationLevels();
    }

    public void setLocalGridHighPassWeight(double highPassWeight) {
        localGridModel.setHighPassWeight(highPassWeight);
    }

    public void setLocalGridStandardDeviationLevels(int levels) {
        localGridModel.setLocalGridStandardDeviationLevels(levels);
    }

    public void scaleGrid(float scale) {
        GridScaleOperator op = new GridScaleOperator(scale);
        op.operate(grid, grid);
        setGrid(grid);
    }

    public void verticallyOffsetGrid(float offset) {
        GridAddOperator op = new GridAddOperator(offset);
        op.operate(grid, grid);
        setGrid(grid);
    }

    public void voidGridValue(float v) {
        GridVoidOperator op = new GridVoidOperator(v);
        op.operate(grid, grid);
        setGrid(grid);
    }
    
    /**
     * @return the bivariateColorRender
     */
    public BivariateColorRenderer getBivariateColorRender() {
        return bivariateColorRender;
    }
}
