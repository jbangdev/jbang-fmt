///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 21+
//DEPS org.eclipse.jdt:org.eclipse.jdt.core:3.37.0
//DEPS org.eclipse.platform:org.eclipse.jface.text:3.28.0
//DEPS info.picocli:picocli:4.7.7

//FILES ../../../google.xml ../../../java.xml ../../../eclipse.xml ../../../jbang.xml ../../../spring.prefs ../../../quarkus.xml

//SOURCES JavaFormatter.java CodeRange.java

package dev.jbang.fmt;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Java formatter CLI tool supporting Eclipse and Google Java formatters.
 */
@Command(name = "javafmt", mixinStandardHelpOptions = true, version = "1.0", description = "Format Java w/JBang source files using Eclipse Java formatter")
public class Main implements Callable<Integer> {

	@Option(names = "--jbang-friendly", hidden = true, negatable = true, description = "Use JBang friendly formatter settings (protects JBang directives)", defaultValue = "true", fallbackValue = "true")
	boolean jbangFriendly;

	@Option(names = "--settings", description = "Eclipse formatter settings file (.xml or .prefs)")
	private Path settingsFile;

	@Parameters(description = "Java files or directories to format", arity = "1..*")
	private List<Path> sources;

	public static void main(String[] args) {
		int exitCode = new CommandLine(new Main()).execute(args);
		System.exit(exitCode);
	}

	@Override
	public Integer call() throws Exception {

		try {
			JavaFormatter formatter;

			if (settingsFile != null) {
				var settings = JavaFormatter.loadEclipseSettings(settingsFile);
				formatter = new JavaFormatter(settingsFile.toString(), settings, jbangFriendly);
			} else {
				formatter = new JavaFormatter("default", null, jbangFriendly);
			}

			System.out.println("Formatting with " + formatter + "...");

			formatFiles(sources, formatter);
			return 0;
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace();
			return 1;
		}
	}

	private static void formatFiles(List<Path> targets, JavaFormatter formatter) throws Exception {
		for (Path path : targets) {
			if (Files.exists(path)) {
				if (Files.isDirectory(path)) {
					formatDirectory(path, formatter);
				} else if (path.toString().endsWith(".java")) {
					formatFile(path, formatter);
				} else {
					//System.out.println("Skipping non-Java file: " + path);
				}
			} else {
				System.err.println("Warning: Path does not exist: " + path);
			}
		}
	}

	private static void formatDirectory(Path dir, JavaFormatter formatter) throws Exception {
		try (Stream<Path> paths = Files.walk(dir)) {
			paths.filter(path -> path.toString().endsWith(".java")).forEach(path -> {
				try {
					formatFile(path, formatter);
				} catch (Exception e) {
					System.err.println("Error formatting " + path + ": " + e.getMessage());
				}
			});
		}
	}

	private static void formatFile(Path file, JavaFormatter formatter) throws Exception {

		try {
            // Read the file content
			String content = new String(Files.readAllBytes(file));

			String formatted = formatter.format(content);

			if (formatted.equals(content)) {
				//System.out.println("No changes needed for " + file);
				return;
			}
			System.out.println(file);

            // Write back the formatted content
			Files.write(file, formatted.getBytes());
		} catch (Exception e) {
			System.err.println("Error formatting " + file + ": " + e.getMessage());
            // Don't rethrow, just continue with other files
		}
	}

}
