plugins {
    java
    application
    id("org.sonarqube") version "2.6.2"
    id("org.owasp.dependencycheck") version "5.2.2"
}

repositories {
    mavenCentral()
}

dependencies {
    compile("com.google.guava:guava:21.0")
    compile("org.owasp:dependency-check-gradle:5.2.2")
    compile("org.springframework:spring:2.0")
    testCompile("junit:junit:4.12")
}

apply(plugin= "org.owasp.dependencycheck")

application {
    mainClassName = "com.example.HelloWorld"
}

dependencyCheck {
    format = org.owasp.dependencycheck.reporting.ReportGenerator.Format.ALL
}

sonarqube {
    properties {
        property("sonar.dependencyCheck.reportPath", "build/reports/dependency-check-report.xml")
        property("sonar.dependencyCheck.htmlReportPath", "build/reports/dependency-check-report.html")
        property("sonar.sources", "${properties["sonar.sources"].toString()},build.gradle.kts")
    }
}
