# Change Log

## [1.2.6](https://github.com/dependency-check/dependency-check-sonar-plugin/tree/1.2.6) (2019-10-09)
[All Commits](https://github.com/dependency-check/dependency-check-sonar-plugin/compare/1.2.5...1.2.6)

-   **misc** - Style Changes ([#177](https://github.com/dependency-check/dependency-check-sonar-plugin/pull/177), [#176](https://github.com/dependency-check/dependency-check-sonar-plugin/pull/176))
-   **new** - Correct Metric Direction of `Vulnerable Component Ratio` and `Inherited Risk Score`([#180](https://github.com/dependency-check/dependency-check-sonar-plugin/pull/180))

## [1.2.5](https://github.com/dependency-check/dependency-check-sonar-plugin/tree/1.2.5) (2019-08-01)
[All Commits](https://github.com/dependency-check/dependency-check-sonar-plugin/compare/1.2.4...1.2.5)

-   **new** - Add skip feature ([#157](https://github.com/dependency-check/dependency-check-sonar-plugin/pull/157))

## [1.2.4](https://github.com/dependency-check/dependency-check-sonar-plugin/tree/1.2.4) (2019-06-04)
[All Commits](https://github.com/dependency-check/dependency-check-sonar-plugin/compare/1.2.3...1.2.4)

-   **testing** - Switch to openjdk8 and openjdk11 for travis tests ([#153](https://github.com/dependency-check/dependency-check-sonar-plugin/pull/153))
-   **misc** - Add compatibility table ([#151](https://github.com/dependency-check/dependency-check-sonar-plugin/pull/151) by [Mobrockers](https://github.com/Mobrockers))
-   **misc** - Use interfaces instead of DefaultIssueLocation ([#150](https://github.com/dependency-check/dependency-check-sonar-plugin/pull/150/files))
-   **examples** - Correct Copyright in example ([#148](https://github.com/dependency-check/dependency-check-sonar-plugin/pull/148))
-   **examples** - Add nodejs examples ([#145](https://github.com/dependency-check/dependency-check-sonar-plugin/pull/145) by [thib3113](https://github.com/thib3113))
-   **parser,new** - Parse reports without score and severity + calculate score from severity ([#144](https://github.com/dependency-check/dependency-check-sonar-plugin/pull/144))
-   **parser,new** - Read vulnerability source ([#138](https://github.com/dependency-check/dependency-check-sonar-plugin/pull/138))
-   **testing** - Add test data for node.Js report ([#136](https://github.com/dependency-check/dependency-check-sonar-plugin/pull/136) by [thib3113](https://github.com/thib3113))

## [1.2.3](https://github.com/dependency-check/dependency-check-sonar-plugin/tree/1.2.3) (2019-04-20)
[All Commits](https://github.com/dependency-check/dependency-check-sonar-plugin/compare/1.2.2...1.2.3)

-   **new** - Support dependency-check 5.0 ([#131](https://github.com/dependency-check/dependency-check-sonar-plugin/pull/131))
-   **testing** - Update test libaries ([#130](https://github.com/dependency-check/dependency-check-sonar-plugin/pull/130))
-   **parser** - Be more null safe ([#129](https://github.com/dependency-check/dependency-check-sonar-plugin/pull/129))
-   **misc** - Update license to 2019 ([#127](https://github.com/dependency-check/dependency-check-sonar-plugin/pull/127))

## [1.2.2](https://github.com/dependency-check/dependency-check-sonar-plugin/tree/1.2.2) (2019-04-07)
[All Commits](https://github.com/dependency-check/dependency-check-sonar-plugin/compare/1.2.1...1.2.2)

-   **new, BREAKING** - Go on search for dependencies in project configuration files ([#99](https://github.com/dependency-check/dependency-check-sonar-plugin/pull/99))
-   **misc** - Improve release process ([#125](https://github.com/dependency-check/dependency-check-sonar-plugin/pull/125))
-   **new** - Use measurecomputer ([#124](https://github.com/dependency-check/dependency-check-sonar-plugin/pull/124) by [reallyinsane](https://github.com/reallyinsane))
-   **misc** - Change project namespace ([#121](https://github.com/dependency-check/dependency-check-sonar-plugin/pull/121))
-   **examples** - Update examples ([#119](https://github.com/dependency-check/dependency-check-sonar-plugin/pull/119/files))
-   **examples** - Rename example project ([#118](https://github.com/dependency-check/dependency-check-sonar-plugin/pull/118))
-   **testing** - Remove old libaries ([#115](https://github.com/dependency-check/dependency-check-sonar-plugin/pull/115))
-   **new** - Add Confidence and Identifier in xml-parser ([#113](https://github.com/dependency-check/dependency-check-sonar-plugin/pull/113))
-   **examples** - Update dependency-check to 4.0.2 ([#110](https://github.com/dependency-check/dependency-check-sonar-plugin/pull/110))
-   **misc** - Some small improvements ([#96](https://github.com/dependency-check/dependency-check-sonar-plugin/pull/96))
-   **new** - Count infoIssues ([#95](https://github.com/dependency-check/dependency-check-sonar-plugin/pull/95))

## [1.2.1](https://github.com/dependency-check/dependency-check-sonar-plugin/tree/1.2.1) (2018-12-15)
[All Commits](https://github.com/dependency-check/dependency-check-sonar-plugin/compare/1.2.0...1.2.1)

-   **examples** - Add gradle example
-   **logging** - Improve logging
-   **testing** - Add jacoco for code coverage
-   **misc** - Rewrite Report loading
-   **new** - summarize vulnerabilities feature ([#92](https://github.com/dependency-check/dependency-check-sonar-plugin/pull/92/files))

## [1.2.0](https://github.com/dependency-check/dependency-check-sonar-plugin/tree/1.2.0) (2018-11-07)
[All Commits](https://github.com/dependency-check/dependency-check-sonar-plugin/compare/1.1.1...1.2.0)

-   **new, BREAKING** - Add Sonarqube 7 Support ([#79](https://github.com/dependency-check/dependency-check-sonar-plugin/pull/79))
-   **new** - Make severity "minor" configurable ([#77](https://github.com/dependency-check/dependency-check-sonar-plugin/pull/77) by [fr33ky](https://github.com/fr33ky))
-   **bugfix** - show HTML-Reports in Submodules ([81](https://github.com/dependency-check/dependency-check-sonar-plugin/pull/81))
-   **new** - add OwaspTop10.A9 , cwe 937 ([#84](https://github.com/dependency-check/dependency-check-sonar-plugin/pull/84))
-   **new** - Add blocker feature (based on [#63](https://github.com/dependency-check/dependency-check-sonar-plugin/pull/63) by [walterdeboer]( https://github.com/walterdeboer))

## [1.1.1](https://github.com/dependency-check/dependency-check-sonar-plugin/tree/1.1.1) (2018-08-17)
[All Commits](https://github.com/dependency-check/dependency-check-sonar-plugin/compare/1.1.0...1.1.1)

-   **new** - Link issues to report files ([#61](https://github.com/dependency-check/dependency-check-sonar-plugin/pull/61))
-   **new** - Make severities configurable ([#48](https://github.com/dependency-check/dependency-check-sonar-plugin/pull/48) by [ttsiebzehntt](https://github.com/ttsiebzehntt))
-   **misc** - Improve Embedded Report ([#43](https://github.com/dependency-check/dependency-check-sonar-plugin/pull/43) by [NickHarvey2](https://github.com/NickHarvey2))

## [1.1.0 and earlier](https://github.com/dependency-check/dependency-check-sonar-plugin/tree/1.1.0)

-   **new** - Add HTML-Report([#40](https://github.com/dependency-check/dependency-check-sonar-plugin/pull/40) by [NickHarvey2](https://github.com/NickHarvey2))
-   **misc** - fix sonar issues and improved test coverage ([#36](https://github.com/dependency-check/dependency-check-sonar-plugin/pull/36) by [gtudan](https://github.com/gtudan))
-   **misc** - Skip analysis if report does not exist ([#35](https://github.com/dependency-check/dependency-check-sonar-plugin/pull/35) by [gtudan](https://github.com/gtudan))
-   **new** - Sonarqube 6.x compatibility ([#28](https://github.com/dependency-check/dependency-check-sonar-plugin/pull/28) by [gtudan](https://github.com/gtudan))
