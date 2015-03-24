[![Build Status](https://travis-ci.org/stevespringett/dependency-check-sonar-plugin.svg?branch=master)](https://travis-ci.org/stevespringett/dependency-check-sonar-plugin)

Dependency-Check Plugin for SonarQube
================================

Integrates Dependency-Check reports into SonarQube. This is a work-in-progress,
but unfortunately, cannot be completed due to strict design decisions made in
SonarQube 4.2.

It is not currently possible to index third-party components if those components
are not source files (i.e. DLL's, JAR's, etc). Because third-party components cannot
be indexed, it is not currently possible to complete this plugin.

Changes have been made in the upcoming SonarQube 5.1 release to allow for this 
functionality. Work on the plugin will continue when an RC is available.

https://jira.codehaus.org/browse/SONAR-5077

Usage
-

> $ mvn clean package

Finally, deploy the resulting jar to your SonarQube extensions->plugins directory

Copyright & License
-

Dependency-Check Sonar Plugin is Copyright (c) Steve Springett. All Rights Reserved.

Permission to modify and redistribute is granted under the terms of the [LGPLv3] license.

  [LGPLv3]: http://www.gnu.org/licenses/lgpl.txt
