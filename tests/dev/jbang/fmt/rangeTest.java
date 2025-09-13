///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS org.junit.jupiter:junit-jupiter-engine:5.12.2
//DEPS org.junit.jupiter:junit-jupiter-params:5.12.2
//DEPS org.junit.platform:junit-platform-console:1.12.2
//DEPS org.assertj:assertj-core:3.25.1

//SOURCES ../../../../src/**/*.java

package dev.jbang.fmt;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.platform.console.ConsoleLauncher;

import dev.jbang.fmt.javafmt;
import dev.jbang.fmt.javafmt.CodeRange;
import dev.jbang.fmt.javafmt.JBangDirectiveHandler;

// JUnit5 Test class for fmt
public class rangeTest {

	@Test
	public void testDirectives() throws Exception {

		assertThat(JBangDirectiveHandler.isJBangDirective("//DEPS org.junit.jupiter:junit-jupiter-engine:5.12.2"))
				.isTrue();
		assertThat(JBangDirectiveHandler.isJBangDirective("//JAVA 21+")).isTrue();
		assertThat(JBangDirectiveHandler.isJBangDirective("//DEPS org.junit.jupiter:junit-jupiter-engine:5.12.2"))
				.isTrue();
	}

	@Test
	public void testPureJava() throws Exception {

		String alljava = """
				package com.example;
				System.out.println("Hello");
				""";

		List<CodeRange> ranges = JBangDirectiveHandler.identifyJavaRanges(alljava);

		assertThat(ranges).hasSize(1);

		assertThat(ranges.get(0).start()).isEqualTo(0);
		assertThat(ranges.get(0).end()).isEqualTo(alljava.length());
	}

	@Test
	public void testPureJBang() throws Exception {

		String pureJBang = """
		        //DEPS org.junit.jupiter:junit-jupiter-engine:5.12.2
        //JAVA 21+""";

		List<CodeRange> ranges = JBangDirectiveHandler.identifyJavaRanges(pureJBang);

		assertThat(ranges).hasSize(0);
	}

	@Test
	public void testMixed() throws Exception {

		String alljava = """
				///usr/bin/env jbang "$0" "$@" ; exit $?
		        //DEPS org.junit.jupiter:junit-jupiter-engine:5.12.2
        //DEPS org.junit.jupiter:junit-jupiter-params:5.12.2
        //DEPS org.junit.platform:junit-platform-console:1.12.2
				public class TestClass{public static void main(String[]args){System.out.println("Hello");}}
				""";

		List<CodeRange> ranges = JBangDirectiveHandler.identifyJavaRanges(alljava);

		assertThat(ranges).hasSize(1);

		assertThat(ranges.get(0).start()).isEqualTo(alljava.indexOf("public"));
		assertThat(ranges.get(0).end()).isEqualTo(alljava.length());

	}

    // Scan the system classpath for tests
    // Include those found in /cache/jars/ which is where
	// jbang will by default put them. Adjust as needed.
	public static void main(final String... args) {
		String jarsList = Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator))
				.filter(path -> path.contains("/cache/jars/")).reduce((a, b) -> a + File.pathSeparator + b).orElse("");

		ConsoleLauncher.main("execute", "--scan-class-path", "-cp", jarsList);
	}
}
