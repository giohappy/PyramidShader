package edu.oregonstate.cartography.gui;

import com.bric.swing.MultiThumbSlider;
import edu.oregonstate.cartography.app.ImageUtils;
import edu.oregonstate.cartography.grid.Model;
import edu.oregonstate.cartography.grid.Model.ColorRamp;
import edu.oregonstate.cartography.grid.Model.ForegroundVisualization;
import edu.oregonstate.cartography.grid.operators.ColorizerOperator;
import edu.oregonstate.cartography.grid.operators.ColorizerOperator.ColorVisualization;
import static edu.oregonstate.cartography.gui.SettingsPanel.RenderSpeed.FAST;
import static edu.oregonstate.cartography.gui.SettingsPanel.RenderSpeed.REGULAR;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ItemEvent;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JRootPane;
import javax.swing.JSlider;
import javax.swing.SwingWorker;
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

    // A SwingWorker for rendering the image when no progress dialog is required.
    class BackgroundRenderer extends SwingWorker<Void, Object> {

        private final BufferedImage backgroundImage;
        private final BufferedImage foregroundImage;

        protected BackgroundRenderer(BufferedImage backgroundImage,
                BufferedImage foregroundImage) {
            this.backgroundImage = backgroundImage;
            this.foregroundImage = foregroundImage;
        }

        @Override
        public Void doInBackground() {
            if (model.getGeneralizedGrid() != null) {
                model.renderBackgroundImage(backgroundImage);

                // directly render to backround image if foreground and background
                // images are the same size. This avoids a copy operation to
                // combine the two images.
                BufferedImage img = backgroundImage != foregroundImage ? foregroundImage : backgroundImage;
                model.renderForegroundImage(img);
            }
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
                    int w = displayImage.getWidth();
                    int h = displayImage.getHeight();
                    g = displayImage.getGraphics();

                    // erase previous image
                    g.setColor(Color.WHITE);
                    g.fillRect(0, 0, displayImage.getWidth(), displayImage.getHeight());

                    // copy background image into the display image
                    g.drawImage(backgroundImage, 0, 0, w, h, null);

                    // copy foreground image into the display image if required.
                    if (backgroundImage != foregroundImage) {
                        BufferedImage img = ImageUtils.getScaledInstance(foregroundImage,
                                displayImage.getWidth(), displayImage.getHeight(),
                                RenderingHints.VALUE_INTERPOLATION_BILINEAR, true);
                        g.drawImage(img, 0, 0, w, h, null);
                    }
                }
            } catch (Exception ignore) {
            } finally {
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
    private BackgroundRenderer renderer;

    public SettingsPanel() {
        initComponents();
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

        if (model != null && model.backgroundVisualization != null) {
            isShading = model.backgroundVisualization.isShading();
            isColored = model.backgroundVisualization.isColored();
            isLocal = model.backgroundVisualization.isLocal();
            isSolidColor = model.backgroundVisualization == ColorVisualization.CONTINUOUS;
        }

        verticalExaggerationPanel.setVisible(isShading);
        colorGradientPanel.setVisible(isColored);
        localHypsoPanel.setVisible(isLocal);
        solidColorPanel.setVisible(isSolidColor);

        // adjust size of dialog to make sure all components are visible
        JRootPane rootPane = getRootPane();
        if (rootPane != null) {
            ((JDialog) (rootPane.getParent())).pack();
        }

        contoursBlankBackgroundButton.setEnabled(!isSolidColor);
    }

    public void updateImage(RenderSpeed renderSpeed) {
        try {
            MainWindow mainWindow = getOwnerWindow();
            if (mainWindow == null || model == null) {
                return;
            }

            // if we are currently rendering an image, first cancel the current rendering
            if (renderer != null && !renderer.isDone()) {
                // FIXME
                //renderer.cancel(false);
            }

            // block the event dispatching thread until the BackgroundRenderer worker thread
            // is done. This is to avoid that two BackgroundRenderer threads write to 
            // the same image.
            try {
                if (renderer != null) {
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
            renderer = new BackgroundRenderer(backgroundImage, foregroundImage);
            renderer.execute();
        } catch (Throwable e) {
            String msg = "<html>An error occured when rendering the terrain.</html>";
            String title = "Error";
            ErrorDialog.showErrorDialog(msg, title, e, null);
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
        localGridLowPassSlider = new javax.swing.JSlider();
        javax.swing.JLabel jLabel3 = new javax.swing.JLabel();
        solidColorPanel = new TransparentMacPanel();
        solidColorButton = new edu.oregonstate.cartography.gui.ColorButton();
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
        javax.swing.JLabel zeLabel = new javax.swing.JLabel();
        azimuthSlider = new javax.swing.JSlider();
        zenithSlider = new javax.swing.JSlider();
        javax.swing.JLabel azLabel = new javax.swing.JLabel();
        contoursPanel = new TransparentMacPanel();
        illuminatedContoursPanel = new TransparentMacPanel();
        contoursComboBox = new javax.swing.JComboBox();
        contoursCardPanel = new TransparentMacPanel();
        contoursEmptyPanel = new TransparentMacPanel();
        contoursSettingsPanel = new TransparentMacPanel();
        javax.swing.JLabel contoursIlluminatedLineWidthLabel = new javax.swing.JLabel();
        javax.swing.JLabel contoursIlluminatedHighestLabel = new javax.swing.JLabel();
        contoursIlluminatedHighestLineWidthSlider = new javax.swing.JSlider();
        contoursIlluminatedLineWidthHighValueLabel = new javax.swing.JLabel();
        contoursIlluminatedLockedToggleButton = new javax.swing.JToggleButton();
        javax.swing.JLabel contoursIlluminatedLowestLabel = new javax.swing.JLabel();
        contoursIlluminatedLowestLineWidthSlider = new javax.swing.JSlider();
        contoursIlluminatedLineWidthLowValueLabel = new javax.swing.JLabel();
        javax.swing.JLabel contoursShadwoLineWidthSlider = new javax.swing.JLabel();
        javax.swing.JLabel contoursShadowedHighest = new javax.swing.JLabel();
        contoursShadowHighestLineWidthSlider = new javax.swing.JSlider();
        contoursShadowLineWidthHighValueLabel = new javax.swing.JLabel();
        contoursShadowedLockedToggleButton = new javax.swing.JToggleButton();
        javax.swing.JLabel contoursShadowedLowest = new javax.swing.JLabel();
        contoursShadowLowestLineWidthSlider = new javax.swing.JSlider();
        contoursShadowLineWidthLowValueLabel = new javax.swing.JLabel();
        javax.swing.JLabel contoursMinLineWidthLabel = new javax.swing.JLabel();
        contoursMinLineWidthSlider = new javax.swing.JSlider();
        contoursMinLineWidthValueLabel = new javax.swing.JLabel();
        javax.swing.JLabel contoursWidthInfoLabel = new javax.swing.JLabel();
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

        colorPopupMenu.setLightWeightPopupEnabled(false);

        setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 2, 2));

        visualizationPanel.setLayout(new java.awt.GridBagLayout());

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
        colorInfoLabel.setText("Click on slider to add color; double-click on triangles to change.");
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
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        localHypsoPanel.add(jLabel2, gridBagConstraints);

        localGridStandardDeviationFilterSizeSlider.setMajorTickSpacing(1);
        localGridStandardDeviationFilterSizeSlider.setMaximum(10);
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
        localHypsoPanel.add(localGridStandardDeviationFilterSizeSlider, gridBagConstraints);

        localGridLowPassSlider.setMaximum(99);
        localGridLowPassSlider.setMinimum(3);
        localGridLowPassSlider.setMinorTickSpacing(2);
        localGridLowPassSlider.setPaintLabels(true);
        localGridLowPassSlider.setPaintTicks(true);
        localGridLowPassSlider.setSnapToTicks(true);
        localGridLowPassSlider.setPreferredSize(new java.awt.Dimension(240, 38));
        localGridLowPassSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                localGridLowPassSliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        localHypsoPanel.add(localGridLowPassSlider, gridBagConstraints);

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
        gridBagConstraints.gridy = 5;
        gridBagConstraints.insets = new java.awt.Insets(20, 0, 0, 0);
        visualizationPanel.add(solidColorPanel, gridBagConstraints);

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

        zeLabel.setText("Zenith (Vertical Direction)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(20, 0, 0, 0);
        illuminationPanel.add(zeLabel, gridBagConstraints);

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
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        illuminationPanel.add(zenithSlider, gridBagConstraints);

        azLabel.setText("Azimuth (Horizontal Direction)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        illuminationPanel.add(azLabel, gridBagConstraints);

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

        contoursIlluminatedLineWidthLabel.setText("Illuminated Line Width");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        contoursSettingsPanel.add(contoursIlluminatedLineWidthLabel, gridBagConstraints);

        contoursIlluminatedHighestLabel.setText("Highest");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
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
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        contoursSettingsPanel.add(contoursIlluminatedHighestLineWidthSlider, gridBagConstraints);

        contoursIlluminatedLineWidthHighValueLabel.setFont(contoursIlluminatedHighestLineWidthSlider.getFont());
        contoursIlluminatedLineWidthHighValueLabel.setText("4.5");
        contoursIlluminatedLineWidthHighValueLabel.setPreferredSize(new java.awt.Dimension(30, 16));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        contoursSettingsPanel.add(contoursIlluminatedLineWidthHighValueLabel, gridBagConstraints);

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
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        contoursSettingsPanel.add(contoursIlluminatedLockedToggleButton, gridBagConstraints);

        contoursIlluminatedLowestLabel.setText("Lowest");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
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
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        contoursSettingsPanel.add(contoursIlluminatedLowestLineWidthSlider, gridBagConstraints);

        contoursIlluminatedLineWidthLowValueLabel.setFont(contoursIlluminatedHighestLineWidthSlider.getFont());
        contoursIlluminatedLineWidthLowValueLabel.setText("4.5");
        contoursIlluminatedLineWidthLowValueLabel.setPreferredSize(new java.awt.Dimension(30, 16));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        contoursSettingsPanel.add(contoursIlluminatedLineWidthLowValueLabel, gridBagConstraints);

        contoursShadwoLineWidthSlider.setText("Shadowed Line Width");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        contoursSettingsPanel.add(contoursShadwoLineWidthSlider, gridBagConstraints);

        contoursShadowedHighest.setText("Highest");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
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
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        contoursSettingsPanel.add(contoursShadowHighestLineWidthSlider, gridBagConstraints);

        contoursShadowLineWidthHighValueLabel.setFont(contoursShadowHighestLineWidthSlider.getFont());
        contoursShadowLineWidthHighValueLabel.setText("123");
        contoursShadowLineWidthHighValueLabel.setPreferredSize(new java.awt.Dimension(30, 16));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        contoursSettingsPanel.add(contoursShadowLineWidthHighValueLabel, gridBagConstraints);

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
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        contoursSettingsPanel.add(contoursShadowedLockedToggleButton, gridBagConstraints);

        contoursShadowedLowest.setText("Lowest");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
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
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        contoursSettingsPanel.add(contoursShadowLowestLineWidthSlider, gridBagConstraints);

        contoursShadowLineWidthLowValueLabel.setFont(contoursShadowHighestLineWidthSlider.getFont());
        contoursShadowLineWidthLowValueLabel.setText("123");
        contoursShadowLineWidthLowValueLabel.setPreferredSize(new java.awt.Dimension(30, 16));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        contoursSettingsPanel.add(contoursShadowLineWidthLowValueLabel, gridBagConstraints);

        contoursMinLineWidthLabel.setText("Minimum Line Width");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
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
        gridBagConstraints.gridy = 10;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        contoursSettingsPanel.add(contoursMinLineWidthSlider, gridBagConstraints);

        contoursMinLineWidthValueLabel.setFont(contoursMinLineWidthSlider.getFont());
        contoursMinLineWidthValueLabel.setText("123");
        contoursMinLineWidthValueLabel.setPreferredSize(new java.awt.Dimension(30, 16));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        contoursSettingsPanel.add(contoursMinLineWidthValueLabel, gridBagConstraints);

        contoursWidthInfoLabel.setFont(contoursWidthInfoLabel.getFont().deriveFont(contoursWidthInfoLabel.getFont().getSize()-2f));
        contoursWidthInfoLabel.setText("Line widths are relative to grid cell size.");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 0);
        contoursSettingsPanel.add(contoursWidthInfoLabel, gridBagConstraints);

        contoursIntervalLabel.setText("Interval");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
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
        gridBagConstraints.gridy = 12;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        contoursSettingsPanel.add(contoursIntervalTextBox, gridBagConstraints);

        contoursGradientLabel.setText("Gradient Angle");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 14;
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
        gridBagConstraints.gridy = 14;
        contoursSettingsPanel.add(contoursGradientSlider, gridBagConstraints);

        contoursDespeckleLabel.setText("Despeckle");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 15;
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
        gridBagConstraints.gridy = 15;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        contoursSettingsPanel.add(contoursDespeckleSlider, gridBagConstraints);

        contoursTransitionLabel.setText("Transition");
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
        gridBagConstraints.gridy = 18;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
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
        gridBagConstraints.gridy = 17;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        contoursSettingsPanel.add(contoursBlankBackgroundButton, gridBagConstraints);

        contoursCardPanel.add(contoursSettingsPanel, "contoursSettingsCard");

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        illuminatedContoursPanel.add(contoursCardPanel, gridBagConstraints);

        contoursPanel.add(illuminatedContoursPanel);

        tabbedPane.addTab("Contours", contoursPanel);

        add(tabbedPane);
    }// </editor-fold>//GEN-END:initComponents

    public void setModel(Model m) {
        this.model = m;

        generalizationMaxLevelsSpinner.setValue(m.generalizationMaxLevels);
        generalizationDetailSlider.setValue((int) Math.round(m.getGeneralizationDetails() * 100));

        azimuthSlider.setValue(m.azimuth);
        zenithSlider.setValue(m.zenith);

        contoursIlluminatedHighestLineWidthSlider.setValue((int) Math.round(m.contoursIlluminatedWidthHigh * 10));
        contoursIlluminatedLowestLineWidthSlider.setValue((int) Math.round(m.contoursIlluminatedWidthLow * 10));
        contoursShadowHighestLineWidthSlider.setValue((int) Math.round(m.contoursShadowWidthHigh * 10));
        contoursShadowLowestLineWidthSlider.setValue((int) Math.round(m.contoursShadowWidthLow * 10));
        contoursMinLineWidthSlider.setValue((int) Math.round(m.contoursMinWidth * 10));
        contoursGradientSlider.setValue(m.contoursGradientAngle);
        contoursIntervalTextBox.setValue(m.contoursInterval);
        contoursDespeckleSlider.setValue((int) Math.round(m.contoursAspectGaussBlur * 20D));
        contoursTransitionSlider.setValue(m.contoursTransitionAngle);

        verticalExaggerationSlider.setValue(Math.round(m.shadingVerticalExaggeration * 10f));
        colorGradientSlider.setValues(m.colorRamp.colorPositions, m.colorRamp.colors);
        solidColorButton.setColor(m.solidColor);
        updateGeneralizationInfoLabelVisiblity();

        localGridLowPassSlider.setValue((int) m.getLocalGridLowPassStandardDeviation());
        localGridStandardDeviationFilterSizeSlider.setValue(m.getLocalGridStandardDeviationLevels());
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

        updateVisualizationPanelsVisibility();
        updateImage(REGULAR);
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
            model.colorRamp.colors = colorGradientSlider.getColors();
            model.colorRamp.colorPositions = colorGradientSlider.getThumbPositions();
            updateImage(colorGradientSlider.isValueAdjusting() ? FAST : REGULAR);
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
        model.contoursIlluminatedWidthHigh = contoursIlluminatedHighestLineWidthSlider.getValue() / 10.f;
        boolean locked = contoursIlluminatedLockedToggleButton.isSelected();
        contoursWidthSliderStateChanged(contoursIlluminatedHighestLineWidthSlider,
                contoursIlluminatedLowestLineWidthSlider,
                contoursIlluminatedLineWidthHighValueLabel,
                contoursIlluminatedLineWidthLowValueLabel,
                locked);
        if (locked) {
            model.contoursIlluminatedWidthLow = model.contoursIlluminatedWidthHigh;
        }
    }//GEN-LAST:event_contoursIlluminatedHighestLineWidthSliderStateChanged

    private void contoursShadowHighestLineWidthSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_contoursShadowHighestLineWidthSliderStateChanged
        model.contoursShadowWidthHigh = contoursShadowHighestLineWidthSlider.getValue() / 10.f;
        boolean locked = contoursShadowedLockedToggleButton.isSelected();
        contoursWidthSliderStateChanged(contoursShadowHighestLineWidthSlider,
                contoursShadowLowestLineWidthSlider,
                contoursShadowLineWidthHighValueLabel,
                contoursShadowLineWidthLowValueLabel,
                locked);
        if (locked) {
            model.contoursShadowWidthLow = model.contoursShadowWidthHigh;
        }
    }//GEN-LAST:event_contoursShadowHighestLineWidthSliderStateChanged

    private void adjustSynchronizedSlider(JSlider slaveSlider, int val) {
        ChangeListener listener = slaveSlider.getChangeListeners()[0];
        slaveSlider.removeChangeListener(listener);
        slaveSlider.setValue(val);
        slaveSlider.addChangeListener(listener);
    }

    private void contoursMinLineWidthSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_contoursMinLineWidthSliderStateChanged
        model.contoursMinWidth = contoursMinLineWidthSlider.getValue() / 10.;

        // remove event listener to avoid triggering a rendering event
        int minWidth = contoursMinLineWidthSlider.getValue();
        int w = Math.max(contoursIlluminatedHighestLineWidthSlider.getValue(), minWidth);
        adjustSynchronizedSlider(contoursIlluminatedHighestLineWidthSlider, w);
        w = Math.max(contoursIlluminatedLowestLineWidthSlider.getValue(), minWidth);
        adjustSynchronizedSlider(contoursIlluminatedLowestLineWidthSlider, w);
        w = Math.max(contoursShadowHighestLineWidthSlider.getValue(), minWidth);
        adjustSynchronizedSlider(contoursShadowHighestLineWidthSlider, w);
        w = Math.max(contoursShadowLowestLineWidthSlider.getValue(), minWidth);
        adjustSynchronizedSlider(contoursShadowLowestLineWidthSlider, w);

        // update model
        model.contoursIlluminatedWidthLow = contoursIlluminatedLowestLineWidthSlider.getValue() / 10d;
        model.contoursIlluminatedWidthHigh = contoursIlluminatedHighestLineWidthSlider.getValue() / 10d;
        model.contoursShadowWidthLow = contoursShadowLowestLineWidthSlider.getValue() / 10d;
        model.contoursShadowWidthHigh = contoursShadowHighestLineWidthSlider.getValue() / 10d;

        updateImage(contoursMinLineWidthSlider.getValueIsAdjusting() ? FAST : REGULAR);
        DecimalFormat df = new DecimalFormat("0.0");
        String t = df.format(model.contoursIlluminatedWidthLow);
        contoursIlluminatedLineWidthHighValueLabel.setText(t);
        t = df.format(model.contoursShadowWidthLow);
        contoursShadowLineWidthHighValueLabel.setText(t);
        t = df.format(model.contoursMinWidth);
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
                    break;
                case 2:
                    model.foregroundVisualization = ForegroundVisualization.SHADED_CONTOURS;
                    ((CardLayout) (contoursCardPanel.getLayout())).show(contoursCardPanel, "contoursSettingsCard");
                    break;
            }
            updateImage(REGULAR);
        }

    }//GEN-LAST:event_contoursComboBoxItemStateChanged

    private void visualizationComboBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_visualizationComboBoxItemStateChanged
        if (evt.getStateChange() == ItemEvent.SELECTED) {
            model.backgroundVisualization
                    = (ColorVisualization) visualizationComboBox.getSelectedItem();
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

    private void localGridLowPassSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_localGridLowPassSliderStateChanged
        if (localGridLowPassSlider.getValueIsAdjusting() == false) {
            int filterSize = localGridLowPassSlider.getValue();
            model.setLocalGridLowPassStd(filterSize);
            updateImage(REGULAR);
        }
    }//GEN-LAST:event_localGridLowPassSliderStateChanged

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
            adjustSynchronizedSlider(slaveSlider, masterSlider.getValue());
            slaveLabel.setText(t);
        }
    }
    private void contoursIlluminatedLowestLineWidthSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_contoursIlluminatedLowestLineWidthSliderStateChanged
        model.contoursIlluminatedWidthLow = contoursIlluminatedLowestLineWidthSlider.getValue() / 10.f;
        boolean locked = contoursIlluminatedLockedToggleButton.isSelected();
        contoursWidthSliderStateChanged(contoursIlluminatedLowestLineWidthSlider,
                contoursIlluminatedHighestLineWidthSlider,
                contoursIlluminatedLineWidthLowValueLabel,
                contoursIlluminatedLineWidthHighValueLabel,
                locked);
        if (locked) {
            model.contoursIlluminatedWidthHigh = model.contoursIlluminatedWidthLow;
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
        model.contoursShadowWidthLow = contoursShadowLowestLineWidthSlider.getValue() / 10.f;
        boolean locked = contoursShadowedLockedToggleButton.isSelected();
        contoursWidthSliderStateChanged(
                contoursShadowLowestLineWidthSlider,
                contoursShadowHighestLineWidthSlider,
                contoursShadowLineWidthLowValueLabel,
                contoursShadowLineWidthHighValueLabel,
                locked);
        if (locked) {
            model.contoursShadowWidthHigh = model.contoursShadowWidthLow;
        }
    }//GEN-LAST:event_contoursShadowLowestLineWidthSliderStateChanged

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JSlider azimuthSlider;
    private javax.swing.JPanel colorGradientPanel;
    private com.bric.swing.GradientSlider colorGradientSlider;
    private javax.swing.JPopupMenu colorPopupMenu;
    private edu.oregonstate.cartography.gui.MenuToggleButton colorPresetsButton;
    private javax.swing.JButton contoursBlankBackgroundButton;
    private javax.swing.JPanel contoursCardPanel;
    private javax.swing.JComboBox contoursComboBox;
    private javax.swing.JSlider contoursDespeckleSlider;
    private javax.swing.JPanel contoursEmptyPanel;
    private javax.swing.JSlider contoursGradientSlider;
    private javax.swing.JSlider contoursIlluminatedHighestLineWidthSlider;
    private javax.swing.JLabel contoursIlluminatedLineWidthHighValueLabel;
    private javax.swing.JLabel contoursIlluminatedLineWidthLowValueLabel;
    private javax.swing.JToggleButton contoursIlluminatedLockedToggleButton;
    private javax.swing.JSlider contoursIlluminatedLowestLineWidthSlider;
    private javax.swing.JFormattedTextField contoursIntervalTextBox;
    private javax.swing.JSlider contoursMinLineWidthSlider;
    private javax.swing.JLabel contoursMinLineWidthValueLabel;
    private javax.swing.JPanel contoursPanel;
    private javax.swing.JPanel contoursSettingsPanel;
    private javax.swing.JSlider contoursShadowHighestLineWidthSlider;
    private javax.swing.JLabel contoursShadowLineWidthHighValueLabel;
    private javax.swing.JLabel contoursShadowLineWidthLowValueLabel;
    private javax.swing.JSlider contoursShadowLowestLineWidthSlider;
    private javax.swing.JToggleButton contoursShadowedLockedToggleButton;
    private javax.swing.JSlider contoursTransitionSlider;
    private javax.swing.JLabel generalizationDetaiIsLabel;
    private javax.swing.JSlider generalizationDetailSlider;
    private javax.swing.JLabel generalizationInfoLabel;
    private javax.swing.JSpinner generalizationMaxLevelsSpinner;
    private javax.swing.JPanel generalizationPanel;
    private javax.swing.JPanel illuminatedContoursPanel;
    private javax.swing.JPanel illuminationPanel;
    private javax.swing.JSlider localGridLowPassSlider;
    private javax.swing.JSlider localGridStandardDeviationFilterSizeSlider;
    private javax.swing.JPanel localHypsoPanel;
    private edu.oregonstate.cartography.gui.ColorButton solidColorButton;
    private javax.swing.JPanel solidColorPanel;
    private javax.swing.JTabbedPane tabbedPane;
    private javax.swing.JLabel verticalExaggerationLabel;
    private javax.swing.JPanel verticalExaggerationPanel;
    private javax.swing.JSlider verticalExaggerationSlider;
    private javax.swing.JComboBox visualizationComboBox;
    private javax.swing.JPanel visualizationPanel;
    private javax.swing.JSlider zenithSlider;
    // End of variables declaration//GEN-END:variables

}
