package gui;

import generic.stl.Pair;
import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.Program;
import ghidra.program.util.ProgramLocation;
import impl.metrics.Halstead;
import metrics.GhidraMetricsToolkitPlugin;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class HalsteadGui {

    private static final String[] columnNames = {"Metric", "Value"};

    private final JPanel panel;
    private final GhidraMetricsToolkitPlugin plugin;

    public HalsteadGui(GhidraMetricsToolkitPlugin plugin) {

        this.plugin = plugin;

        panel = new JPanel(new GridLayout(2, 1));
        JPanel panelFunction = new JPanel(new BorderLayout());
        panelFunction.add(new JLabel("Current function: "), BorderLayout.NORTH);

        DefaultTableModel tableModelFunction = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable tableFunction = new JTable(tableModelFunction);
        panelFunction.add(new JScrollPane(tableFunction), BorderLayout.CENTER);

        JPanel panelProgram = new JPanel(new BorderLayout());
        panelProgram.add(new JLabel("Current program:"), BorderLayout.NORTH);

        DefaultTableModel tableModelProgram = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable tableProgram = new JTable(tableModelProgram);
        panelProgram.add(new JScrollPane(tableProgram), BorderLayout.CENTER);

        panel.add(panelFunction);
        panel.add(panelProgram);

        panel.putClientProperty("tableModelFunction", tableModelFunction);
        panel.putClientProperty("tableModelProgram", tableModelProgram);
    }

    public void populateFunctionTable() {
        DefaultTableModel tableModelFunction = (DefaultTableModel) panel.getClientProperty("tableModelFunction");

        if (tableModelFunction != null) {
            tableModelFunction.setRowCount(0);
            ProgramLocation location = plugin.getProgramLocation();
            if (location != null) {
                Function currentFunction = plugin.getCurrentProgram().getFunctionManager().getFunctionContaining(location.getAddress());
                if (currentFunction != null) {
                    Halstead halstead = new Halstead(plugin.getCurrentProgram(), currentFunction);
                    Halstead.Result result = (Halstead.Result) halstead.compute();
                    if (result != null && result.functionHalstead != null) {
                        for (Pair<String, Double> row : result.functionHalstead) {
                            tableModelFunction.addRow(new Object[] {row.first, row.second});
                        }
                    }
                }
            }
        }
    }

    public void populateProgramTable() {
        DefaultTableModel tableModelProgram = (DefaultTableModel) panel.getClientProperty("tableModelProgram");

        Program currentProgram = plugin.getCurrentProgram();
        if (tableModelProgram != null) {
            tableModelProgram.setRowCount(0);
            if (currentProgram != null) {
                Halstead halstead = new Halstead(plugin.getCurrentProgram());
                Halstead.Result result = (Halstead.Result) halstead.compute();
                if (result != null) {
                    for (Pair<String, Double> row : result.programHalstead) {
                        tableModelProgram.addRow(new Object[] {row.first, row.second});
                    }
                }
            }
        }
    }

    public JPanel getPanel() {
        return panel;
    }
}
