# JBang Java Formatter (`jbang-fmt`)

> **⚠️ Experimental:**  
> This formatter is experimental and under active development.  
> Please report issues or suggestions on [GitHub](https://github.com/jbangdev/jbang-fmt).


This project provides a command-line tool for formatting Java source files, with special support for [JBang](https://www.jbang.dev/) scripts and common Eclipse formatter styles.

## Why does this exist?

Most Java code formatters (including Eclipse, Google Java Format, etc.) do not handle JBang script files well. JBang scripts often start with special comment-based directives (like `//DEPS`, `//JAVA`, etc.) that are standard Java syntax, but standard formatters may reformat, move, or even delete these directives, breaking the script or making it unreadable.

This tool was created to solve that problem: it **formats Java code while leaving JBang directives untouched**. It is especially useful for developers who want to keep their JBang scripts clean and consistently formatted, without risking the loss or corruption of important script metadata. 

## Features

- **Works 100% with Java while having JBang-friendly formatting:** By default, the tool detects and protects JBang directives, only formatting the actual true Java code and comments.
- **Check mode for CI/commit hooks:** Use `--check` to check if files would be formatted. Exit with 1 if any files would change.
- **Stdout output:** Use `--stdout` to print formatted content to stdout instead of modifying files.
- **Detailed statistics:** Shows processing time and file counts (processed, changed, clean, skipped).
- **Eclipse formatter support:** Uses the Eclipse Java code formatter under the hood, with the ability to load custom Eclipse `.xml` or `.prefs` style settings.
- **Easy to use:** Simple CLI interface, works with files and directories, and can be run via [JBang](https://www.jbang.dev/).
- **Customizable:** Supports toggling JBang-friendly mode and specifying custom formatter settings.

## Usage

You can install (and run) the tool using JBang:

```bash
jbang app install jbang-fmt@jbangdev/jbang-fmt
```

### Basic Examples

**Format a single Java file:**
```bash
jbang-fmt MyFile.java
```

**Format all Java files in a directory:**
```bash
jbang-fmt src/
```

**Format multiple files and directories:**
```bash
jbang-fmt MyFile.java src/ tests/
```

### Check Mode and CI Integration

**Check if files would be formatted (perfect for CI):**
```bash
jbang-fmt --check MyFile.java
```

**Check multiple files and directories:**
```bash
jbang-fmt --check src/ tests/
```

The `--check` flag will:
- Show which files would be changed
- Exit with code 1 if any files would change
- Exit with code 0 if no changes are needed
- Display timing and file statistics

### Output to Stdout

**Print formatted content to stdout instead of modifying files:**
```bash
jbang-fmt --stdout MyFile.java
```

This is useful for:
- Piping formatted content to other tools
- Previewing changes before applying them
- Integration with other build tools

### Using Different Formatter Styles

**Use Google Java Format style:**
```bash
jbang-fmt --settings google MyFile.java
```

The following styles are default bundled:

- `eclipse` &mdash; Eclipse default Java formatter (Eclipse IDE style)
- `google` &mdash; Google Java Style Guide
- `java` &mdash; Java community style (OpenJDK-inspired)
- `jbang` &mdash; JBang's recommended style
- `quarkus` &mdash; Quarkus project style
- `spring` &mdash; Spring Framework style


**Use custom Eclipse settings file:**
```bash
jbang-fmt --settings /path/to/my-formatter.xml MyFile.java
```

you can also use JBang magic url fetching for arguments



### JBang-Friendly Mode (Default)

**Disable JBang-friendly mode (format everything including directives):**

```bash
jbang-fmt --no-jbang-friendly MyFile.java
```

## Output Format

The tool provides detailed feedback about the formatting process:

**Normal mode output:**
```
Formatting with default[0 properties, jbang-friendly=false]...
MyFile.java
Formatted 3 files (1 changed, 2 clean, 0 skipped) in 0.2s
```

**Check mode output:**
```
Formatting with default[0 properties, jbang-friendly=false]...
MyFile.java
Would reformat 1 files (out of 3) in 0.2s. Run without --check to apply.
```

The statistics show:
- **Total files processed:** All Java files that were examined
- **Changed:** Files that were modified by the formatter
- **Clean:** Files that were already properly formatted
- **Skipped:** Non-Java files that were ignored
- **Processing time:** How long the formatting took

## TODO

- Document how you can use jbang plugin in maven/gradle to run formatting

