#!/bin/bash

# Place build-specific tasks here - anything that can be built from the command line.
# i.e. Java, .NET, C/C++, Node.js, etc


# Invoke Dependency-Check
# dependency-check should be in your path
# ./lib is assumed to be a directory of third-party components to be scanned
# ./reports is the directory where the Dependency-Check report will be written
dependency-check.sh --format XML --scan lib --out reports/dependency-check --project "Command-line example"

# Invoke Sonar-Runner. This reads from sonar-project.properties
sonar-runner


