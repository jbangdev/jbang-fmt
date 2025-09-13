# Java Formatter CLI Tool

A command-line tool for formatting Java source files using the actual Eclipse JDT or Google Java formatters.

## Usage

```bash
# Format a single file
jbang src/fmt.java --eclipse MyFile.java
jbang src/fmt.java --google MyFile.java

# Format all Java files in a directory
jbang src/fmt.java --eclipse src/
jbang src/fmt.java --google src/

# Format multiple files and directories
jbang src/fmt.java --eclipse file1.java file2.java dir1/ dir2/

# Show help
jbang src/fmt.java --help
```

## Formatters

### Eclipse Formatter (`--eclipse`)
- Uses the real Eclipse JDT formatter
- Follows Eclipse Java formatting conventions
- Uses tabs for indentation by default
- Full Eclipse formatting rules applied

### Google Formatter (`--google`)
- Uses the real Google Java Format library
- Follows Google Java Style Guide conventions
- Uses 2-space indentation
- Full Google formatting rules applied

## Requirements

- Java 11 or higher
- JBang (for running the script)

## Installation

No installation required! Just run with JBang:

```bash
jbang src/fmt.java --help
```

## Examples

### Before formatting:
```java
public class Test{public static void main(String[]args){System.out.println("Hello");}}
```

### After Eclipse formatting:
```java
public class Test {
    public static void main(String[] args) {
        System.out.println("Hello");
    }
}
```

### After Google formatting:
```java
public class Test {
  public static void main(String[] args) {
    System.out.println("Hello");
  }
}
```

## Features

- **Interface-Based Design**: Clean separation with `JavaFormatter` interface and concrete implementations
- **Real Formatters**: Uses actual Eclipse JDT and Google Java Format libraries
- **Picocli Integration**: Professional command-line interface with help and validation
- **Mutual Exclusivity**: Eclipse and Google options are mutually exclusive (enforced by picocli)
- **JVM Module Support**: Includes necessary JVM options for Google formatter compatibility
- **Directory Processing**: Recursively processes all Java files in directories
- **Error Handling**: Graceful error handling with informative messages

## Architecture

The tool uses a clean interface-based design:

- **`JavaFormatter`**: Base interface with `format(String)` and `getName()` methods
- **`EclipseJavaFormatter`**: Implements Eclipse JDT formatting
- **`GoogleJavaFormatter`**: Implements Google Java Format formatting
- **Unified Processing**: Single `formatFiles()` method works with any formatter implementation

This design makes it easy to add new formatters in the future.

## Dependencies

- Eclipse JDT Core (org.eclipse.jdt:org.eclipse.jdt.core:3.37.0)
- Google Java Format (com.google.googlejavaformat:google-java-format:1.17.0)
- Eclipse JFace Text (org.eclipse.platform:org.eclipse.jface.text:3.28.0)
- Picocli (info.picocli:picocli:4.7.5)

## Notes

- The tool processes files in-place (modifies the original files)
- Non-Java files are skipped with a warning message
- JVM module exports are automatically configured for Google formatter compatibility


Bundled styles:

- Java: Export from Eclipse settings
- Eclipse: Export from Eclipse settings
- Google: https://github.com/google/styleguide/blob/gh-pages/eclipse-java-google-style.xml
- Spring: https://github.com/spring-projects/spring-framework/blob/main/src/eclipse/org.eclipse.jdt.core.prefs
- Quarkus: https://github.com/quarkusio/quarkus/blob/main/independent-projects/ide-config/src/main/resources/eclipse-format.xml
- JBang: https://github.com/jbangdev/jbang/blob/main/misc/eclipse_formatting_nowrap.xml