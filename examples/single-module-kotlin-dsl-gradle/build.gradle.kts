import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    kotlin("jvm") version "1.3.50"
    id("org.sonarqube") version "2.8"
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
    implementation(kotlin("stdlib-jdk8"))
}

apply(plugin= "org.owasp.dependencycheck")

application {
    mainClassName = "com.example.HelloWorldKt"
}

dependencyCheck {
    format = org.owasp.dependencycheck.reporting.ReportGenerator.Format.ALL
}

sonarqube {
    properties {
        property("sonar.dependencyCheck.xmlReportPath", "build/reports/dependency-check-report.xml")
        property("sonar.dependencyCheck.htmlReportPath", "build/reports/dependency-check-report.html")
        property("sonar.sources", "src,build.gradle.kts")
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}
