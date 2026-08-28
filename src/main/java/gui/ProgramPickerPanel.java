package gui;

import ghidra.framework.main.ProgramFileChooser;
import ghidra.framework.model.DomainFile;
import ghidra.program.model.listing.Program;
import ghidra.util.task.TaskMonitor;
import impl.utils.ProjectUtils;
import metrics.GhidraMetricsToolkitPlugin;

import javax.swing.*;
import java.awt.*;

/**
 * Reusable "Compare to" program picker: a read-only text field showing the
 * currently selected program plus a Browse button that opens Ghidra's native
 * program chooser ({@link ProgramFileChooser}).
 *
 * Keeps track of the picked {@link DomainFile} and the loaded {@link Program},
 * releasing the loaded program (via {@link ProjectUtils#releaseProgram}) when it
 * is replaced or reset. The compared program is not opened in the tool; it is
 * loaded on demand through {@link #loadComparedProgram(TaskMonitor)} and
 * registered with {@link #setComparedProgram(Program)} once available.
 */
public class ProgramPickerPanel extends JPanel {

    private final GhidraMetricsToolkitPlugin plugin;
    private final Runnable onProgramPicked;
    private final JTextField programNameField;
    private DomainFile selectedFile;
    private Program comparedProgram;

    public ProgramPickerPanel(GhidraMetricsToolkitPlugin plugin, Runnable onProgramPicked) {
        super(new GridBagLayout());
        this.plugin = plugin;
        this.onProgramPicked = onProgramPicked;

        programNameField = new JTextField(20);
        programNameField.setEditable(false);
        // allow the field to shrink when the window is narrow instead of clipping
        programNameField.setMinimumSize(new Dimension(60, programNameField.getPreferredSize().height));
        JButton browseButton = new JButton("Browse...");
        browseButton.addActionListener(e -> selectProgram());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 0, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        add(new JLabel("Compare to: "), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        add(programNameField, gbc);

        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        add(browseButton, gbc);
    }

    private void selectProgram() {
        ProgramFileChooser chooser = new ProgramFileChooser(plugin.getTool().getToolFrame(), "Select Program", plugin.getTool().getProject());
        chooser.show();
        DomainFile domainFile = chooser.getDomainFile();
        if (domainFile == null) {
            return; // user cancelled
        }

        releaseComparedProgram();
        selectedFile = domainFile;
        programNameField.setText(domainFile.getName());
        onProgramPicked.run();
    }

    public DomainFile getSelectedFile() {
        return selectedFile;
    }

    public Program getComparedProgram() {
        return comparedProgram;
    }

    /**
     * Registers the loaded comparison program (call after the computation task
     * has loaded it). Replaces and releases any previously loaded program.
     */
    public void setComparedProgram(Program program) {
        releaseComparedProgram();
        comparedProgram = program;
    }

    /**
     * Loads (or reuses, if already open in the tool) the picked program. The
     * returned program is owned by the picker and released on replace/reset.
     */
    public Program loadComparedProgram(TaskMonitor monitor) {
        return ProjectUtils.getProgramFromDomainFile(selectedFile, plugin, monitor);
    }

    /**
     * Clears the selection and releases the loaded comparison program.
     */
    public void reset() {
        releaseComparedProgram();
        selectedFile = null;
        programNameField.setText("");
    }

    private void releaseComparedProgram() {
        ProjectUtils.releaseProgram(comparedProgram, plugin);
        comparedProgram = null;
    }
}
