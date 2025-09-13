package dev.jbang.fmt;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for handling JBang directives and identifying Java code ranges
 */
public record CodeRange(int start, int end) {
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