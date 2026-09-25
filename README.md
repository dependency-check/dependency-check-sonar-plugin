# Dependency-Check Plugin for SonarQube Server 2025.x and up

![Build Status](https://github.com/dependency-check/dependency-check-sonar-plugin/actions/workflows/testing.yml/badge.svg?branch=master)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/e9cebd3112ec4252804bba68a5b44071)](https://www.codacy.com/gh/dependency-check/dependency-check-sonar-plugin/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=dependency-check/dependency-check-sonar-plugin&amp;utm_campaign=Badge_Grade)
[![Download](https://img.shields.io/github/v/release/dependency-check/dependency-check-sonar-plugin)](https://github.com/dependency-check/dependency-check-sonar-plugin/releases/latest)
![Downloads](https://img.shields.io/github/downloads/dependency-check/dependency-check-sonar-plugin/total)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=dependency-check_dependency-check-sonar-plugin&metric=alert_status)](https://sonarcloud.io/dashboard?id=dependency-check_dependency-check-sonar-plugin)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=dependency-check_dependency-check-sonar-plugin&metric=coverage)](https://sonarcloud.io/dashboard?id=dependency-check_dependency-check-sonar-plugin)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=dependency-check_dependency-check-sonar-plugin&metric=security_rating)](https://sonarcloud.io/dashboard?id=dependency-check_dependency-check-sonar-plugin)

Integrates [Dependency-Check][] reports into SonarQube Server 2025.x and up.

## About Dependency-Check

Dependency-Check is a utility that attempts to detect publicly disclosed vulnerabilities contained within project dependencies. It does this by determining if there is a Common Platform Enumeration (CPE) identifier for a given dependency. If found, it will generate a report linking to the associated CVE entries.

Dependency-Check supports the identification of project dependencies in a number of different languages including Java, .NET, Node.js, Ruby, and Python.

## Note

**This SonarQube plugin does not perform analysis**, rather, it reads existing Dependency-Check reports. Use one of the other available methods to scan project dependencies and generate the necessary JSON report which can then be consumed by this plugin. Refer to the [Dependency-Check project](https://github.com/jeremylong/DependencyCheck) for relevant [documentation](https://jeremylong.github.io/DependencyCheck/).

## Metrics

The plugin keeps track of a number of statistics including:

-   Total number of dependencies scanned
-   Total number of vulnerabilities found across all dependencies
-   Total number of vulnerable components
-   Total number of critical, high, medium, and low severity vulnerabilities

Additionally, the following two metrics are defined:

### Inherited Risk Score (IRS)

```java
 (high * 5) + (medium * 3) + (low * 1)
```

The IRS is simply a weighted measurement of the vulnerabilities inherited by the application through the use of vulnerable components. It does not measure the applications actual risk due to those components. The higher the score the more risk the application inherits.

### Vulnerable Component Ratio

(vulnerabilities / vulnerableComponents)

This is simply a measurement of the number of vulnerabilities to the vulnerable components (as a percentage). A higher percentage indicates that a large number of components contain vulnerabilities. Lower percentages are better.

## Compiling

> $ mvn clean package

## Distribution

Ready to use binaries are available from [GitHub][].

## Plugin version compatibility

Please use the newest version. Please keep in mind that this plugin only supports the latest SonarQube LTS version, and the latest non SonarQube LTS version.

| Plugin Version | SonarQube version           |
|----------------|-----------------------------|
| 6.0.0 and up   | SonarQube 2025.x and up     |
| 5.0.0          | SonarQube 10.2 - 10.8       |
| 4.0.0 - 4.0.1  | SonarQube 9.9 LTS - 10.2    |
| 3.0.0 - 3.1.0  | SonarQube 8.9 LTS - 9.9 LTS |
| 2.0.6 - 2.0.8  | SonarQube 7.9 LTS - 8.9 LTS |
| 1.2.x - 2.0.5  | SonarQube 7.6 - 7.9 LTS     |
| 1.1.x          | SonarQube 6.7 LTS           |
| 1.0.3          | SonarQube 5.6 LTS           |

## Installation

Copy the plugin (jar file) to $SONAR_INSTALL_DIR/extensions/plugins and restart SonarQube or install via SonarQube Marketplace.

## Using

Create aggregate reports with Dependency-Check. Dependency-Check will output a file named 'dependency-check-report.json'. The Dependency-Check SonarQube plugin reads an existing Dependency-Check JSON report.

## Plugin Configuration

A typical SonarQube configuration will have the following parameter. This example assumes the use of a Jenkins workspace, but can easily be altered for other CI/CD systems.

```ini
sonar.dependencyCheck.jsonReportPath=${WORKSPACE}/dependency-check-report.json
```

Only the JSON report is required. The HTML report is optional; the Dependency-Check page of the project links to it once a report store has been configured, see [HTML report](#html-report).

This plugin tries to add SonarQube issues to your project configuration files (e.g. pom.xml, \*.gradle, package-json.lock). Please make sure, that these files are part of `sonar.sources`.

To configure the severity of the created issues you can optionally specify the minimum score for each severity with the following parameter. Specify a score of `-1` to completely disable a severity.

```ini
sonar.dependencyCheck.severity.high=7.0
sonar.dependencyCheck.severity.medium=4.0
sonar.dependencyCheck.severity.low=0.0
```

In large projects you have many dependencies with (hopefully) no vulnerabilities. The following configuration summarize all vulnerabilities of one dependency into one issue.

```ini
sonar.dependencyCheck.summarize=true
sonar.dependencyCheck.summarize=false (default)
```

If you want skip this plugin, it's possible with following configuration.

```ini
sonar.dependencyCheck.skip=true
sonar.dependencyCheck.skip=false (default)
```

If you want to work with [Security-Hotspots][Security-Hotspot] to enable a review process in your team, use the following configuration.

```ini
sonar.dependencyCheck.securityHotspot=true
sonar.dependencyCheck.securityHotspot=false (default)
```

If you want to have the complete jar file path instead of the name, use the following configuration.

```ini
sonar.dependencyCheck.useFilePath=true
sonar.dependencyCheck.useFilePath=false (default)
```

## HTML report

The Dependency-Check page of a project links to the HTML report that Dependency-Check generated;
opening the link shows the report in a new browser tab. It opens in a new tab, rather than inline
on the page, because SonarQube sends a content security policy that does not allow embedding it.
The scanner sends the report to SonarQube, which writes it into the store you configured. Only
the location is kept in the SonarQube database - earlier versions kept the whole report there and
put megabytes of HTML into it on every analysis, which is why the page was removed in version
6.0.0. The report link asks SonarQube for the report and SonarQube reads it back from the store,
so the store never has to be reachable from a browser, and store credentials stay on the
SonarQube server instead of on your CI runners.

The store is configured once for the whole SonarQube instance, in **Administration > Configuration >
Dependency-Check > HTML Report**, since only the SonarQube server reads and writes it. Without a
store nothing is published and the page stays empty.

Each analysis replaces the report of that project and branch. A user sees a report exactly when they
are allowed to see the project it belongs to, SonarQube decides that, not the store.

Publishing a report is gated on the same browse permission, not on Execute Analysis, because that is
what the SonarQube endpoint the plugin can reach actually enforces. In practice, anyone who can browse
a project can publish a report for it, replacing whatever is shown there. Treat the store as holding
project-visible content, not as a trusted, analysis-only channel.

There is one case in which that permission check cannot run: a component SonarQube does not know
yet. The scanner sends the report before the Compute Engine has created the component, so the
first analysis of a new project, a new branch or a new pull request finds nothing to authorize
against - and refusing there would mean a pull request that is analysed once and then merged never
gets a report at all. Such an upload is therefore accepted from **any authenticated caller**,
whatever project key it names. The key is still derived by the server from that component, so the
report can only land under it and never overwrite an existing project's report; and if the
component never materialises, the report is reclaimed as an orphan by the `cleanup` action
described below. On an instance where anyone can obtain a token, this is a way to put a file of up
to 100 MB into the store under a project key that does not exist.

Upgrading from a version before 6.0.0 leaves the measures of the removed `report` metric behind
in the database. They are no longer read by anything; removing them is a manual cleanup.

Reports are not removed by deleting the branch, pull request or project they belong to - the
plugin API has no hook for that. Instead, each analysis of a project prunes that project's own
store entries for branches and pull requests SonarQube no longer knows about, so a removed branch
or pull request is cleaned up at the next analysis of the same project. A removed project is
different: nothing analyses it again to trigger that cleanup, so an administrator reconciles the
whole store with `POST api/dependencycheck/cleanup`. It defaults to a dry run, which answers with
the keys it would delete under `wouldDelete` without touching anything; pass `dryRun=false` to
actually delete, and the keys that really were removed come back under `deleted`. The action
requires global administration rights.

The cleanup deletes nothing it is not sure about:

- Only keys the plugin itself writes are considered - three segments, the fixed report file name,
  and a scope that is `default` or carries the branch or pull request prefix. Any other file in
  the store directory is skipped, never deleted.
- A project counts as removed only when the administration-scoped `api/projects/search` answers
  with a `components` array that does not list it. A failed, unparsable or paged answer from that
  search or from the branch and pull request listings skips the project entirely.
- The `default` scope, written by an analysis of the main branch that names neither a branch nor a
  pull request, is always live and is never pruned.
- Each project is reconciled on its own, so a failure on one does not abort the run: it is
  reported under `failedProjects`, individual reports that could not be deleted under `failed`,
  and the summary is returned in every case.

### Storing the reports in a directory

For a directory that the SonarQube server can read and write, for example a local disk or a volume
mounted into the SonarQube server:

```ini
sonar.dependencyCheck.htmlReport.store=filesystem
sonar.dependencyCheck.htmlReport.filesystem.path=/var/sonarqube/dependency-check-reports
```

On a clustered SonarQube the directory has to be a volume that **every** server node can read and
write, an NFS or a shared block volume for example. A node local disk fails silently: the node that
received the upload has the report, every other node answers 404 for it, so the page works or not
depending on which node served the request.

### Storing the reports in S3

```ini
sonar.dependencyCheck.htmlReport.store=s3
sonar.dependencyCheck.htmlReport.s3.bucket=my-reports
sonar.dependencyCheck.htmlReport.s3.region=eu-central-1
sonar.dependencyCheck.htmlReport.s3.prefix=dependency-check (default)
```

Credentials are taken from the properties `sonar.dependencyCheck.htmlReport.s3.accessKeyId`,
`sonar.dependencyCheck.htmlReport.s3.secretAccessKey.secured` and, for temporary credentials,
`sonar.dependencyCheck.htmlReport.s3.sessionToken.secured`. These are read only by the SonarQube
server, never by the scanner. Leave them empty to use the default AWS credential chain of the
SonarQube server, for example an IAM role attached to it.

The two credential properties end in `.secured` so that SonarQube never hands them out to a
scanner.

The SonarQube server needs `s3:PutObject` and `s3:GetObject` on the prefix, plus `s3:ListBucket` on
the bucket and `s3:DeleteObject` on the prefix - the reconciliation lists the stored reports and
deletes the ones whose project, branch or pull request is gone. The scanner does not need any S3
permissions, it only sends the report to SonarQube.

For S3 compatible servers such as MinIO or Ceph, point the plugin at their endpoint and address the
bucket as a path:

```ini
sonar.dependencyCheck.htmlReport.store=s3
sonar.dependencyCheck.htmlReport.s3.endpoint=https://minio.example.com:9000
sonar.dependencyCheck.htmlReport.s3.pathStyleAccess=true
sonar.dependencyCheck.htmlReport.s3.bucket=my-reports
```

## Ecosystem

Dependency-Check is available as a:

-   Command-line utility
-   Ant Task
-   Gradle Plugin
-   Jenkins Plugin
-   Maven Plugin
-   SonarQube Plugin

## Copyright & License

Dependency-Check Sonar Plugin is Copyright (c) dependency-check. All Rights Reserved.

Dependency-Check is Copyright (c) Jeremy Long. All Rights Reserved.

Permission to modify and redistribute is granted under the terms of the [LGPLv3][] license.

[lgplv3]: http://www.gnu.org/licenses/lgpl.txt
[github]: https://github.com/dependency-check/dependency-check-sonar-plugin/releases
[dependency-check]: https://www.owasp.org/index.php/OWASP_Dependency_Check
[security-hotspot]: https://docs.sonarsource.com/sonarqube-server/latest/user-guide/security-hotspots/
