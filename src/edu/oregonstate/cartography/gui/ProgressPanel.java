/*
 * ProgressPanel.java
 *
 * Created on August 15, 2006, 12:18 PM
 */
package edu.oregonstate.cartography.gui;

import java.awt.event.ActionListener;
import javax.swing.Action;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/**
 * ProgressPanel displays a progress dialog for lengthy operations.
 *
 * @author Bernhard Jenny, Institute of Cartography, ETH Zurich.
 */
public class ProgressPanel extends javax.swing.JPanel {

    /**
     * Creates new ProgressPanel. Must be called from the Swing Event Dispatch
     * Thread. Important: Call dispose() when the panel is no longer needed,
     * otherwise memory is leaked.
     */
    public ProgressPanel() {
        assert (SwingUtilities.isEventDispatchThread());
        this.initComponents();
    }

    /**
     * dispose() must be called when the panel is no longer needed and the
     * parent dialog is disposed. This is to avoid a memory leak that may fill
     * up heap space when listeners are not removed (which make garbage
     * collecting a Frame impossible). Must be called from the Swing Event
     * Dispatch Thread.
     */
    public void dispose() {
        assert (SwingUtilities.isEventDispatchThread());
        ActionListener[] als = cancelButton.getActionListeners();
        for (ActionListener al : als) {
            cancelButton.removeActionListener(al);
        }
        cancelButton.getInputMap().clear();
        cancelButton.getActionMap().clear();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        messageLabel = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();
        cancelButton = new javax.swing.JButton();

        setLayout(new java.awt.GridBagLayout());

        messageLabel.setText("Working. Please wait…");
        messageLabel.setMinimumSize(new java.awt.Dimension(350, 45));
        messageLabel.setPreferredSize(new java.awt.Dimension(350, 45));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 0);
        add(messageLabel, gridBagConstraints);

        progressBar.setPreferredSize(new java.awt.Dimension(250, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        add(progressBar, gridBagConstraints);

        cancelButton.setText("Cancel");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 0);
        add(cancelButton, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Removes the message field. The message field cannot be shown again.
     */
    public void removeMessageField() {
        assert (SwingUtilities.isEventDispatchThread());
        remove(messageLabel);
    }

    /**
     * Update the progress indicator. Switches to GUI with determinate length
     * if the length is currently indeterminate.
     * @param percentage Progress between 0 and 100.
     */
    public void progress(final int percentage) {
        assert (SwingUtilities.isEventDispatchThread());
        progressBar.setIndeterminate(false);
        progressBar.setValue(percentage);
    }

    /**
     * @param cancelAction A callback handler that is called when the
     * user presses the cancel button.
     */
    public void setCancelAction(Action cancelAction) {
        assert (SwingUtilities.isEventDispatchThread());
        cancelButton.addActionListener(cancelAction);
        cancelButton.getInputMap().put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE,
                0, true), "EscapeKey");
        cancelButton.getActionMap().put("EscapeKey", cancelAction);
    }
    
    /**
     * Enable or disable the cancel button
     * @param cancellable If true the cancel button is enabled.
     */
    public void setCancellable(boolean cancellable) {
        assert (SwingUtilities.isEventDispatchThread());
        cancelButton.setEnabled(cancellable);
    }
    
    /**
     * Update the GUI message.
     * @param msg The new message to display.
     */
    public void setMessage(final String msg) {
        assert (SwingUtilities.isEventDispatchThread());
        messageLabel.setText(msg);
    }

    /**
     * An indeterminate progress bar has not defined length.
     * @param indeterminate 
     */
    public void setIndeterminate(boolean indeterminate) {
        assert (SwingUtilities.isEventDispatchThread());
        progressBar.setIndeterminate(indeterminate);

    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancelButton;
    private javax.swing.JLabel messageLabel;
    private javax.swing.JProgressBar progressBar;
    // End of variables declaration//GEN-END:variables
}
