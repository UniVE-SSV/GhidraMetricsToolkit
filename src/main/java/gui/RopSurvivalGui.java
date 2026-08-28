package gui;

import ghidra.program.model.listing.Program;
import ghidra.util.Msg;
import ghidra.util.exception.CancelledException;
import ghidra.util.task.TaskMonitor;
import impl.metrics.RopSurvival;
import metrics.GhidraMetricsToolkitPlugin;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class RopSurvivalGui {

    private final JPanel panel;
    private final GhidraTaskRunner taskRunner;
    private final ProgramPickerPanel programPicker;
    private final JLabel bagOfGadgetsResult;
    private final JLabel survivorResult;
    private final JTextField depthField;

    public RopSurvivalGui(GhidraMetricsToolkitPlugin plugin) {
        this.plugin = plugin;

        panel = new JPanel(new BorderLayout());
        taskRunner = new GhidraTaskRunner(plugin.getTool(), panel);

        // The program picker sits in the CENTER and flexes its text field; the
        // depth field is in the EAST at its natural size (BorderLayout never
        // shrinks EAST components, so the field stays on the same line as its
        // label and is not clipped until the window is extremely narrow).
        JPanel topPanel = new JPanel(new BorderLayout(4, 4));

        JPanel depthChooserPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        depthChooserPanel.add(new JLabel("Depth: "));

        depthField = new JTextField("10", 4);
        depthChooserPanel.add(depthField);

        programPicker = new ProgramPickerPanel(plugin, this::runComputation);

        topPanel.add(programPicker, BorderLayout.CENTER);
        topPanel.add(depthChooserPanel, BorderLayout.EAST);

        JPanel resultsPanel = new JPanel(new BorderLayout());
        JPanel bagOfGadgetsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bagOfGadgetsPanel.add(new JLabel("Bag of Gadgets: "));

        bagOfGadgetsResult = new JLabel("N/A");
        bagOfGadgetsPanel.add(bagOfGadgetsResult);
        JPanel survivorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        survivorPanel.add(new JLabel("Survivor: "));

        survivorResult = new JLabel("N/A");
        survivorPanel.add(survivorResult);

        resultsPanel.add(bagOfGadgetsPanel, BorderLayout.NORTH);
        resultsPanel.add(survivorPanel, BorderLayout.CENTER);

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(resultsPanel, BorderLayout.CENTER);
    }

    private final GhidraMetricsToolkitPlugin plugin;

    private void runComputation() {
        if (programPicker.getSelectedFile() == null) {
            return;
        }

        int depth;
        try {
            depth = Integer.parseInt(depthField.getText());
        } catch (NumberFormatException ex) {
            Msg.showError(getClass(), panel, "Invalid depth", "Depth must be an integer.");
            return;
        }
        if (depth < 1) {
            Msg.showError(getClass(), panel, "Invalid depth", "Depth must be greater than zero.");
            return;
        }

        Program currentProgram = plugin.getCurrentProgram();
        if (currentProgram == null) {
            return;
        }

        bagOfGadgetsResult.setText("N/A");
        survivorResult.setText("N/A");

        taskRunner.launch("Computing ROP Survival", (session, monitor) -> {
            monitor.setMessage("Loading compared program...");
            Program program = programPicker.loadComparedProgram(monitor);
            if (program == null) {
                throw new IOException("Could not load the selected program");
            }

            RopSurvival ropSurvival = new RopSurvival(currentProgram, program, depth);
            RopSurvival.Result result = ropSurvival.compute(monitor);

            session.runOnUi(() -> {
                programPicker.setComparedProgram(program);
                if (result == null) {
                    Msg.showError(getClass(), panel, "Metric computation failed!",
                            "The programs use different processors.");
                    return;
                }
                bagOfGadgetsResult.setText(result.bagOfGadgets + "");
                survivorResult.setText(result.survivor + "");
            });
        });
    }

    public void resetGui() {
        bagOfGadgetsResult.setText("N/A");
        survivorResult.setText("N/A");
        taskRunner.invalidate();
        programPicker.reset();
        depthField.setText("10");
    }

    public JPanel getPanel() {
        return panel;
    }
}
