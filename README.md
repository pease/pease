Pease is an extension to the [Spock](http://spockframework.org/) framework
that adds support for acceptance test driven development (ATDD).

With Pease you can use [Gherkin](https://github.com/cucumber/gherkin/)
(known from [Cucumber](http://cukes.info)) to describe
how your software should behave and generate a spock test.

# Building

## Prerequisites

You need the following tools to build this project:
* Java (>= 1.6.0\_24)
* Git (>= 1.7.4)

## Submodules

Before you can begin to build this project, you first have
to initialise the git submodules. Use the following commands to get the job done:

    git submodule init
    git submodule update

These commands will fetch all required subprojects (spock).

## Gradle

You can build the whole project using Gradle and the embedded Gradle wrapper.

    ./gradlew build

# Development

To generate project configuration files for IntelliJ IDEA, you can use the Gradle task `idea`.

    ./gradlew idea

## Testing

Make sure to write or modify a test if you introduce a change and check the results with the `test` task.

    ./gradlew test

# Versioning

This project is following the [Semantic Versioning Specification](http://semver.org/) (SemVer).
Each version number is formed by X.Y.Z, where X, Y and Z are integers. X is the major version, Y is the minor version
and Z is the patch version. The major version is incremented if backwards incompatible changed are introduced.
Please note an exception to that rule in the [SemVer specification](http://semver.org/): Major version zero (0.x.y) is
for initial development and anything may change at any time.

## Public API

Each public method (Java and Groovy) is part of the public API.
Note that the public API of versions prior to `1.0.0` is considered unstable.
