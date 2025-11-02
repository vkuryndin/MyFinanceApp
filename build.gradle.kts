
plugins {
    java
    id("checkstyle")
    id("com.diffplug.spotless") version "6.25.0"
    id("com.github.spotbugs") version "6.2.4"
    jacoco
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
    //testImplementation(platform("org.junit:junit-bom:5.10.0"))
    //testImplementation("org.junit.jupiter:junit-jupiter")
    //testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.mindrot:jbcrypt:0.4")
    //testImplementation("org.mockito:mockito-core:5.14.2")
    //testImplementation("org.mockito:mockito-junit-jupiter:5.14.2")
}
/** Testing, Gradle 9 compatible */
testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter() // <-- явный выбор фреймворка

            dependencies {
                implementation(platform("org.junit:junit-bom:5.10.0"))
                implementation("org.junit.jupiter:junit-jupiter")
                runtimeOnly("org.junit.platform:junit-platform-launcher")

                implementation("org.mockito:mockito-core:5.14.2")
                implementation("org.mockito:mockito-junit-jupiter:5.14.2")
            }
        }
    }
}
tasks.test {
    // чтобы все дочерние JVM писали в тот же файл покрытия
    //extensions.configure<JacocoTaskExtension> {
    //    isAppend = true
    //}
    doFirst {
        // передадим тестам путь к агенту и итоговому .exec
        val agentJar = configurations.getByName("jacocoAgent").singleFile.absolutePath
        val dest = extensions.getByType(JacocoTaskExtension::class).destinationFile!!.absolutePath
        systemProperty("jacoco.agent.path", agentJar)
        systemProperty("jacoco.agent.destfile", dest)
    }
}

// если используешь Jacoco:
tasks.named("test") {
    // для совместимости с IDE: useJUnitPlatform() не обязателен при suites, но не мешает
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

/** Checkstyle */
tasks.withType<Checkstyle> {
    reports {
        xml.required.set(false)
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.file("reports/checkstyle/${name}.html"))
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
        outputLocation = layout.buildDirectory.file("reports/spotbugs/main.html").get().asFile
    }
}
tasks.spotbugsTest {
    reports.create("html") {
        required = true
        outputLocation = layout.buildDirectory.file("reports/spotbugs/test.html").get().asFile
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
