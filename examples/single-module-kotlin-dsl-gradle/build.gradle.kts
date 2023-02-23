import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    kotlin("jvm") version "1.3.50"
    id("org.sonarqube") version "3.3"
    id("org.owasp.dependencycheck") version "8.0.2"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.guava:guava:21.0")
    implementation("org.owasp:dependency-check-gradle:5.3.0")
    implementation("org.springframework:spring:2.0")
    testImplementation("junit:junit:4.12")
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
        property("sonar.dependencyCheck.jsonReportPath", "build/reports/dependency-check-report.json")
        property("sonar.dependencyCheck.htmlReportPath", "build/reports/dependency-check-report.html")
        property("sonar.sources", "src,build.gradle.kts")
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}
