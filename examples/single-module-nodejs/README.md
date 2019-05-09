
Example Single Module Nodejs Project
=====================================

Integrates [Dependency-Check] analysis and reporting into SonarQube v6.7 or higher in a single module (flat) Nodejs project.

Usage
-------------------
Install dependencies
```
npm install
```

if you want to use it fastly :
Use the `command-line-project` :
```
mkdir ../command-line-project/lib
cp * ../command-line-project/lib
cd ../command-line-project/lib
npm install
cd ..
./build.sh
```

Dependencies with vulnerabilities
-------------------

braces@2.3.0 => vulnerability low from NPM
lodash@4.17.10 => vulnerability moderate from NPM
open@0.0.5 => vulnerability critical from NPM
tar@4.4.0 => vulnerability high from NPM
jquery@2.2.0 => vulnerabilities moderate and high from NPM AND vulnerabilities MEDIUM (multiple times) from NVD


Copyright & License
-------------------

Dependency-Check Sonar Plugin is Copyright (c) Steve Springett. All Rights Reserved.

Dependency-Check is Copyright (c) Jeremy Long. All Rights Reserved.

Permission to modify and redistribute is granted under the terms of the [LGPLv3] license.

  [LGPLv3]: http://www.gnu.org/licenses/lgpl.txt
  [Dependency-Check]: https://www.owasp.org/index.php/OWASP_Dependency_Check
