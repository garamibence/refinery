# Refinery

[![Build](https://github.com/graphs4value/refinery/actions/workflows/build.yml/badge.svg)](https://github.com/graphs4value/refinery/actions/workflows/build.yml) [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=graphs4value_refinery&metric=alert_status)](https://sonarcloud.io/dashboard?id=graphs4value_refinery) [![Coverage](https://sonarcloud.io/api/project_badges/measure?project=graphs4value_refinery&metric=coverage)](https://sonarcloud.io/dashboard?id=graphs4value_refinery)

## How to contribute

### With Eclipse IDE

1. Download and install a _Java 17_ compatible JDK. For Windows, prefer OpenJDK builds from [Adoptium](https://adoptium.net/).

2. Download and extract the [Eclipse IDE for Java and DSL Developers 2021-09](https://www.eclipse.org/downloads/packages/release/2021-09/r/eclipse-ide-java-and-dsl-developers) package.

3. Launch Eclipse and create a new workspace.

4. Open _Help > Install New Software..._ and install the following software from the _2021-09_ update site:
    * _Modeling > Ecore Diagram Editor (SDK)_

5. Open _Help > Eclipse Marketplace_ and install the following software:
    * _Java 17 Support for Eclipse 2021-09_
    * _EclEmma Java Code Coverage_
    * _SonarLint_

6. Open _Window > Preferences_ and set the following preferences:
    * _General > Workspace > Text file encoding_ should be _UTF-8_.
    * _General > Workspace > New text file line delimiter_ should be _Unix_.
    * Add the JDK 17 to _Java > Installed JREs_.
    * Make sure JDK 17 is selected for _JavaSE-17_ at _Java > Installed JREs > Execution Environments_.
    * Set _Gradle > Java home_ to the `JAVA_HOME` directory (the directory which contains the `bin` directory) of JDK 17. Here, Buildship will show a yellow warning sign, which can be safely ignored.
    * Set _Java > Compiler > JDK Compliance > Compiler compliance level_ to _17_. The warning about using Java 16 system libraries during compilation should disappear.
	  
7. Clone the project Git repository but do not import it into Eclipse yet.

8. Open a new terminal an run `./gradlew prepareEclipse` (`.\gradlew prepareEclipse` on Windows) in the cloned repository.
    * This should complete without any compilation errors.
    * If you get any errors about the JVM version, check whether the `JAVA_HOME` environment variable is set to the location of JDK. You can query the variable with `echo $JAVA_HOME` on Linux and `echo $Env:JAVA_HOME` in PowerShell on Windows. To set it, use `export JAVA_HOME=/java/path/here` or `$Env:JAVA_HOME="C:\java\path\here"`, respectively.

9. Select _File > Import... > Gradle > Existing Gradle Project_ and import the cloned repository in Eclipse.
    * Make sure to select the root of the repository (containing this file) as the _Project root directory_ and that the _Gradle distribution_ is _Gradle wrapper_.
    * If you have previously imported the project into Eclipse, this step will likely fail. In that case, you should remove the projects from Eclipse, run `git clean -fxd` in the repository, and start over from step 8. 

### With IntelliJ IDEA

It is possible to import the project into IntelliJ IDEA, but it gives no editing help for Xtext (`*.xtext`), MWE2 (`*.mwe2`), and Xtend (`*.xtend`) and Ecore class diagrams (`*.aird`, `*.ecore`, `*.genmodel`). 

## License

All code in this repository is available under the [Eclipse Public License - v 2.0](https://www.eclipse.org/legal/epl-2.0/).
