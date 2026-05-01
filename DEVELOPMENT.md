# PlayStation 5 BD-JB Autoloader - Development Guide

For general usage instructions, see the main [README.md](README.md).

## Prerequisites
* JDK 11 (PS5 uses Java 11 runtime)
* Apache Maven
* IntelliJ IDEA Community Edition (optional, but recommended)

## Structure
The project comprises the following components:
* Root `pom.xml` defines the common properties and Maven plugin configuration.
* `assembly` subproject creates the directory that should be burned to a BD-R disc. Use `ImgBurn` with the UDF 2.50 filesystem.
* `bdj-tools` subproject contains utilities from HD Cookbook, integrated into the build process.
* `stubs` subproject contains BD-J class files and PS5-specific stubs.
* `sdk` subproject contains helper classes for native invocation, embedded in the final payloads.
* `xlet` subproject contains the main entry point (`LoaderXlet`). It initializes the UI and starts the `SequentialJarLoader`.
* `xploit` subproject contains the payloads bundled with the disc:
    * `bdjb-autoloader` - The core logic that scans for `autoload.txt` and executes payloads.
    * `umtx` - Kernel exploit implementation.
    * `jar` - Utility classes for JAR payload execution.

## Build Instructions

The recommended way to build the project is using Docker, as it ensures a consistent build environment (JDK 11 and Maven) and automatically handles the signing process.

### Recommended Method (Docker)
1. Ensure you have Docker and Docker Compose installed.
2. Run the build script:
   ```bash
   ./build_iso.sh
   ```
3. Upon success, the `ps5-bdjb-autoloader.iso` will be generated in the root directory.

### Manual Method
1. Make sure environment variable `JAVA_HOME` points to the root of JDK 11.
2. Execute `mvn clean package` from the root of the project.
3. The result will be in `assembly/target/assembly-[version]`. This directory contains the filesystem layout for the BD-R disc.

## Notes
1. **IntelliJ Setup**: Open the project as a Maven project. If you encounter issues with BD-J classes, ensure that `bdj-api`, `javatv-api`, and `gem-api` have "Provided" scope in the project structure.
2. **Sequential Loading**: The `LoaderXlet` is hardcoded to run a specific sequence (UMTX followed by the Autoloader). Any changes to this boot order must be made in `LoaderXlet.java`.
3. **Internal vs USB**: The autoloader logic in `xploit/bdjb-autoloader` is responsible for scanning the paths defined in `README.md`.
