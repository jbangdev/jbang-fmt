package dev.jbang.fmt;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.text.edits.TextEdit;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Eclipse Java formatter implementation
 */
class JavaFormatter {

	private final Map<String, String> settings;
	private final boolean touchJBang;
	private String settingsName;

	public JavaFormatter(String settingsName, Map<String, String> settings, boolean touchJBang) {
		this.settings = settings;
		this.touchJBang = touchJBang;
		this.settingsName = settingsName;
	}

	public String format(String content) throws Exception {
		return format(content,
				!touchJBang ? CodeRange.identifyJavaRanges(content) : List.of(new CodeRange(0, content.length())));
	}

	String format(String content, List<CodeRange> ranges) throws Exception {
		// Convert CodeRange objects to IRegion array
		List<IRegion> regions = new ArrayList<>();
		for (CodeRange range : ranges) {
			regions.add(new Region(range.start(), range.end() - range.start()));
		}

		CodeFormatter codeFormatter = ToolFactory.createCodeFormatter(settings, ToolFactory.M_FORMAT_EXISTING);
		TextEdit edit = codeFormatter.format(CodeFormatter.K_COMPILATION_UNIT, content, regions.toArray(new IRegion[0]),
				0, null);

		if (edit != null) {
			IDocument doc = new Document(content);
			edit.apply(doc);
			return doc.get();
		} else {
			System.err.println("Warning: Eclipse formatter could not format the content");
			return content;
		}
	}

	public String getName() {
		return "Eclipse";
	}

	@Override
	public String toString() {
		return settingsName + "[" + (settings == null ? 0 : settings.size()) + " properties, touchJBang="
				+ touchJBang + "]";
	}

	/**
	 * Shared parsing logic for both file and resource streams
	 */
	static Map<String, String> parseSettingsFromStream(InputStream is, String fileName, String sourceDescription)
			throws IOException {
		Map<String, String> settings = new HashMap<>();
		String lowerFileName = fileName.toLowerCase();

		if (lowerFileName.endsWith(".prefs")) {
			// Load from .prefs properties
			Properties props = new Properties();
			props.load(is);

			// Convert Properties to Map<String, String>
			for (String key : props.stringPropertyNames()) {
				settings.put(key, props.getProperty(key));
			}

			//System.out.println("Loaded " + settings.size()
			//		+ " formatter settings from .prefs " + sourceDescription);

		} else if (lowerFileName.endsWith(".xml")) {
			// Load from XML
			try {
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder = factory.newDocumentBuilder();
				org.w3c.dom.Document document = builder.parse(is);

				// Get all setting elements
				NodeList settingNodes = document.getElementsByTagName("setting");

				for (int i = 0; i < settingNodes.getLength(); i++) {
					Element settingElement = (Element) settingNodes.item(i);
					String id = settingElement.getAttribute("id");
					String value = settingElement.getAttribute("value");

					if (id != null && !id.isEmpty()) {
						settings.put(id, value != null ? value : "");
					}
				}

				System.out.println("Loaded " + settings.size() + " formatter settings from XML " + sourceDescription);

			} catch (Exception e) {
				throw new IOException(
						"Failed to parse Eclipse XML settings " + sourceDescription + ": " + e.getMessage(), e);
			}
		} else {
			throw new IOException("Unsupported settings format. Expected .xml or .prefs " + sourceDescription);
		}

		return settings;
	}

	/**
	 * Loads settings from a classpath resource
	 */
	static Map<String, String> loadSettingsFromResource(String resourceName) throws IOException {
		try (InputStream is = Main.class.getClassLoader().getResourceAsStream(resourceName)) {
			if (is == null) {
				throw new IOException("Resource not found in classpath: " + resourceName);
			}
			return JavaFormatter.parseSettingsFromStream(is, resourceName, "resource: " + resourceName);
		}
	}

	/**
	 * Loads settings from classpath resources with intelligent name resolution
	 */
	static Map<String, String> loadSettingsFromClasspath(String fileName) throws IOException {
		String resourceName = fileName;

		// If no dots in name, try to find .xml or .prefs versions
		if (!fileName.contains(".")) {
			// Try in order: name.xml, name.prefs
			String[] extensions = { ".xml", ".prefs" };
			for (String ext : extensions) {
				resourceName = fileName + ext;
				try {
					return JavaFormatter.loadSettingsFromResource(resourceName);
				} catch (IOException e) {
					// Continue to next extension
				}
			}
			throw new IOException("Could not find settings resource: " + fileName + ".xml or " + fileName + ".prefs");
		}

		// Direct resource name
		return JavaFormatter.loadSettingsFromResource(resourceName);
	}

	/**
	 * Loads settings from a direct file path
	 */
	static Map<String, String> loadSettingsFromFile(Path settingsFile, String fileName) throws IOException {
		try (FileInputStream fis = new FileInputStream(settingsFile.toFile())) {
			return JavaFormatter.parseSettingsFromStream(fis, fileName, "file: " + settingsFile);
		}
	}

	/**
	 * Loads Eclipse formatter settings from an XML file or .prefs properties file
	 */
	static Map<String, String> loadEclipseSettings(Path settingsFile) throws IOException {
		String fileName = settingsFile.getFileName().toString();

		// First try to load as a direct file
		if (Files.exists(settingsFile)) {
			return loadSettingsFromFile(settingsFile, fileName);
		}

		// If file doesn't exist, try to load from classpath as resource
		return loadSettingsFromClasspath(fileName);
	}
}