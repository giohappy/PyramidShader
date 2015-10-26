package edu.oregonstate.cartography.app;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Bernie Jenny, Oregon State University.
 */
public class Main {

    /**
     * Start the GUI version of Terrain Sculptor in a separate JVM to maximize
     * the available heap memory space.
     */
    private static void launchGUIProcess() throws IOException {
        String className = MainGUI.class.getName();
        String xDockAppName = ApplicationInfo.getApplicationName();
        ProcessLauncher processLauncher = new ProcessLauncher();
        String xDockIconPath = processLauncher.findXDockIconPath("icon.icns");
        processLauncher.startJVM(className, xDockAppName, xDockIconPath);
    }

    /**
     * Main method for Pyramid Shader
     *
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        try {
            launchGUIProcess();            
            System.exit(0);
        } catch (IOException ex) {
            MainGUI.main(args);
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null,ex);
        }
    }
}
