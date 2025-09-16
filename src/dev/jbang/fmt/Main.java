///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 21+
//DEPS org.eclipse.jdt:org.eclipse.jdt.core:3.43.0
//DEPS org.eclipse.platform:org.eclipse.jface.text:3.28.0
//DEPS info.picocli:picocli:4.7.7

//JAVAC_OPTIONS -proc:full -Averbose=true

//FILES ../../../google.xml ../../../java.xml ../../../eclipse.xml ../../../jbang.xml ../../../spring.prefs ../../../quarkus.xml

//SOURCES JavaFormatter.java CodeRange.java KeyValueConsumer.java CommaSeparatedConverter.java FmtLogger.java

package dev.jbang.fmt;

import static dev.jbang.fmt.FmtLogger.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jdt.core.JavaCore;

import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.IParameterExceptionHandler;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.UnmatchedArgumentException;

/**
 * Java formatter CLI tool supporting Eclipse and Google Java formatters.
 */
@Command(name = "javafmt", mixinStandardHelpOptions = true, showAtFileInUsageHelp = true, version = "1.0", description = "Format Java w/JBang source files using Eclipse Java formatter")
public class Main implements Callable<Integer> {

	static class ShortErrorMessageHandler implements IParameterExceptionHandler {

		public int handleParseException(ParameterException ex, String[] args) {
			CommandLine cmd = ex.getCommandLine();
			PrintWriter err = cmd.getErr();

			// if tracing at DEBUG level, show the location of the issue
			if ("DEBUG".equalsIgnoreCase(System.getProperty("picocli.trace"))) {
				err.println(cmd.getColorScheme().stackTraceText(ex));
			}

			err.println(cmd.getColorScheme().errorText(ex.getMessage())); // bold red
			UnmatchedArgumentException.printSuggestions(ex, err);
			err.println();
			err.print(cmd.getHelp().fullSynopsis());

			CommandSpec spec = cmd.getCommandSpec();
			err.printf("Try '%s --help' for more information.%n", spec.qualifiedName());

			return cmd.getExitCodeExceptionMapper() != null
					? cmd.getExitCodeExceptionMapper().getExitCode(ex)
					: spec.exitCodeOnInvalidInput();
		}
	}

	/**
	 * Tracks file processing statistics - thread-safe for concurrent access
	 */
	private static class FileStats {
		private final AtomicInteger processed = new AtomicInteger(0);
		private final AtomicInteger modified = new AtomicInteger(0);
		private final AtomicInteger skipped = new AtomicInteger(0);

		private final long startTime;

		FileStats() {
			startTime = System.nanoTime();
		}

		void addProcessed() {
			processed.incrementAndGet();
		}

		void addModified() {
			modified.incrementAndGet();
		}

		void addSkipped() {
			skipped.incrementAndGet();
		}

		double getElapsedSeconds() {
			return (System.nanoTime() - startTime) / 1_000_000_000.0;
		}

		String getNormalOutput() {
			int proc = processed.get();
			int mod = modified.get();
			int skip = skipped.get();
			int clean = proc - mod;
			return String.format("Processed %d files (%d changed, %d clean, %d skipped) in %.1fs", proc, mod,
					clean, skip, getElapsedSeconds());
		}

		String getCheckOutput() {
			int mod = modified.get();
			int proc = processed.get();
			return String.format("Would reformat %d files (out of %d) in %.1fs. Run without --check to apply.",
					mod, proc, getElapsedSeconds());
		}
	}

	@Option(names = "--touch-jbang", hidden = true, negatable = true, description = "Let formatter touch JBang directives", defaultValue = "false")
	boolean touchJBang;

	@Option(names = "--stdout", description = "Print formatted content to stdout")
	private boolean stdout;

	@CommandLine.ArgGroup(exclusive = true)
	FmtLogger verboseQuietExclusive = new FmtLogger();

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

		enum IndentWith {
			space,
			tab
		}

		@Option(names = "--indent-with", description = "Use spaces or tabs for indentation")
		private Optional<IndentWith> indentWith;

		@Option(names = "--indent-size", description = "Override the indent size")
		private Optional<Integer> indentSize;

		@Option(names = "--java", description = "Override the java version in formatter (8, 11, 17, 21, etc.)")
		private Optional<String> javaVersion;

		@Option(names = { "--settings",
				"-S" }, description = "Override of settings by key. Can be used multiple times or use comma separated list.", converter = CommaSeparatedConverter.class)
		private List<String> settings;

	}

	public static void main(String[] args) {
		int exitCode = new CommandLine(new Main())
			.setParameterExceptionHandler(new ShortErrorMessageHandler())
			.execute(args);
		System.exit(exitCode);
	}

	void overrideSettings(Map<String, String> settings, String key, String value) {
		var override = settings.put(key, value);
		if (override != null) {
			verbose("Overriding " + key.replaceFirst("^org.eclipse.jdt.core.formatter.", "") + " from "
					+ override + " to " + value);
		} else {
			verbose("Setting " + key.replaceFirst("^org.eclipse.jdt.core.formatter.", "") + " to " + value);
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

			formattingSettings.indentWith.ifPresent(iw -> {
				overrideSettings(realsettings, "org.eclipse.jdt.core.formatter.tabulation.char", iw.name());
			});
			formattingSettings.indentSize.ifPresent(is -> {
				overrideSettings(realsettings, "org.eclipse.jdt.core.formatter.indentation.size", is.toString());

				overrideSettings(realsettings, "org.eclipse.jdt.core.formatter.tabulation.size", is.toString());
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

			verbose("Formatting with " + formatter + "...");

			FileStats stats = new FileStats();
			formatFiles(sources, formatter, stdout, check, stats);

			// Print summary based on mode
			if (stdout) {
				// For stdout mode, don't print summary as it would interfere with the output
			} else if (check) {
				requiredInfo(stats.getCheckOutput());
			} else {
				requiredInfo(stats.getNormalOutput());
			}

			return (check && stats.modified.get() > 0) ? 1 : 0;
		} catch (Exception e) {
			error("Error: " + e.getMessage(), e);
			return 1;
		}
	}

	private static void formatFiles(List<Path> sourcePaths, JavaFormatter formatter, boolean stdout, boolean check,
			FileStats stats) throws Exception {

		// Track processed files to avoid duplicates
		Set<Path> processedFiles = ConcurrentHashMap.newKeySet();

		// CPU limit semaphore to prevent overwhelming the system
		var cpuLimit = new Semaphore(Runtime.getRuntime().availableProcessors());

		var namingFactory = Thread.ofVirtual().name("fmt-", 0).factory();

		try (var executor = Executors.newThreadPerTaskExecutor(namingFactory)) {
			// Back-pressure: bounded queue of Paths
			BlockingQueue<Path> queue = new ArrayBlockingQueue<Path>(10_000);

			// Producer: walk directories and put files in queue
			var walking = executor.submit(producePaths(sourcePaths, stats, queue));

			// create Consumers: process files from queue, but no more than 2x the number of
			// CPUs
			int consumers = Math.max(2, Runtime.getRuntime().availableProcessors() * 2);
			var tasks = new ArrayList<Future<?>>();
			for (int i = 0; i < consumers; i++) {
				tasks.add(executor.submit(() -> {
					for (;;) {
						Path p;
						try {
							p = queue.take();
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							break;
						}

						if (p.equals(DONE)) {
							queue.offer(p);
							break;
						}

						// Skip if already processed
						if (!processedFiles.add(p.toAbsolutePath())) {
							continue;
						}

						// Acquire CPU permit before formatting
						try {
							cpuLimit.acquire();
							formatFile(p, formatter, stdout, check, stats);
						} catch (Exception e) {
							error("Failed " + p + ": " + e.getMessage());
						} finally {
							cpuLimit.release();
						}

					}
				}));
			}

			walking.get(); // propagate discovery errors
			for (var t : tasks)
				t.get(); // propagate formatting errors
		}
	}

	private static final Path DONE = Path.of("ENDENDEND");

	private static Runnable producePaths(List<Path> sourcePaths, FileStats stats, BlockingQueue<Path> queue) {
		return () -> {
			for (Path target : sourcePaths) {
				if (Files.exists(target)) {
					if (Files.isDirectory(target)) {
						try (var paths = Files.walk(target)) {
							paths.filter(Files::isRegularFile)
								.filter(p -> p.toString().endsWith(".java"))
								.forEach(p -> {
									try {
										queue.put(p);
									} catch (InterruptedException ie) {
										Thread.currentThread().interrupt();
									}
								});
						} catch (IOException e) {
							throw new UncheckedIOException(e);
						}
					} else if (target.toString().endsWith(".java")) {
						try {
							queue.put(target);
						} catch (InterruptedException ie) {
							Thread.currentThread().interrupt();
						}
					} else {
						stats.addSkipped();
					}
				} else {
					error("Warning: Path does not exist: " + target);
				}
			}
			// Poison pill to signal done
			queue.offer(DONE);
		};
	}

	private static void formatFile(Path file, JavaFormatter formatter, boolean stdout, boolean check, FileStats stats)
			throws Exception {

		// Read the file content
		String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
		String formatted = formatter.format(content);
		boolean fileChanged = !formatted.equals(content);

		// Always count as processed
		stats.addProcessed();

		// Count as modified if it would change
		if (fileChanged) {
			stats.addModified();
		}

		if (stdout) {
			// Always print formatted content to stdout if requested.
			requiredInfo(formatted);
		}

		if (fileChanged) {
			info(file.toString());
			if (!check && !stdout) {
				//could consider using atomic file operations for safety
				//but for now keep it simple.
				//Files.copy(file, file.resolveSibling(file.getFileName().toString() + ".bak"),
				//		StandardCopyOption.REPLACE_EXISTING);
				Files.write(file, formatted.getBytes(StandardCharsets.UTF_8));
			}
		}
	}

}
