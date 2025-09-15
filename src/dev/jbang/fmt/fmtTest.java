///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS org.junit.jupiter:junit-jupiter-engine:5.12.2
//DEPS org.junit.jupiter:junit-jupiter-params:5.12.2
//DEPS org.junit.platform:junit-platform-console:1.12.2
//DEPS org.assertj:assertj-core:3.25.1

//SOURCES ../../../../src/**/*.java

package dev.jbang.fmt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.File;
import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.platform.console.ConsoleLauncher;

import dev.jbang.fmt.JavaFormatter;

// JUnit5 Test class for fmt
public class fmtTest {

	private static Stream<JavaFormatter> formatters() {
		return Stream.of(new JavaFormatter("test", null, true));
	}

	@ParameterizedTest
	@MethodSource("formatters")
	public void testBasicHelloWorld(JavaFormatter formatter) throws Exception {
		String input = """
				public class TestClass{public static void main(String[]args){System.out.println("Hello");}}
				""";

		String result = formatter.format(input);

		// Verify it's not the same as input (unless input was already formatted)
		assertThat(result.trim()).as("Formatter %s should change unformatted code", formatter.getName())
			.isNotEqualTo(input.trim());
	}

	@ParameterizedTest
	@MethodSource("formatters")
	public void testEmptyInput(JavaFormatter formatter) throws Exception {

		String emptyInput = "";

		// Both formatters should handle empty input gracefully
		assertThatCode(() -> {
			String result = formatter.format(emptyInput);
			assertThat(result.trim()).as("Formatter %s should handle empty input gracefully", formatter.getName())
				.isEqualTo(emptyInput); //trim needed as google does add a newline
		}).doesNotThrowAnyException();
	}

	@ParameterizedTest
	@MethodSource("formatters")
	public void testInvalidJavaCode(JavaFormatter formatter) throws Exception {
		String invalidCode = "this is not valid java code {";

		// Both formatters should handle invalid code gracefully
		assertThatCode(() -> {
			String result = formatter.format(invalidCode);
			assertThat(result).as("Formatter %s should handle invalid Java code gracefully", formatter.getName())
				.isEqualTo(invalidCode);
		}).doesNotThrowAnyException();
	}

	@ParameterizedTest
	@MethodSource("formatters")
	public void testJBangDirectivesSurvive(JavaFormatter formatter) throws Exception {
		String input = """
				///usr/bin/env jbang "$0" "$@" ; exit $?
				      //DEPS org.junit.jupiter:junit-jupiter-engine:5.12.2
				    //DEPS org.junit.jupiter:junit-jupiter-params:5.12.2
				    //DEPS org.junit.platform:junit-platform-console:1.12.2

				public class TestClass{public static void main(String[]args){System.out.println("Hello");}}

				      //DEPS org.assertj:assertj-core:3.25.1
				""";
		String result = formatter.format(input);

		//verify that the result is still a valid jbang directive
		// Note: Google formatter may add spaces after //, so we check for the core shebang
		assertThat(result.lines().findFirst().orElse(""))
			.as("Formatter %s should preserve JBang shebang directive", formatter.getName())
			.contains("///usr/bin/env jbang \"$0\" \"$@\" ; exit $?");

		assertThat(result).as("Formatter %s should preserve DEPS directives", formatter.getName())
			.contains("//DEPS org.junit.jupiter:junit-jupiter-engine:5.12.2")
			.contains("//DEPS org.junit.jupiter:junit-jupiter-params:5.12.2")
			.contains("//DEPS org.junit.platform:junit-platform-console:1.12.2")
			.contains("//DEPS org.assertj:assertj-core:3.25.1");

		// Verify Java code is formatted (should not be the same as input)
		assertThat(result).as("Formatter %s should format Java code", formatter.getName())
			.isNotEqualTo(input)
			.contains("public class TestClass {");
	}

	@ParameterizedTest
	@MethodSource("formatters")
	public void testFormatterNames(JavaFormatter formatter) throws Exception {
		assertThat(formatter.getName()).as("Formatter should have a non-null name")
			.isNotNull()
			.isIn("Eclipse",
					"Google");
	}

	@ParameterizedTest
	@MethodSource("formatters")
	public void testComplexJavaCode(JavaFormatter formatter) throws Exception {
		String input = """
				public class ComplexTest{private String name;public ComplexTest(String name){this.name=name;}public String getName(){return name;}public void setName(String name){this.name=name;}}
				""";

		String result = formatter.format(input);

		assertThat(result).as("Formatter %s should format complex Java code", formatter.getName())
			.isNotEqualTo(input)
			.contains("public class ComplexTest")
			.contains("private String name")
			.contains("public ComplexTest(String name)")
			.contains("public String getName()")
			.contains("public void setName(String name)");
	}

	@ParameterizedTest
	@MethodSource("formatters")
	public void testNestedStructures(JavaFormatter formatter) throws Exception {
		String input = """
				public class NestedTest{public static void main(String[]args){if(true){if(false){System.out.println("Inner");}else{System.out.println("Outer");}}}}
				""";

		String result = formatter.format(input);

		assertThat(result).as("Formatter %s should handle nested structures", formatter.getName())
			.isNotEqualTo(input)
			.contains("public class NestedTest")
			.contains("if (true)")
			.contains("if (false)")
			.contains("System.out.println(\"Inner\")")
			.contains("System.out.println(\"Outer\")");
	}

	// Scan the system classpath for tests
	// Include those found in /cache/jars/ which is where
	// jbang will by default put them. Adjust as needed.
	public static void main(final String... args) {
		String jarsList = Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator))
			.filter(path -> path.contains("/cache/jars/"))
			.reduce((a, b) -> a + File.pathSeparator + b)
			.orElse("");

		ConsoleLauncher.main("execute", "--scan-class-path", "-cp", jarsList);
	}
}
