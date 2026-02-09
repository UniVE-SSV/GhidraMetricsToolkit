package gui;

import generic.stl.Pair;
import ghidra.app.services.GoToService;
import ghidra.framework.model.DomainFile;
import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.Program;
import ghidra.util.Msg;
import impl.common.Similarity;
import impl.common.SimilarityInterface;
import impl.common.SimilarityResult;
import impl.metrics.*;
import impl.utils.FunctionUtils;
import impl.utils.ProjectUtils;
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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SimilarityGui {

    private static final String[] columnNames = {"Simil.", "Weight", "Current Program", "Compared Program"};
    private static final SimilarityInterface[] metrics = {new Jaccard(), new JaroWinkler(), new Levenshtein(), new Lcs(), new Ncd(), new OpcodeFrequency()};
    private final JPanel panel;
    private final JComboBox<SimilarityInterface> metricChooser;
    private final JComboBox<DomainFile> programChooser;
    private final JCheckBox exclusive;
    private final JCheckBox weighted;
    private final JCheckBox symmetric;
    private final JLabel overallSimilarity;

    private final GhidraMetricsToolkitPlugin plugin;

    private Similarity similarity;
    private SimilarityResult lastResult;

    public SimilarityGui(GhidraMetricsToolkitPlugin plugin) {

        this.plugin = plugin;

        panel = new JPanel(new BorderLayout());

        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
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

        List<DomainFile> programFiles = ProjectUtils.getPrograms(plugin.getTool().getProject());

        JPanel topPanel = new JPanel(new BorderLayout());

        JPanel metricSelection = new JPanel(new FlowLayout(FlowLayout.LEFT));
        metricChooser = new JComboBox<>(metrics);
        metricChooser.setSelectedIndex(3);
        metricSelection.add(new JLabel("Metric: "));
        metricSelection.add(metricChooser);

        topPanel.add(metricSelection, BorderLayout.NORTH);

        JPanel settingsPanel = new JPanel(new BorderLayout());

        JPanel leftTopPanel = new JPanel(new BorderLayout());
        JPanel rightTopPanel = new JPanel(new FlowLayout());

        exclusive = new JCheckBox("Exclusive");
        weighted = new JCheckBox("Weighted");
        symmetric = new JCheckBox("Symmetric");

        rightTopPanel.add(exclusive);
        rightTopPanel.add(weighted);
        rightTopPanel.add(symmetric);

        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel outputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        overallSimilarity = new JLabel("N/A");
        outputPanel.add(new JLabel("Overall Similarity: "));
        outputPanel.add(overallSimilarity);


        programChooser = new JComboBox<>();
        for (DomainFile program : programFiles) {
            programChooser.addItem(program);
        }
        programChooser.setSelectedIndex(-1);
        programChooser.setVisible(true);

        programChooser.addActionListener(e -> computeMetrics());

        metricChooser.addActionListener(e -> computeMetrics());

        ActionListener checkBoxHandler = e -> {
            if (programChooser.getSelectedIndex() >= 0 && similarity != null) {
                lastResult = similarity.getOverallSimilarity(exclusive.isSelected(), weighted.isSelected(), symmetric.isSelected());
                lastResult.sortBySimilarity();
                overallSimilarity.setText(String.format("%.2f", lastResult.overallSimilarity));
                populateTable();
            }
        };

        exclusive.addActionListener(checkBoxHandler);
        weighted.addActionListener(checkBoxHandler);
        symmetric.addActionListener(checkBoxHandler);

        inputPanel.add(new JLabel("Compare to: "));
        inputPanel.add(programChooser);

        leftTopPanel.add(inputPanel, BorderLayout.NORTH);
        leftTopPanel.add(outputPanel, BorderLayout.CENTER);

        settingsPanel.add(leftTopPanel, BorderLayout.WEST);
        settingsPanel.add(rightTopPanel, BorderLayout.EAST);
        topPanel.add(settingsPanel, BorderLayout.CENTER);

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        applyNameRight.addActionListener(evt -> {
            int[] selectedRows = table.getSelectedRows();
            List<Pair<Function, String>> replacements = new ArrayList<>();
            DomainFile choice = (DomainFile) programChooser.getSelectedItem();
            Program program = ProjectUtils.getProgramFromDomainFile(choice);

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

        panel.putClientProperty("tableModel", tableModel);
    }

    private void computeMetrics() {
        try {
            DomainFile choice = (DomainFile) programChooser.getSelectedItem();
            if (choice != null) {
                Program program = ProjectUtils.getProgramFromDomainFile(choice);
                SimilarityInterface metric = (SimilarityInterface) metricChooser.getSelectedItem();
                similarity = new Similarity(plugin.getCurrentProgram(), program, metric);
                lastResult = similarity.getOverallSimilarity(exclusive.isSelected(), weighted.isSelected(), symmetric.isSelected());
                lastResult.sortBySimilarity();
                overallSimilarity.setText(String.format("%.2f", lastResult.overallSimilarity));
                populateTable();
            }
        } catch (Exception ex) {
            Msg.showError(getClass(), panel, "Metric computation failed!", Arrays.toString(ex.getStackTrace()));
            overallSimilarity.setText("N/A");
            programChooser.setSelectedIndex(-1);
        }
    }

    public void populateTable() {
        DefaultTableModel tableModel = (DefaultTableModel) panel.getClientProperty("tableModel");
        if (tableModel != null) {
            tableModel.setRowCount(0);
            for (Object[] row : lastResult.getFunctionSimilarities()) {
                tableModel.addRow(row);
            }
        }
    }

    public void resetPanel() {
        DefaultTableModel tableModel = (DefaultTableModel) panel.getClientProperty("tableModel");
        if (tableModel != null) {
            tableModel.setRowCount(0);
            programChooser.setSelectedIndex(-1);
        }
        lastResult = null;
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
