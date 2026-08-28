package gui;

import ghidra.framework.plugintool.PluginTool;
import ghidra.util.Msg;
import ghidra.util.Swing;
import ghidra.util.exception.CancelledException;
import ghidra.util.task.Task;
import ghidra.util.task.TaskLauncher;
import ghidra.util.task.TaskMonitor;

import java.awt.*;

/**
 * Runs long computations as native Ghidra Tasks with an integrated, cancellable
 * progress dialog. Every new task supersedes the previous one; results of
 * superseded tasks are discarded (generation counter).
 */
public class GhidraTaskRunner {

    /**
     * The work to execute off the EDT, inside a {@link Task}.
     */
    public interface TaskBody {
        void run(Session session, TaskMonitor monitor) throws Exception;
    }

    /**
     * Per-task handle passed to the {@link TaskBody}. Use
     * {@link #runOnUi(Runnable)} to schedule UI updates that are discarded if a
     * newer task superseded this one.
     */
    public static final class Session {
        private final GhidraTaskRunner runner;
        private final int generation;

        private Session(GhidraTaskRunner runner, int generation) {
            this.runner = runner;
            this.generation = generation;
        }

        public void runOnUi(Runnable uiUpdate) {
            runner.runIfCurrent(generation, uiUpdate);
        }
    }

    private final PluginTool tool;
    private final Component parent;
    private volatile TaskMonitor lastMonitor;
    private int generation;

    public GhidraTaskRunner(PluginTool tool, Component parent) {
        this.tool = tool;
        this.parent = parent;
    }

    /**
     * Launches a cancellable Ghidra Task. Errors are reported in a dialog
     * (unless superseded); cancellations are handled silently by the framework.
     */
    public void launch(String title, TaskBody body) {
        final int taskGeneration = ++generation;
        // Supersede any still-running task (e.g. rapid option toggling) so that
        // only the latest computation runs and earlier ones abort at the next
        // cancellation check point.
        TaskMonitor previous = lastMonitor;
        if (previous != null) {
            previous.cancel();
        }
        final Session session = new Session(this, taskGeneration);
        Task task = new Task(title, true, true, true) {
            @Override
            public void run(TaskMonitor monitor) throws CancelledException {
                lastMonitor = monitor;
                try {
                    body.run(session, monitor);
                    if (monitor.isCancelled()) {
                        throw new CancelledException();
                    }
                } catch (CancelledException ex) {
                    throw ex;
                } catch (Exception ex) {
                    reportError(title, taskGeneration, ex);
                }
            }
        };
        new TaskLauncher(task, tool.getToolFrame());
    }

    private void runIfCurrent(int taskGeneration, Runnable uiUpdate) {
        Swing.runLater(() -> {
            if (taskGeneration == generation) {
                uiUpdate.run();
            }
        });
    }

    /**
     * Invalidates any in-flight tasks (e.g. when the active program changes).
     */
    public void invalidate() {
        generation++;
    }

    private void reportError(String title, int taskGeneration, Exception ex) {
        Swing.runLater(() -> {
            if (taskGeneration != generation) {
                return;
            }
            Throwable cause = (ex.getCause() != null) ? ex.getCause() : ex;
            Msg.showError(getClass(), parent, title + " failed", cause.getMessage());
        });
    }
}
