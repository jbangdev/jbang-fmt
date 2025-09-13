# JBang Java Formatter (`javafmt`)

> **⚠️ Experimental:**  
> This formatter is experimental and under active development.  
> Please report issues or suggestions on [GitHub](https://github.com/jbangdev/jbang-fmt).


This project provides a command-line tool for formatting Java source files, with special support for [JBang](https://www.jbang.dev/) scripts and common Eclipse formatter styles.

## Why does this exist?

Most Java code formatters (including Eclipse, Google Java Format, etc.) do not handle JBang script files well. JBang scripts often start with special comment-based directives (like `//DEPS`, `//JAVA`, etc.) that are standard Java syntax, but standard formatters may reformat, move, or even delete these directives, breaking the script or making it unreadable.

This tool was created to solve that problem: it **formats Java code while leaving JBang directives untouched**. It is especially useful for developers who want to keep their JBang scripts clean and consistently formatted, without risking the loss or corruption of important script metadata. 

## Features

- **JBang-friendly formatting:** By default, the tool detects and protects JBang directives, only formatting the actual true Java code and comments.
- **Eclipse formatter support:** Uses the Eclipse Java code formatter under the hood, with the ability to load custom Eclipse `.xml` or `.prefs` style settings.
- **Easy to use:** Simple CLI interface, works with files and directories, and can be run via [JBang](https://www.jbang.dev/).
- **Customizable:** Supports toggling JBang-friendly mode and specifying custom formatter settings.

## Usage

You can install (and run) the tool using JBang:

```bash
jbang app install javafmt@jbangdev/jbang-fmt
```

### Basic Examples

**Format a single Java file:**
```bash
javafmt MyFile.java
```

**Format all Java files in a directory:**
```bash
javafmt src/
```

**Format multiple files and directories:**
```bash
javafmt MyFile.java src/ tests/
```

### Using Different Formatter Styles

**Use Google Java Format style:**
```bash
javafmt --settings google MyFile.java
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
javafmt --settings /path/to/my-formatter.xml MyFile.java
```

you can also use JBang magic url fetching for arguments



### JBang-Friendly Mode (Default)

**Disable JBang-friendly mode (format everything including directives):**

```bash
jbang src/dev/jbang/fmt/fmt.java --no-jbang-friendly MyFile.java
```

TODO:

document how you can use jbang plugin in maven/gradle to run formatting

