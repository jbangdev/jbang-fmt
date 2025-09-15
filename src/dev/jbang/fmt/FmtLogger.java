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

	public static boolean info(String message) {
		if (quiet) {
			return false;
		}
		System.out.println(message);
		return true;
	}

	public static boolean requiredInfo(String message) {
		System.out.println(message);
		return true;
	}

	public static boolean verbose(String message) {
		if (!verbose) {
			return false;
		}
		System.out.println(message);
		return true;
	}

	public static boolean error(String message) {
		System.err.println(message);
		return true;
	}
}