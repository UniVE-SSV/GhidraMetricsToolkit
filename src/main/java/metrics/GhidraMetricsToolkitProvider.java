package metrics;

import ghidra.framework.plugintool.ComponentProviderAdapter;
import gui.*;

import javax.swing.*;
import java.awt.*;

public class GhidraMetricsToolkitProvider extends ComponentProviderAdapter {

    private final GhidraMetricsToolkitPlugin plugin;
    private JPanel panel;

    private EntropyGui entropyGui;
    private HalsteadGui halsteadGui;
    private McCabeGui mcCabeGui;
    private SimilarityGui similarityGui;
    private RopSurvivalGui ropSimilarityGui;

    public GhidraMetricsToolkitProvider(GhidraMetricsToolkitPlugin ghidraMetricsPlugin, String pluginName) {
        super(ghidraMetricsPlugin.getTool(), pluginName, pluginName);
        this.plugin = ghidraMetricsPlugin;
        buildPanel();
    }

    // Customize GUI
    private void buildPanel() {
        panel = new JPanel(new BorderLayout());
        JTabbedPane tabbedPane = new JTabbedPane();

        mcCabeGui = new McCabeGui(plugin);
        tabbedPane.addTab("Complexity", mcCabeGui.getPanel());

        entropyGui = new EntropyGui(plugin);
        tabbedPane.addTab("Entropy", entropyGui.getPanel());

        halsteadGui = new HalsteadGui(plugin);
        tabbedPane.addTab("Halstead", halsteadGui.getPanel());

        ropSimilarityGui = new RopSurvivalGui(plugin);
        tabbedPane.addTab("ROP Survival", ropSimilarityGui.getPanel());

        similarityGui = new SimilarityGui(plugin);
        tabbedPane.addTab("Similarity", similarityGui.getPanel());

        panel.add(tabbedPane);
        setVisible(true);
    }

    public void handleProgramActivated() {
        halsteadGui.populateProgramTable();
        entropyGui.resetTable();
        mcCabeGui.resetTable();
        ropSimilarityGui.resetGui();
        similarityGui.resetPanel();
    }

    public void handleLocationChanged() {
        halsteadGui.populateFunctionTable();
    }

    @Override
    public JComponent getComponent() {
        return panel;
    }

}
