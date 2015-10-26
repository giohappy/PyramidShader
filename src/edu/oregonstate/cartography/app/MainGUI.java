package edu.oregonstate.cartography.app;

import edu.oregonstate.cartography.grid.Model;
import edu.oregonstate.cartography.gui.SettingsDialog;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 * Main entry point for GUI version of Terrain Sculptor. This main method is 
 * launched in a separate JVM to maximize available heap memory.
 * @author Bernhard Jenny, Cartography and Geovisualization Group, Oregon State
 * University
 */
public class MainGUI {

    public static void main(String[] args) {
        // on Mac OS X: take the menu bar out of the window and put it on top
        // of the main screen.
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Pyramid Shader");
        System.setProperty("apple.laf.useScreenMenuBar", "true");

        // use the standard look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException e) {
        }
        // Create and display the main window
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                // create model object, main window and settings dialog
                Model model = new Model();
                edu.oregonstate.cartography.gui.MainWindow mainWindow = new edu.oregonstate.cartography.gui.MainWindow(model);
                SettingsDialog settingsDialog = new SettingsDialog(mainWindow, false);
                settingsDialog.setModel(model);
                mainWindow.getProgressPanel().removeCancelButton();
                mainWindow.getProgressPanel().removeMessageField();
                mainWindow.getProgressPanel().horizontalDesign();
                mainWindow.setSettingsDialog(settingsDialog);
                settingsDialog.setProgressPanel(mainWindow.getProgressPanel());
                
                // find available screen real estate (without taskbar, etc.)
                Rectangle screen = GraphicsEnvironment.
                        getLocalGraphicsEnvironment().getMaximumWindowBounds();
                Dimension dlgDim = settingsDialog.getPreferredSize();

                // position settings dialog in top-right corner
                settingsDialog.pack();
                int x = (int) (screen.getMaxX() - dlgDim.getWidth() - 5);
                int y = (int) mainWindow.getLocation().getY();
                settingsDialog.setLocation(x, y);

                // use rest of screen space for main frame
                int frameWidth = (int) (screen.getWidth() - dlgDim.getWidth() - 2 * 5);
                mainWindow.setSize(frameWidth, (int) screen.getHeight());
                mainWindow.setLocation((int) screen.getMinX(), (int) screen.getMinY());

                // show windows and open terrain model
                mainWindow.setVisible(true);
                settingsDialog.setVisible(true);
                mainWindow.openGrid();
            }
        });
    }
}