package edu.oregonstate.cartography.gui;

import edu.oregonstate.cartography.grid.Model;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.io.IOException;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 *
 * @author Bernie Jenny, Oregon State University.
 */
public class PyramidShader {

    /**
     * Main method for Pyramid Shader
     *
     * @param args the command line arguments
     */
    public static void main(String args[]) {

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
                MainWindow frame = new MainWindow(model);
                SettingsDialog dlg = new SettingsDialog(frame, false);
                dlg.setModel(model);
                frame.setSettingsDialog(dlg);

                // find available screen real estate (without taskbar, etc.)
                Rectangle screen = GraphicsEnvironment.
                        getLocalGraphicsEnvironment().getMaximumWindowBounds();
                Dimension dlgDim = dlg.getPreferredSize();

                // position settings dialog in top-right corner
                dlg.pack();
                int x = (int) (screen.getMaxX() - dlgDim.getWidth() - 5);
                int y = (int) frame.getLocation().getY();
                dlg.setLocation(x, y);

                // use rest of screen space for main frame
                int frameWidth = (int) (screen.getWidth() - dlgDim.getWidth() - 2 * 5);
                frame.setSize(frameWidth, (int) screen.getHeight());
                frame.setLocation((int) screen.getMinX(), (int) screen.getMinY());

                // show windows and open terrain model
                frame.setVisible(true);
                dlg.setVisible(true);
                frame.openGrid();
            }
        });
    }
}
