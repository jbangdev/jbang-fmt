///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 21+
//DEPS org.eclipse.jdt:org.eclipse.jdt.core:3.37.0
//DEPS org.eclipse.platform:org.eclipse.jface.text:3.28.0
//DEPS info.picocli:picocli:4.7.7

//FILES ../../../google.xml ../../../java.xml ../../../eclipse.xml ../../../jbang.xml ../../../spring.prefs ../../../quarkus.xml

//SOURCES JavaFormatter.java CodeRange.java KeyValueConsumer.java CommaSeparatedConverter.java

package dev.jbang.fmt;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Java formatter CLI tool supporting Eclipse and Google Java formatters.
 */
@Command(name = "javafmt", mixinStandardHelpOptions = true, version = "1.0", description = "Format Java w/JBang source files using Eclipse Java formatter")
public class Main implements Callable<Integer> {

	/**
	 * Tracks file processing statistics
	 */
	private static class FileStats {
		int processed = 0;
		int modified = 0;
		int skipped = 0;

		long startTime;

		FileStats() {
			startTime = System.nanoTime();
		}

		void addProcessed() {
			processed++;
		}

		void addModified() {
			modified++;
		}

		void addSkipped() {
			skipped++;
		}

		double getElapsedSeconds() {
			return (System.nanoTime() - startTime) / 1_000_000_000.0;
		}

		String getNormalOutput() {
			int clean = processed - modified;
			return String.format("Formatted %d files (%d changed, %d clean, %d skipped) in %.1fs", processed, modified,
					clean, skipped, getElapsedSeconds());
		}

		String getCheckOutput() {
			return String.format("Would reformat %d files (out of %d) in %.1fs. Run without --check to apply.",
					modified, processed, getElapsedSeconds());
		}
	}

	@Option(names = "--touch-jbang", hidden = true, negatable = true, description = "Let formatter touch JBang directives", defaultValue = "false")
	boolean touchJBang;

	@Option(names = "--stdout", description = "Print formatted content to stdout")
	private boolean stdout;

	@Option(names = "--check", description = "Check if files would change. Exit 1 if any file would change.")
	private boolean check;

	@Option(names = "--style", description = "Formatter settings file (.xml or .prefs) or predefined style (jbang, eclipse, google, java, quarkus or spring)", defaultValue = "jbang")
	private Path styleFile;

	@Parameters(description = "Java files or directories to format", arity = "1..*")
	private List<Path> sources;

	@ArgGroup(heading = "%nOverride Formatting Settings%n%n", validate = false)
	private FormattingSettings formattingSettings = new FormattingSettings();

	static class FormattingSettings {

		@Option(names = "--line-length", description = "Override line length for formatter")
		private Optional<Integer> lineLength;

		@Option(names = "--java", description = "Override the java version in formatter (8, 11, 17, 21, etc.)")
		private Optional<String> javaVersion;

		@Option(names = "--settings", description = "Override of matter settings file (key=value)", converter = CommaSeparatedConverter.class)
		private List<String> settings;

	}

	public static void main(String[] args) {
		int exitCode = new CommandLine(new Main()).execute(args);
		System.exit(exitCode);
	}

	void overrideSettings(Map<String, String> settings, String key, String value) {
		var override = settings.put(key, value);
		if (override != null) {
			System.out.println("Overriding " + key.replaceFirst("^org.eclipse.jdt.core.formatter.", "") + " from "
					+ override + " to " + value);
		}
	}

	Map<String, String> majorVersionToEclipseVersion = Map.of(
			"1", JavaCore.VERSION_1_1,
			"2", JavaCore.VERSION_1_2,
			"3", JavaCore.VERSION_1_3,
			"4", JavaCore.VERSION_1_4,
			"5", JavaCore.VERSION_1_5,
			"6", JavaCore.VERSION_1_6,
			"7", JavaCore.VERSION_1_7,
			"8", JavaCore.VERSION_1_8);

	@Override
	public Integer call() throws Exception {

		try {
			JavaFormatter formatter;

			final Map<String, String> realsettings = JavaFormatter.loadEclipseSettings(styleFile);

			formattingSettings.lineLength.ifPresent(ll -> {
				overrideSettings(realsettings, "org.eclipse.jdt.core.formatter.comment.line_length",
						ll.toString());
				overrideSettings(realsettings, "org.eclipse.jdt.core.formatter.lineSplit",
						ll.toString());
			});
			formattingSettings.javaVersion.ifPresent(jv -> {
				jv = majorVersionToEclipseVersion.getOrDefault(jv, jv);

				overrideSettings(realsettings, JavaCore.COMPILER_COMPLIANCE,
						jv);
				overrideSettings(realsettings, JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM,
						jv);
				overrideSettings(realsettings, JavaCore.COMPILER_SOURCE,
						jv);
			});

			if (formattingSettings.settings != null) {
				formattingSettings.settings.stream().map(s -> s.split("=")).forEach(kv -> {
					if (!kv[0].startsWith("org.eclipse.jdt.core.formatter.")) {
						kv[0] = "org.eclipse.jdt.core.formatter." + kv[0];
					}
					overrideSettings(realsettings, kv[0], kv.length > 1 ? kv[1] : "true");
				});
			}

			formatter = new JavaFormatter(styleFile.toString(), realsettings, touchJBang);

			System.out.println("Formatting with " + formatter + "...");

			FileStats stats = new FileStats();
			formatFiles(sources, formatter, stdout, check, stats);

			// Print summary based on mode
			if (stdout) {
				// For stdout mode, don't print summary as it would interfere with the output
			} else if (check) {
				System.out.println(stats.getCheckOutput());
			} else {
				System.out.println(stats.getNormalOutput());
			}

			return (check && stats.modified > 0) ? 1 : 0;
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace();
			return 1;
		}
	}

	private static void formatFiles(List<Path> targets, JavaFormatter formatter, boolean stdout, boolean check,
			FileStats stats) throws Exception {
		for (Path path : targets) {
			if (Files.exists(path)) {
				if (Files.isDirectory(path)) {
					formatDirectory(path, formatter, stdout, check, stats);
				} else if (path.toString().endsWith(".java")) {
					formatFile(path, formatter, stdout, check, stats);
				} else {
					stats.addSkipped();
					//System.out.println("Skipping non-Java file: " + path);
				}
			} else {
				System.err.println("Warning: Path does not exist: " + path);
			}
		}
	}

	private static void formatDirectory(Path dir, JavaFormatter formatter, boolean stdout, boolean check,
			FileStats stats) throws Exception {
		try (Stream<Path> paths = Files.walk(dir)) {
			for (Path path : paths.filter(path -> path.toString().endsWith(".java")).toList()) {
				try {
					formatFile(path, formatter, stdout, check, stats);
				} catch (Exception e) {
					System.err.println("Error formatting " + path + ": " + e.getMessage());
				}
			}
		}
	}

	private static void formatFile(Path file, JavaFormatter formatter, boolean stdout, boolean check, FileStats stats)
			throws Exception {

		try {
			// Read the file content
			String content = new String(Files.readAllBytes(file));

			String formatted = formatter.format(content);

			boolean fileChanged = !formatted.equals(content);

			// Always count as processed
			stats.addProcessed();

			// Count as modified if it would change
			if (fileChanged) {
				stats.addModified();
			}

			if (stdout) {
				// Print formatted content to stdout
				System.out.print(formatted);
			} else if (check) {
				// Check mode: just report if file would change
				if (fileChanged) {
					System.out.println(file);
				}
			} else {
				// Normal mode: write back if changed
				if (fileChanged) {
					System.out.println(file);
					// Write back the formatted content
					Files.write(file, formatted.getBytes());
				}
			}
		} catch (Exception e) {
			System.err.println("Error formatting " + file + ": " + e.getMessage());
			// Don't rethrow, just continue with other files
		}
	}

}
