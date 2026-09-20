///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 21+
//DEPS org.eclipse.jdt:org.eclipse.jdt.core:3.43.0
//DEPS org.eclipse.platform:org.eclipse.jface.text:3.28.0
//DEPS com.fifesoft:rsyntaxtextarea:3.3.4
//DEPS com.formdev:flatlaf:3.4.1

//SOURCES ../Main.java SettingsPanel.java

package dev.jbang.fmt.ui;

import dev.jbang.fmt.FmtLogger;

import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import com.formdev.flatlaf.FlatLightLaf;

import dev.jbang.fmt.JavaFormatter;

/**
 * GUI application for Java code formatting with live preview and settings management.
 */
public class FormatterUI extends JFrame {

	private static final String DEFAULT_SAMPLE_CODE = """
			package com.example;

			import java.util.List;
			import java.util.ArrayList;

			public class SampleClass {
			    private String name;
			    private List<String> items;

			    public SampleClass(String name) {
			        this.name = name;
			        this.items = new ArrayList<>();
			    }

			    public void addItem(String item) {
			        if (item != null && !item.isEmpty()) {
			            items.add(item);
			        }
			    }

			    public List<String> getItems() {
			        return new ArrayList<>(items);
			    }
			}
			""";

	private RSyntaxTextArea sourceEditor;
	private RSyntaxTextArea previewPane;
	private RTextScrollPane sourceScrollPane;
	private RTextScrollPane previewScrollPane;
	private SettingsPanel settingsPanel;
	private JavaFormatter currentFormatter;
	private ScheduledExecutorService previewScheduler;
	private boolean isUpdating = false;
	private JLabel statusLabel;

	public FormatterUI() {
		initializeUI();
		setupEventHandlers();
		loadDefaultFormatter();
		updatePreview();
	}

	private void initializeUI() {
		setTitle("Java Formatter UI - Live Preview");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(new BorderLayout());

		// Create main vertical split pane (top: code panels, bottom: settings)
		JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		mainSplitPane.setDividerLocation(500);
		mainSplitPane.setResizeWeight(0.7);

		// Top: Source and preview panels side by side
		JSplitPane codePanel = createCodePanel();
		mainSplitPane.setTopComponent(codePanel);

		// Bottom: Settings panel
		settingsPanel = new SettingsPanel(this::onSettingsChanged);
		mainSplitPane.setBottomComponent(settingsPanel);

		add(mainSplitPane, BorderLayout.CENTER);

		// Menu bar
		setJMenuBar(createMenuBar());

		// Status bar
		add(createStatusBar(), BorderLayout.SOUTH);

		setSize(1400, 900);
		setLocationRelativeTo(null);

		// Initialize preview scheduler
		previewScheduler = Executors.newSingleThreadScheduledExecutor();
	}

	private JSplitPane createCodePanel() {
		// Create horizontal split pane for source and preview
		JSplitPane codeSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		codeSplitPane.setDividerLocation(700);
		codeSplitPane.setResizeWeight(0.5);

		// Left side: Source editor
		JPanel sourcePanel = createSourcePanel();
		codeSplitPane.setLeftComponent(sourcePanel);

		// Right side: Preview
		JPanel previewPanel = createPreviewPanel();
		codeSplitPane.setRightComponent(previewPanel);

		return codeSplitPane;
	}

	private JPanel createSourcePanel() {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(new TitledBorder("Source Code"));

		sourceEditor = createSyntaxTextArea(true);
		sourceEditor.setText(DEFAULT_SAMPLE_CODE);

		sourceScrollPane = createScrollPane(sourceEditor);

		panel.add(sourceScrollPane, BorderLayout.CENTER);

		return panel;
	}

	private JPanel createPreviewPanel() {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(new TitledBorder("Formatted Preview"));

		previewPane = createSyntaxTextArea(false);
		previewPane.setEditable(false);
		previewPane.setBackground(new Color(248, 248, 248));

		previewScrollPane = createScrollPane(previewPane);

		panel.add(previewScrollPane, BorderLayout.CENTER);

		return panel;
	}

	/**
	 * Creates a configured RSyntaxTextArea with common Java syntax highlighting settings.
	 * 
	 * @param editable true if the editor should be editable, false for read-only
	 * @return configured RSyntaxTextArea instance
	 */
	private RSyntaxTextArea createSyntaxTextArea(boolean editable) {
		RSyntaxTextArea textArea = new RSyntaxTextArea();
		textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
		textArea.setHighlightCurrentLine(false);
		textArea.setCodeFoldingEnabled(true);
		textArea.setAntiAliasingEnabled(true);
		textArea.setWhitespaceVisible(true);
		textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		textArea.setTabSize(4);
		textArea.setTabsEmulated(true);
		textArea.setEditable(editable);
		return textArea;
	}

	/**
	 * Creates a configured RTextScrollPane for the given RSyntaxTextArea.
	 * 
	 * @param textArea the RSyntaxTextArea to wrap in a scroll pane
	 * @return configured RTextScrollPane instance
	 */
	private RTextScrollPane createScrollPane(RSyntaxTextArea textArea) {
		RTextScrollPane scrollPane = new RTextScrollPane(textArea);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		return scrollPane;
	}

	private JMenuBar createMenuBar() {
		JMenuBar menuBar = new JMenuBar();

		// File menu
		JMenu fileMenu = new JMenu("File");

		JMenuItem newItem = new JMenuItem("New");
		newItem.addActionListener(e -> newFile());
		fileMenu.add(newItem);

		JMenuItem openItem = new JMenuItem("Open...");
		openItem.addActionListener(e -> openFile());
		fileMenu.add(openItem);

		JMenuItem saveItem = new JMenuItem("Save");
		saveItem.addActionListener(e -> saveFile());
		fileMenu.add(saveItem);

		JMenuItem saveAsItem = new JMenuItem("Save As...");
		saveAsItem.addActionListener(e -> saveAsFile());
		fileMenu.add(saveAsItem);

		fileMenu.addSeparator();

		JMenuItem loadSettingsItem = new JMenuItem("Load Settings...");
		loadSettingsItem.addActionListener(e -> loadSettings());
		fileMenu.add(loadSettingsItem);

		JMenuItem saveSettingsItem = new JMenuItem("Save Settings...");
		saveSettingsItem.addActionListener(e -> saveSettings());
		fileMenu.add(saveSettingsItem);

		fileMenu.addSeparator();

		JMenuItem exitItem = new JMenuItem("Exit");
		exitItem.addActionListener(e -> System.exit(0));
		fileMenu.add(exitItem);

		menuBar.add(fileMenu);

		// Edit menu
		JMenu editMenu = new JMenu("Edit");

		JMenuItem formatItem = new JMenuItem("Format Now");
		formatItem.addActionListener(e -> updatePreview());
		editMenu.add(formatItem);

		JMenuItem resetItem = new JMenuItem("Reset to Defaults");
		resetItem.addActionListener(e -> resetToDefaults());
		editMenu.add(resetItem);

		menuBar.add(editMenu);

		return menuBar;
	}

	private JPanel createStatusBar() {
		JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
		statusBar.setBorder(BorderFactory.createLoweredBevelBorder());

		statusLabel = new JLabel("Ready");
		statusBar.add(statusLabel);

		return statusBar;
	}

	private void setupEventHandlers() {
		// Document listener for source editor
		sourceEditor.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				schedulePreviewUpdate();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				schedulePreviewUpdate();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				schedulePreviewUpdate();
			}
		});

		// Synchronize scroll positions with percentage-based scrolling
		sourceScrollPane.getVerticalScrollBar().addAdjustmentListener(e -> {
			if (!isUpdating) {
				isUpdating = true;
				JScrollBar sourceBar = sourceScrollPane.getVerticalScrollBar();
				JScrollBar previewBar = previewScrollPane.getVerticalScrollBar();

				// Calculate percentage position
				int sourceMax = sourceBar.getMaximum() - sourceBar.getVisibleAmount();
				int sourceValue = sourceBar.getValue();
				double percentage = sourceMax > 0 ? (double) sourceValue / sourceMax : 0.0;

				// Apply percentage to preview scrollbar
				int previewMax = previewBar.getMaximum() - previewBar.getVisibleAmount();
				int previewValue = (int) (percentage * previewMax);
				previewBar.setValue(Math.min(previewValue, previewMax));

				isUpdating = false;
			}
		});

		previewScrollPane.getVerticalScrollBar().addAdjustmentListener(e -> {
			if (!isUpdating) {
				isUpdating = true;
				JScrollBar sourceBar = sourceScrollPane.getVerticalScrollBar();
				JScrollBar previewBar = previewScrollPane.getVerticalScrollBar();

				// Calculate percentage position
				int previewMax = previewBar.getMaximum() - previewBar.getVisibleAmount();
				int previewValue = previewBar.getValue();
				double percentage = previewMax > 0 ? (double) previewValue / previewMax : 0.0;

				// Apply percentage to source scrollbar
				int sourceMax = sourceBar.getMaximum() - sourceBar.getVisibleAmount();
				int sourceValue = (int) (percentage * sourceMax);
				sourceBar.setValue(Math.min(sourceValue, sourceMax));

				isUpdating = false;
			}
		});
	}

	private void schedulePreviewUpdate() {
		previewScheduler.schedule(this::updatePreview, 500, TimeUnit.MILLISECONDS);
	}

	private void onSettingsChanged() {
		// Update the formatter with the current settings from the settings panel
		Map<String, String> currentSettings = settingsPanel.getCurrentSettings();
		currentFormatter = new JavaFormatter("Custom", currentSettings, false);
		updatePreview();
	}

	private void updatePreview() {
		try {
			String sourceCode = sourceEditor.getText();
			if (sourceCode.trim().isEmpty()) {
				previewPane.setText("");
				updateStatus("Ready - No content to format");
				return;
			}

			long startTime = System.currentTimeMillis();
			String formatted = currentFormatter.format(sourceCode);
			long endTime = System.currentTimeMillis();

			previewPane.setText(formatted);

			// Highlight differences
			highlightDifferences(sourceCode, formatted);

			// Update status with timestamp and formatting info
			String timestamp = java.time.LocalTime.now()
				.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
			boolean changed = !sourceCode.equals(formatted);
			String changeInfo = changed ? " (formatted)" : " (no changes)";
			updateStatus("Last formatted: " + timestamp + changeInfo + " - " + (endTime - startTime) + "ms");

		} catch (Exception e) {
			previewPane.setText("Error formatting code:\n" + e.getMessage());
			FmtLogger.error("Formatting error: " + e.getMessage(), e);
			updateStatus("Error: " + e.getMessage());
		}
	}

	private void highlightDifferences(String original, String formatted) {
		if (original.equals(formatted)) {
			previewPane.setBackground(new Color(240, 255, 240)); // Light green
		} else {
			previewPane.setBackground(new Color(255, 240, 240)); // Light red
		}
	}

	private void updateStatus(String message) {
		if (statusLabel != null) {
			SwingUtilities.invokeLater(() -> statusLabel.setText(message));
		}
	}

	private void loadDefaultFormatter() {
		try {
			Map<String, String> defaultSettings = JavaFormatter.loadSettingsFromClasspath("jbang");
			currentFormatter = new JavaFormatter("Default", defaultSettings, false);
			settingsPanel.loadSettings(defaultSettings);
			updateStatus("Ready - JBang formatter loaded");
		} catch (Exception e) {
			FmtLogger.error("Failed to load default formatter: " + e.getMessage(), e);
			// Fallback to empty settings
			currentFormatter = new JavaFormatter("Empty", new HashMap<>(), false);
			updateStatus("Warning - Using empty formatter settings");
		}
	}

	private void newFile() {
		sourceEditor.setText(DEFAULT_SAMPLE_CODE);
		updateStatus("New file created");
	}

	private void openFile() {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
			@Override
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().toLowerCase().endsWith(".java");
			}

			@Override
			public String getDescription() {
				return "Java files (*.java)";
			}
		});

		if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			try {
				String content = Files.readString(fileChooser.getSelectedFile().toPath(), StandardCharsets.UTF_8);
				sourceEditor.setText(content);
				updateStatus("File loaded: " + fileChooser.getSelectedFile().getName());
			} catch (IOException e) {
				JOptionPane.showMessageDialog(this, "Error reading file: " + e.getMessage(),
						"Error", JOptionPane.ERROR_MESSAGE);
				updateStatus("Error loading file: " + e.getMessage());
			}
		}
	}

	private void saveFile() {
		// For now, just show save as dialog
		saveAsFile();
	}

	private void saveAsFile() {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
			@Override
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().toLowerCase().endsWith(".java");
			}

			@Override
			public String getDescription() {
				return "Java files (*.java)";
			}
		});

		if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
			try {
				Files.writeString(fileChooser.getSelectedFile().toPath(),
						sourceEditor.getText(), StandardCharsets.UTF_8);
				updateStatus("File saved: " + fileChooser.getSelectedFile().getName());
			} catch (IOException e) {
				JOptionPane.showMessageDialog(this, "Error saving file: " + e.getMessage(),
						"Error", JOptionPane.ERROR_MESSAGE);
				updateStatus("Error saving file: " + e.getMessage());
			}
		}
	}

	private void loadSettings() {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
			@Override
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().toLowerCase().endsWith(".xml") ||
						f.getName().toLowerCase().endsWith(".prefs");
			}

			@Override
			public String getDescription() {
				return "Eclipse formatter settings (*.xml, *.prefs)";
			}
		});

		if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			try {
				Map<String, String> settings = JavaFormatter
					.loadEclipseSettings(fileChooser.getSelectedFile().toPath());
				currentFormatter = new JavaFormatter("Custom", settings, false);
				settingsPanel.loadSettings(settings);
				updatePreview();
				updateStatus("Settings loaded: " + fileChooser.getSelectedFile().getName());
			} catch (IOException e) {
				JOptionPane.showMessageDialog(this, "Error loading settings: " + e.getMessage(),
						"Error", JOptionPane.ERROR_MESSAGE);
				updateStatus("Error loading settings: " + e.getMessage());
			}
		}
	}

	private void saveSettings() {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
			@Override
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().toLowerCase().endsWith(".xml");
			}

			@Override
			public String getDescription() {
				return "Eclipse XML settings (*.xml)";
			}
		});

		if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
			try {
				Map<String, String> settings = settingsPanel.getCurrentSettings();
				saveSettingsAsXML(settings, fileChooser.getSelectedFile().toPath());
				updateStatus("Settings saved: " + fileChooser.getSelectedFile().getName());
			} catch (IOException e) {
				JOptionPane.showMessageDialog(this, "Error saving settings: " + e.getMessage(),
						"Error", JOptionPane.ERROR_MESSAGE);
				updateStatus("Error saving settings: " + e.getMessage());
			}
		}
	}

	private void saveSettingsAsXML(Map<String, String> settings, Path filePath) throws IOException {
		StringBuilder xml = new StringBuilder();
		xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n");
		xml.append("<profiles version=\"18\">\n");
		xml.append("    <profile kind=\"CodeFormatterProfile\" name=\"Custom\" version=\"18\">\n");

		for (Map.Entry<String, String> entry : settings.entrySet()) {
			xml.append("        <setting id=\"")
				.append(entry.getKey())
				.append("\" value=\"")
				.append(entry.getValue())
				.append("\"/>\n");
		}

		xml.append("    </profile>\n");
		xml.append("</profiles>\n");

		Files.writeString(filePath, xml.toString(), StandardCharsets.UTF_8);
	}

	private void resetToDefaults() {
		loadDefaultFormatter();
		updatePreview();
	}

	@Override
	public void dispose() {
		if (previewScheduler != null) {
			previewScheduler.shutdown();
		}
		super.dispose();
	}

	public static void main(String[] args) {
		// Set FlatLaf look and feel
		try {
			UIManager.setLookAndFeel(new FlatLightLaf());
		} catch (Exception e) {
			// Fall back to system look and feel
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (Exception ex) {
				// Use default
			}
		}

		SwingUtilities.invokeLater(() -> {
			new FormatterUI().setVisible(true);
		});
	}
}
