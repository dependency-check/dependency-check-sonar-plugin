# Example Single Module Nodejs Project

Integrates [Dependency-Check][] analysis and reporting into SonarQube v6.7 or higher in a single module (flat) Nodejs project.

## Pre requirements

-   [npm][]
-   [dependency-check-cli][]
-   [sonar-runner][]

## Usage

```bash
# Install dependencies
npm install
# Create Folder for reports
mkdir -p reports/dependency-check
# Run Dependency-Check
dependency-check.sh --format ALL -s . --out reports/dependency-check --project "nodejs example"
# Invoke Sonar-Runner. This reads from sonar-project.properties
sonar-runner
```

## Dependencies with vulnerabilities

-   braces@2.3.0 => vulnerability low from NPM
-   lodash@4.17.10 => vulnerability moderate from NPM
-   open@0.0.5 => vulnerability critical from NPM
-   tar@4.4.0 => vulnerability high from NPM
-   jquery@2.2.0 => vulnerabilities moderate and high from NPM AND vulnerabilities MEDIUM (multiple times) from NVD

## Copyright & License

Dependency-Check Sonar Plugin is Copyright (c) dependency-check. All Rights Reserved.

Dependency-Check is Copyright (c) Jeremy Long. All Rights Reserved.

Permission to modify and redistribute is granted under the terms of the [LGPLv3][] license.

  [lgplv3]: http://www.gnu.org/licenses/lgpl.txt
  [dependency-check]: https://www.owasp.org/index.php/OWASP_Dependency_Check
  [npm]: https://www.npmjs.com/get-npm
  [dependency-check-cli]: https://jeremylong.github.io/DependencyCheck/dependency-check-cli/index.html
  [sonar-runner]: https://docs.sonarqube.org/display/SCAN/Analyzing+with+SonarQube+Scanner
