Dependency-Check Plugin for SonarQube
================================

Integrates Dependency-Check reports into SonarQube. This is a work-in-progress,
but unfortunately, cannot be completed due to strict design decisions made in
SonarQube 4.2.

It is not currently possible to index third-party components if those components
are not source files (i.e. DLL's, JAR's, etc). Because third-party components cannot
be indexed, it is not currently possible to complete this plugin.

http://marc.info/?l=sonar-dev&m=142107878432267&w=2

Please vote to have binary files indexed to make plugins like Dependency-Check possible:

https://jira.codehaus.org/browse/SONAR-5077
