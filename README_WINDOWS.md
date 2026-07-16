SGV — Windows Setup and Run Instructions

Problem observed
- Terminal shows `mvn` and `java` are not recognized. The project requires a Java JDK (17+) and Maven to build and run.

Options to install (recommended: Chocolatey)

1) Install Chocolatey (if not present)
- Open PowerShell as Administrator and run:

  Set-ExecutionPolicy Bypass -Scope Process -Force; iwr https://community.chocolatey.org/install.ps1 -UseBasicParsing | iex

2) Use the helper script (requires Chocolatey)
- Open PowerShell as Administrator and run:

  .\scripts\setup-maven-java.ps1

This script will attempt to install an OpenJDK distribution (`temurin`) and `maven` via Chocolatey, then refresh environment variables.

3) Use the build-and-run wrapper
- After Java and Maven are available, you can build and run the project with:

  .\scripts\build-and-run.ps1

- To package only and stop after build:

  .\scripts\build-and-run.ps1 -PackageOnly

Note: run this from the repository root (`C:\xampp\htdocs\SGV`).

3) Manual install
- Java: download an LTS JDK (Temurin / Azul / Oracle) and install it. Make sure `java -version` works in a new terminal.
  - Temurin: https://adoptium.net/
- Maven: download binary zip from https://maven.apache.org/download.cgi, extract and add `bin` to PATH.

Verify
- In a new PowerShell window, run:

  java -version
  mvn -v

Build & run the project
- From workspace root run:

  cd java-sgv
  mvn -DskipTests package
  mvn -DskipTests spring-boot:run

Or after package:

  java -jar target/java-sgv-0.1.0.jar

Notes & permissions
- Installing via Chocolatey requires Administrator privileges.
- If you prefer not to use Chocolatey, follow the manual install links above and ensure PATH variables are set.
- If Maven complains that `JAVA_HOME` is not defined correctly, set `JAVA_HOME` to your JDK root and restart your shell.
  Example:
  ```powershell
  setx JAVA_HOME "C:\Program Files\Eclipse Adoptium\jdk-21.0.8.9-hotspot"
  ```

If you want, I can add a `README` under `java-sgv` and/or attempt to detect Java/Maven in a script and print exact instructions. What do you prefer?