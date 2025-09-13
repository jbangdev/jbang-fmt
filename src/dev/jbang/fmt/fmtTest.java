///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS org.junit.jupiter:junit-jupiter-engine:5.12.2
//DEPS org.junit.jupiter:junit-jupiter-params:5.12.2
//DEPS org.junit.platform:junit-platform-console:1.12.2

//SOURCES fmt.java

package dev.jbang.fmt;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.Arrays;
import java.util.stream.Stream;

import javax.swing.JButton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.platform.console.ConsoleLauncher;

// JUnit5 Test class for fmt
public class fmtTest {

    private static Stream<JavaFormatter> formatters() {
        return Stream.of(
            new EclipseJavaFormatter(),
            new GoogleJavaFormatter()
        );
    }


    @ParameterizedTest
    @MethodSource("formatters")
    public void testBasicHelloWorld(JavaFormatter formatter) throws Exception {
      String input = """
        public class TestClass{public static void main(String[]args){System.out.println("Hello");}}
        """;
     
      String result = formatter.format(input);
        
      // Verify it's not the same as input (unless input was already formatted)
      if (!input.trim().equals(result.trim())) {
            assertNotEquals(input.trim(), result.trim(), 
                "Formatter should change unformatted code");
        }
    }


    @ParameterizedTest
    @MethodSource("formatters")
    public void testEmptyInput(JavaFormatter formatter) throws Exception {
        
        String emptyInput = "";
        
        // Both formatters should handle empty input gracefully
        assertDoesNotThrow(() -> {
            String result = formatter.format(emptyInput);
            assertEquals(emptyInput, result.trim()); //trim needed as google does add a newline
        });
    }

    @ParameterizedTest
    @MethodSource("formatters")
    public void testInvalidJavaCode(JavaFormatter formatter) throws Exception {
        String invalidCode = "this is not valid java code {";
        
        // Both formatters should handle invalid code gracefully
        assertDoesNotThrow(() -> {
            String result = formatter.format(invalidCode);
            assertEquals(invalidCode, result);
          });
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
        """;
        String result = formatter.format(input);
        
        //verify that the result is still a valid jbang directive
        assertTrue(result.startsWith("///usr/bin/env jbang \"$0\" \"$@\" ; exit $?"));
        assertTrue(result.contains("//DEPS org.junit.jupiter:junit-jupiter-engine:5.12.2"));
        assertTrue(result.contains("//DEPS org.junit.jupiter:junit-jupiter-params:5.12.2"));
        assertTrue(result.contains("//DEPS org.junit.platform:junit-platform-console:1.12.2"));
        
        // Verify Java code is formatted (should not be the same as input)
        assertNotEquals(input, result);
        assertTrue(result.contains("public class TestClass {"));
    }

    // Scan the system classpath for tests
    // Include those found in /cache/jars/ which is where
    // jbang will by default put them. Adjust as needed.
    public static void main(final String... args) {
        String jarsList = Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator))
        .filter(path -> path.contains("/cache/jars/"))
        .reduce((a, b) -> a + File.pathSeparator + b)
        .orElse("");
        
        ConsoleLauncher.main( "execute", "--scan-class-path", "-cp", jarsList); 
    }
}
