Example Multi-Project C# Project
=====================================

Integrates [Dependency-Check][] analysis and reporting into SonarQube v6.7 or higher in a multi-project C# project.
 
Usage
-------------------

```bash
docker build --no-cache --build-arg sonarHost=<sonar host> -t sonar:v1 .
```

Copyright & License
-------------------

Dependency-Check Sonar Plugin is Copyright (c) SonarSecurityCommunity. All Rights Reserved.

Dependency-Check is Copyright (c) Jeremy Long. All Rights Reserved.

Permission to modify and redistribute is granted under the terms of the [LGPLv3][] license.

  [lgplv3]: http://www.gnu.org/licenses/lgpl.txt
  [dependency-check]: https://www.owasp.org/index.php/OWASP_Dependency_Check
