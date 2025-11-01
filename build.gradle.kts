import com.github.spotbugs.snom.SpotBugsTask

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

// (Опционально) базовые настройки SpotBugs
spotbugs {
    ignoreFailures = false // билд падает при найденных проблемах
}

// HTML-отчёты для задач spotbugsMain / spotbugsTest
tasks.spotbugsMain {
    reports.create("html") {
        required = true
        outputLocation = file("$buildDir/reports/spotbugs/main.html")
        // setStylesheet("fancy-hist.xsl") // можно подключить стиль при желании
    }
}
tasks.spotbugsTest {
    reports.create("html") {
        required = true
        outputLocation = file("$buildDir/reports/spotbugs/test.html")
    }
}

// чтобы общий gradle check автоматически гонял SpotBugs тоже
tasks.named("check") {
    dependsOn("spotbugsMain", "spotbugsTest")
}


// Чтобы gradle check запускал SpotBugs автоматически
tasks.named("check") {
    dependsOn("spotbugsMain", "spotbugsTest")
}

// Чтобы gradle check запускал SpotBugs автоматически
tasks.named("check") {
    dependsOn("spotbugsMain", "spotbugsTest")
}


spotless {
    java {
        googleJavaFormat("1.22.0")
        target("src/**/*.java")
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

tasks.named("check") {
    dependsOn("spotlessCheck")
}

tasks.test {
    useJUnitPlatform()
}