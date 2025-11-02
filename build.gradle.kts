plugins {
    java
    id("checkstyle")
    id("com.diffplug.spotless") version "6.25.0"
    id("com.github.spotbugs") version "6.2.4"
}

group = "org.example"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.mindrot:jbcrypt:0.4")
}

/** Checkstyle */
tasks.withType<Checkstyle> {
    reports {
        xml.required.set(false)
        html.required.set(true)
        html.outputLocation.set(file("$buildDir/reports/checkstyle/${name}.html"))
    }
}
checkstyle {
    toolVersion = "10.17.0"
    config = resources.text.fromFile("config/checkstyle/google_checks.xml")
    isShowViolations = true
    maxWarnings = 0
}

/** SpotBugs */
spotbugs {
    ignoreFailures = false // падать при найденных проблемах
}
tasks.spotbugsMain {
    reports.create("html") {
        required = true
        outputLocation = file("$buildDir/reports/spotbugs/main.html")
    }
}
tasks.spotbugsTest {
    reports.create("html") {
        required = true
        outputLocation = file("$buildDir/reports/spotbugs/test.html")
    }
}

/** Spotless */
spotless {
    java {
        googleJavaFormat("1.22.0")
        target("src/**/*.java")
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

/** All checks build task */
tasks.named("check") {
    dependsOn("spotlessCheck", "spotbugsMain", "spotbugsTest")
}

tasks.test {
    useJUnitPlatform()
}
