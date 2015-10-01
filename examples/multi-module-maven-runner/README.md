Example Multi-Module Maven Project
=====================================

Integrates [OWASP Dependency-Check] analysis and reporting into SonarQube v5.1 or higher in a multi-module Maven project.
 
Note: The Sonar Maven plugin is not used in this project. It appears there may be issues when using it along with 
the Dependency-Check SonarQube plugin. Therefore, this project demonstrates the scanning of a multi-module Maven 
project and uses Sonar Runner to perform the analysis. This will be a typical usage scenario in a Continuous Integration
environment anyway. I'd welcome any input into why the Sonar Maven plugin does not work.

Usage
-------------------

```
mvn clean dependency-check:check
sonar-runner
```

Copyright & License
-------------------

OWASP Dependency-Check Sonar Plugin is Copyright (c) Steve Springett. All Rights Reserved.

OWASP Dependency-Check is Copyright (c) Jeremy Long. All Rights Reserved.

Permission to modify and redistribute is granted under the terms of the [LGPLv3] license.

  [LGPLv3]: http://www.gnu.org/licenses/lgpl.txt
  [OWASP Dependency-Check]: https://www.owasp.org/index.php/OWASP_Dependency_Check
