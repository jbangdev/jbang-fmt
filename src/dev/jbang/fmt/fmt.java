///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 21+
//DEPS org.eclipse.jdt:org.eclipse.jdt.core:3.37.0
//DEPS org.eclipse.platform:org.eclipse.jface.text:3.28.0
//DEPS info.picocli:picocli:4.7.5

package dev.jbang.fmt;

import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import java.util.concurrent.Callable;

import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.text.edits.TextEdit;
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
static class EclipseJavaFormatter implements JavaFormatter
    {

    @Override
    public String format(String content) throws Exception {
        return format(content, JBangDirectiveHandler.identifyJavaRanges(content));
    }

    @Override
    public String format(String content, List<CodeRange> ranges) throws Exception {
        System.err.println(content + "\n Ranges: " + ranges);

        // Convert CodeRange objects to IRegion array
        List<IRegion> regions = new ArrayList<>();
        for (CodeRange range : ranges) {
                regions.add(new Region(range.start(), range.end() - range.start()));
        }

        CodeFormatter codeFormatter = ToolFactory.createCodeFormatter(null);
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

            if (isJBangDirective(line) || (i==0 && line.startsWith("//"))) {
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
    String format(String content, List<CodeRange> ranges) throws Exception;
    String getName();
}

    @Parameters(description = "Java files or directories to format", arity = "1..*")
    private List<Path> sources;
    
    public static void main(String[] args) {
        int exitCode = new CommandLine(new fmt()).execute(args);
        System.exit(exitCode);
    }
    
    @Override
    public Integer call() throws Exception {
        
        try {
            JavaFormatter formatter = new EclipseJavaFormatter();
            formatFiles(sources, formatter);
            return 0;
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            return 1;
        }
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
                        
            List<CodeRange> ranges = JBangDirectiveHandler.identifyJavaRanges(content);

            String formatted = formatter.format(content, ranges);
            
            // Write back the formatted content
            Files.write(file, formatted.getBytes());
        } catch (Exception e) {
            System.err.println("Error formatting " + file + ": " + e.getMessage());
            // Don't rethrow, just continue with other files
        }
    }
    
}
