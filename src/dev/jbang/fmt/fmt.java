///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 21+
//DEPS org.eclipse.jdt:org.eclipse.jdt.core:3.37.0
//DEPS com.google.googlejavaformat:google-java-format:1.17.0
//DEPS org.eclipse.platform:org.eclipse.jface.text:3.28.0
//DEPS info.picocli:picocli:4.7.5
//JAVA_OPTIONS --add-exports jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED
//JAVA_OPTIONS --add-exports jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED
//JAVA_OPTIONS --add-exports jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED
//JAVA_OPTIONS --add-exports jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED

package dev.jbang.fmt;

import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import java.util.concurrent.Callable;

import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.TextEdit;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;
import picocli.CommandLine.ArgGroup;

/**
 * Interface for Java formatters
 */
interface JavaFormatter {
    String format(String content) throws Exception;
    String getName();
}

/**
 * Eclipse Java formatter implementation
 */
class EclipseJavaFormatter implements JavaFormatter {
    @Override
    public String format(String content) throws Exception {
        CodeFormatter codeFormatter = ToolFactory.createCodeFormatter(null);
        TextEdit edit = codeFormatter.format(
            CodeFormatter.K_COMPILATION_UNIT, 
            content, 
            0, 
            content.length(), 
            0, 
            null
        );
        
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
 * Google Java formatter implementation
 */
class GoogleJavaFormatter implements JavaFormatter {
    @Override
    public String format(String content) throws Exception {
        try {
            Formatter formatter = new Formatter();
            return formatter.formatSource(content);
        } catch (FormatterException e) {
            System.err.println("Google formatter error: " + e.getMessage());
            return content; // Return original content if formatting fails
        }
    }
    
    @Override
    public String getName() {
        return "Google";
    }   

    @Override
    public String toString() {
        return "GoogleJavaFormatter";
    }
}

/**
 * Option group for mutually exclusive formatter selection
 */
class FormatterGroup {
    @Option(names = "--eclipse", description = "Use Eclipse Java formatter")
    boolean useEclipse;
    
    @Option(names = "--google", description = "Use Google Java formatter")
    boolean useGoogle;
}

/**
 * Java formatter CLI tool supporting Eclipse and Google Java formatters.
 */
@Command(name = "fmt", mixinStandardHelpOptions = true, version = "1.0",
         description = "Format Java source files using Eclipse or Google Java formatters")
public class fmt implements Callable<Integer> {
    
    @ArgGroup(exclusive = true, multiplicity = "1")
    FormatterGroup formatterGroup;
    
    @Parameters(description = "Java files or directories to format", arity = "1..*")
    private List<Path> sources;
    
    public static void main(String[] args) {
        int exitCode = new CommandLine(new fmt()).execute(args);
        System.exit(exitCode);
    }
    
    @Override
    public Integer call() throws Exception {
        
        try {
            JavaFormatter formatter = formatterGroup.useEclipse ? 
                new EclipseJavaFormatter() : new GoogleJavaFormatter();
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
            
            // Use the provided formatter
            String formatted = formatter.format(content);
            
            // Write back the formatted content
            Files.write(file, formatted.getBytes());
        } catch (Exception e) {
            System.err.println("Error formatting " + file + ": " + e.getMessage());
            // Don't rethrow, just continue with other files
        }
    }
    
}
