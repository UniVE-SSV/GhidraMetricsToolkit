package gui;

import ghidra.app.services.GoToService;
import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.Program;
import impl.common.Pair;
import impl.common.Similarity;
import impl.common.SimilarityInterface;
import impl.common.SimilarityResult;
import impl.metrics.*;
import impl.utils.FunctionUtils;
import metrics.GhidraMetricsToolkitPlugin;
import resources.Icons;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class SimilarityGui {

    private static final String[] columnNames = {"Simil.", "Weight", "Current Program", "Compared Program"};
    private static final SimilarityInterface[] metrics = {new Jaccard(), new JaroWinkler(), new Levenshtein(), new Lcs(), new Ncd(), new OpcodeFrequency(), new TLSH()};

    private final JPanel panel;
    private final GhidraTaskRunner taskRunner;
    private final ProgramPickerPanel programPicker;
    private final JComboBox<SimilarityInterface> metricChooser;
    private final JCheckBoxMenuItem exclusive;
    private final JCheckBoxMenuItem weighted;
    private final JCheckBoxMenuItem symmetric;
    private final JLabel overallSimilarity;
    private final DefaultTableModel tableModel;

    private final GhidraMetricsToolkitPlugin plugin;

    private Similarity similarity;
    private SimilarityResult lastResult;

    public SimilarityGui(GhidraMetricsToolkitPlugin plugin) {

        this.plugin = plugin;

        panel = new JPanel(new BorderLayout());
        taskRunner = new GhidraTaskRunner(plugin.getTool(), panel);

        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(tableModel);
        table.setRowSelectionAllowed(true);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setSelectionBackground(new Color(30, 144, 255)); // DodgerBlue
        table.setSelectionForeground(Color.WHITE);

        table.setRowSorter(new TableRowSorter<>(tableModel));

        DefaultTableCellRenderer doubleRenderer = new DefaultTableCellRenderer() {
            private final DecimalFormat formatter = new DecimalFormat("0.00");

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value instanceof Double) {

                    double doubleValue = (Double) value;

                    int red = (int) ((1.0 - doubleValue) * 255);
                    int green = (int) (doubleValue * 255);
                    int blue = 100;
                    red = (red + 255) / 2;
                    green = (green + 255) / 2;

                    c.setBackground(new Color(red, green, blue));
                    c.setForeground(Color.BLACK);

                    setText(formatter.format(value));
                } else {
                    c.setBackground(Color.WHITE);
                    c.setForeground(Color.BLACK);
                    setText("―");
                }

                return c;
            }
        };
        table.getColumnModel().getColumn(0).setCellRenderer(doubleRenderer);
        table.getColumnModel().getColumn(1).setCellRenderer(doubleRenderer);
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(0).setMinWidth(50);
        table.getColumnModel().getColumn(0).setMaxWidth(100);
        table.getColumnModel().getColumn(1).setPreferredWidth(50);
        table.getColumnModel().getColumn(1).setMinWidth(50);
        table.getColumnModel().getColumn(1).setMaxWidth(100);

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    int row = table.rowAtPoint(e.getPoint());
                    int column = table.columnAtPoint(e.getPoint());

                    if (row != -1 && column == 2) {
                        String functionName = (String) table.getValueAt(row, column);
                        Function function = FunctionUtils.getFunctionByName(plugin.getCurrentProgram(), functionName);
                        if (function != null) {
                            GoToService goToService = plugin.getTool().getService(GoToService.class);
                            goToService.goTo(function.getEntryPoint());
                        }
                    }
                }
            }
        });

        JPopupMenu popupMenu = new JPopupMenu();

        JMenuItem applyNameRight = new JMenuItem("Apply Name Right", Icons.RIGHT_ICON);
        JMenuItem applyNameLeft = new JMenuItem("Apply Name Left", Icons.LEFT_ICON);

        popupMenu.add(applyNameRight);
        popupMenu.add(applyNameLeft);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                showPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                showPopup(e);
            }

            private void showPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = table.rowAtPoint(e.getPoint());

                    if (row != -1 && !table.isRowSelected(row)) {
                        table.setRowSelectionInterval(row, row);
                    }

                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);

        exclusive = new JCheckBoxMenuItem("Exclusive");
        weighted = new JCheckBoxMenuItem("Weighted");
        symmetric = new JCheckBoxMenuItem("Symmetric");

        // Matching options are grouped behind a settings button
        JPopupMenu optionsPopup = new JPopupMenu();
        optionsPopup.add(exclusive);
        optionsPopup.add(weighted);
        optionsPopup.add(symmetric);

        JButton settingsButton = new JButton("Settings");
        settingsButton.setToolTipText("Matching options");
        settingsButton.addActionListener(e -> optionsPopup.show(settingsButton, 0, settingsButton.getHeight()));

        metricChooser = new JComboBox<>(metrics);
        metricChooser.setSelectedIndex(3);

        // Top panel: the metric and output rows are wrapping FlowLayouts (labels
        // keep their full text and wrap to a new line when narrow), while the
        // program picker sits in the CENTER and flexes its text field instead of
        // being clipped.
        JPanel topPanel = new JPanel(new BorderLayout(4, 4));

        JPanel metricSelection = new JPanel(new FlowLayout(FlowLayout.LEFT));
        metricSelection.add(new JLabel("Metric: "));
        metricSelection.add(metricChooser);
        metricSelection.add(settingsButton);
        topPanel.add(metricSelection, BorderLayout.NORTH);

        programPicker = new ProgramPickerPanel(plugin, this::computeMetrics);
        topPanel.add(programPicker, BorderLayout.CENTER);

        overallSimilarity = new JLabel("N/A");
        JPanel outputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        outputPanel.add(new JLabel("Overall Similarity: "));
        outputPanel.add(overallSimilarity);
        topPanel.add(outputPanel, BorderLayout.SOUTH);

        metricChooser.addActionListener(e -> computeMetrics());

        ActionListener checkBoxHandler = e -> {
            if (programPicker.getSelectedFile() != null && similarity != null) {
                recomputeOverallSimilarity();
            }
        };

        exclusive.addActionListener(checkBoxHandler);
        weighted.addActionListener(checkBoxHandler);
        symmetric.addActionListener(checkBoxHandler);

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        applyNameRight.addActionListener(evt -> {
            int[] selectedRows = table.getSelectedRows();
            Program program = programPicker.getComparedProgram();
            if (program == null) {
                return;
            }
            List<Pair<Function, String>> replacements = new ArrayList<>();

            for (int selectedRow : selectedRows) {
                int modelRow = table.convertRowIndexToModel(selectedRow);

                Function targetFunction = FunctionUtils.getFunctionByName(program, (String) tableModel.getValueAt(modelRow, 3));
                String newName = (String) tableModel.getValueAt(modelRow, 2);

                replacements.add(new Pair<>(targetFunction, newName));
            }

            FunctionUtils.applyNames(program, replacements);
            populateTable();
        });

        applyNameLeft.addActionListener(evt -> {
            int[] selectedRows = table.getSelectedRows();
            List<Pair<Function, String>> replacements = new ArrayList<>();

            for (int selectedRow : selectedRows) {
                int modelRow = table.convertRowIndexToModel(selectedRow);

                Function targetFunction = FunctionUtils.getFunctionByName(plugin.getCurrentProgram(), (String) tableModel.getValueAt(modelRow, 2));
                String newName = (String) tableModel.getValueAt(modelRow, 3);

                replacements.add(new Pair<>(targetFunction, newName));
            }

            FunctionUtils.applyNames(plugin.getCurrentProgram(), replacements);
            populateTable();
        });
    }

    private void computeMetrics() {
        SimilarityInterface metric = (SimilarityInterface) metricChooser.getSelectedItem();
        if (metric == null || programPicker.getSelectedFile() == null) {
            return;
        }
        Program currentProgram = plugin.getCurrentProgram();
        if (currentProgram == null) {
            return;
        }

        overallSimilarity.setText("N/A");

        // Run the (potentially very slow) pairwise computation in a Ghidra Task:
        // it executes off the EDT and shows an integrated, cancellable progress dialog.
        taskRunner.launch("Computing Similarity", (session, monitor) -> {
            monitor.setMessage("Loading compared program...");
            Program program = programPicker.loadComparedProgram(monitor);
            if (program == null) {
                throw new IOException("Could not load the selected program");
            }

            Similarity sim = new Similarity(currentProgram, program, metric, monitor);
            monitor.setMessage("Computing overall similarity...");
            SimilarityResult res = sim.getOverallSimilarity(exclusive.isSelected(), weighted.isSelected(), symmetric.isSelected(), monitor);
            res.sortBySimilarity();

            session.runOnUi(() -> {
                similarity = sim;
                lastResult = res;
                programPicker.setComparedProgram(program);
                overallSimilarity.setText(String.format("%.2f", lastResult.overallSimilarity));
                populateTable();
            });
        });
    }

    private void recomputeOverallSimilarity() {
        Similarity sim = similarity;
        if (sim == null) {
            return;
        }

        // Matching/overall similarity can be slow (Hungarian matching, NCD binary
        // compression), so run it in a Ghidra Task with progress and cancellation.
        taskRunner.launch("Computing Overall Similarity", (session, monitor) -> {
            monitor.setMessage("Matching functions...");
            SimilarityResult res = sim.getOverallSimilarity(exclusive.isSelected(), weighted.isSelected(), symmetric.isSelected(), monitor);
            res.sortBySimilarity();

            session.runOnUi(() -> {
                lastResult = res;
                overallSimilarity.setText(String.format("%.2f", lastResult.overallSimilarity));
                populateTable();
            });
        });
    }

    public void populateTable() {
        if (lastResult == null) {
            return;
        }
        tableModel.setRowCount(0);
        for (Object[] row : lastResult.getFunctionSimilarities()) {
            tableModel.addRow(row);
        }
    }

    public void resetPanel() {
        tableModel.setRowCount(0);
        taskRunner.invalidate();
        programPicker.reset();
        lastResult = null;
        similarity = null;
        metricChooser.setSelectedIndex(3);
        overallSimilarity.setText("N/A");
        exclusive.setSelected(false);
        weighted.setSelected(false);
        symmetric.setSelected(false);
    }

    public JPanel getPanel() {
        return panel;
    }
}
