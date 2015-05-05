[![Build Status](https://travis-ci.org/stevespringett/dependency-check-sonar-plugin.svg?branch=master)](https://travis-ci.org/stevespringett/dependency-check-sonar-plugin)

Dependency-Check Plugin for SonarQube
================================

Integrates Dependency-Check reports into SonarQube v5.1 or higher. If components being 
analyzed by Dependency-Check are non-source files (jar, dll, etc), then the value of 
sonar.import_unknown_files needs to be set to True in the SonarQube configuration.

Usage
-

> $ mvn clean package

Finally, deploy the resulting jar to your SonarQube extensions->plugins directory

Copyright & License
-

Dependency-Check Sonar Plugin is Copyright (c) Steve Springett. All Rights Reserved.

Permission to modify and redistribute is granted under the terms of the [LGPLv3] license.

  [LGPLv3]: http://www.gnu.org/licenses/lgpl.txt
