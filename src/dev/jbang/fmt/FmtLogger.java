package dev.jbang.fmt;

import picocli.CommandLine.Option;
import picocli.CommandLine.ScopeType;

// stupid simple "hack" for easy control of verbose and quiet output
class FmtLogger {

	static boolean verbose = false;
	static boolean quiet = false;

	@Option(names = {
			"--verbose" }, description = "Be verbose on what it does.", scope = ScopeType.INHERIT)
	void setVerbose(boolean value) {
		verbose = value;
	}

	@Option(names = {
			"--quiet", "-q" }, description = "Be quiet, only print when error occurs.", scope = ScopeType.INHERIT)
	void setQuiet(boolean value) {
		quiet = value;
	}

	public static void info(String message) {
		if (!quiet) {
			System.out.println(message);
		}
	}

	public static void requiredInfo(String message) {
		System.out.println(message);
	}

	public static void verbose(String message) {
		if (verbose) {
			System.out.println(message);
		}
	}

	public static void error(String message) {
		System.err.println(message);
	}

	public static void error(String message, Exception e) {
		System.err.println(message);
		e.printStackTrace(System.err);
	}
}