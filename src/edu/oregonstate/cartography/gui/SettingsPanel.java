package edu.oregonstate.cartography.gui;

import com.bric.swing.MultiThumbSlider;
import edu.oregonstate.cartography.app.FileUtils;
import edu.oregonstate.cartography.app.ImageUtils;
import edu.oregonstate.cartography.grid.EsriASCIIGridReader;
import edu.oregonstate.cartography.grid.Grid;
import edu.oregonstate.cartography.grid.Model;
import edu.oregonstate.cartography.grid.Model.ColorRamp;
import edu.oregonstate.cartography.grid.Model.ForegroundVisualization;
import edu.oregonstate.cartography.grid.operators.ColorizerOperator;
import edu.oregonstate.cartography.grid.operators.ColorizerOperator.ColorVisualization;
import static edu.oregonstate.cartography.gui.SettingsPanel.RenderSpeed.FAST;
import static edu.oregonstate.cartography.gui.SettingsPanel.RenderSpeed.REGULAR;
import edu.oregonstate.cartography.gui.bivariate.BivariateColorPoint;
import edu.oregonstate.cartography.gui.bivariate.BivariateColorRenderer;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ItemEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JRootPane;
import javax.swing.JSlider;
import javax.swing.event.ChangeListener;

/**
 *
 * @author Bernhard Jenny, Cartography and Geovisualization Group, Oregon State
 * University
 */
public class SettingsPanel extends javax.swing.JPanel {

    protected enum RenderSpeed {

        FAST, REGULAR
    }

    // A SwingWorker for rendering the image.
    class Renderer extends SwingWorkerWithProgressIndicatorPanel<Void> {

        private final BufferedImage backgroundImage;
        private final BufferedImage foregroundImage;

        protected Renderer(BufferedImage backgroundImage,
                BufferedImage foregroundImage, ProgressPanel progressPanel) {
            super(progressPanel);
            this.backgroundImage = backgroundImage;
            this.foregroundImage = foregroundImage;
            this.setIndeterminate(false);
            this.setCancellable(false);
            this.removeMessageField();
        }

        @Override
        public Void doInBackground() {
            // initialize the progress dialog
            start();
            model.renderBackgroundImage(backgroundImage, this);
            model.renderForegroundImage(foregroundImage, this);
            return null;
        }

        @Override
        protected void done() {
            Graphics g = null;
            MainWindow mainWindow = getOwnerWindow();
            try {
                if (!isCancelled() && mainWindow != null) {
                    get();

                    BufferedImage displayImage = mainWindow.getImage();
                    if (displayImage == null) {
                        mainWindow.initDisplayImage();
                        displayImage = mainWindow.getImage();
                    }

                    int w = displayImage.getWidth();
                    int h = displayImage.getHeight();
                    g = displayImage.getGraphics();

                    // erase previous image
                    g.setColor(Color.WHITE);
                    g.fillRect(0, 0, displayImage.getWidth(), displayImage.getHeight());

                    // copy background image into the display image
                    g.drawImage(backgroundImage, 0, 0, w, h, null);

                    // copy foreground image into the display image if required.
                    if (model.isRenderingForeground()) {
                        BufferedImage img = ImageUtils.getScaledInstance(foregroundImage,
                                displayImage.getWidth(), displayImage.getHeight(),
                                RenderingHints.VALUE_INTERPOLATION_BILINEAR, true);
                        g.drawImage(img, 0, 0, w, h, null);
                    }
                }
            } catch (Exception ignore) {
            } finally {
                completeProgress();
                if (g != null) {
                    g.dispose();
                }
                if (!isCancelled() && mainWindow != null) {
                    mainWindow.repaintImage();
                }
            }
        }
    }
    private Model model;
    private Renderer renderer;
    private ProgressPanel progressPanel;

    public SettingsPanel() {
        initComponents();
        // hide experimental plan oblique rendering panel
        planObliquePanel.setVisible(false);
    }

    public void setProgressPanel(ProgressPanel progressPanel) {
        this.progressPanel = progressPanel;
    }

    private void updateColorPickerFromBivariatePanel() {
        BivariateColorPoint pt = bivariateColorPanel.getSelectedPoint();
        if (pt == null || pt.getColor() == null) {
            colorPicker.setColor(Color.GRAY);
        } else {
            colorPicker.setColor(pt.getColor());
        }
    }

    private void updateColorPickerFromColorGradientSlider() {
        Color[] colors = colorGradientSlider.getValues();
        int selectedID = colorGradientSlider.getSelectedThumb(false);
        if (selectedID >= 0 && selectedID < colors.length) {
            colorPicker.setColor(colors[selectedID]);
        } else {
            colorGradientSlider.setSelectedThumb(0);
        }
    }

    /**
     * Adjusts the visibility of the GUI components for configuring the
     * different visualization types.
     */
    private void updateVisualizationPanelsVisibility() {

        boolean isShading = false;
        boolean isColored = false;
        boolean isLocal = false;
        boolean isSolidColor = false;
        boolean isBivariate = false;

        if (model != null && model.backgroundVisualization != null) {
            isShading = model.backgroundVisualization.isShading();
            isColored = model.backgroundVisualization.isColored();
            isLocal = model.backgroundVisualization.isLocal();
            isSolidColor = model.backgroundVisualization == ColorVisualization.CONTINUOUS;
            isBivariate = model.backgroundVisualization.isBivariate();
        }

        boolean isIlluminatedContours = model != null
                && model.foregroundVisualization != ForegroundVisualization.NONE;

        verticalExaggerationPanel.setVisible(isShading);
        colorGradientPanel.setVisible(isColored && !isBivariate);
        colorPicker.setVisible(isColored);
        localHypsoPanel.setVisible(isLocal);
        solidColorPanel.setVisible(isSolidColor);
        bivariateColorGroupPanel.setVisible(isBivariate);
        azimuthSlider.setEnabled(isShading || isIlluminatedContours);
        zenithSlider.setEnabled(isShading);
        ambientLightSlider.setEnabled(isShading);

        // adjust size of dialog to make sure all components are visible
        JRootPane rootPane = getRootPane();
        if (rootPane != null) {
            ((JDialog) (rootPane.getParent())).pack();
        }

        contoursBlankBackgroundButton.setEnabled(!isSolidColor);

        if (isBivariate) {
            updateColorPickerFromBivariatePanel();
        } else {
            updateColorPickerFromColorGradientSlider();
        }
    }

    public void updateImage(RenderSpeed renderSpeed) {
        try {
            MainWindow mainWindow = getOwnerWindow();
            if (mainWindow == null || model == null) {
                return;
            }

            // if we are currently rendering an image, first cancel the current rendering
            if (renderer != null && !renderer.isDone()) {
                renderer.cancel(false);
            }

            // block the event dispatching thread until the BackgroundRenderer worker thread
            // is done. This is to avoid that two BackgroundRenderer threads write to 
            // the same image.
            // get() throws a CancellationException if the worker has been cancelled.
            try {
                if (renderer != null && !renderer.isCancelled()) {
                    renderer.get();
                }
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(SettingsPanel.class.getName()).log(Level.SEVERE, null, ex);
            }

            // create destination image
            // note: it is not possible to reuse this image. Flickering artifacts
            // will appear otherwise.
            BufferedImage backgroundImage = model.createDestinationImage(1);
            if (backgroundImage == null) {
                return;
            }
            int foregroundScale = (renderSpeed == FAST ? 1 : 2);
            BufferedImage foregroundImage = model.createDestinationImage(foregroundScale);

            // create a new renderer and run it
            renderer = new Renderer(backgroundImage, foregroundImage, progressPanel);
            renderer.execute();
        } catch (Throwable e) {
            String msg = "<html>An error occured when rendering the terrain.</html>";
            String title = "Error";
            ErrorDialog.showErrorDialog(msg, title, e, null);
            e.printStackTrace();
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        colorPopupMenu = new javax.swing.JPopupMenu();
        bivariateInterpolationButtonGroup = new javax.swing.ButtonGroup();
        tabbedPane = new javax.swing.JTabbedPane();
        javax.swing.JPanel visualizationContainer = new TransparentMacPanel();
        visualizationPanel = new TransparentMacPanel();
        visualizationComboBox = new javax.swing.JComboBox();
        verticalExaggerationPanel = new TransparentMacPanel();
        verticalExaggerationLabel = new javax.swing.JLabel();
        verticalExaggerationSlider = new javax.swing.JSlider();
        colorGradientPanel = new TransparentMacPanel();
        javax.swing.JLabel colorInfoLabel = new javax.swing.JLabel();
        colorGradientSlider = new com.bric.swing.GradientSlider();
        colorPresetsButton = new edu.oregonstate.cartography.gui.MenuToggleButton();
        localHypsoPanel = new TransparentMacPanel();
        javax.swing.JLabel jLabel1 = new javax.swing.JLabel();
        javax.swing.JLabel jLabel2 = new javax.swing.JLabel();
        localGridStandardDeviationFilterSizeSlider = new javax.swing.JSlider();
        localGridHighPassSlider = new javax.swing.JSlider();
        javax.swing.JLabel jLabel3 = new javax.swing.JLabel();
        solidColorPanel = new TransparentMacPanel();
        solidColorButton = new edu.oregonstate.cartography.gui.ColorButton();
        planObliquePanel = new TransparentMacPanel();
        planObliqueSlider = new javax.swing.JSlider();
        jLabel4 = new javax.swing.JLabel();
        colorPicker = new com.bric.swing.ColorPicker();
        bivariateColorGroupPanel = new TransparentMacPanel();
        bivariateColorPanel = new edu.oregonstate.cartography.gui.bivariate.BivariateColorPanel();
        idwRadioButton = new javax.swing.JRadioButton();
        gaussRadioButton = new javax.swing.JRadioButton();
        jLabel9 = new javax.swing.JLabel();
        javax.swing.JLabel idwExponentSliderLabel = new javax.swing.JLabel();
        idwExponentSlider = new javax.swing.JSlider();
        bivariateColorExponentValueLabel = new javax.swing.JLabel();
        bivariateHorizontalButton = new javax.swing.JButton();
        bivariateVerticalButton = new javax.swing.JButton();
        bivariateVerticalLabel = new edu.oregonstate.cartography.gui.RotatedLabel();
        bivariateHorizontalLabel = new javax.swing.JLabel();
        javax.swing.JPanel generalizationContainer = new TransparentMacPanel();
        generalizationPanel = new TransparentMacPanel();
        javax.swing.JLabel generalizationDetailsRemovalLabel = new javax.swing.JLabel();
        generalizationDetailSlider = new javax.swing.JSlider();
        javax.swing.JLabel generalizationMaxLabel = new javax.swing.JLabel();
        generalizationMaxLevelsSpinner = new javax.swing.JSpinner();
        generalizationDetaiIsLabel = new javax.swing.JLabel();
        generalizationInfoLabel = new javax.swing.JLabel();
        javax.swing.JPanel illuminationContainer = new TransparentMacPanel();
        illuminationPanel = new TransparentMacPanel();
        javax.swing.JLabel azLabel = new javax.swing.JLabel();
        azimuthSlider = new javax.swing.JSlider();
        javax.swing.JLabel zeLabel = new javax.swing.JLabel();
        zenithSlider = new javax.swing.JSlider();
        javax.swing.JLabel ambientLightLabel = new javax.swing.JLabel();
        ambientLightSlider = new javax.swing.JSlider();
        contoursPanel = new TransparentMacPanel();
        illuminatedContoursPanel = new TransparentMacPanel();
        contoursComboBox = new javax.swing.JComboBox();
        contoursCardPanel = new TransparentMacPanel();
        contoursEmptyPanel = new TransparentMacPanel();
        contoursSettingsPanel = new TransparentMacPanel();
        javax.swing.JLabel contoursIlluminatedLineWidthLabel = new javax.swing.JLabel();
        javax.swing.JLabel contoursIlluminatedHighestLabel = new javax.swing.JLabel();
        contoursIlluminatedHighestLineWidthSlider = new javax.swing.JSlider();
        contoursIlluminatedLockedToggleButton = new javax.swing.JToggleButton();
        javax.swing.JLabel contoursIlluminatedLowestLabel = new javax.swing.JLabel();
        contoursIlluminatedLowestLineWidthSlider = new javax.swing.JSlider();
        javax.swing.JLabel contoursShadwoLineWidthSlider = new javax.swing.JLabel();
        javax.swing.JLabel contoursShadowedHighest = new javax.swing.JLabel();
        contoursShadowHighestLineWidthSlider = new javax.swing.JSlider();
        contoursShadowedLockedToggleButton = new javax.swing.JToggleButton();
        javax.swing.JLabel contoursShadowedLowest = new javax.swing.JLabel();
        contoursShadowLowestLineWidthSlider = new javax.swing.JSlider();
        javax.swing.JLabel contoursMinLineWidthLabel = new javax.swing.JLabel();
        contoursMinLineWidthSlider = new javax.swing.JSlider();
        contoursMinLineWidthValueLabel = new javax.swing.JLabel();
        contoursMinDistanceSlider = new javax.swing.JSlider();
        javax.swing.JLabel contoursWidthInfoLabel = new javax.swing.JLabel();
        contoursMinDistanceLabel = new javax.swing.JLabel();
        javax.swing.JLabel contoursIntervalLabel = new javax.swing.JLabel();
        contoursIntervalTextBox = new javax.swing.JFormattedTextField();
        javax.swing.JLabel contoursGradientLabel = new javax.swing.JLabel();
        contoursGradientSlider = new javax.swing.JSlider();
        javax.swing.JLabel contoursDespeckleLabel = new javax.swing.JLabel();
        contoursDespeckleSlider = new javax.swing.JSlider();
        javax.swing.JLabel contoursTransitionLabel = new javax.swing.JLabel();
        contoursTransitionSlider = new javax.swing.JSlider();
        javax.swing.JLabel contoursExportInfoLabel = new javax.swing.JLabel();
        contoursBlankBackgroundButton = new javax.swing.JButton();
        contoursMinDistanceValueLabel = new javax.swing.JLabel();
        contoursShadowLineWidthHighValueField = new javax.swing.JFormattedTextField();
        contoursShadowLineWidthLowValueField = new javax.swing.JFormattedTextField();
        contoursIlluminatedLineWidthHighValueField = new javax.swing.JFormattedTextField();
        contoursIlluminatedLineWidthLowValueField = new javax.swing.JFormattedTextField();
        contoursIlluminatedColorButton = new edu.oregonstate.cartography.gui.ColorButton();
        contoursShadowedColorButton = new edu.oregonstate.cartography.gui.ColorButton();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        spacingPanel = new javax.swing.JPanel();

        colorPopupMenu.setLightWeightPopupEnabled(false);

        setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 2, 2));

        visualizationPanel.setLayout(new java.awt.GridBagLayout());

        visualizationComboBox.setMaximumRowCount(15);
        visualizationComboBox.setModel(new DefaultComboBoxModel(ColorizerOperator.ColorVisualization.values()));
        visualizationComboBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                visualizationComboBoxItemStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        visualizationPanel.add(visualizationComboBox, gridBagConstraints);

        verticalExaggerationPanel.setLayout(new java.awt.GridBagLayout());

        verticalExaggerationLabel.setText("Vertical Exaggeration for Shading");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        verticalExaggerationPanel.add(verticalExaggerationLabel, gridBagConstraints);

        verticalExaggerationSlider.setMajorTickSpacing(5);
        verticalExaggerationSlider.setMaximum(50);
        verticalExaggerationSlider.setMinorTickSpacing(1);
        verticalExaggerationSlider.setPaintLabels(true);
        verticalExaggerationSlider.setPaintTicks(true);
        verticalExaggerationSlider.setSnapToTicks(true);
        verticalExaggerationSlider.setToolTipText("Vertical exaggeration to the grid applied for shading calculation.");
        verticalExaggerationSlider.setValue(10);
        verticalExaggerationSlider.setPreferredSize(new java.awt.Dimension(300, 52));
        {
            java.util.Hashtable labels = verticalExaggerationSlider.createStandardLabels(10);
            java.util.Enumeration e = labels.elements();
            DecimalFormat df = new DecimalFormat("#.#");
            while(e.hasMoreElements()) {
                javax.swing.JComponent comp = (javax.swing.JComponent)e.nextElement();
                if (comp instanceof javax.swing.JLabel) {
                    javax.swing.JLabel label = (javax.swing.JLabel)(comp);
                    String v = df.format(Integer.parseInt(label.getText()) / 10f);
                    label.setText("\u00d7" + v);
                }
            }
            verticalExaggerationSlider.setLabelTable(labels);
        }
        verticalExaggerationSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                verticalExaggerationSliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        verticalExaggerationPanel.add(verticalExaggerationSlider, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(20, 0, 0, 0);
        visualizationPanel.add(verticalExaggerationPanel, gridBagConstraints);

        colorGradientPanel.setLayout(new java.awt.GridBagLayout());

        colorInfoLabel.setFont(colorInfoLabel.getFont().deriveFont(colorInfoLabel.getFont().getSize()-2f));
        colorInfoLabel.setText("Click on slider to add color. Hit delete key to remove selected color.");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 0);
        colorGradientPanel.add(colorInfoLabel, gridBagConstraints);

        colorGradientSlider.setPreferredSize(new java.awt.Dimension(360, 30));
        colorGradientSlider.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                colorGradientSliderPropertyChange(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        colorGradientPanel.add(colorGradientSlider, gridBagConstraints);

        colorPresetsButton.setText("Color Presets");
        colorPresetsButton.setPopupMenu(colorPopupMenu);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        colorGradientPanel.add(colorPresetsButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(20, 0, 0, 0);
        visualizationPanel.add(colorGradientPanel, gridBagConstraints);

        localHypsoPanel.setLayout(new java.awt.GridBagLayout());

        jLabel1.setText("High Pass Filter Size");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        localHypsoPanel.add(jLabel1, gridBagConstraints);

        jLabel2.setText("Std Dev Filter Size");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        localHypsoPanel.add(jLabel2, gridBagConstraints);

        localGridStandardDeviationFilterSizeSlider.setMajorTickSpacing(1);
        localGridStandardDeviationFilterSizeSlider.setMaximum(10);
        localGridStandardDeviationFilterSizeSlider.setMinimum(1);
        localGridStandardDeviationFilterSizeSlider.setPaintLabels(true);
        localGridStandardDeviationFilterSizeSlider.setPaintTicks(true);
        localGridStandardDeviationFilterSizeSlider.setSnapToTicks(true);
        localGridStandardDeviationFilterSizeSlider.setPreferredSize(new java.awt.Dimension(240, 38));
        localGridStandardDeviationFilterSizeSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                localGridStandardDeviationFilterSizeSliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        localHypsoPanel.add(localGridStandardDeviationFilterSizeSlider, gridBagConstraints);

        localGridHighPassSlider.setMajorTickSpacing(10);
        localGridHighPassSlider.setMinorTickSpacing(5);
        localGridHighPassSlider.setPaintLabels(true);
        localGridHighPassSlider.setPaintTicks(true);
        localGridHighPassSlider.setValue(10);
        localGridHighPassSlider.setPreferredSize(new java.awt.Dimension(240, 38));
        localGridHighPassSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                localGridHighPassSliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        localHypsoPanel.add(localGridHighPassSlider, gridBagConstraints);

        jLabel3.setText("Local Terrain Filtering");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        localHypsoPanel.add(jLabel3, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new java.awt.Insets(20, 0, 0, 0);
        visualizationPanel.add(localHypsoPanel, gridBagConstraints);

        solidColorPanel.setLayout(new java.awt.GridBagLayout());

        solidColorButton.setText("Background Color");
        solidColorButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                solidColorButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        solidColorPanel.add(solidColorButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.insets = new java.awt.Insets(20, 0, 0, 0);
        visualizationPanel.add(solidColorPanel, gridBagConstraints);

        planObliquePanel.setLayout(new java.awt.GridBagLayout());

        planObliqueSlider.setMajorTickSpacing(15);
        planObliqueSlider.setMaximum(90);
        planObliqueSlider.setMinimum(15);
        planObliqueSlider.setMinorTickSpacing(5);
        planObliqueSlider.setPaintLabels(true);
        planObliqueSlider.setPaintTicks(true);
        planObliqueSlider.setValue(90);
        {
            java.util.Hashtable labels = planObliqueSlider.createStandardLabels(15);
            java.util.Enumeration e = labels.elements();
            while(e.hasMoreElements()) {
                javax.swing.JComponent comp = (javax.swing.JComponent)e.nextElement();
                if (comp instanceof javax.swing.JLabel) {
                    javax.swing.JLabel label = (javax.swing.JLabel)(comp);
                    label.setText(label.getText() + "\u00b0");
                }
            }
            planObliqueSlider.setLabelTable(labels);
        }
        planObliqueSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                planObliqueSliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        planObliquePanel.add(planObliqueSlider, gridBagConstraints);

        jLabel4.setText("Plan Oblique Relief (experimental - not for local hypsometry)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        planObliquePanel.add(jLabel4, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.insets = new java.awt.Insets(20, 0, 0, 0);
        visualizationPanel.add(planObliquePanel, gridBagConstraints);

        colorPicker.setPreferredSize(new java.awt.Dimension(380, 300));
        colorPicker.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                colorPickerPropertyChange(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.insets = new java.awt.Insets(20, 0, 0, 0);
        visualizationPanel.add(colorPicker, gridBagConstraints);

        bivariateColorGroupPanel.setLayout(new java.awt.GridBagLayout());

        bivariateColorPanel.setPreferredSize(new java.awt.Dimension(200, 200));
        bivariateColorPanel.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                bivariateColorPanelPropertyChange(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        bivariateColorGroupPanel.add(bivariateColorPanel, gridBagConstraints);

        bivariateInterpolationButtonGroup.add(idwRadioButton);
        idwRadioButton.setText("Inverse Distance");
        idwRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                idwRadioButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        bivariateColorGroupPanel.add(idwRadioButton, gridBagConstraints);

        bivariateInterpolationButtonGroup.add(gaussRadioButton);
        gaussRadioButton.setSelected(true);
        gaussRadioButton.setText("Gaussian Weight");
        gaussRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gaussRadioButtonidwRadioButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        bivariateColorGroupPanel.add(gaussRadioButton, gridBagConstraints);

        jLabel9.setFont(jLabel9.getFont().deriveFont(jLabel9.getFont().getSize()-2f));
        jLabel9.setText("<html>Click to add a point. Click and drag to move a point. Press the<br>delete key to remove the selected point. Right-click on the map<br>to add color points to the diagram.</html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 0, 0);
        bivariateColorGroupPanel.add(jLabel9, gridBagConstraints);

        idwExponentSliderLabel.setText("Exponent");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(15, 6, 0, 0);
        bivariateColorGroupPanel.add(idwExponentSliderLabel, gridBagConstraints);

        idwExponentSlider.setMajorTickSpacing(10);
        idwExponentSlider.setMaximum(50);
        idwExponentSlider.setMinorTickSpacing(5);
        idwExponentSlider.setPaintTicks(true);
        idwExponentSlider.setPreferredSize(new java.awt.Dimension(130, 38));
        idwExponentSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                idwExponentSliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        bivariateColorGroupPanel.add(idwExponentSlider, gridBagConstraints);

        bivariateColorExponentValueLabel.setFont(bivariateColorExponentValueLabel.getFont().deriveFont(bivariateColorExponentValueLabel.getFont().getSize()-2f));
        bivariateColorExponentValueLabel.setText("1.3");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        bivariateColorGroupPanel.add(bivariateColorExponentValueLabel, gridBagConstraints);

        bivariateHorizontalButton.setText("Horizontal Axis");
        bivariateHorizontalButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bivariateHorizontalButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_END;
        gridBagConstraints.weighty = 1.0;
        bivariateColorGroupPanel.add(bivariateHorizontalButton, gridBagConstraints);

        bivariateVerticalButton.setText("Vertical Axis");
        bivariateVerticalButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bivariateVerticalButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_END;
        bivariateColorGroupPanel.add(bivariateVerticalButton, gridBagConstraints);

        bivariateVerticalLabel.setText("-");
        bivariateVerticalLabel.setFont(bivariateVerticalLabel.getFont().deriveFont(bivariateVerticalLabel.getFont().getSize()-2f));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 7;
        bivariateColorGroupPanel.add(bivariateVerticalLabel, gridBagConstraints);

        bivariateHorizontalLabel.setFont(bivariateHorizontalLabel.getFont().deriveFont(bivariateHorizontalLabel.getFont().getSize()-2f));
        bivariateHorizontalLabel.setText("-");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 8, 0);
        bivariateColorGroupPanel.add(bivariateHorizontalLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.insets = new java.awt.Insets(20, 0, 0, 0);
        visualizationPanel.add(bivariateColorGroupPanel, gridBagConstraints);

        visualizationContainer.add(visualizationPanel);

        tabbedPane.addTab("Visualization", visualizationContainer);

        generalizationPanel.setLayout(new java.awt.GridBagLayout());

        generalizationDetailsRemovalLabel.setText("Details Removal");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 10);
        generalizationPanel.add(generalizationDetailsRemovalLabel, gridBagConstraints);

        generalizationDetailSlider.setMajorTickSpacing(100);
        generalizationDetailSlider.setMinimum(-100);
        generalizationDetailSlider.setMinorTickSpacing(10);
        generalizationDetailSlider.setPaintTicks(true);
        generalizationDetailSlider.setToolTipText("");
        generalizationDetailSlider.setValue(-100);
        generalizationDetailSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                generalizationDetailSliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 0);
        generalizationPanel.add(generalizationDetailSlider, gridBagConstraints);

        generalizationMaxLabel.setText("Landforms Removal");
        generalizationMaxLabel.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 10);
        generalizationPanel.add(generalizationMaxLabel, gridBagConstraints);

        generalizationMaxLevelsSpinner.setModel(new javax.swing.SpinnerNumberModel(3, 1, 10, 1));
        generalizationMaxLevelsSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                generalizationMaxLevelsSpinnerStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        generalizationPanel.add(generalizationMaxLevelsSpinner, gridBagConstraints);

        generalizationDetaiIsLabel.setText("100%");
        generalizationDetaiIsLabel.setPreferredSize(new java.awt.Dimension(40, 16));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        generalizationPanel.add(generalizationDetaiIsLabel, gridBagConstraints);

        generalizationInfoLabel.setText("No Generalization");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 9, 0, 0);
        generalizationPanel.add(generalizationInfoLabel, gridBagConstraints);

        generalizationContainer.add(generalizationPanel);

        tabbedPane.addTab("Generalization", generalizationContainer);

        illuminationPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        illuminationPanel.setLayout(new java.awt.GridBagLayout());

        azLabel.setText("Azimuth (Horizontal Direction)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        illuminationPanel.add(azLabel, gridBagConstraints);

        azimuthSlider.setMajorTickSpacing(45);
        azimuthSlider.setMaximum(360);
        azimuthSlider.setMinorTickSpacing(15);
        azimuthSlider.setPaintLabels(true);
        azimuthSlider.setPaintTicks(true);
        azimuthSlider.setValue(45);
        azimuthSlider.setPreferredSize(new java.awt.Dimension(380, 52));
        {
            java.util.Hashtable labels = azimuthSlider.createStandardLabels(45);
            java.util.Enumeration e = labels.elements();
            while(e.hasMoreElements()) {
                javax.swing.JComponent comp = (javax.swing.JComponent)e.nextElement();
                if (comp instanceof javax.swing.JLabel) {
                    javax.swing.JLabel label = (javax.swing.JLabel)(comp);
                    int angle = Integer.parseInt(label.getText());
                    switch (angle) {
                        case 0:
                        case 360:
                        label.setText("N");
                        break;
                        case 45:
                        label.setText("NE");
                        break;
                        case 90:
                        label.setText("E");
                        break;
                        case 135:
                        label.setText("SE");
                        break;
                        case 180:
                        label.setText("S");
                        break;
                        case 225:
                        label.setText("SW");
                        break;
                        case 270:
                        label.setText("W");
                        break;
                        case 315:
                        label.setText("NW");
                        break;
                    }
                }
            }
            azimuthSlider.setLabelTable(labels);
        }
        azimuthSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                azimuthSliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        illuminationPanel.add(azimuthSlider, gridBagConstraints);

        zeLabel.setText("Zenith (Vertical Direction)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(20, 0, 0, 0);
        illuminationPanel.add(zeLabel, gridBagConstraints);

        zenithSlider.setMajorTickSpacing(15);
        zenithSlider.setMaximum(90);
        zenithSlider.setMinorTickSpacing(5);
        zenithSlider.setPaintLabels(true);
        zenithSlider.setPaintTicks(true);
        zenithSlider.setValue(45);
        {
            java.util.Hashtable labels = zenithSlider.createStandardLabels(15);
            java.util.Enumeration e = labels.elements();
            while(e.hasMoreElements()) {
                javax.swing.JComponent comp = (javax.swing.JComponent)e.nextElement();
                if (comp instanceof javax.swing.JLabel) {
                    javax.swing.JLabel label = (javax.swing.JLabel)(comp);
                    label.setText(label.getText() + "\u00b0");
                }
            }
            zenithSlider.setLabelTable(labels);
        }
        zenithSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                zenithSliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        illuminationPanel.add(zenithSlider, gridBagConstraints);

        ambientLightLabel.setText("Ambient Light");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(20, 0, 0, 0);
        illuminationPanel.add(ambientLightLabel, gridBagConstraints);

        ambientLightSlider.setMajorTickSpacing(10);
        ambientLightSlider.setMaximum(50);
        ambientLightSlider.setMinimum(-50);
        ambientLightSlider.setMinorTickSpacing(5);
        ambientLightSlider.setPaintLabels(true);
        ambientLightSlider.setPaintTicks(true);
        ambientLightSlider.setValue(0);
        {
            java.util.Hashtable labels = ambientLightSlider.createStandardLabels(10);
            java.util.Enumeration e = labels.elements();
            DecimalFormat df = new DecimalFormat("0.#");
            while(e.hasMoreElements()) {
                javax.swing.JComponent comp = (javax.swing.JComponent)e.nextElement();
                if (comp instanceof javax.swing.JLabel) {
                    javax.swing.JLabel label = (javax.swing.JLabel)(comp);
                    double d = Double.parseDouble(label.getText()) / 100d;
                    label.setText(df.format(d));
                }
            }
            ambientLightSlider.setLabelTable(labels);
        }
        ambientLightSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                ambientLightSliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        illuminationPanel.add(ambientLightSlider, gridBagConstraints);

        illuminationContainer.add(illuminationPanel);

        tabbedPane.addTab("Illumination", illuminationContainer);

        illuminatedContoursPanel.setLayout(new java.awt.GridBagLayout());

        contoursComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "No Contours", "Illuminated & Shadowed Contours", "Shadowed Contours" }));
        contoursComboBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                contoursComboBoxItemStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 0);
        illuminatedContoursPanel.add(contoursComboBox, gridBagConstraints);

        contoursCardPanel.setLayout(new java.awt.CardLayout());
        contoursCardPanel.add(contoursEmptyPanel, "emptyCard");

        contoursSettingsPanel.setLayout(new java.awt.GridBagLayout());

        contoursIlluminatedLineWidthLabel.setText("Maximum Illuminated Line Width");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 0);
        contoursSettingsPanel.add(contoursIlluminatedLineWidthLabel, gridBagConstraints);

        contoursIlluminatedHighestLabel.setText("Highest");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        contoursSettingsPanel.add(contoursIlluminatedHighestLabel, gridBagConstraints);

        contoursIlluminatedHighestLineWidthSlider.setMajorTickSpacing(10);
        contoursIlluminatedHighestLineWidthSlider.setMaximum(50);
        contoursIlluminatedHighestLineWidthSlider.setMinorTickSpacing(5);
        contoursIlluminatedHighestLineWidthSlider.setPaintLabels(true);
        contoursIlluminatedHighestLineWidthSlider.setPaintTicks(true);
        contoursIlluminatedHighestLineWidthSlider.setToolTipText("Line widths are relative to grid cell size.");
        contoursIlluminatedHighestLineWidthSlider.setValue(0);
        {
            java.util.Hashtable labels = contoursIlluminatedHighestLineWidthSlider.createStandardLabels(10);
            java.util.Enumeration e = labels.elements();
            while(e.hasMoreElements()) {
                javax.swing.JComponent comp = (javax.swing.JComponent)e.nextElement();
                if (comp instanceof javax.swing.JLabel) {
                    javax.swing.JLabel label = (javax.swing.JLabel)(comp);
                    int sliderValue = Integer.parseInt(label.getText());
                    label.setText(Integer.toString(sliderValue / 10));
                }
            }
            contoursIlluminatedHighestLineWidthSlider.setLabelTable(labels);
        }
        contoursIlluminatedHighestLineWidthSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                contoursIlluminatedHighestLineWidthSliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        contoursSettingsPanel.add(contoursIlluminatedHighestLineWidthSlider, gridBagConstraints);

        contoursIlluminatedLockedToggleButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/edu/oregonstate/cartography/gui/icons/unlocked14.png"))); // NOI18N
        contoursIlluminatedLockedToggleButton.setSelected(true);
        contoursIlluminatedLockedToggleButton.setBorderPainted(false);
        contoursIlluminatedLockedToggleButton.setContentAreaFilled(false);
        contoursIlluminatedLockedToggleButton.setPreferredSize(new java.awt.Dimension(14, 14));
        contoursIlluminatedLockedToggleButton.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/edu/oregonstate/cartography/gui/icons/locked14.png"))); // NOI18N
        contoursIlluminatedLockedToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                contoursIlluminatedLockedToggleButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        contoursSettingsPanel.add(contoursIlluminatedLockedToggleButton, gridBagConstraints);

        contoursIlluminatedLowestLabel.setText("Lowest");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        contoursSettingsPanel.add(contoursIlluminatedLowestLabel, gridBagConstraints);

        contoursIlluminatedLowestLineWidthSlider.setMajorTickSpacing(10);
        contoursIlluminatedLowestLineWidthSlider.setMaximum(50);
        contoursIlluminatedLowestLineWidthSlider.setMinorTickSpacing(5);
        contoursIlluminatedLowestLineWidthSlider.setPaintLabels(true);
        contoursIlluminatedLowestLineWidthSlider.setPaintTicks(true);
        contoursIlluminatedLowestLineWidthSlider.setToolTipText("Line widths are relative to grid cell size.");
        contoursIlluminatedLowestLineWidthSlider.setValue(0);
        {
            java.util.Hashtable labels = contoursIlluminatedLowestLineWidthSlider.createStandardLabels(10);
            java.util.Enumeration e = labels.elements();
            while(e.hasMoreElements()) {
                javax.swing.JComponent comp = (javax.swing.JComponent)e.nextElement();
                if (comp instanceof javax.swing.JLabel) {
                    javax.swing.JLabel label = (javax.swing.JLabel)(comp);
                    int sliderValue = Integer.parseInt(label.getText());
                    label.setText(Integer.toString(sliderValue / 10));
                }
            }
            contoursIlluminatedLowestLineWidthSlider.setLabelTable(labels);
        }
        contoursIlluminatedLowestLineWidthSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                contoursIlluminatedLowestLineWidthSliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        contoursSettingsPanel.add(contoursIlluminatedLowestLineWidthSlider, gridBagConstraints);

        contoursShadwoLineWidthSlider.setText("Maximum Shadowed Line Width");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(15, 0, 0, 0);
        contoursSettingsPanel.add(contoursShadwoLineWidthSlider, gridBagConstraints);

        contoursShadowedHighest.setText("Highest");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        contoursSettingsPanel.add(contoursShadowedHighest, gridBagConstraints);

        contoursShadowHighestLineWidthSlider.setMajorTickSpacing(10);
        contoursShadowHighestLineWidthSlider.setMaximum(50);
        contoursShadowHighestLineWidthSlider.setMinorTickSpacing(5);
        contoursShadowHighestLineWidthSlider.setPaintLabels(true);
        contoursShadowHighestLineWidthSlider.setPaintTicks(true);
        contoursShadowHighestLineWidthSlider.setToolTipText("Line widths are relative to grid cell size.");
        contoursShadowHighestLineWidthSlider.setValue(0);
        {
            java.util.Hashtable labels = contoursShadowHighestLineWidthSlider.createStandardLabels(10);
            java.util.Enumeration e = labels.elements();
            while(e.hasMoreElements()) {
                javax.swing.JComponent comp = (javax.swing.JComponent)e.nextElement();
                if (comp instanceof javax.swing.JLabel) {
                    javax.swing.JLabel label = (javax.swing.JLabel)(comp);
                    int sliderValue = Integer.parseInt(label.getText());
                    label.setText(Integer.toString(sliderValue / 10));
                }
            }
            contoursShadowHighestLineWidthSlider.setLabelTable(labels);
        }
        contoursShadowHighestLineWidthSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                contoursShadowHighestLineWidthSliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        contoursSettingsPanel.add(contoursShadowHighestLineWidthSlider, gridBagConstraints);

        contoursShadowedLockedToggleButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/edu/oregonstate/cartography/gui/icons/unlocked14.png"))); // NOI18N
        contoursShadowedLockedToggleButton.setSelected(true);
        contoursShadowedLockedToggleButton.setBorderPainted(false);
        contoursShadowedLockedToggleButton.setContentAreaFilled(false);
        contoursShadowedLockedToggleButton.setPreferredSize(new java.awt.Dimension(16, 16));
        contoursShadowedLockedToggleButton.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/edu/oregonstate/cartography/gui/icons/locked14.png"))); // NOI18N
        contoursShadowedLockedToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                contoursShadowedLockedToggleButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        contoursSettingsPanel.add(contoursShadowedLockedToggleButton, gridBagConstraints);

        contoursShadowedLowest.setText("Lowest");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        contoursSettingsPanel.add(contoursShadowedLowest, gridBagConstraints);

        contoursShadowLowestLineWidthSlider.setMajorTickSpacing(10);
        contoursShadowLowestLineWidthSlider.setMaximum(50);
        contoursShadowLowestLineWidthSlider.setMinorTickSpacing(5);
        contoursShadowLowestLineWidthSlider.setPaintLabels(true);
        contoursShadowLowestLineWidthSlider.setPaintTicks(true);
        contoursShadowLowestLineWidthSlider.setToolTipText("Line widths are relative to grid cell size.");
        contoursShadowLowestLineWidthSlider.setValue(0);
        {
            java.util.Hashtable labels = contoursShadowLowestLineWidthSlider.createStandardLabels(10);
            java.util.Enumeration e = labels.elements();
            while(e.hasMoreElements()) {
                javax.swing.JComponent comp = (javax.swing.JComponent)e.nextElement();
                if (comp instanceof javax.swing.JLabel) {
                    javax.swing.JLabel label = (javax.swing.JLabel)(comp);
                    int sliderValue = Integer.parseInt(label.getText());
                    label.setText(Integer.toString(sliderValue / 10));
                }
            }
            contoursShadowLowestLineWidthSlider.setLabelTable(labels);
        }
        contoursShadowLowestLineWidthSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                contoursShadowLowestLineWidthSliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        contoursSettingsPanel.add(contoursShadowLowestLineWidthSlider, gridBagConstraints);

        contoursMinLineWidthLabel.setText("Minimum Line Width");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        contoursSettingsPanel.add(contoursMinLineWidthLabel, gridBagConstraints);

        contoursMinLineWidthSlider.setMajorTickSpacing(10);
        contoursMinLineWidthSlider.setMaximum(50);
        contoursMinLineWidthSlider.setMinorTickSpacing(5);
        contoursMinLineWidthSlider.setPaintLabels(true);
        contoursMinLineWidthSlider.setPaintTicks(true);
        contoursMinLineWidthSlider.setToolTipText("Line widths are relative to grid cell size.");
        contoursMinLineWidthSlider.setValue(0);
        {
            java.util.Hashtable labels = contoursMinLineWidthSlider.createStandardLabels(10);
            java.util.Enumeration e = labels.elements();
            while(e.hasMoreElements()) {
                javax.swing.JComponent comp = (javax.swing.JComponent)e.nextElement();
                if (comp instanceof javax.swing.JLabel) {
                    javax.swing.JLabel label = (javax.swing.JLabel)(comp);
                    int sliderValue = Integer.parseInt(label.getText());
                    label.setText(Integer.toString(sliderValue / 10));
                }
            }
            contoursMinLineWidthSlider.setLabelTable(labels);
        }
        contoursMinLineWidthSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                contoursMinLineWidthSliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        contoursSettingsPanel.add(contoursMinLineWidthSlider, gridBagConstraints);

        contoursMinLineWidthValueLabel.setFont(contoursMinLineWidthSlider.getFont());
        contoursMinLineWidthValueLabel.setText("123");
        contoursMinLineWidthValueLabel.setPreferredSize(new java.awt.Dimension(30, 16));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        contoursSettingsPanel.add(contoursMinLineWidthValueLabel, gridBagConstraints);

        contoursMinDistanceSlider.setMajorTickSpacing(10);
        contoursMinDistanceSlider.setMaximum(50);
        contoursMinDistanceSlider.setMinorTickSpacing(5);
        contoursMinDistanceSlider.setPaintLabels(true);
        contoursMinDistanceSlider.setPaintTicks(true);
        contoursMinDistanceSlider.setToolTipText("Line widths are relative to grid cell size.");
        contoursMinDistanceSlider.setValue(0);
        {
            java.util.Hashtable labels = contoursMinDistanceSlider.createStandardLabels(10);
            java.util.Enumeration e = labels.elements();
            while(e.hasMoreElements()) {
                javax.swing.JComponent comp = (javax.swing.JComponent)e.nextElement();
                if (comp instanceof javax.swing.JLabel) {
                    javax.swing.JLabel label = (javax.swing.JLabel)(comp);
                    int sliderValue = Integer.parseInt(label.getText());
                    label.setText(Integer.toString(sliderValue / 10));
                }
            }
            contoursMinDistanceSlider.setLabelTable(labels);
        }
        contoursMinDistanceSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                contoursMinDistanceSliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        contoursSettingsPanel.add(contoursMinDistanceSlider, gridBagConstraints);

        contoursWidthInfoLabel.setFont(contoursWidthInfoLabel.getFont().deriveFont(contoursWidthInfoLabel.getFont().getSize()-2f));
        contoursWidthInfoLabel.setText("Line widths and minimum distance are relative to grid cell size.");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 0);
        contoursSettingsPanel.add(contoursWidthInfoLabel, gridBagConstraints);

        contoursMinDistanceLabel.setText("Minimum Distance");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        contoursSettingsPanel.add(contoursMinDistanceLabel, gridBagConstraints);

        contoursIntervalLabel.setText("Interval");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        contoursSettingsPanel.add(contoursIntervalLabel, gridBagConstraints);

        contoursIntervalTextBox.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter()));
        contoursIntervalTextBox.setToolTipText("Contour interval");
        contoursIntervalTextBox.setPreferredSize(new java.awt.Dimension(120, 28));
        contoursIntervalTextBox.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                contoursIntervalTextBoxPropertyChange(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        contoursSettingsPanel.add(contoursIntervalTextBox, gridBagConstraints);

        contoursGradientLabel.setText("Gradient Angle");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 15;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        contoursSettingsPanel.add(contoursGradientLabel, gridBagConstraints);

        contoursGradientSlider.setMajorTickSpacing(10);
        contoursGradientSlider.setMaximum(50);
        contoursGradientSlider.setMinorTickSpacing(5);
        contoursGradientSlider.setPaintLabels(true);
        contoursGradientSlider.setPaintTicks(true);
        contoursGradientSlider.setToolTipText("A gradient between black and white is created within this angle.");
        contoursGradientSlider.setValue(0);
        {
            java.util.Hashtable labels = contoursGradientSlider.createStandardLabels(10);
            java.util.Enumeration e = labels.elements();
            while(e.hasMoreElements()) {
                javax.swing.JComponent comp = (javax.swing.JComponent)e.nextElement();
                if (comp instanceof javax.swing.JLabel) {
                    javax.swing.JLabel label = (javax.swing.JLabel)(comp);
                    label.setText(label.getText() + "\u00b0");
                }
            }
            contoursGradientSlider.setLabelTable(labels);
        }
        contoursGradientSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                contoursGradientSliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 15;
        contoursSettingsPanel.add(contoursGradientSlider, gridBagConstraints);

        contoursDespeckleLabel.setText("Despeckle");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        contoursSettingsPanel.add(contoursDespeckleLabel, gridBagConstraints);

        contoursDespeckleSlider.setMajorTickSpacing(25);
        contoursDespeckleSlider.setMinorTickSpacing(5);
        contoursDespeckleSlider.setPaintLabels(true);
        contoursDespeckleSlider.setPaintTicks(true);
        contoursDespeckleSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                contoursDespeckleSliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        contoursSettingsPanel.add(contoursDespeckleSlider, gridBagConstraints);

        contoursTransitionLabel.setText("Transition Angle");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        contoursSettingsPanel.add(contoursTransitionLabel, gridBagConstraints);

        contoursTransitionSlider.setMajorTickSpacing(45);
        contoursTransitionSlider.setMaximum(180);
        contoursTransitionSlider.setMinorTickSpacing(15);
        contoursTransitionSlider.setPaintLabels(true);
        contoursTransitionSlider.setPaintTicks(true);
        contoursTransitionSlider.setValue(90);
        {
            java.util.Hashtable labels = contoursTransitionSlider.createStandardLabels(45);
            java.util.Enumeration e = labels.elements();
            while(e.hasMoreElements()) {
                javax.swing.JComponent comp = (javax.swing.JComponent)e.nextElement();
                if (comp instanceof javax.swing.JLabel) {
                    javax.swing.JLabel label = (javax.swing.JLabel)(comp);
                    label.setText(label.getText() + "\u00b0");
                }
            }
            contoursTransitionSlider.setLabelTable(labels);
        }
        contoursTransitionSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                contoursTransitionSliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        contoursSettingsPanel.add(contoursTransitionSlider, gridBagConstraints);

        contoursExportInfoLabel.setFont(contoursExportInfoLabel.getFont().deriveFont(contoursExportInfoLabel.getFont().getSize()-2f));
        contoursExportInfoLabel.setText("Use File > Save Contour Image for high resolution contour image.");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 21;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 0);
        contoursSettingsPanel.add(contoursExportInfoLabel, gridBagConstraints);

        contoursBlankBackgroundButton.setText("Blank Background");
        contoursBlankBackgroundButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                contoursBlankBackgroundButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 20;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        contoursSettingsPanel.add(contoursBlankBackgroundButton, gridBagConstraints);

        contoursMinDistanceValueLabel.setFont(contoursMinLineWidthSlider.getFont());
        contoursMinDistanceValueLabel.setText("123");
        contoursMinDistanceValueLabel.setPreferredSize(new java.awt.Dimension(30, 16));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        contoursSettingsPanel.add(contoursMinDistanceValueLabel, gridBagConstraints);

        contoursShadowLineWidthHighValueField.setFont(contoursShadowHighestLineWidthSlider.getFont());
        contoursShadowLineWidthHighValueField.setPreferredSize(new java.awt.Dimension(45, 28));
        {
            javax.swing.text.NumberFormatter nf = new javax.swing.text.NumberFormatter(new DecimalFormat("#0.0#"));
            nf.setMinimum(0.);
            contoursShadowLineWidthHighValueField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(nf));
        }
        contoursShadowLineWidthHighValueField.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                contoursShadowLineWidthHighValueFieldPropertyChange(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 0);
        contoursSettingsPanel.add(contoursShadowLineWidthHighValueField, gridBagConstraints);

        contoursShadowLineWidthLowValueField.setFont(contoursShadowHighestLineWidthSlider.getFont());
        contoursShadowLineWidthLowValueField.setPreferredSize(new java.awt.Dimension(45, 28));
        {
            javax.swing.text.NumberFormatter nf = new javax.swing.text.NumberFormatter(new DecimalFormat("#0.0#"));
            nf.setMinimum(0.);
            contoursShadowLineWidthLowValueField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(nf));
        }
        contoursShadowLineWidthLowValueField.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                contoursShadowLineWidthLowValueFieldPropertyChange(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 0);
        contoursSettingsPanel.add(contoursShadowLineWidthLowValueField, gridBagConstraints);

        contoursIlluminatedLineWidthHighValueField.setFont(contoursShadowHighestLineWidthSlider.getFont());
        contoursIlluminatedLineWidthHighValueField.setPreferredSize(new java.awt.Dimension(45, 28));
        {
            javax.swing.text.NumberFormatter nf = new javax.swing.text.NumberFormatter(new DecimalFormat("#,##0.0#"));
            nf.setMinimum(0.);
            contoursIlluminatedLineWidthHighValueField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(nf));
        }
        contoursIlluminatedLineWidthHighValueField.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                contoursIlluminatedLineWidthHighValueFieldPropertyChange(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 0);
        contoursSettingsPanel.add(contoursIlluminatedLineWidthHighValueField, gridBagConstraints);

        contoursIlluminatedLineWidthLowValueField.setFont(contoursShadowHighestLineWidthSlider.getFont());
        contoursIlluminatedLineWidthLowValueField.setPreferredSize(new java.awt.Dimension(45, 28));
        {
            javax.swing.text.NumberFormatter nf = new javax.swing.text.NumberFormatter(new DecimalFormat("#0.0#"));
            nf.setMinimum(0.);
            contoursIlluminatedLineWidthLowValueField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(nf));
        }
        contoursIlluminatedLineWidthLowValueField.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                contoursIlluminatedLineWidthLowValueFieldPropertyChange(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 0);
        contoursSettingsPanel.add(contoursIlluminatedLineWidthLowValueField, gridBagConstraints);

        contoursIlluminatedColorButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                contoursIlluminatedColorButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 18;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        contoursSettingsPanel.add(contoursIlluminatedColorButton, gridBagConstraints);

        contoursShadowedColorButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                contoursShadowedColorButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 17;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        contoursSettingsPanel.add(contoursShadowedColorButton, gridBagConstraints);

        jLabel5.setText("Illuminated Line Color");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 18;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 7, 0, 0);
        contoursSettingsPanel.add(jLabel5, gridBagConstraints);

        jLabel6.setText("Shadowed Line Color");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 17;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 7, 0, 0);
        contoursSettingsPanel.add(jLabel6, gridBagConstraints);

        spacingPanel.setMinimumSize(new java.awt.Dimension(12, 10));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 5;
        contoursSettingsPanel.add(spacingPanel, gridBagConstraints);

        contoursCardPanel.add(contoursSettingsPanel, "contoursSettingsCard");

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        illuminatedContoursPanel.add(contoursCardPanel, gridBagConstraints);

        contoursPanel.add(illuminatedContoursPanel);

        tabbedPane.addTab("Contours", contoursPanel);

        add(tabbedPane);
    }// </editor-fold>//GEN-END:initComponents

    protected void updateGUI() {
        generalizationMaxLevelsSpinner.setValue(model.generalizationMaxLevels);
        generalizationDetailSlider.setValue((int) Math.round(model.getGeneralizationDetails() * 100));

        azimuthSlider.setValue(model.azimuth);
        zenithSlider.setValue(model.zenith);
        ambientLightSlider.setValue((int) Math.round(model.ambientLight * 100));

        contoursShadowLineWidthHighValueField.setValue(model.contoursShadowWidthHigh);
        contoursShadowLineWidthLowValueField.setValue(model.contoursShadowWidthLow);
        contoursIlluminatedLineWidthHighValueField.setValue(model.contoursIlluminatedWidthHigh);
        contoursIlluminatedLineWidthLowValueField.setValue(model.contoursIlluminatedWidthLow);

        contoursMinLineWidthSlider.setValue((int) Math.round(model.contoursMinWidth * 10));
        contoursMinDistanceSlider.setValue((int) Math.round(model.contoursMinDist * 10));
        contoursGradientSlider.setValue(model.contoursGradientAngle);
        contoursIntervalTextBox.setValue(model.contoursInterval);
        contoursDespeckleSlider.setValue((int) Math.round(model.contoursAspectGaussBlur * 20D));
        contoursTransitionSlider.setValue(model.contoursTransitionAngle);

        verticalExaggerationSlider.setValue(Math.round(model.shadingVerticalExaggeration * 10f));
        colorGradientSlider.setValues(model.colorRamp.colorPositions, model.colorRamp.colors);
        solidColorButton.setColor(model.solidColor);
        planObliqueSlider.setValue(model.planObliqueAngle);
        updateGeneralizationInfoLabelVisiblity();

        if (model.laplacianPyramid != null && model.laplacianPyramid.getLevels() != null) {
            localGridHighPassSlider.setMaximum(model.laplacianPyramid.getLevels().length * 10);
            // adjust slider labels
            java.util.Hashtable labels = localGridHighPassSlider.createStandardLabels(10);
            java.util.Enumeration e = labels.elements();
            while (e.hasMoreElements()) {
                javax.swing.JComponent comp = (javax.swing.JComponent) e.nextElement();
                if (comp instanceof javax.swing.JLabel) {
                    javax.swing.JLabel label = (javax.swing.JLabel) (comp);
                    String str = Integer.toString(Integer.parseInt(label.getText()) / 10);
                    label.setText(str);
                }
            }
            localGridHighPassSlider.setLabelTable(labels);
        } else {
            localGridHighPassSlider.setMajorTickSpacing(0);
        }
        localGridHighPassSlider.setValue((int) Math.round(model.getLocalGridHighPassWeight() * 10));
        localGridStandardDeviationFilterSizeSlider.setValue(model.getLocalGridStandardDeviationLevels());

        colorPopupMenu.removeAll();
        for (ColorRamp cr : model.predefinedColorRamps) {
            JMenuItem colorMenuItem = new JMenuItem(cr.name);
            colorMenuItem.addActionListener(new java.awt.event.ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    JMenuItem menuItem = (JMenuItem) evt.getSource();
                    model.selectColorRamp(menuItem.getText());
                    colorGradientSlider.setValues(model.colorRamp.colorPositions, model.colorRamp.colors);
                    updateImage(REGULAR);
                }
            });
            colorPopupMenu.add(colorMenuItem);
        }

        contoursIlluminatedColorButton.setColor(new Color(model.contoursIlluminatedColor));
        contoursShadowedColorButton.setColor(new Color(model.contoursShadowedColor));

        int exp = (int) Math.round(model.getBivariateColorRenderer().getExponentP() * 10);
        idwExponentSlider.setValue(exp);
        idwRadioButton.setSelected(model.getBivariateColorRenderer().isUseIDW());

        updateVisualizationPanelsVisibility();
        updateImage(REGULAR);

    }

    public void setModel(Model m) {
        this.model = m;
        bivariateColorPanel.setBivariateColorRenderer(model.getBivariateColorRenderer());
        bivariateColorPanel.selectFirstPoint();
        updateGUI();
    }

    private MainWindow getOwnerWindow() {
        JRootPane rootPane = getRootPane();
        if (rootPane != null) {
            JDialog dialog = (JDialog) (rootPane.getParent());
            return (MainWindow) (dialog.getOwner());
        }
        return null;
    }

    private void azimuthSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_azimuthSliderStateChanged
        model.azimuth = azimuthSlider.getValue();
        updateImage(azimuthSlider.getValueIsAdjusting() ? FAST : REGULAR);
    }//GEN-LAST:event_azimuthSliderStateChanged

    private void updateGeneralizationInfoLabelVisiblity() {
        generalizationInfoLabel.setVisible(!model.isGeneralizing());
    }

    private void generalizationDetailSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_generalizationDetailSliderStateChanged
        //don't take action until user has finished adjusting
        if (generalizationDetailSlider.getValueIsAdjusting() == false) {
            model.setGeneralizationDetails(generalizationDetailSlider.getValue() / 100d);
            //compute the summed pyramids using the original grid
            model.updateGeneralizedGrid();
            //shade, color, and redraw
            updateImage(REGULAR);
        }

        // write value to GUI
        double detailVal = generalizationDetailSlider.getValue() / 100d;
        detailVal = (detailVal + 1) / 2;
        String valString = new DecimalFormat("#%").format(detailVal);
        generalizationDetaiIsLabel.setText(valString);

        updateGeneralizationInfoLabelVisiblity();
    }//GEN-LAST:event_generalizationDetailSliderStateChanged

    private void zenithSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_zenithSliderStateChanged
        model.zenith = zenithSlider.getValue();
        updateImage(zenithSlider.getValueIsAdjusting() ? FAST : REGULAR);
    }//GEN-LAST:event_zenithSliderStateChanged

    private void generalizationMaxLevelsSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_generalizationMaxLevelsSpinnerStateChanged
        model.generalizationMaxLevels = (Integer) (generalizationMaxLevelsSpinner.getValue());
        //compute the summed pyramids using the original grid
        model.updateGeneralizedGrid();
        //shade, color, and redraw
        updateImage(REGULAR);
        updateGeneralizationInfoLabelVisiblity();
    }//GEN-LAST:event_generalizationMaxLevelsSpinnerStateChanged

    private void verticalExaggerationSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_verticalExaggerationSliderStateChanged
        model.shadingVerticalExaggeration = verticalExaggerationSlider.getValue() / 10f;
        updateImage(verticalExaggerationSlider.getValueIsAdjusting() ? FAST : REGULAR);
    }//GEN-LAST:event_verticalExaggerationSliderStateChanged

    private void colorGradientSliderPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_colorGradientSliderPropertyChange
        String propName = evt.getPropertyName();
        //Check if the changed property is either value or color for one of the thumbs
        if (MultiThumbSlider.VALUES_PROPERTY.equals(propName) || MultiThumbSlider.ADJUST_PROPERTY.equals(propName)) {
            model.colorRamp.colors = colorGradientSlider.getValues();
            model.colorRamp.colorPositions = colorGradientSlider.getThumbPositions();
            updateImage(colorGradientSlider.isValueAdjusting() ? FAST : REGULAR);
        }
        if (MultiThumbSlider.VALUES_PROPERTY.equals(propName) || MultiThumbSlider.SELECTED_THUMB_PROPERTY.equals(propName)) {
            updateColorPickerFromColorGradientSlider();
        }
    }//GEN-LAST:event_colorGradientSliderPropertyChange

    private void contoursIntervalTextBoxPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_contoursIntervalTextBoxPropertyChange
        Object contourTextVal = contoursIntervalTextBox.getValue();
        if (contourTextVal != null) {
            double d = ((Number) contourTextVal).doubleValue();
            if (d != model.contoursInterval) {
                model.contoursInterval = d;
                updateImage(REGULAR);
            }
        }
    }//GEN-LAST:event_contoursIntervalTextBoxPropertyChange

    /**
     * Adjust the value of the slider with the minimum contours line width. This
     * value is always smaller than the other width values.
     *
     * @param movingSlider The slider that is currently moved.
     */
    private void adjustContoursMinLineWidthSlider(JSlider movingSlider) {
        // temporarily remove event listener to avoid triggering a render event
        ChangeListener listener = contoursMinLineWidthSlider.getChangeListeners()[0];
        contoursMinLineWidthSlider.removeChangeListener(listener);
        int width = Math.min(movingSlider.getValue(), (int) Math.round(model.contoursMinWidth * 10));
        contoursMinLineWidthSlider.setValue(width);
        // add the event lister back to the slider
        contoursMinLineWidthSlider.addChangeListener(listener);

        // also update the model
        model.contoursMinWidth = width / 10d;
        String t = new DecimalFormat("0.0").format(model.contoursMinWidth);
        contoursMinLineWidthValueLabel.setText(t);
    }

    private void contoursIlluminatedHighestLineWidthSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_contoursIlluminatedHighestLineWidthSliderStateChanged
        double val = contoursIlluminatedHighestLineWidthSlider.getValue() / 10.d;
        contoursIlluminatedLineWidthHighValueField.setValue(val);
        if (!contoursIlluminatedHighestLineWidthSlider.getValueIsAdjusting()) {
            updateImage(REGULAR);
        }
    }//GEN-LAST:event_contoursIlluminatedHighestLineWidthSliderStateChanged

    private void contoursShadowHighestLineWidthSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_contoursShadowHighestLineWidthSliderStateChanged
        double val = contoursShadowHighestLineWidthSlider.getValue() / 10.d;
        contoursShadowLineWidthHighValueField.setValue(val);
        if (!contoursShadowHighestLineWidthSlider.getValueIsAdjusting()) {
            updateImage(REGULAR);
        }
    }//GEN-LAST:event_contoursShadowHighestLineWidthSliderStateChanged

    private static void setSliderValueWithoutTriggeringEvent(JSlider slider, int value) {
        // temporarily remove event listeners
        ChangeListener[] listeners = slider.getChangeListeners();
        for (ChangeListener l : listeners) {
            slider.removeChangeListener(l);
        }
        slider.setValue(value);
        // add the event lister back to the slider
        for (ChangeListener l : listeners) {
            slider.addChangeListener(l);
        }
    }

    private static void setFieldValueWithoutTriggeringEvent(JFormattedTextField field, double value) {
        // temporarily remove event listeners
        PropertyChangeListener[] listeners = field.getPropertyChangeListeners();
        for (PropertyChangeListener l : listeners) {
            field.removePropertyChangeListener(l);
        }
        field.setValue(value);
        // add the event listeners back to the slider
        for (PropertyChangeListener l : listeners) {
            field.addPropertyChangeListener(l);
        }
    }

    private void contoursMinLineWidthSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_contoursMinLineWidthSliderStateChanged
        double v;
        int sliderVal;

        model.contoursMinWidth = contoursMinLineWidthSlider.getValue() / 10.;

        v = ((Number) contoursShadowLineWidthHighValueField.getValue()).doubleValue();
        model.contoursShadowWidthHigh = Math.max(v, model.contoursMinWidth);
        sliderVal = (int) Math.round(model.contoursShadowWidthHigh * 10);
        setSliderValueWithoutTriggeringEvent(contoursShadowHighestLineWidthSlider, sliderVal);

        v = ((Number) contoursShadowLineWidthLowValueField.getValue()).doubleValue();
        model.contoursShadowWidthLow = Math.max(v, model.contoursMinWidth);
        sliderVal = (int) Math.round(model.contoursShadowWidthLow * 10);
        setSliderValueWithoutTriggeringEvent(contoursShadowLowestLineWidthSlider, sliderVal);

        v = ((Number) contoursIlluminatedLineWidthHighValueField.getValue()).doubleValue();
        model.contoursIlluminatedWidthHigh = Math.max(v, model.contoursMinWidth);
        sliderVal = (int) Math.round(model.contoursIlluminatedWidthHigh * 10);
        setSliderValueWithoutTriggeringEvent(contoursIlluminatedHighestLineWidthSlider, sliderVal);

        v = ((Number) contoursIlluminatedLineWidthLowValueField.getValue()).doubleValue();
        model.contoursIlluminatedWidthLow = Math.max(v, model.contoursMinWidth);
        sliderVal = (int) Math.round(model.contoursIlluminatedWidthLow * 10);
        setSliderValueWithoutTriggeringEvent(contoursIlluminatedLowestLineWidthSlider, sliderVal);

        // update text fields
        setFieldValueWithoutTriggeringEvent(contoursShadowLineWidthHighValueField, model.contoursShadowWidthHigh);
        setFieldValueWithoutTriggeringEvent(contoursShadowLineWidthLowValueField, model.contoursShadowWidthLow);
        setFieldValueWithoutTriggeringEvent(contoursIlluminatedLineWidthHighValueField, model.contoursIlluminatedWidthHigh);
        setFieldValueWithoutTriggeringEvent(contoursIlluminatedLineWidthLowValueField, model.contoursIlluminatedWidthLow);

        // update map
        updateImage(contoursMinLineWidthSlider.getValueIsAdjusting() ? FAST : REGULAR);

        DecimalFormat df = new DecimalFormat("0.0");
        String t = df.format(model.contoursMinWidth);
        contoursMinLineWidthValueLabel.setText(t);
    }//GEN-LAST:event_contoursMinLineWidthSliderStateChanged

    private void contoursGradientSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_contoursGradientSliderStateChanged
        model.contoursGradientAngle = contoursGradientSlider.getValue();
        updateImage(contoursGradientSlider.getValueIsAdjusting() ? FAST : REGULAR);
    }//GEN-LAST:event_contoursGradientSliderStateChanged

    private void solidColorButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_solidColorButtonActionPerformed
        model.solidColor = solidColorButton.getColor();
        updateImage(REGULAR);
    }//GEN-LAST:event_solidColorButtonActionPerformed

    private void updateContoursGUI(boolean illuminated) {
        contoursIlluminatedHighestLineWidthSlider.setEnabled(illuminated);
        contoursIlluminatedLowestLineWidthSlider.setEnabled(illuminated);
        contoursTransitionSlider.setEnabled(illuminated);
        contoursGradientSlider.setEnabled(illuminated);
        contoursIlluminatedColorButton.setEnabled(illuminated);
    }

    private void contoursComboBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_contoursComboBoxItemStateChanged
        if (evt.getStateChange() == ItemEvent.SELECTED) {
            switch (contoursComboBox.getSelectedIndex()) {
                case 0:
                    model.foregroundVisualization = ForegroundVisualization.NONE;
                    ((CardLayout) (contoursCardPanel.getLayout())).show(contoursCardPanel, "emptyCard");
                    break;
                case 1:
                    model.foregroundVisualization = ForegroundVisualization.ILLUMINATED_CONTOURS;
                    ((CardLayout) (contoursCardPanel.getLayout())).show(contoursCardPanel, "contoursSettingsCard");
                    updateContoursGUI(true);
                    break;
                case 2:
                    model.foregroundVisualization = ForegroundVisualization.SHADED_CONTOURS;
                    ((CardLayout) (contoursCardPanel.getLayout())).show(contoursCardPanel, "contoursSettingsCard");
                    updateContoursGUI(false);
                    break;
            }
            updateVisualizationPanelsVisibility();
            updateImage(REGULAR);
        }

    }//GEN-LAST:event_contoursComboBoxItemStateChanged

    private void visualizationComboBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_visualizationComboBoxItemStateChanged
        if (evt.getStateChange() == ItemEvent.SELECTED) {
            model.backgroundVisualization = (ColorVisualization) visualizationComboBox.getSelectedItem();
            updateVisualizationPanelsVisibility();
            updateImage(REGULAR);
        }
    }//GEN-LAST:event_visualizationComboBoxItemStateChanged

    private void localGridStandardDeviationFilterSizeSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_localGridStandardDeviationFilterSizeSliderStateChanged
        if (localGridStandardDeviationFilterSizeSlider.getValueIsAdjusting() == false) {
            int levels = localGridStandardDeviationFilterSizeSlider.getValue();
            model.setLocalGridStandardDeviationLevels(levels);
            updateImage(REGULAR);
        }
    }//GEN-LAST:event_localGridStandardDeviationFilterSizeSliderStateChanged

    private void localGridHighPassSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_localGridHighPassSliderStateChanged
        if (localGridHighPassSlider.getValueIsAdjusting() == false) {
            int filterSize = localGridHighPassSlider.getValue();
            model.setLocalGridHighPassWeight(filterSize / 10d);
            updateImage(REGULAR);
        }
    }//GEN-LAST:event_localGridHighPassSliderStateChanged

    private void contoursDespeckleSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_contoursDespeckleSliderStateChanged
        model.contoursAspectGaussBlur = contoursDespeckleSlider.getValue() / 20D;
        updateImage(contoursDespeckleSlider.getValueIsAdjusting() ? FAST : REGULAR);
    }//GEN-LAST:event_contoursDespeckleSliderStateChanged

    private void contoursTransitionSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_contoursTransitionSliderStateChanged
        model.contoursTransitionAngle = contoursTransitionSlider.getValue();
        updateImage(contoursTransitionSlider.getValueIsAdjusting() ? FAST : REGULAR);
    }//GEN-LAST:event_contoursTransitionSliderStateChanged

    private void contoursBlankBackgroundButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_contoursBlankBackgroundButtonActionPerformed
        model.backgroundVisualization = ColorVisualization.CONTINUOUS;
        visualizationComboBox.setSelectedIndex(ColorVisualization.CONTINUOUS.ordinal());
        updateVisualizationPanelsVisibility();
        updateImage(REGULAR);
    }//GEN-LAST:event_contoursBlankBackgroundButtonActionPerformed

    /**
     * Call this after a slider for adjusting contour line width on illuminated
     * or shadowed slopes has been adjusted.
     *
     * @param masterSlider The dragged slider
     * @param slaveSlider The slider linked to the master slider.
     * @param masterLabel The label for the master slider.
     * @param slaveLabel The label for the slave slider.
     * @param locked True if the slave slider is linked to the master slider.
     */
    private void contoursWidthSliderStateChanged(JSlider masterSlider,
            JSlider slaveSlider, JLabel masterLabel, JLabel slaveLabel, boolean locked) {
        double w = masterSlider.getValue() / 10.f;
        adjustContoursMinLineWidthSlider(masterSlider);
        updateImage(masterSlider.getValueIsAdjusting() ? FAST : REGULAR);
        String t = new DecimalFormat("0.0").format(w);
        masterLabel.setText(t);
        if (locked) {
            setSliderValueWithoutTriggeringEvent(slaveSlider, masterSlider.getValue());
            slaveLabel.setText(t);
        }
    }
    private void contoursIlluminatedLowestLineWidthSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_contoursIlluminatedLowestLineWidthSliderStateChanged
        double val = contoursIlluminatedLowestLineWidthSlider.getValue() / 10.d;
        contoursIlluminatedLineWidthLowValueField.setValue(val);
        if (!contoursIlluminatedLowestLineWidthSlider.getValueIsAdjusting()) {
            updateImage(REGULAR);
        }
    }//GEN-LAST:event_contoursIlluminatedLowestLineWidthSliderStateChanged

    private void contoursIlluminatedLockedToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_contoursIlluminatedLockedToggleButtonActionPerformed
        if (contoursIlluminatedLockedToggleButton.isSelected()) {
            int val = contoursIlluminatedHighestLineWidthSlider.getValue();
            contoursIlluminatedLowestLineWidthSlider.setValue(val);
        }
    }//GEN-LAST:event_contoursIlluminatedLockedToggleButtonActionPerformed

    private void contoursShadowedLockedToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_contoursShadowedLockedToggleButtonActionPerformed
        if (contoursShadowedLockedToggleButton.isSelected()) {
            int val = contoursShadowHighestLineWidthSlider.getValue();
            contoursShadowLowestLineWidthSlider.setValue(val);
        }
    }//GEN-LAST:event_contoursShadowedLockedToggleButtonActionPerformed

    private void contoursShadowLowestLineWidthSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_contoursShadowLowestLineWidthSliderStateChanged
        double val = contoursShadowLowestLineWidthSlider.getValue() / 10.d;
        contoursShadowLineWidthLowValueField.setValue(val);
        if (!contoursShadowLowestLineWidthSlider.getValueIsAdjusting()) {
            updateImage(REGULAR);
        }
    }//GEN-LAST:event_contoursShadowLowestLineWidthSliderStateChanged

    private void planObliqueSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_planObliqueSliderStateChanged
        model.planObliqueAngle = planObliqueSlider.getValue();
        updateImage(planObliqueSlider.getValueIsAdjusting() ? FAST : REGULAR);
    }//GEN-LAST:event_planObliqueSliderStateChanged

    private void contoursMinDistanceSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_contoursMinDistanceSliderStateChanged
        model.contoursMinDist = contoursMinDistanceSlider.getValue() / 10.;
        updateImage(contoursMinDistanceSlider.getValueIsAdjusting() ? FAST : REGULAR);
        DecimalFormat df = new DecimalFormat("0.0");
        String t = df.format(model.contoursMinDist);
        contoursMinDistanceValueLabel.setText(t);
    }//GEN-LAST:event_contoursMinDistanceSliderStateChanged

    private void adjustMinWidth(double val) {
        model.contoursMinWidth = Math.min(val, model.contoursMinWidth);
        int sliderVal = (int) Math.round(model.contoursMinWidth * 10f);
        setSliderValueWithoutTriggeringEvent(contoursMinLineWidthSlider, sliderVal);
        String t = new DecimalFormat("0.0").format(model.contoursMinWidth);
        contoursMinLineWidthValueLabel.setText(t);
    }

    private void contoursShadowLineWidthHighValueFieldPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_contoursShadowLineWidthHighValueFieldPropertyChange
        if ("value".equals(evt.getPropertyName()) == false) {
            return;
        }
        model.contoursShadowWidthHigh = ((Number) contoursShadowLineWidthHighValueField.getValue()).doubleValue();
        int sliderVal = (int) Math.round(model.contoursShadowWidthHigh * 10);
        setSliderValueWithoutTriggeringEvent(contoursShadowHighestLineWidthSlider, sliderVal);
        boolean locked = contoursShadowedLockedToggleButton.isSelected();
        if (locked) {
            setSliderValueWithoutTriggeringEvent(contoursShadowLowestLineWidthSlider, sliderVal);
            model.contoursShadowWidthLow = model.contoursShadowWidthHigh;
            setFieldValueWithoutTriggeringEvent(contoursShadowLineWidthLowValueField, model.contoursShadowWidthLow);
        }
        adjustMinWidth(model.contoursShadowWidthHigh);
        updateImage(contoursShadowHighestLineWidthSlider.getValueIsAdjusting() ? FAST : REGULAR);
    }//GEN-LAST:event_contoursShadowLineWidthHighValueFieldPropertyChange

    private void contoursShadowLineWidthLowValueFieldPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_contoursShadowLineWidthLowValueFieldPropertyChange
        if ("value".equals(evt.getPropertyName()) == false) {
            return;
        }
        model.contoursShadowWidthLow = ((Number) contoursShadowLineWidthLowValueField.getValue()).doubleValue();
        int sliderVal = (int) Math.round(model.contoursShadowWidthLow * 10);
        setSliderValueWithoutTriggeringEvent(contoursShadowLowestLineWidthSlider, sliderVal);
        boolean locked = contoursShadowedLockedToggleButton.isSelected();
        if (locked) {
            setSliderValueWithoutTriggeringEvent(contoursShadowHighestLineWidthSlider, sliderVal);
            model.contoursShadowWidthHigh = model.contoursShadowWidthLow;
            setFieldValueWithoutTriggeringEvent(contoursShadowLineWidthHighValueField, model.contoursShadowWidthHigh);
        }
        adjustMinWidth(model.contoursShadowWidthLow);
        updateImage(contoursShadowLowestLineWidthSlider.getValueIsAdjusting() ? FAST : REGULAR);
    }//GEN-LAST:event_contoursShadowLineWidthLowValueFieldPropertyChange

    private void contoursIlluminatedLineWidthHighValueFieldPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_contoursIlluminatedLineWidthHighValueFieldPropertyChange
        if ("value".equals(evt.getPropertyName()) == false) {
            return;
        }
        model.contoursIlluminatedWidthHigh = ((Number) contoursIlluminatedLineWidthHighValueField.getValue()).doubleValue();
        int sliderVal = (int) Math.round(model.contoursIlluminatedWidthHigh * 10);
        setSliderValueWithoutTriggeringEvent(contoursIlluminatedHighestLineWidthSlider, sliderVal);
        boolean locked = contoursIlluminatedLockedToggleButton.isSelected();
        if (locked) {
            setSliderValueWithoutTriggeringEvent(contoursIlluminatedLowestLineWidthSlider, sliderVal);
            model.contoursIlluminatedWidthLow = model.contoursIlluminatedWidthHigh;
            setFieldValueWithoutTriggeringEvent(contoursIlluminatedLineWidthLowValueField, model.contoursIlluminatedWidthLow);
        }
        adjustMinWidth(model.contoursIlluminatedWidthHigh);
        updateImage(contoursIlluminatedHighestLineWidthSlider.getValueIsAdjusting() ? FAST : REGULAR);
    }//GEN-LAST:event_contoursIlluminatedLineWidthHighValueFieldPropertyChange

    private void contoursIlluminatedLineWidthLowValueFieldPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_contoursIlluminatedLineWidthLowValueFieldPropertyChange
        if ("value".equals(evt.getPropertyName()) == false) {
            return;
        }
        model.contoursIlluminatedWidthLow = ((Number) contoursIlluminatedLineWidthLowValueField.getValue()).doubleValue();
        int sliderVal = (int) Math.round(model.contoursIlluminatedWidthLow * 10);
        setSliderValueWithoutTriggeringEvent(contoursIlluminatedLowestLineWidthSlider, sliderVal);
        boolean locked = contoursIlluminatedLockedToggleButton.isSelected();
        if (locked) {
            setSliderValueWithoutTriggeringEvent(contoursIlluminatedHighestLineWidthSlider, sliderVal);
            model.contoursIlluminatedWidthHigh = model.contoursIlluminatedWidthLow;
            setFieldValueWithoutTriggeringEvent(contoursIlluminatedLineWidthHighValueField, model.contoursIlluminatedWidthHigh);
        }
        adjustMinWidth(model.contoursIlluminatedWidthLow);
        updateImage(contoursIlluminatedLowestLineWidthSlider.getValueIsAdjusting() ? FAST : REGULAR);
    }//GEN-LAST:event_contoursIlluminatedLineWidthLowValueFieldPropertyChange

    private void contoursIlluminatedColorButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_contoursIlluminatedColorButtonActionPerformed
        Color color = contoursIlluminatedColorButton.getColor();
        model.contoursIlluminatedColor = color.getRGB();
        updateImage(REGULAR);
    }//GEN-LAST:event_contoursIlluminatedColorButtonActionPerformed

    private void contoursShadowedColorButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_contoursShadowedColorButtonActionPerformed
        Color color = contoursShadowedColorButton.getColor();
        model.contoursShadowedColor = color.getRGB();
        updateImage(REGULAR);
    }//GEN-LAST:event_contoursShadowedColorButtonActionPerformed

    private void ambientLightSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_ambientLightSliderStateChanged
        model.ambientLight = ambientLightSlider.getValue() / 100d;
        updateImage(ambientLightSlider.getValueIsAdjusting() ? FAST : REGULAR);
    }//GEN-LAST:event_ambientLightSliderStateChanged

    private void idwRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_idwRadioButtonActionPerformed
        model.getBivariateColorRenderer().setUseIDW(idwRadioButton.isSelected());
        updateImage(REGULAR);
        bivariateColorGroupPanel.repaint();
    }//GEN-LAST:event_idwRadioButtonActionPerformed

    private void gaussRadioButtonidwRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gaussRadioButtonidwRadioButtonActionPerformed
        model.getBivariateColorRenderer().setUseIDW(idwRadioButton.isSelected());
        updateImage(REGULAR);
        bivariateColorGroupPanel.repaint();
    }//GEN-LAST:event_gaussRadioButtonidwRadioButtonActionPerformed

    private void idwExponentSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_idwExponentSliderStateChanged
        double exp = idwExponentSlider.getValue() / 10d;
        model.getBivariateColorRenderer().setExponentP(exp);
        updateImage(idwExponentSlider.getValueIsAdjusting() ? FAST : REGULAR);
        bivariateColorExponentValueLabel.setText(Double.toString(exp));
        bivariateColorGroupPanel.repaint();
    }//GEN-LAST:event_idwExponentSliderStateChanged

    private void bivariateColorPanelPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_bivariateColorPanelPropertyChange
        if ("selectedPoint".equals(evt.getPropertyName())) {
            updateColorPickerFromBivariatePanel();
        }

        // color changed or point was moved
        if ("colorChanged".equals(evt.getPropertyName()) || "colorDeleted".equals(evt.getPropertyName())) {
            if (bivariateColorPanel.isValueAdjusting() == false || "colorDeleted".equals(evt.getPropertyName())) {
                updateImage(REGULAR);
            }
        }
    }//GEN-LAST:event_bivariateColorPanelPropertyChange

    private void colorPickerPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_colorPickerPropertyChange
        if ("selected color".equals(evt.getPropertyName()) && model != null) {
            if (model.backgroundVisualization.isBivariate()) {
                bivariateColorPanel.setSelectedColor(colorPicker.getColor());
                model.getBivariateColorRenderer().colorPointsChanged();
            } else {
                int selectedThumbID = colorGradientSlider.getSelectedThumb(false);
                Color[] colors = colorGradientSlider.getValues();
                if (selectedThumbID >= 0 && selectedThumbID < colors.length) {
                    colors[selectedThumbID] = colorPicker.getColor();
                    float[] thumbPositions = colorGradientSlider.getThumbPositions();
                    colorGradientSlider.setValues(thumbPositions, colors);
                }
            }
            updateImage(REGULAR);
        }
    }//GEN-LAST:event_colorPickerPropertyChange

    /**
     * Open a grid file.
     *
     * @param filePath The file to open
     * @throws IOException
     */
    private void openGridForBivariateColor(final String filePath, final boolean horizontalGrid) throws IOException {
        SwingWorkerWithProgressIndicatorDialog worker;
        String dialogTitle = "Pyramid Shader";

        worker = new SwingWorkerWithProgressIndicatorDialog<Void>(null, dialogTitle, "", true) {

            @Override
            public void done() {
                try {
                    // a call to get() will throw an ExecutionException if an 
                    // exception occured in doInBackground
                    get();

                    // hide the progress dialog before rendering the image
                    // if rendering throws an error, the progress dialog should 
                    // have been closed
                    completeProgress();

                    String fileName = FileUtils.getFileNameWithoutExtension(filePath);
                    if (horizontalGrid) {
                        bivariateHorizontalLabel.setText(fileName);
                    } else {
                        bivariateVerticalLabel.setText(fileName);
                    }
                    updateImage(REGULAR);
                    bivariateColorPanel.repaint();
                } catch (InterruptedException | CancellationException e) {

                } catch (Throwable e) {
                    // hide the progress dialog
                    completeProgress();
                    // an exception was thrown in doInBackground
                    String msg = "<html>An error occured when importing the terrain model."
                            + "<br>The file must be in Esri ASCII Grid format.</html>";
                    ErrorDialog.showErrorDialog(msg, "Error", null, null);
                } finally {
                    // hide the progress dialog
                    completeProgress();
                }
            }

            @Override
            protected Void doInBackground() throws Exception {
                start();
                //import the DEM and create pyramids
                Grid grid = EsriASCIIGridReader.read(filePath, this);
                this.setIndeterminate(true);
                this.setCancellable(false);
                if (horizontalGrid) {
                    model.getBivariateColorRenderer().setAttribute1Grid(grid);
                } else {
                    model.getBivariateColorRenderer().setAttribute2Grid(grid);
                }
                return null;
            }
        };

        worker.setMaxTimeWithoutDialogMilliseconds(1000);
        worker.setIndeterminate(false);
        worker.setMessage("Importing Terrain Model");
        worker.execute();
    }

    private void bivariateHorizontalButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bivariateHorizontalButtonActionPerformed
        try {
            // ask the user for a file
            String filePath = FileUtils.askFile(null, "Select an Esri ASCII Grid", true);
            if (filePath != null) {
                openGridForBivariateColor(filePath, true);
            }
        } catch (IOException ex) {
            ErrorDialog.showErrorDialog("Could not open grid file.", "Error", ex, this);
        }
    }//GEN-LAST:event_bivariateHorizontalButtonActionPerformed

    private void bivariateVerticalButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bivariateVerticalButtonActionPerformed
        try {
            // ask the user for a file
            String filePath = FileUtils.askFile(null, "Select an Esri ASCII Grid", true);
            if (filePath != null) {
                openGridForBivariateColor(filePath, false);
            }
        } catch (IOException ex) {
            ErrorDialog.showErrorDialog("Could not open grid file.", "Error", ex, this);
        }
    }//GEN-LAST:event_bivariateVerticalButtonActionPerformed

    /**
     * user right-clicked on the map. Use the clicked location to add a color point
     * to the bivariate color panel
     * @param xPerc
     * @param yPerc 
     */
    public void mouseRightClicked(double xPerc, double yPerc) {
        if (model.getBivariateColorRenderer().hasGrids() == false
                || model.backgroundVisualization.isBivariate() == false) {
            return;
        }
        BivariateColorRenderer bivariateRenderer = model.getBivariateColorRenderer();
        Point lutPt = bivariateRenderer.getLUTCoordinates(xPerc, yPerc);
        BivariateColorPoint pt = new BivariateColorPoint();
        int rgb = bivariateRenderer.getLUTColor(lutPt.x, lutPt.y);
        Color color = new Color(rgb);
        pt.setColor(color);
        pt.setAttribute1((double) lutPt.x / BivariateColorRenderer.LUT_SIZE);
        pt.setAttribute2(1 - (double) lutPt.y / BivariateColorRenderer.LUT_SIZE);
        bivariateRenderer.addPoint(pt);
        bivariateRenderer.colorPointsChanged();
        bivariateColorPanel.selectPoint(pt);
        updateImage(RenderSpeed.REGULAR);
        bivariateColorPanel.repaint();
    }

    /**
     * Mouse pointer was moved over the map. Extract LUT location for the passed
     * location.
     * @param xPerc Horizontal pointer location in percentage.
     * @param yPerc Vertical pointer location in percentage, from top to bottom.
     */
    public void mouseMoved(double xPerc, double yPerc) {
        if (model.getBivariateColorRenderer().hasGrids() == false
                || model.backgroundVisualization.isBivariate() == false) {
            return;
        }
        Point pt = model.getBivariateColorRenderer().getLUTCoordinates(xPerc, yPerc);
        if (pt != null) {
            xPerc = 100d * pt.x / BivariateColorRenderer.LUT_SIZE;
            yPerc = 100d * pt.y / BivariateColorRenderer.LUT_SIZE;
        } else {
            xPerc = -1;
            yPerc = -1;
        }
        bivariateColorPanel.setCrossPerc(xPerc, yPerc);
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JSlider ambientLightSlider;
    private javax.swing.JSlider azimuthSlider;
    private javax.swing.JLabel bivariateColorExponentValueLabel;
    private javax.swing.JPanel bivariateColorGroupPanel;
    private edu.oregonstate.cartography.gui.bivariate.BivariateColorPanel bivariateColorPanel;
    private javax.swing.JButton bivariateHorizontalButton;
    private javax.swing.JLabel bivariateHorizontalLabel;
    private javax.swing.ButtonGroup bivariateInterpolationButtonGroup;
    private javax.swing.JButton bivariateVerticalButton;
    private edu.oregonstate.cartography.gui.RotatedLabel bivariateVerticalLabel;
    private javax.swing.JPanel colorGradientPanel;
    private com.bric.swing.GradientSlider colorGradientSlider;
    private com.bric.swing.ColorPicker colorPicker;
    private javax.swing.JPopupMenu colorPopupMenu;
    private edu.oregonstate.cartography.gui.MenuToggleButton colorPresetsButton;
    private javax.swing.JButton contoursBlankBackgroundButton;
    private javax.swing.JPanel contoursCardPanel;
    private javax.swing.JComboBox contoursComboBox;
    private javax.swing.JSlider contoursDespeckleSlider;
    private javax.swing.JPanel contoursEmptyPanel;
    private javax.swing.JSlider contoursGradientSlider;
    private edu.oregonstate.cartography.gui.ColorButton contoursIlluminatedColorButton;
    private javax.swing.JSlider contoursIlluminatedHighestLineWidthSlider;
    private javax.swing.JFormattedTextField contoursIlluminatedLineWidthHighValueField;
    private javax.swing.JFormattedTextField contoursIlluminatedLineWidthLowValueField;
    private javax.swing.JToggleButton contoursIlluminatedLockedToggleButton;
    private javax.swing.JSlider contoursIlluminatedLowestLineWidthSlider;
    private javax.swing.JFormattedTextField contoursIntervalTextBox;
    private javax.swing.JLabel contoursMinDistanceLabel;
    private javax.swing.JSlider contoursMinDistanceSlider;
    private javax.swing.JLabel contoursMinDistanceValueLabel;
    private javax.swing.JSlider contoursMinLineWidthSlider;
    private javax.swing.JLabel contoursMinLineWidthValueLabel;
    private javax.swing.JPanel contoursPanel;
    private javax.swing.JPanel contoursSettingsPanel;
    private javax.swing.JSlider contoursShadowHighestLineWidthSlider;
    private javax.swing.JFormattedTextField contoursShadowLineWidthHighValueField;
    private javax.swing.JFormattedTextField contoursShadowLineWidthLowValueField;
    private javax.swing.JSlider contoursShadowLowestLineWidthSlider;
    private edu.oregonstate.cartography.gui.ColorButton contoursShadowedColorButton;
    private javax.swing.JToggleButton contoursShadowedLockedToggleButton;
    private javax.swing.JSlider contoursTransitionSlider;
    private javax.swing.JRadioButton gaussRadioButton;
    private javax.swing.JLabel generalizationDetaiIsLabel;
    private javax.swing.JSlider generalizationDetailSlider;
    private javax.swing.JLabel generalizationInfoLabel;
    private javax.swing.JSpinner generalizationMaxLevelsSpinner;
    private javax.swing.JPanel generalizationPanel;
    private javax.swing.JSlider idwExponentSlider;
    private javax.swing.JRadioButton idwRadioButton;
    private javax.swing.JPanel illuminatedContoursPanel;
    private javax.swing.JPanel illuminationPanel;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JSlider localGridHighPassSlider;
    private javax.swing.JSlider localGridStandardDeviationFilterSizeSlider;
    private javax.swing.JPanel localHypsoPanel;
    private javax.swing.JPanel planObliquePanel;
    private javax.swing.JSlider planObliqueSlider;
    private edu.oregonstate.cartography.gui.ColorButton solidColorButton;
    private javax.swing.JPanel solidColorPanel;
    private javax.swing.JPanel spacingPanel;
    private javax.swing.JTabbedPane tabbedPane;
    private javax.swing.JLabel verticalExaggerationLabel;
    private javax.swing.JPanel verticalExaggerationPanel;
    private javax.swing.JSlider verticalExaggerationSlider;
    private javax.swing.JComboBox visualizationComboBox;
    private javax.swing.JPanel visualizationPanel;
    private javax.swing.JSlider zenithSlider;
    // End of variables declaration//GEN-END:variables
}
