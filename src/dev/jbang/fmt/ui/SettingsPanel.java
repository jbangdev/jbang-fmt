package dev.jbang.fmt.ui;

import java.awt.*;
import java.util.*;
import java.util.Map;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import javax.swing.RowFilter;

import dev.jbang.fmt.FmtLogger;

/**
 * Panel for managing Eclipse formatter settings organized in logical groups.
 */
public class SettingsPanel extends JPanel {

	private final Runnable onSettingsChanged;
	private final Map<String, JComponent> settingComponents = new HashMap<>();
	private final Map<String, String> currentSettings = new HashMap<>();
	private final Map<String, PropertyDefinition> propertyDefinitions = new HashMap<>();
	private SettingsTableModel tableModel;

	public SettingsPanel(Runnable onSettingsChanged) {
		this.onSettingsChanged = onSettingsChanged;
		initializePropertyDefinitions();
		initializeUI();
	}

	/**
	 * Defines the possible values and UI component type for a formatter property.
	 */
	private static class PropertyDefinition {
		final String[] possibleValues;
		final ComponentType componentType;
		final String displayName;

		PropertyDefinition(String[] possibleValues, ComponentType componentType, String displayName) {
			this.possibleValues = possibleValues;
			this.componentType = componentType;
			this.displayName = displayName;
		}

		enum ComponentType {
			SPINNER, COMBOBOX, CHECKBOX, TEXTFIELD
		}
	}

	private void initializePropertyDefinitions() {
		// Line length settings
		propertyDefinitions.put("org.eclipse.jdt.core.formatter.lineSplit",
				new PropertyDefinition(new String[] { "80", "100", "120", "140", "160" },
						PropertyDefinition.ComponentType.SPINNER, "Line Length"));
		propertyDefinitions.put("org.eclipse.jdt.core.formatter.comment.line_length",
				new PropertyDefinition(new String[] { "80", "100", "120", "140", "160" },
						PropertyDefinition.ComponentType.SPINNER, "Comment Line Length"));

		// Tabulation settings
		propertyDefinitions.put("org.eclipse.jdt.core.formatter.tabulation.char",
				new PropertyDefinition(new String[] { "space", "tab" },
						PropertyDefinition.ComponentType.COMBOBOX, "Indent Character"));
		propertyDefinitions.put("org.eclipse.jdt.core.formatter.tabulation.size",
				new PropertyDefinition(new String[] { "2", "4", "8" },
						PropertyDefinition.ComponentType.SPINNER, "Tab Size"));
		propertyDefinitions.put("org.eclipse.jdt.core.formatter.indentation.size",
				new PropertyDefinition(new String[] { "2", "4", "8" },
						PropertyDefinition.ComponentType.SPINNER, "Indent Size"));
		propertyDefinitions.put("org.eclipse.jdt.core.formatter.continuation_indentation",
				new PropertyDefinition(new String[] { "1", "2", "4" },
						PropertyDefinition.ComponentType.SPINNER, "Continuation Indent"));

		// Brace position settings
		propertyDefinitions.put("org.eclipse.jdt.core.formatter.brace_position_for_type_declaration",
				new PropertyDefinition(new String[] { "end_of_line", "next_line", "next_line_indented" },
						PropertyDefinition.ComponentType.COMBOBOX, "Type Declaration Braces"));
		propertyDefinitions.put("org.eclipse.jdt.core.formatter.brace_position_for_method_declaration",
				new PropertyDefinition(new String[] { "end_of_line", "next_line", "next_line_indented" },
						PropertyDefinition.ComponentType.COMBOBOX, "Method Declaration Braces"));
		propertyDefinitions.put("org.eclipse.jdt.core.formatter.brace_position_for_constructor_declaration",
				new PropertyDefinition(new String[] { "end_of_line", "next_line", "next_line_indented" },
						PropertyDefinition.ComponentType.COMBOBOX, "Constructor Declaration Braces"));
		propertyDefinitions.put("org.eclipse.jdt.core.formatter.brace_position_for_block",
				new PropertyDefinition(new String[] { "end_of_line", "next_line", "next_line_indented" },
						PropertyDefinition.ComponentType.COMBOBOX, "Block Braces"));
		propertyDefinitions.put("org.eclipse.jdt.core.formatter.brace_position_for_switch",
				new PropertyDefinition(new String[] { "end_of_line", "next_line", "next_line_indented" },
						PropertyDefinition.ComponentType.COMBOBOX, "Switch Braces"));

		// Boolean settings
		propertyDefinitions.put("org.eclipse.jdt.core.formatter.never_join_already_wrapped_lines",
				new PropertyDefinition(new String[] { "true", "false" },
						PropertyDefinition.ComponentType.CHECKBOX, "Never join already wrapped lines"));
		propertyDefinitions.put("org.eclipse.jdt.core.formatter.wrap_before_or_operator_multicatch",
				new PropertyDefinition(new String[] { "true", "false" },
						PropertyDefinition.ComponentType.CHECKBOX, "Wrap before operators"));
		propertyDefinitions.put("org.eclipse.jdt.core.formatter.align_with_spaces",
				new PropertyDefinition(new String[] { "true", "false" },
						PropertyDefinition.ComponentType.CHECKBOX, "Align with spaces"));

		// Comment settings
		propertyDefinitions.put("org.eclipse.jdt.core.formatter.comment.format_javadoc_comments",
				new PropertyDefinition(new String[] { "true", "false" },
						PropertyDefinition.ComponentType.CHECKBOX, "Format Javadoc comments"));
		propertyDefinitions.put("org.eclipse.jdt.core.formatter.comment.new_lines_at_block_boundaries",
				new PropertyDefinition(new String[] { "true", "false" },
						PropertyDefinition.ComponentType.CHECKBOX, "New lines at block boundaries"));
		propertyDefinitions.put("org.eclipse.jdt.core.formatter.comment.insert_new_line_for_parameter",
				new PropertyDefinition(new String[] { "insert", "do not insert" },
						PropertyDefinition.ComponentType.CHECKBOX, "Insert new line for parameter"));
		propertyDefinitions.put("org.eclipse.jdt.core.formatter.comment.insert_new_line_before_root_tags",
				new PropertyDefinition(new String[] { "insert", "do not insert" },
						PropertyDefinition.ComponentType.CHECKBOX, "Insert new line before root tags"));
		propertyDefinitions.put("org.eclipse.jdt.core.formatter.comment.indent_root_tags",
				new PropertyDefinition(new String[] { "true", "false" },
						PropertyDefinition.ComponentType.CHECKBOX, "Indent root tags"));
		propertyDefinitions.put("org.eclipse.jdt.core.formatter.comment.count_line_length_from_starting_position",
				new PropertyDefinition(new String[] { "true", "false" },
						PropertyDefinition.ComponentType.CHECKBOX, "Count line length from starting position"));

		// Whitespace settings
		propertyDefinitions.put("org.eclipse.jdt.core.formatter.insert_space_after_comma_in_type_arguments",
				new PropertyDefinition(new String[] { "insert", "do not insert" },
						PropertyDefinition.ComponentType.CHECKBOX, "Space after comma"));
		propertyDefinitions.put("org.eclipse.jdt.core.formatter.insert_space_before_comma_in_type_arguments",
				new PropertyDefinition(new String[] { "insert", "do not insert" },
						PropertyDefinition.ComponentType.CHECKBOX, "Space before comma"));
		propertyDefinitions.put("org.eclipse.jdt.core.formatter.insert_space_after_semicolon_in_for",
				new PropertyDefinition(new String[] { "insert", "do not insert" },
						PropertyDefinition.ComponentType.CHECKBOX, "Space after semicolon"));
		propertyDefinitions.put("org.eclipse.jdt.core.formatter.insert_space_after_assignment_operator",
				new PropertyDefinition(new String[] { "insert", "do not insert" },
						PropertyDefinition.ComponentType.CHECKBOX, "Space after assignment operator"));
		propertyDefinitions.put("org.eclipse.jdt.core.formatter.insert_space_after_logical_operator",
				new PropertyDefinition(new String[] { "insert", "do not insert" },
						PropertyDefinition.ComponentType.CHECKBOX, "Space after logical operator"));

		// Blank line settings
		propertyDefinitions.put("org.eclipse.jdt.core.formatter.blank_lines_before_imports",
				new PropertyDefinition(new String[] { "0", "1", "2", "3" },
						PropertyDefinition.ComponentType.SPINNER, "Blank lines before imports"));
		propertyDefinitions.put("org.eclipse.jdt.core.formatter.blank_lines_after_imports",
				new PropertyDefinition(new String[] { "0", "1", "2", "3" },
						PropertyDefinition.ComponentType.SPINNER, "Blank lines after imports"));
		propertyDefinitions.put("org.eclipse.jdt.core.formatter.blank_lines_after_package",
				new PropertyDefinition(new String[] { "0", "1", "2", "3" },
						PropertyDefinition.ComponentType.SPINNER, "Blank lines after package"));
		propertyDefinitions.put("org.eclipse.jdt.core.formatter.number_of_blank_lines_before_code_block",
				new PropertyDefinition(new String[] { "0", "1", "2", "3" },
						PropertyDefinition.ComponentType.SPINNER, "Blank lines before code block"));
		propertyDefinitions.put("org.eclipse.jdt.core.formatter.number_of_blank_lines_at_end_of_method_body",
				new PropertyDefinition(new String[] { "0", "1", "2", "3" },
						PropertyDefinition.ComponentType.SPINNER, "Blank lines at end of method"));

		// Control statement settings
		propertyDefinitions.put("org.eclipse.jdt.core.formatter.keep_then_statement_on_same_line",
				new PropertyDefinition(new String[] { "true", "false" },
						PropertyDefinition.ComponentType.CHECKBOX, "Keep then statement on same line"));
		propertyDefinitions.put("org.eclipse.jdt.core.formatter.keep_else_statement_on_same_line",
				new PropertyDefinition(new String[] { "true", "false" },
						PropertyDefinition.ComponentType.CHECKBOX, "Keep else statement on same line"));
		propertyDefinitions.put("org.eclipse.jdt.core.formatter.keep_simple_if_on_one_line",
				new PropertyDefinition(new String[] { "true", "false" },
						PropertyDefinition.ComponentType.CHECKBOX, "Keep simple if on one line"));
	}

	/**
	 * Creates a UI component based on the property definition.
	 */
	private JComponent createComponentForProperty(String propertyKey) {
		PropertyDefinition def = propertyDefinitions.get(propertyKey);
		if (def == null) {
			// Fallback to text field for unknown properties
			return new JTextField(10);
		}

		switch (def.componentType) {
		case SPINNER:
			// For spinners, use the first value as default and create a reasonable range
			int defaultValue = Integer.parseInt(def.possibleValues[0]);
			int min = Math.max(0, defaultValue - 20);
			int max = defaultValue + 100;
			int step = 1;
			JSpinner spinner = new JSpinner(new SpinnerNumberModel(defaultValue, min, max, step));
			return spinner;

		case COMBOBOX:
			JComboBox<String> comboBox = new JComboBox<>(def.possibleValues);
			return comboBox;

		case CHECKBOX:
			JCheckBox checkBox = new JCheckBox();
			return checkBox;

		case TEXTFIELD:
		default:
			JTextField textField = new JTextField(10);
			return textField;
		}
	}

	/**
	 * Sets the value of a component based on the property definition.
	 */
	private void setComponentValue(JComponent component, String value, String propertyKey) {
		PropertyDefinition def = propertyDefinitions.get(propertyKey);
		if (def == null) {
			if (component instanceof JTextField) {
				((JTextField) component).setText(value != null ? value : "");
			}
			return;
		}

		switch (def.componentType) {
		case SPINNER:
			if (component instanceof JSpinner) {
				try {
					((JSpinner) component).setValue(Integer.parseInt(value));
				} catch (NumberFormatException e) {
					// Keep default value
				}
			}
			break;

		case COMBOBOX:
			if (component instanceof JComboBox) {
				((JComboBox<?>) component).setSelectedItem(value);
			}
			break;

		case CHECKBOX:
			if (component instanceof JCheckBox) {
				boolean isChecked = "true".equals(value) || "insert".equals(value);
				((JCheckBox) component).setSelected(isChecked);
			}
			break;

		case TEXTFIELD:
			if (component instanceof JTextField) {
				((JTextField) component).setText(value != null ? value : "");
			}
			break;
		}
	}

	/**
	 * Gets the value from a component based on the property definition.
	 */
	private String getComponentValue(JComponent component, String propertyKey) {
		PropertyDefinition def = propertyDefinitions.get(propertyKey);
		if (def == null) {
			if (component instanceof JTextField) {
				return ((JTextField) component).getText();
			}
			return "";
		}

		switch (def.componentType) {
		case SPINNER:
			if (component instanceof JSpinner) {
				return ((JSpinner) component).getValue().toString();
			}
			break;

		case COMBOBOX:
			if (component instanceof JComboBox) {
				Object selected = ((JComboBox<?>) component).getSelectedItem();
				return selected != null ? selected.toString() : "";
			}
			break;

		case CHECKBOX:
			if (component instanceof JCheckBox) {
				boolean isChecked = ((JCheckBox) component).isSelected();
				// For insert/do not insert properties, return the appropriate string
				if (def.possibleValues.length > 0 &&
						(def.possibleValues[0].equals("insert") || def.possibleValues[0].equals("do not insert"))) {
					return isChecked ? "insert" : "do not insert";
				}
				return isChecked ? "true" : "false";
			}
			break;

		case TEXTFIELD:
			if (component instanceof JTextField) {
				return ((JTextField) component).getText();
			}
			break;
		}
		return "";
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

		// All settings table tab
		tabbedPane.addTab("All Settings", createAllSettingsTab());

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
		gbc.gridx = 0;
		gbc.gridy = row++;
		panel.add(new JLabel("Line Length:"), gbc);
		gbc.gridx = 1;
		JSpinner lineLengthSpinner = new JSpinner(new SpinnerNumberModel(120, 40, 200, 10));
		settingComponents.put("org.eclipse.jdt.core.formatter.lineSplit", lineLengthSpinner);
		panel.add(lineLengthSpinner, gbc);

		// Comment line length
		gbc.gridx = 0;
		gbc.gridy = row++;
		panel.add(new JLabel("Comment Line Length:"), gbc);
		gbc.gridx = 1;
		JSpinner commentLineLengthSpinner = new JSpinner(new SpinnerNumberModel(120, 40, 200, 10));
		settingComponents.put("org.eclipse.jdt.core.formatter.comment.line_length", commentLineLengthSpinner);
		panel.add(commentLineLengthSpinner, gbc);

		// Tabulation character
		gbc.gridx = 0;
		gbc.gridy = row++;
		panel.add(new JLabel("Indent Character:"), gbc);
		gbc.gridx = 1;
		JComboBox<String> tabCharCombo = new JComboBox<>(new String[] { "space", "tab" });
		settingComponents.put("org.eclipse.jdt.core.formatter.tabulation.char", tabCharCombo);
		panel.add(tabCharCombo, gbc);

		// Tabulation size
		gbc.gridx = 0;
		gbc.gridy = row++;
		panel.add(new JLabel("Tab Size:"), gbc);
		gbc.gridx = 1;
		JSpinner tabSizeSpinner = new JSpinner(new SpinnerNumberModel(4, 1, 8, 1));
		settingComponents.put("org.eclipse.jdt.core.formatter.tabulation.size", tabSizeSpinner);
		panel.add(tabSizeSpinner, gbc);

		// Indentation size
		gbc.gridx = 0;
		gbc.gridy = row++;
		panel.add(new JLabel("Indent Size:"), gbc);
		gbc.gridx = 1;
		JSpinner indentSizeSpinner = new JSpinner(new SpinnerNumberModel(4, 1, 8, 1));
		settingComponents.put("org.eclipse.jdt.core.formatter.indentation.size", indentSizeSpinner);
		panel.add(indentSizeSpinner, gbc);

		// Continuation indentation
		gbc.gridx = 0;
		gbc.gridy = row++;
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
		gbc.gridx = 0;
		gbc.gridy = row++;
		panel.add(new JLabel("Array Initializer Indent:"), gbc);
		gbc.gridx = 1;
		JComboBox<String> arrayInitCombo = new JComboBox<>(new String[] { "0", "1", "2" });
		settingComponents.put("org.eclipse.jdt.core.formatter.indentation.size", arrayInitCombo);
		panel.add(arrayInitCombo, gbc);

		// Type declaration indentation
		gbc.gridx = 0;
		gbc.gridy = row++;
		panel.add(new JLabel("Type Declaration Indent:"), gbc);
		gbc.gridx = 1;
		JComboBox<String> typeDeclCombo = new JComboBox<>(new String[] { "0", "1", "2" });
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
		gbc.gridx = 0;
		gbc.gridy = row++;
		panel.add(new JLabel("Type Declaration Braces:"), gbc);
		gbc.gridx = 1;
		JComboBox<String> typeBraceCombo = new JComboBox<>(
				new String[] { "end_of_line", "next_line", "next_line_indented" });
		settingComponents.put("org.eclipse.jdt.core.formatter.brace_position_for_type_declaration", typeBraceCombo);
		panel.add(typeBraceCombo, gbc);

		// Brace position for method declaration
		gbc.gridx = 0;
		gbc.gridy = row++;
		panel.add(new JLabel("Method Declaration Braces:"), gbc);
		gbc.gridx = 1;
		JComboBox<String> methodBraceCombo = new JComboBox<>(
				new String[] { "end_of_line", "next_line", "next_line_indented" });
		settingComponents.put("org.eclipse.jdt.core.formatter.brace_position_for_method_declaration", methodBraceCombo);
		panel.add(methodBraceCombo, gbc);

		// Brace position for constructor declaration
		gbc.gridx = 0;
		gbc.gridy = row++;
		panel.add(new JLabel("Constructor Declaration Braces:"), gbc);
		gbc.gridx = 1;
		JComboBox<String> constructorBraceCombo = new JComboBox<>(
				new String[] { "end_of_line", "next_line", "next_line_indented" });
		settingComponents.put("org.eclipse.jdt.core.formatter.brace_position_for_constructor_declaration",
				constructorBraceCombo);
		panel.add(constructorBraceCombo, gbc);

		// Brace position for blocks
		gbc.gridx = 0;
		gbc.gridy = row++;
		panel.add(new JLabel("Block Braces:"), gbc);
		gbc.gridx = 1;
		JComboBox<String> blockBraceCombo = new JComboBox<>(
				new String[] { "end_of_line", "next_line", "next_line_indented" });
		settingComponents.put("org.eclipse.jdt.core.formatter.brace_position_for_block", blockBraceCombo);
		panel.add(blockBraceCombo, gbc);

		// Brace position for switch
		gbc.gridx = 0;
		gbc.gridy = row++;
		panel.add(new JLabel("Switch Braces:"), gbc);
		gbc.gridx = 1;
		JComboBox<String> switchBraceCombo = new JComboBox<>(
				new String[] { "end_of_line", "next_line", "next_line_indented" });
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
		gbc.gridx = 0;
		gbc.gridy = row++;
		JCheckBox neverJoinCheckBox = new JCheckBox("Never join already wrapped lines");
		settingComponents.put("org.eclipse.jdt.core.formatter.never_join_already_wrapped_lines", neverJoinCheckBox);
		panel.add(neverJoinCheckBox, gbc);

		// Wrap before operators
		gbc.gridx = 0;
		gbc.gridy = row++;
		JCheckBox wrapBeforeOpsCheckBox = new JCheckBox("Wrap before operators");
		settingComponents.put("org.eclipse.jdt.core.formatter.wrap_before_or_operator_multicatch",
				wrapBeforeOpsCheckBox);
		panel.add(wrapBeforeOpsCheckBox, gbc);

		// Align with spaces
		gbc.gridx = 0;
		gbc.gridy = row++;
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
		gbc.gridx = 0;
		gbc.gridy = row++;
		JCheckBox formatJavadocCheckBox = new JCheckBox("Format Javadoc comments");
		settingComponents.put("org.eclipse.jdt.core.formatter.comment.format_javadoc_comments", formatJavadocCheckBox);
		panel.add(formatJavadocCheckBox, gbc);

		// New lines at block boundaries
		gbc.gridx = 0;
		gbc.gridy = row++;
		JCheckBox newLinesAtBlockCheckBox = new JCheckBox("New lines at block boundaries");
		settingComponents.put("org.eclipse.jdt.core.formatter.comment.new_lines_at_block_boundaries",
				newLinesAtBlockCheckBox);
		panel.add(newLinesAtBlockCheckBox, gbc);

		// Insert new line for parameter
		gbc.gridx = 0;
		gbc.gridy = row++;
		JCheckBox newLineForParamCheckBox = new JCheckBox("Insert new line for parameter");
		settingComponents.put("org.eclipse.jdt.core.formatter.comment.insert_new_line_for_parameter",
				newLineForParamCheckBox);
		panel.add(newLineForParamCheckBox, gbc);

		// Insert new line before root tags
		gbc.gridx = 0;
		gbc.gridy = row++;
		JCheckBox newLineBeforeRootTagsCheckBox = new JCheckBox("Insert new line before root tags");
		settingComponents.put("org.eclipse.jdt.core.formatter.comment.insert_new_line_before_root_tags",
				newLineBeforeRootTagsCheckBox);
		panel.add(newLineBeforeRootTagsCheckBox, gbc);

		// Indent root tags
		gbc.gridx = 0;
		gbc.gridy = row++;
		JCheckBox indentRootTagsCheckBox = new JCheckBox("Indent root tags");
		settingComponents.put("org.eclipse.jdt.core.formatter.comment.indent_root_tags", indentRootTagsCheckBox);
		panel.add(indentRootTagsCheckBox, gbc);

		// Count line length from starting position
		gbc.gridx = 0;
		gbc.gridy = row++;
		JCheckBox countLineLengthCheckBox = new JCheckBox("Count line length from starting position");
		settingComponents.put("org.eclipse.jdt.core.formatter.comment.count_line_length_from_starting_position",
				countLineLengthCheckBox);
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
		gbc.gridx = 0;
		gbc.gridy = row++;
		JCheckBox spaceAfterCommaCheckBox = new JCheckBox("Space after comma");
		settingComponents.put("org.eclipse.jdt.core.formatter.insert_space_after_comma_in_type_arguments",
				spaceAfterCommaCheckBox);
		panel.add(spaceAfterCommaCheckBox, gbc);

		// Space before comma
		gbc.gridx = 0;
		gbc.gridy = row++;
		JCheckBox spaceBeforeCommaCheckBox = new JCheckBox("Space before comma");
		settingComponents.put("org.eclipse.jdt.core.formatter.insert_space_before_comma_in_type_arguments",
				spaceBeforeCommaCheckBox);
		panel.add(spaceBeforeCommaCheckBox, gbc);

		// Space after semicolon
		gbc.gridx = 0;
		gbc.gridy = row++;
		JCheckBox spaceAfterSemicolonCheckBox = new JCheckBox("Space after semicolon");
		settingComponents.put("org.eclipse.jdt.core.formatter.insert_space_after_semicolon_in_for",
				spaceAfterSemicolonCheckBox);
		panel.add(spaceAfterSemicolonCheckBox, gbc);

		// Space after assignment operator
		gbc.gridx = 0;
		gbc.gridy = row++;
		JCheckBox spaceAfterAssignmentCheckBox = new JCheckBox("Space after assignment operator");
		settingComponents.put("org.eclipse.jdt.core.formatter.insert_space_after_assignment_operator",
				spaceAfterAssignmentCheckBox);
		panel.add(spaceAfterAssignmentCheckBox, gbc);

		// Space after logical operator
		gbc.gridx = 0;
		gbc.gridy = row++;
		JCheckBox spaceAfterLogicalCheckBox = new JCheckBox("Space after logical operator");
		settingComponents.put("org.eclipse.jdt.core.formatter.insert_space_after_logical_operator",
				spaceAfterLogicalCheckBox);
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
		gbc.gridx = 0;
		gbc.gridy = row++;
		panel.add(new JLabel("Blank lines before imports:"), gbc);
		gbc.gridx = 1;
		JSpinner blankLinesBeforeImportsSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 5, 1));
		settingComponents.put("org.eclipse.jdt.core.formatter.blank_lines_before_imports",
				blankLinesBeforeImportsSpinner);
		panel.add(blankLinesBeforeImportsSpinner, gbc);

		// Blank lines after imports
		gbc.gridx = 0;
		gbc.gridy = row++;
		panel.add(new JLabel("Blank lines after imports:"), gbc);
		gbc.gridx = 1;
		JSpinner blankLinesAfterImportsSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 5, 1));
		settingComponents.put("org.eclipse.jdt.core.formatter.blank_lines_after_imports",
				blankLinesAfterImportsSpinner);
		panel.add(blankLinesAfterImportsSpinner, gbc);

		// Blank lines after package
		gbc.gridx = 0;
		gbc.gridy = row++;
		panel.add(new JLabel("Blank lines after package:"), gbc);
		gbc.gridx = 1;
		JSpinner blankLinesAfterPackageSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 5, 1));
		settingComponents.put("org.eclipse.jdt.core.formatter.blank_lines_after_package",
				blankLinesAfterPackageSpinner);
		panel.add(blankLinesAfterPackageSpinner, gbc);

		// Number of blank lines before code block
		gbc.gridx = 0;
		gbc.gridy = row++;
		panel.add(new JLabel("Blank lines before code block:"), gbc);
		gbc.gridx = 1;
		JSpinner blankLinesBeforeCodeBlockSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 5, 1));
		settingComponents.put("org.eclipse.jdt.core.formatter.number_of_blank_lines_before_code_block",
				blankLinesBeforeCodeBlockSpinner);
		panel.add(blankLinesBeforeCodeBlockSpinner, gbc);

		// Number of blank lines at end of method body
		gbc.gridx = 0;
		gbc.gridy = row++;
		panel.add(new JLabel("Blank lines at end of method:"), gbc);
		gbc.gridx = 1;
		JSpinner blankLinesAtEndOfMethodSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 5, 1));
		settingComponents.put("org.eclipse.jdt.core.formatter.number_of_blank_lines_at_end_of_method_body",
				blankLinesAtEndOfMethodSpinner);
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
		gbc.gridx = 0;
		gbc.gridy = row++;
		JCheckBox keepThenOnSameLineCheckBox = new JCheckBox("Keep then statement on same line");
		settingComponents.put("org.eclipse.jdt.core.formatter.keep_then_statement_on_same_line",
				keepThenOnSameLineCheckBox);
		panel.add(keepThenOnSameLineCheckBox, gbc);

		// Keep else statement on same line
		gbc.gridx = 0;
		gbc.gridy = row++;
		JCheckBox keepElseOnSameLineCheckBox = new JCheckBox("Keep else statement on same line");
		settingComponents.put("org.eclipse.jdt.core.formatter.keep_else_statement_on_same_line",
				keepElseOnSameLineCheckBox);
		panel.add(keepElseOnSameLineCheckBox, gbc);

		// Keep simple if on one line
		gbc.gridx = 0;
		gbc.gridy = row++;
		JCheckBox keepSimpleIfOnOneLineCheckBox = new JCheckBox("Keep simple if on one line");
		settingComponents.put("org.eclipse.jdt.core.formatter.keep_simple_if_on_one_line",
				keepSimpleIfOnOneLineCheckBox);
		panel.add(keepSimpleIfOnOneLineCheckBox, gbc);

		addChangeListeners();

		return panel;
	}

	private JPanel createAllSettingsTab() {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(new TitledBorder("All Formatter Settings"));

		// Create search panel
		JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		searchPanel.add(new JLabel("Search:"));
		JTextField searchField = new JTextField(20);
		searchPanel.add(searchField);

		// Create table model
		tableModel = new SettingsTableModel();
		JTable table = new JTable(tableModel);

		// Create row sorter for filtering
		TableRowSorter<SettingsTableModel> sorter = new TableRowSorter<>(tableModel);
		table.setRowSorter(sorter);

		// Add search functionality
		searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
			@Override
			public void insertUpdate(javax.swing.event.DocumentEvent e) {
				filterTable();
			}

			@Override
			public void removeUpdate(javax.swing.event.DocumentEvent e) {
				filterTable();
			}

			@Override
			public void changedUpdate(javax.swing.event.DocumentEvent e) {
				filterTable();
			}

			private void filterTable() {
				String searchText = searchField.getText().trim();
				if (searchText.isEmpty()) {
					sorter.setRowFilter(null);
				} else {
					sorter.setRowFilter(RowFilter.regexFilter("(?i)" + searchText, 0)); // Case-insensitive search on column 0 (setting name)
				}
			}
		});

		// Configure table
		table.setRowHeight(25);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.getColumnModel().getColumn(0).setPreferredWidth(400); // Setting name
		table.getColumnModel().getColumn(1).setPreferredWidth(200); // Current value
		table.getColumnModel().getColumn(2).setPreferredWidth(200); // New value

		// Custom renderer for better display
		DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
		renderer.setHorizontalAlignment(SwingConstants.LEFT);
		table.setDefaultRenderer(Object.class, renderer);

		// Add search panel to top
		panel.add(searchPanel, BorderLayout.NORTH);

		// Add scroll pane
		JScrollPane scrollPane = new JScrollPane(table);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		panel.add(scrollPane, BorderLayout.CENTER);

		// Add buttons panel
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

		JButton refreshButton = new JButton("Refresh");
		refreshButton.addActionListener(e -> tableModel.refreshData());
		buttonPanel.add(refreshButton);

		JButton applyButton = new JButton("Apply Changes");
		applyButton.addActionListener(e -> {
			tableModel.applyChanges();
			onSettingsChanged.run();
		});
		buttonPanel.add(applyButton);

		JButton resetButton = new JButton("Reset to Current");
		resetButton.addActionListener(e -> tableModel.resetToCurrent());
		buttonPanel.add(resetButton);

		panel.add(buttonPanel, BorderLayout.SOUTH);

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

		JButton quarkusButton = new JButton("Quarkus Style");
		quarkusButton.addActionListener(e -> loadPreset("quarkus"));
		panel.add(quarkusButton);

		JButton springButton = new JButton("Spring Style");
		springButton.addActionListener(e -> loadPreset("spring"));
		panel.add(springButton);

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
			} else {
				throw new IllegalArgumentException("Unsupported component type: " + component.getClass().getName());
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

		// Update UI components using property-aware methods
		for (Map.Entry<String, JComponent> entry : settingComponents.entrySet()) {
			String key = entry.getKey();
			JComponent component = entry.getValue();
			String value = settings.get(key);

			setComponentValue(component, value, key);
		}

		// Refresh table model if it exists
		if (tableModel != null) {
			tableModel.refreshData();
		}
	}

	public Map<String, String> getCurrentSettings() {
		Map<String, String> settings = new HashMap<>(currentSettings);

		// Update from UI components using property-aware methods
		for (Map.Entry<String, JComponent> entry : settingComponents.entrySet()) {
			String key = entry.getKey();
			JComponent component = entry.getValue();

			String value = getComponentValue(component, key);
			if (!value.isEmpty()) {
				settings.put(key, value);
			}
		}

		return settings;
	}

	/**
	 * Table model for displaying and editing all formatter settings.
	 */
	private class SettingsTableModel extends AbstractTableModel {
		private final String[] columnNames = { "Setting Name", "Current Value", "New Value" };
		private final java.util.List<SettingRow> settings = new ArrayList<>();

		public SettingsTableModel() {
			refreshData();
		}

		public void refreshData() {
			settings.clear();
			Map<String, String> currentSettings = getCurrentSettings();

			// Add all known Eclipse formatter settings
			String[] knownSettings = {
					"org.eclipse.jdt.core.formatter.lineSplit",
					"org.eclipse.jdt.core.formatter.comment.line_length",
					"org.eclipse.jdt.core.formatter.tabulation.char",
					"org.eclipse.jdt.core.formatter.tabulation.size",
					"org.eclipse.jdt.core.formatter.indentation.size",
					"org.eclipse.jdt.core.formatter.continuation_indentation",
					"org.eclipse.jdt.core.formatter.brace_position_for_type_declaration",
					"org.eclipse.jdt.core.formatter.brace_position_for_method_declaration",
					"org.eclipse.jdt.core.formatter.brace_position_for_constructor_declaration",
					"org.eclipse.jdt.core.formatter.brace_position_for_block",
					"org.eclipse.jdt.core.formatter.brace_position_for_switch",
					"org.eclipse.jdt.core.formatter.never_join_already_wrapped_lines",
					"org.eclipse.jdt.core.formatter.wrap_before_or_operator_multicatch",
					"org.eclipse.jdt.core.formatter.align_with_spaces",
					"org.eclipse.jdt.core.formatter.comment.format_javadoc_comments",
					"org.eclipse.jdt.core.formatter.comment.new_lines_at_block_boundaries",
					"org.eclipse.jdt.core.formatter.comment.insert_new_line_for_parameter",
					"org.eclipse.jdt.core.formatter.comment.insert_new_line_before_root_tags",
					"org.eclipse.jdt.core.formatter.comment.indent_root_tags",
					"org.eclipse.jdt.core.formatter.comment.count_line_length_from_starting_position",
					"org.eclipse.jdt.core.formatter.insert_space_after_comma_in_type_arguments",
					"org.eclipse.jdt.core.formatter.insert_space_before_comma_in_type_arguments",
					"org.eclipse.jdt.core.formatter.insert_space_after_semicolon_in_for",
					"org.eclipse.jdt.core.formatter.insert_space_after_assignment_operator",
					"org.eclipse.jdt.core.formatter.insert_space_after_logical_operator",
					"org.eclipse.jdt.core.formatter.blank_lines_before_imports",
					"org.eclipse.jdt.core.formatter.blank_lines_after_imports",
					"org.eclipse.jdt.core.formatter.blank_lines_after_package",
					"org.eclipse.jdt.core.formatter.number_of_blank_lines_before_code_block",
					"org.eclipse.jdt.core.formatter.number_of_blank_lines_at_end_of_method_body",
					"org.eclipse.jdt.core.formatter.keep_then_statement_on_same_line",
					"org.eclipse.jdt.core.formatter.keep_else_statement_on_same_line",
					"org.eclipse.jdt.core.formatter.keep_simple_if_on_one_line"
			};

			for (String setting : knownSettings) {
				String currentValue = currentSettings.getOrDefault(setting, "");
				settings.add(new SettingRow(setting, currentValue, currentValue));
			}

			// Sort by setting name
			settings.sort((a, b) -> a.settingName.compareTo(b.settingName));

			fireTableDataChanged();
		}

		public void applyChanges() {
			Map<String, String> newSettings = new HashMap<>();
			for (SettingRow row : settings) {
				if (!row.newValue.isEmpty()) {
					newSettings.put(row.settingName, row.newValue);
				}
			}
			loadSettings(newSettings);
		}

		public void resetToCurrent() {
			for (SettingRow row : settings) {
				row.newValue = row.currentValue;
			}
			fireTableDataChanged();
		}

		@Override
		public int getRowCount() {
			return settings.size();
		}

		@Override
		public int getColumnCount() {
			return columnNames.length;
		}

		@Override
		public String getColumnName(int column) {
			return columnNames[column];
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			SettingRow row = settings.get(rowIndex);
			switch (columnIndex) {
			case 0:
				return row.settingName.replace("org.eclipse.jdt.core.formatter.", "");
			case 1:
				return row.currentValue;
			case 2:
				return row.newValue;
			default:
				return null;
			}
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return columnIndex == 2; // Only "New Value" column is editable
		}

		@Override
		public void setValueAt(Object value, int rowIndex, int columnIndex) {
			if (columnIndex == 2) {
				SettingRow row = settings.get(rowIndex);
				row.newValue = value != null ? value.toString() : "";
				fireTableCellUpdated(rowIndex, columnIndex);
			}
		}

		private static class SettingRow {
			final String settingName;
			final String currentValue;
			String newValue;

			SettingRow(String settingName, String currentValue, String newValue) {
				this.settingName = settingName;
				this.currentValue = currentValue;
				this.newValue = newValue;
			}
		}
	}
}
