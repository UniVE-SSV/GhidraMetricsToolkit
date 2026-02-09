package gui;

import ghidra.framework.model.DomainFile;
import ghidra.program.model.listing.Program;
import ghidra.util.Msg;
import impl.metrics.RopSurvival;
import metrics.GhidraMetricsToolkitPlugin;
import impl.utils.ProjectUtils;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class RopSurvivalGui {

    private final JPanel panel;
    private final GhidraMetricsToolkitPlugin plugin;
    private final JComboBox<DomainFile> programChooser;
    private final JLabel bagOfGadgetsResult;
    private final JLabel survivorResult;
    private final JTextField depthField;

    public RopSurvivalGui(GhidraMetricsToolkitPlugin plugin) {
        this.plugin = plugin;

        panel = new JPanel(new BorderLayout());

        List<DomainFile> programFiles = ProjectUtils.getPrograms(plugin.getTool().getProject());

        JPanel topPanel = new JPanel(new BorderLayout());

        JPanel depthChooserPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        depthChooserPanel.add(new JLabel("Depth: "));

        depthField = new JTextField("10", 4);
        depthChooserPanel.add(depthField);

        JPanel programChooserPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        programChooser = new JComboBox<>();
        for (DomainFile program : programFiles) {
            programChooser.addItem(program);
        }
        programChooser.setSelectedIndex(-1);
        programChooser.setVisible(true);


        programChooserPanel.add(new JLabel("Compare to: "));
        programChooserPanel.add(programChooser);

        topPanel.add(programChooserPanel, BorderLayout.WEST);
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

        programChooser.addActionListener(e -> {
            try {
                DomainFile choice = (DomainFile) programChooser.getSelectedItem();
                if (choice != null) {
                    Program program = ProjectUtils.getProgramFromDomainFile(choice);

                    RopSurvival ropSurvival = new RopSurvival(plugin.getCurrentProgram(), program);
                    RopSurvival.Result result = (RopSurvival.Result) ropSurvival.compute();

                    bagOfGadgetsResult.setText(result.bagOfGadgets + "");
                    survivorResult.setText(result.survivor + "");
                }
            } catch (Exception ex) {
                Msg.showError(getClass(), panel, "Metric computation failed!", ex.getMessage());
                programChooser.setSelectedIndex(-1);
            }
        });

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(resultsPanel, BorderLayout.CENTER);
    }

    public void resetGui() {
        bagOfGadgetsResult.setText("N/A");
        survivorResult.setText("N/A");
        programChooser.setSelectedIndex(-1);
        depthField.setText("10");
    }

    public JPanel getPanel() {
        return panel;
    }
}
