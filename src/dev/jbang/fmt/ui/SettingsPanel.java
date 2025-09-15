package dev.jbang.fmt.ui;

import java.awt.*;
import java.util.*;
import java.util.Map;

import javax.swing.*;
import javax.swing.border.TitledBorder;

/**
 * Panel for managing Eclipse formatter settings organized in logical groups.
 */
public class SettingsPanel extends JPanel {
    
    private final Runnable onSettingsChanged;
    private final Map<String, JComponent> settingComponents = new HashMap<>();
    private final Map<String, String> currentSettings = new HashMap<>();
    
    public SettingsPanel(Runnable onSettingsChanged) {
        this.onSettingsChanged = onSettingsChanged;
        initializeUI();
    }
    
    private void initializeUI() {
        setLayout(new BorderLayout());
        
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // Basic formatting tab
        tabbedPane.addTab("Basic", createBasicTab());
        
        // Indentation tab
        tabbedPane.addTab("Indentation", createIndentationTab());
        
        // Braces tab
        tabbedPane.addTab("Braces", createBracesTab());
        
        // Line wrapping tab
        tabbedPane.addTab("Line Wrapping", createLineWrappingTab());
        
        // Comments tab
        tabbedPane.addTab("Comments", createCommentsTab());
        
        // Whitespace tab
        tabbedPane.addTab("Whitespace", createWhitespaceTab());
        
        // Blank lines tab
        tabbedPane.addTab("Blank Lines", createBlankLinesTab());
        
        // Control statements tab
        tabbedPane.addTab("Control Statements", createControlStatementsTab());
        
        add(tabbedPane, BorderLayout.CENTER);
        
        // Bottom panel with preset buttons
        add(createPresetPanel(), BorderLayout.SOUTH);
    }
    
    private JPanel createBasicTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.anchor = GridBagConstraints.WEST;
        
        int row = 0;
        
        // Line length
        gbc.gridx = 0; gbc.gridy = row++;
        panel.add(new JLabel("Line Length:"), gbc);
        gbc.gridx = 1;
        JSpinner lineLengthSpinner = new JSpinner(new SpinnerNumberModel(120, 40, 200, 10));
        settingComponents.put("org.eclipse.jdt.core.formatter.lineSplit", lineLengthSpinner);
        panel.add(lineLengthSpinner, gbc);
        
        // Comment line length
        gbc.gridx = 0; gbc.gridy = row++;
        panel.add(new JLabel("Comment Line Length:"), gbc);
        gbc.gridx = 1;
        JSpinner commentLineLengthSpinner = new JSpinner(new SpinnerNumberModel(120, 40, 200, 10));
        settingComponents.put("org.eclipse.jdt.core.formatter.comment.line_length", commentLineLengthSpinner);
        panel.add(commentLineLengthSpinner, gbc);
        
        // Tabulation character
        gbc.gridx = 0; gbc.gridy = row++;
        panel.add(new JLabel("Indent Character:"), gbc);
        gbc.gridx = 1;
        JComboBox<String> tabCharCombo = new JComboBox<>(new String[]{"space", "tab"});
        settingComponents.put("org.eclipse.jdt.core.formatter.tabulation.char", tabCharCombo);
        panel.add(tabCharCombo, gbc);
        
        // Tabulation size
        gbc.gridx = 0; gbc.gridy = row++;
        panel.add(new JLabel("Tab Size:"), gbc);
        gbc.gridx = 1;
        JSpinner tabSizeSpinner = new JSpinner(new SpinnerNumberModel(4, 1, 8, 1));
        settingComponents.put("org.eclipse.jdt.core.formatter.tabulation.size", tabSizeSpinner);
        panel.add(tabSizeSpinner, gbc);
        
        // Indentation size
        gbc.gridx = 0; gbc.gridy = row++;
        panel.add(new JLabel("Indent Size:"), gbc);
        gbc.gridx = 1;
        JSpinner indentSizeSpinner = new JSpinner(new SpinnerNumberModel(4, 1, 8, 1));
        settingComponents.put("org.eclipse.jdt.core.formatter.indentation.size", indentSizeSpinner);
        panel.add(indentSizeSpinner, gbc);
        
        // Continuation indentation
        gbc.gridx = 0; gbc.gridy = row++;
        panel.add(new JLabel("Continuation Indent:"), gbc);
        gbc.gridx = 1;
        JSpinner continuationSpinner = new JSpinner(new SpinnerNumberModel(2, 0, 8, 1));
        settingComponents.put("org.eclipse.jdt.core.formatter.continuation_indentation", continuationSpinner);
        panel.add(continuationSpinner, gbc);
        
        addChangeListeners();
        
        return panel;
    }
    
    private JPanel createIndentationTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.anchor = GridBagConstraints.WEST;
        
        int row = 0;
        
        // Array initializer indentation
        gbc.gridx = 0; gbc.gridy = row++;
        panel.add(new JLabel("Array Initializer Indent:"), gbc);
        gbc.gridx = 1;
        JComboBox<String> arrayInitCombo = new JComboBox<>(new String[]{"0", "1", "2"});
        settingComponents.put("org.eclipse.jdt.core.formatter.indentation.size", arrayInitCombo);
        panel.add(arrayInitCombo, gbc);
        
        // Type declaration indentation
        gbc.gridx = 0; gbc.gridy = row++;
        panel.add(new JLabel("Type Declaration Indent:"), gbc);
        gbc.gridx = 1;
        JComboBox<String> typeDeclCombo = new JComboBox<>(new String[]{"0", "1", "2"});
        settingComponents.put("org.eclipse.jdt.core.formatter.indentation.size", typeDeclCombo);
        panel.add(typeDeclCombo, gbc);
        
        addChangeListeners();
        
        return panel;
    }
    
    private JPanel createBracesTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.anchor = GridBagConstraints.WEST;
        
        int row = 0;
        
        // Brace position for type declaration
        gbc.gridx = 0; gbc.gridy = row++;
        panel.add(new JLabel("Type Declaration Braces:"), gbc);
        gbc.gridx = 1;
        JComboBox<String> typeBraceCombo = new JComboBox<>(new String[]{"end_of_line", "next_line", "next_line_indented"});
        settingComponents.put("org.eclipse.jdt.core.formatter.brace_position_for_type_declaration", typeBraceCombo);
        panel.add(typeBraceCombo, gbc);
        
        // Brace position for method declaration
        gbc.gridx = 0; gbc.gridy = row++;
        panel.add(new JLabel("Method Declaration Braces:"), gbc);
        gbc.gridx = 1;
        JComboBox<String> methodBraceCombo = new JComboBox<>(new String[]{"end_of_line", "next_line", "next_line_indented"});
        settingComponents.put("org.eclipse.jdt.core.formatter.brace_position_for_method_declaration", methodBraceCombo);
        panel.add(methodBraceCombo, gbc);
        
        // Brace position for constructor declaration
        gbc.gridx = 0; gbc.gridy = row++;
        panel.add(new JLabel("Constructor Declaration Braces:"), gbc);
        gbc.gridx = 1;
        JComboBox<String> constructorBraceCombo = new JComboBox<>(new String[]{"end_of_line", "next_line", "next_line_indented"});
        settingComponents.put("org.eclipse.jdt.core.formatter.brace_position_for_constructor_declaration", constructorBraceCombo);
        panel.add(constructorBraceCombo, gbc);
        
        // Brace position for blocks
        gbc.gridx = 0; gbc.gridy = row++;
        panel.add(new JLabel("Block Braces:"), gbc);
        gbc.gridx = 1;
        JComboBox<String> blockBraceCombo = new JComboBox<>(new String[]{"end_of_line", "next_line", "next_line_indented"});
        settingComponents.put("org.eclipse.jdt.core.formatter.brace_position_for_block", blockBraceCombo);
        panel.add(blockBraceCombo, gbc);
        
        // Brace position for switch
        gbc.gridx = 0; gbc.gridy = row++;
        panel.add(new JLabel("Switch Braces:"), gbc);
        gbc.gridx = 1;
        JComboBox<String> switchBraceCombo = new JComboBox<>(new String[]{"end_of_line", "next_line", "next_line_indented"});
        settingComponents.put("org.eclipse.jdt.core.formatter.brace_position_for_switch", switchBraceCombo);
        panel.add(switchBraceCombo, gbc);
        
        addChangeListeners();
        
        return panel;
    }
    
    private JPanel createLineWrappingTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.anchor = GridBagConstraints.WEST;
        
        int row = 0;
        
        // Never join already wrapped lines
        gbc.gridx = 0; gbc.gridy = row++;
        JCheckBox neverJoinCheckBox = new JCheckBox("Never join already wrapped lines");
        settingComponents.put("org.eclipse.jdt.core.formatter.never_join_already_wrapped_lines", neverJoinCheckBox);
        panel.add(neverJoinCheckBox, gbc);
        
        // Wrap before operators
        gbc.gridx = 0; gbc.gridy = row++;
        JCheckBox wrapBeforeOpsCheckBox = new JCheckBox("Wrap before operators");
        settingComponents.put("org.eclipse.jdt.core.formatter.wrap_before_or_operator_multicatch", wrapBeforeOpsCheckBox);
        panel.add(wrapBeforeOpsCheckBox, gbc);
        
        // Align with spaces
        gbc.gridx = 0; gbc.gridy = row++;
        JCheckBox alignWithSpacesCheckBox = new JCheckBox("Align with spaces");
        settingComponents.put("org.eclipse.jdt.core.formatter.align_with_spaces", alignWithSpacesCheckBox);
        panel.add(alignWithSpacesCheckBox, gbc);
        
        addChangeListeners();
        
        return panel;
    }
    
    private JPanel createCommentsTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.anchor = GridBagConstraints.WEST;
        
        int row = 0;
        
        // Format Javadoc comments
        gbc.gridx = 0; gbc.gridy = row++;
        JCheckBox formatJavadocCheckBox = new JCheckBox("Format Javadoc comments");
        settingComponents.put("org.eclipse.jdt.core.formatter.comment.format_javadoc_comments", formatJavadocCheckBox);
        panel.add(formatJavadocCheckBox, gbc);
        
        // New lines at block boundaries
        gbc.gridx = 0; gbc.gridy = row++;
        JCheckBox newLinesAtBlockCheckBox = new JCheckBox("New lines at block boundaries");
        settingComponents.put("org.eclipse.jdt.core.formatter.comment.new_lines_at_block_boundaries", newLinesAtBlockCheckBox);
        panel.add(newLinesAtBlockCheckBox, gbc);
        
        // Insert new line for parameter
        gbc.gridx = 0; gbc.gridy = row++;
        JCheckBox newLineForParamCheckBox = new JCheckBox("Insert new line for parameter");
        settingComponents.put("org.eclipse.jdt.core.formatter.comment.insert_new_line_for_parameter", newLineForParamCheckBox);
        panel.add(newLineForParamCheckBox, gbc);
        
        // Insert new line before root tags
        gbc.gridx = 0; gbc.gridy = row++;
        JCheckBox newLineBeforeRootTagsCheckBox = new JCheckBox("Insert new line before root tags");
        settingComponents.put("org.eclipse.jdt.core.formatter.comment.insert_new_line_before_root_tags", newLineBeforeRootTagsCheckBox);
        panel.add(newLineBeforeRootTagsCheckBox, gbc);
        
        // Indent root tags
        gbc.gridx = 0; gbc.gridy = row++;
        JCheckBox indentRootTagsCheckBox = new JCheckBox("Indent root tags");
        settingComponents.put("org.eclipse.jdt.core.formatter.comment.indent_root_tags", indentRootTagsCheckBox);
        panel.add(indentRootTagsCheckBox, gbc);
        
        // Count line length from starting position
        gbc.gridx = 0; gbc.gridy = row++;
        JCheckBox countLineLengthCheckBox = new JCheckBox("Count line length from starting position");
        settingComponents.put("org.eclipse.jdt.core.formatter.comment.count_line_length_from_starting_position", countLineLengthCheckBox);
        panel.add(countLineLengthCheckBox, gbc);
        
        addChangeListeners();
        
        return panel;
    }
    
    private JPanel createWhitespaceTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.anchor = GridBagConstraints.WEST;
        
        int row = 0;
        
        // Space after comma
        gbc.gridx = 0; gbc.gridy = row++;
        JCheckBox spaceAfterCommaCheckBox = new JCheckBox("Space after comma");
        settingComponents.put("org.eclipse.jdt.core.formatter.insert_space_after_comma_in_type_arguments", spaceAfterCommaCheckBox);
        panel.add(spaceAfterCommaCheckBox, gbc);
        
        // Space before comma
        gbc.gridx = 0; gbc.gridy = row++;
        JCheckBox spaceBeforeCommaCheckBox = new JCheckBox("Space before comma");
        settingComponents.put("org.eclipse.jdt.core.formatter.insert_space_before_comma_in_type_arguments", spaceBeforeCommaCheckBox);
        panel.add(spaceBeforeCommaCheckBox, gbc);
        
        // Space after semicolon
        gbc.gridx = 0; gbc.gridy = row++;
        JCheckBox spaceAfterSemicolonCheckBox = new JCheckBox("Space after semicolon");
        settingComponents.put("org.eclipse.jdt.core.formatter.insert_space_after_semicolon_in_for", spaceAfterSemicolonCheckBox);
        panel.add(spaceAfterSemicolonCheckBox, gbc);
        
        // Space after assignment operator
        gbc.gridx = 0; gbc.gridy = row++;
        JCheckBox spaceAfterAssignmentCheckBox = new JCheckBox("Space after assignment operator");
        settingComponents.put("org.eclipse.jdt.core.formatter.insert_space_after_assignment_operator", spaceAfterAssignmentCheckBox);
        panel.add(spaceAfterAssignmentCheckBox, gbc);
        
        // Space after logical operator
        gbc.gridx = 0; gbc.gridy = row++;
        JCheckBox spaceAfterLogicalCheckBox = new JCheckBox("Space after logical operator");
        settingComponents.put("org.eclipse.jdt.core.formatter.insert_space_after_logical_operator", spaceAfterLogicalCheckBox);
        panel.add(spaceAfterLogicalCheckBox, gbc);
        
        addChangeListeners();
        
        return panel;
    }
    
    private JPanel createBlankLinesTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.anchor = GridBagConstraints.WEST;
        
        int row = 0;
        
        // Blank lines before imports
        gbc.gridx = 0; gbc.gridy = row++;
        panel.add(new JLabel("Blank lines before imports:"), gbc);
        gbc.gridx = 1;
        JSpinner blankLinesBeforeImportsSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 5, 1));
        settingComponents.put("org.eclipse.jdt.core.formatter.blank_lines_before_imports", blankLinesBeforeImportsSpinner);
        panel.add(blankLinesBeforeImportsSpinner, gbc);
        
        // Blank lines after imports
        gbc.gridx = 0; gbc.gridy = row++;
        panel.add(new JLabel("Blank lines after imports:"), gbc);
        gbc.gridx = 1;
        JSpinner blankLinesAfterImportsSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 5, 1));
        settingComponents.put("org.eclipse.jdt.core.formatter.blank_lines_after_imports", blankLinesAfterImportsSpinner);
        panel.add(blankLinesAfterImportsSpinner, gbc);
        
        // Blank lines after package
        gbc.gridx = 0; gbc.gridy = row++;
        panel.add(new JLabel("Blank lines after package:"), gbc);
        gbc.gridx = 1;
        JSpinner blankLinesAfterPackageSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 5, 1));
        settingComponents.put("org.eclipse.jdt.core.formatter.blank_lines_after_package", blankLinesAfterPackageSpinner);
        panel.add(blankLinesAfterPackageSpinner, gbc);
        
        // Number of blank lines before code block
        gbc.gridx = 0; gbc.gridy = row++;
        panel.add(new JLabel("Blank lines before code block:"), gbc);
        gbc.gridx = 1;
        JSpinner blankLinesBeforeCodeBlockSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 5, 1));
        settingComponents.put("org.eclipse.jdt.core.formatter.number_of_blank_lines_before_code_block", blankLinesBeforeCodeBlockSpinner);
        panel.add(blankLinesBeforeCodeBlockSpinner, gbc);
        
        // Number of blank lines at end of method body
        gbc.gridx = 0; gbc.gridy = row++;
        panel.add(new JLabel("Blank lines at end of method:"), gbc);
        gbc.gridx = 1;
        JSpinner blankLinesAtEndOfMethodSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 5, 1));
        settingComponents.put("org.eclipse.jdt.core.formatter.number_of_blank_lines_at_end_of_method_body", blankLinesAtEndOfMethodSpinner);
        panel.add(blankLinesAtEndOfMethodSpinner, gbc);
        
        addChangeListeners();
        
        return panel;
    }
    
    private JPanel createControlStatementsTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.anchor = GridBagConstraints.WEST;
        
        int row = 0;
        
        // Keep then statement on same line
        gbc.gridx = 0; gbc.gridy = row++;
        JCheckBox keepThenOnSameLineCheckBox = new JCheckBox("Keep then statement on same line");
        settingComponents.put("org.eclipse.jdt.core.formatter.keep_then_statement_on_same_line", keepThenOnSameLineCheckBox);
        panel.add(keepThenOnSameLineCheckBox, gbc);
        
        // Keep else statement on same line
        gbc.gridx = 0; gbc.gridy = row++;
        JCheckBox keepElseOnSameLineCheckBox = new JCheckBox("Keep else statement on same line");
        settingComponents.put("org.eclipse.jdt.core.formatter.keep_else_statement_on_same_line", keepElseOnSameLineCheckBox);
        panel.add(keepElseOnSameLineCheckBox, gbc);
        
        // Keep simple if on one line
        gbc.gridx = 0; gbc.gridy = row++;
        JCheckBox keepSimpleIfOnOneLineCheckBox = new JCheckBox("Keep simple if on one line");
        settingComponents.put("org.eclipse.jdt.core.formatter.keep_simple_if_on_one_line", keepSimpleIfOnOneLineCheckBox);
        panel.add(keepSimpleIfOnOneLineCheckBox, gbc);
        
        addChangeListeners();
        
        return panel;
    }
    
    private JPanel createPresetPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(new TitledBorder("Presets"));
        
        JButton eclipseButton = new JButton("Eclipse Default");
        eclipseButton.addActionListener(e -> loadPreset("eclipse"));
        panel.add(eclipseButton);
        
        JButton googleButton = new JButton("Google Style");
        googleButton.addActionListener(e -> loadPreset("google"));
        panel.add(googleButton);
        
        JButton jbangButton = new JButton("JBang Style");
        jbangButton.addActionListener(e -> loadPreset("jbang"));
        panel.add(jbangButton);
        
        JButton javaButton = new JButton("Java Style");
        javaButton.addActionListener(e -> loadPreset("java"));
        panel.add(javaButton);
        
        return panel;
    }
    
    private void addChangeListeners() {
        for (JComponent component : settingComponents.values()) {
            if (component instanceof JSpinner) {
                ((JSpinner) component).addChangeListener(e -> onSettingsChanged.run());
            } else if (component instanceof JComboBox) {
                ((JComboBox<?>) component).addActionListener(e -> onSettingsChanged.run());
            } else if (component instanceof JCheckBox) {
                ((JCheckBox) component).addActionListener(e -> onSettingsChanged.run());
            }
        }
    }
    
    private void loadPreset(String presetName) {
        try {
            Map<String, String> settings = dev.jbang.fmt.JavaFormatter.loadSettingsFromClasspath(presetName);
            loadSettings(settings);
            onSettingsChanged.run();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading preset: " + e.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public void loadSettings(Map<String, String> settings) {
        currentSettings.clear();
        currentSettings.putAll(settings);
        
        // Update UI components
        for (Map.Entry<String, JComponent> entry : settingComponents.entrySet()) {
            String key = entry.getKey();
            JComponent component = entry.getValue();
            String value = settings.get(key);
            
            if (value != null) {
                if (component instanceof JSpinner) {
                    try {
                        ((JSpinner) component).setValue(Integer.parseInt(value));
                    } catch (NumberFormatException e) {
                        // Ignore invalid numbers
                    }
                } else if (component instanceof JComboBox) {
                    ((JComboBox<?>) component).setSelectedItem(value);
                } else if (component instanceof JCheckBox) {
                    ((JCheckBox) component).setSelected("true".equals(value) || "insert".equals(value));
                }
            }
        }
    }
    
    public Map<String, String> getCurrentSettings() {
        Map<String, String> settings = new HashMap<>(currentSettings);
        
        // Update from UI components
        for (Map.Entry<String, JComponent> entry : settingComponents.entrySet()) {
            String key = entry.getKey();
            JComponent component = entry.getValue();
            
            if (component instanceof JSpinner) {
                settings.put(key, ((JSpinner) component).getValue().toString());
            } else if (component instanceof JComboBox) {
                Object selected = ((JComboBox<?>) component).getSelectedItem();
                if (selected != null) {
                    settings.put(key, selected.toString());
                }
            } else if (component instanceof JCheckBox) {
                settings.put(key, ((JCheckBox) component).isSelected() ? "insert" : "do not insert");
            }
        }
        
        return settings;
    }
}
