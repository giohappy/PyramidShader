package edu.oregonstate.cartography.gui;

import java.awt.event.ActionEvent;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

/**
 *
 * @author Bernhard Jenny, Institute of Cartography, ETH Zurich.
 * @param <T>
 */
public abstract class SwingWorkerWithProgressIndicatorPanel<T> extends SwingWorker<T, Integer>
        implements ProgressIndicator {

    /**
     * The GUI. Must be accessed by the Swing thread only.
     */
    protected final ProgressPanel progressPanel;
    
    /**
     * The number of tasks to execute. The default is 1.
     */
    private int totalTasksCount = 1;
    /**
     * The ID of the current task.
     */
    private int currentTask = 1;

    /**
     * Must be called in the Event Dispatching Thread.
     * @param progressPanel Panel to show progress.
     */
    public SwingWorkerWithProgressIndicatorPanel(ProgressPanel progressPanel) {
        assert (SwingUtilities.isEventDispatchThread());
        assert(progressPanel != null);
        this.progressPanel = progressPanel;
        Action cancelAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        };
        progressPanel.removeActionListeners();
        progressPanel.setCancelAction(cancelAction);
    }

    /**
     * Initialize the dialog. Can be called from any thread.
     */
    @Override
    public void start() {
        // initialize the GUI in the event dispatching thread
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                progressPanel.progress(0);
                progressPanel.setIndeterminate(false);
            }
        });
    }

    /**
     * Stop the operation. The dialog will be hidden and the operation will stop
     * as soon as possible. This might not happen synchronously. Can be called
     * from any thread. The client must invoke SwingWorker.isCancelled at short
     * intervals to test for cancellation.
     */
    @Override
    public void cancel() {
        synchronized (this) {
            cancel(false);
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                progressPanel.progress(0);
            }
        });
    }

    /**
     * Inform the dialog that the operation has completed and it can be hidden.
     */
    @Override
    public void completeProgress() {
        progress(100);
    }

    /**
     * Update the progress indicator.
     *
     * @param percentage A value between 0 and 100.
     * @return True if the operation should continue, false otherwise.
     */
    @Override
    public boolean progress(int percentage) {
        setProgress(percentage);
        publish(percentage);
        return !isCancelled();
    }

    /**
     * Enable or disable button to cancel the operation
     * @param cancellable If true, the button is enabled.
     */
    @Override
    public void setCancellable(final boolean cancellable) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                progressPanel.setCancellable(cancellable);
            }
        });

    }

    @Override
    public void setMessage(final String msg) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                progressPanel.setMessage(msg);
            }
        });
    }

    public void removeMessageField() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                progressPanel.removeMessageField();
            }
        });
    }

    /**
     * Invoked when the task's publish() method is called. Update the value
     * displayed by the progress dialog. This is called from within the event
     * dispatching thread.
     *
     * @param progressList
     */
    @Override
    protected void process(List<Integer> progressList) {
        if (isCancelled()) {
            return;
        }
        int progress = progressList.get(progressList.size() - 1);
        progress = progress / totalTasksCount + (currentTask - 1) * 100 / totalTasksCount;
        progressPanel.progress(progress);
    }

    /**
     * Sets the number of tasks. Each task has a progress between 0 and 100. If
     * the number of tasks is larger than 1, progress of task 1 will be rescaled
     * to 0..50.
     *
     * @param tasksCount The total number of tasks.
     */
    @Override
    public void setTotalTasksCount(int tasksCount) {
        synchronized (this) {
            this.totalTasksCount = tasksCount;
        }
    }

    /**
     * Returns the total numbers of tasks for this progress indicator.
     *
     * @return The total numbers of tasks.
     */
    @Override
    public int getTotalTasksCount() {
        synchronized (this) {
            return this.totalTasksCount;
        }
    }

    /**
     * Switch to the next task.
     */
    @Override
    public void nextTask() {
        synchronized (this) {
            ++this.currentTask;
            if (this.currentTask > this.totalTasksCount) {
                this.totalTasksCount = this.currentTask;
            }

            // set progress to 0 for the new task, otherwise the progress bar would
            // jump to the end of this new task and jump back when setProgress(0)
            // is called
            this.setProgress(0);
        }
    }

    @Override
    public void nextTask(String message) {
        this.nextTask();
        this.setMessage(message);
    }

    /**
     * Returns the ID of the current task. The first task has ID 1 (and not 0).
     *
     * @return The ID of the current task.
     */
    @Override
    public int currentTask() {
        synchronized (this) {
            return this.currentTask;
        }
    }

    public void setIndeterminate(final boolean indeterminate) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                progressPanel.setIndeterminate(indeterminate);
            }
        });
    }
}
