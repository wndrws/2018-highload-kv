import org.gradle.api.plugins.internal.SourceSetUtil

// See https://gradle.org and https://github.com/gradle/kotlin-dsl

// Apply the java plugin to add support for Java
plugins {
    java
    application
    id("io.franzbecker.gradle-lombok").version("1.14")
}

lombok {
    version = "1.18.2"
    sha256 = ""
}

repositories {
    jcenter()
}


val loggingDependencies = listOf(
        "org.slf4j:slf4j-api:1.7.25",
        "org.apache.logging.log4j:log4j-api:2.11.1",
        "org.apache.logging.log4j:log4j-1.2-api:2.11.1",
        "org.apache.logging.log4j:log4j-slf4j-impl:2.11.1",
        "org.apache.logging.log4j:log4j-core:2.11.1")

dependencies {
    // Annotations for better code documentation
    compile("com.intellij:annotations:12.0")

    // H2 Database for persistence
    runtime("com.h2database:h2:1.4.197")

    // jOOQ to work with SQL
    compile("org.jooq:jooq:3.11.5")

    // one-nio tools
    compile("ru.odnoklassniki:one-nio:1.0.2")

    // Logging
    loggingDependencies.map { compile(it) }

    // JUnit Jupiter test framework
    testCompile("org.junit.jupiter:junit-jupiter-api:5.3.1")

    // HTTP client for unit tests
    testCompile("org.apache.httpcomponents:fluent-hc:4.5.3")
}

tasks {
    "test"(Test::class) {
        maxHeapSize = "128m"
    }
}

application {
    // Define the main class for the application
    mainClassName = "ru.mail.polis.Server"

    // And limit Xmx
    applicationDefaultJvmArgs = listOf("-Xmx128m")
}
