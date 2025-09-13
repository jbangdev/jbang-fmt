///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 21+
//DEPS org.eclipse.jdt:org.eclipse.jdt.core:3.37.0
//DEPS org.eclipse.platform:org.eclipse.jface.text:3.28.0
//DEPS info.picocli:picocli:4.7.5

//FILES ../../../google.xml ../../../java.xml ../../../eclipse.xml ../../../jbang.xml ../../../spring.prefs ../../../quarkus.xml

package dev.jbang.fmt;

import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import java.util.concurrent.Callable;

import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.text.edits.TextEdit;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;
import picocli.CommandLine.ArgGroup;

/**
 * Java formatter CLI tool supporting Eclipse and Google Java formatters.
 */
@Command(name = "fmt", mixinStandardHelpOptions = true, version = "1.0", description = "Format Java w/JBang source files using EclipseJava formatters")
public class fmt implements Callable<Integer> {

    /**
     * Eclipse Java formatter implementation
     */
    static class EclipseJavaFormatter implements JavaFormatter {

        private final Map<String, String> settings;
        private final boolean jbangFriendly;

        public EclipseJavaFormatter() {
            this.settings = null;
        }

        public EclipseJavaFormatter(Map<String, String> settings, boolean jbangFriendly) {
            this.settings = settings;
            this.jbangFriendly = jbangFriendly;
        }

        @Override
        public String format(String content) throws Exception {
            return format(content, jbangFriendly ? JBangDirectiveHandler.identifyJavaRanges(content) : List.of(new CodeRange(0, content.length())));
        }

        String format(String content, List<CodeRange> ranges) throws Exception {
            // Convert CodeRange objects to IRegion array
            List<IRegion> regions = new ArrayList<>();
            for (CodeRange range : ranges) {
                regions.add(new Region(range.start(), range.end() - range.start()));
            }

            CodeFormatter codeFormatter = ToolFactory.createCodeFormatter(settings);
            TextEdit edit = codeFormatter.format(
                    CodeFormatter.K_COMPILATION_UNIT,
                    content,
                    regions.toArray(new IRegion[0]),
                    0,
                    null);

            if (edit != null) {
                IDocument doc = new Document(content);
                edit.apply(doc);
                return doc.get();
            } else {
                System.err.println("Warning: Eclipse formatter could not format the content");
                return content;
            }
        }

        @Override
        public String getName() {
            return "Eclipse";
        }

        @Override
        public String toString() {
            return "EclipseJavaFormatter";
        }
    }

    /**
     * Utility class for handling JBang directives and identifying Java code ranges
     */
    static class JBangDirectiveHandler {

        /**
         * Identifies character ranges that contain Java code (excluding JBang
         * directives)
         */
        public static List<CodeRange> identifyJavaRanges(String content) {
            List<CodeRange> ranges = new ArrayList<>();
            String[] lines = content.split("\n", -1);

            int currentPos = 0;
            boolean inJavaCode = false;
            int javaStart = 0;

            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                int lineLength = line.length();

                if (isJBangDirective(line) || (i == 0 && line.startsWith("//"))) {
                    // End current Java range if we were in one
                    if (inJavaCode) {
                        ranges.add(new CodeRange(javaStart, currentPos));
                        inJavaCode = false;
                    }
                } else {
                    // Start Java range if we weren't in one
                    if (!inJavaCode) {
                        javaStart = currentPos;
                        inJavaCode = true;
                    }
                }

                currentPos += lineLength;
                // Add newline character except for the last line
                if (i < lines.length - 1) {
                    currentPos += 1;
                }
            }

            // Close final Java range if we were in one
            if (inJavaCode) {
                ranges.add(new CodeRange(javaStart, currentPos));
            }

            return ranges;
        }

        /**
         * Checks if a line is a JBang directive
         */
        static boolean isJBangDirective(String line) {
            if (line == null || line.trim().isEmpty()) {
                return false;
            }

            String trimmed = line.trim();
            if (!trimmed.startsWith("//")) {
                return false;
            }

            // Check if it's followed by capital letters (JBang directive pattern)
            String afterComment = trimmed.substring(2).trim();
            if (afterComment.isEmpty()) {
                return false;
            }

            // Check if it starts with capital letters (like DEPS, JAVA_OPTIONS, etc.)
            return Character.isUpperCase(afterComment.charAt(0));
        }

    }

    static record CodeRange(int start, int end) {
    }

    /**
     * Interface for Java formatters
     */
    interface JavaFormatter {
        String format(String content) throws Exception;

        String getName();
    }

    @Option(names = "--jbang-friendly", description = "Use JBang friendly formatter settings (protects JBang directives)", defaultValue = "true")
    boolean jbangFriendly = true;
    
    @Option(names = "--settings", description = "Eclipse formatter settings file (.xml or .prefs)")
    private Path settingsFile;

    @Parameters(description = "Java files or directories to format", arity = "1..*")
    private List<Path> sources;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new fmt()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {

        try {
            JavaFormatter formatter;

            if (settingsFile != null) {
                var settings = loadEclipseSettings(settingsFile);
                formatter = new EclipseJavaFormatter(settings, jbangFriendly);
                System.out.println("Using Eclipse formatter settings from: " + settingsFile);
            } else {
                formatter = new EclipseJavaFormatter();
                System.out.println("Using default Eclipse formatter settings");
            }

            formatFiles(sources, formatter);
            return 0;
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            return 1;
        }
    }

    /**
     * Loads Eclipse formatter settings from an XML file or .prefs properties file
     */
    private static Map<String, String> loadEclipseSettings(Path settingsFile) throws IOException {
        Map<String, String> settings = new HashMap<>();
        String fileName = settingsFile.getFileName().toString();

        // First try to load as a direct file
        if (Files.exists(settingsFile)) {
            return loadSettingsFromFile(settingsFile, fileName);
        }

        // If file doesn't exist, try to load from classpath as resource
        return loadSettingsFromClasspath(fileName);
    }

    /**
     * Loads settings from a direct file path
     */
    private static Map<String, String> loadSettingsFromFile(Path settingsFile, String fileName) throws IOException {
        try (FileInputStream fis = new FileInputStream(settingsFile.toFile())) {
            return parseSettingsFromStream(fis, fileName, "file: " + settingsFile);
        }
    }

    /**
     * Loads settings from classpath resources with intelligent name resolution
     */
    private static Map<String, String> loadSettingsFromClasspath(String fileName) throws IOException {
        String resourceName = fileName;

        // If no dots in name, try to find .xml or .prefs versions
        if (!fileName.contains(".")) {
            // Try in order: name.xml, name.prefs
            String[] extensions = { ".xml", ".prefs" };
            for (String ext : extensions) {
                resourceName = fileName + ext;
                try {
                    return loadSettingsFromResource(resourceName);
                } catch (IOException e) {
                    // Continue to next extension
                }
            }
            throw new IOException("Could not find settings resource: " + fileName + ".xml or " + fileName + ".prefs");
        }

        // Direct resource name
        return loadSettingsFromResource(resourceName);
    }

    /**
     * Loads settings from a classpath resource
     */
    private static Map<String, String> loadSettingsFromResource(String resourceName) throws IOException {
        try (InputStream is = fmt.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (is == null) {
                throw new IOException("Resource not found in classpath: " + resourceName);
            }
            return parseSettingsFromStream(is, resourceName, "resource: " + resourceName);
        }
    }

    /**
     * Shared parsing logic for both file and resource streams
     */
    private static Map<String, String> parseSettingsFromStream(InputStream is, String fileName,
            String sourceDescription) throws IOException {
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

            System.out.println("Loaded " + settings.size() + " formatter settings from .prefs " + sourceDescription);

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

    private static void formatFiles(List<Path> targets, JavaFormatter formatter) throws Exception {
        System.out.println("Formatting with " + formatter.getName() + " formatter...");

        for (Path path : targets) {
            if (Files.exists(path)) {
                if (Files.isDirectory(path)) {
                    formatDirectory(path, formatter);
                } else if (path.toString().endsWith(".java")) {
                    formatFile(path, formatter);
                } else {
                    System.out.println("Skipping non-Java file: " + path);
                }
            } else {
                System.err.println("Warning: Path does not exist: " + path);
            }
        }
    }

    private static void formatDirectory(Path dir, JavaFormatter formatter) throws Exception {
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> {
                        try {
                            formatFile(path, formatter);
                        } catch (Exception e) {
                            System.err.println("Error formatting " + path + ": " + e.getMessage());
                        }
                    });
        }
    }

    private static void formatFile(Path file, JavaFormatter formatter) throws Exception {
        System.out.println("Formatting: " + file);

        try {
            // Read the file content
            String content = new String(Files.readAllBytes(file));

            String formatted = formatter.format(content);

            // Write back the formatted content
            Files.write(file, formatted.getBytes());
        } catch (Exception e) {
            System.err.println("Error formatting " + file + ": " + e.getMessage());
            // Don't rethrow, just continue with other files
        }
    }

}
