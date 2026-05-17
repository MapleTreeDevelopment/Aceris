import org.gradle.external.javadoc.StandardJavadocDocletOptions

plugins {
    `java-library`
    `maven-publish`
    signing
}

val acerisGroup = providers.gradleProperty("mavenGroup").orElse("dev.mapletree").get()
val acerisVersion = providers.gradleProperty("version").orElse("0.1.0-SNAPSHOT").get()

group = acerisGroup
version = acerisVersion

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    withJavadocJar()
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(17)
}

tasks.withType<Javadoc>().configureEach {
    options.encoding = "UTF-8"
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:all,-missing", "-quiet")
}

tasks.named<Test>("test") {
    enabled = false
}

tasks.register<JavaExec>("acceptanceTest") {
    group = "verification"
    description = "Runs dependency-free test suite."
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("dev.mapletree.aceris.TomlTestSuite")
}

val dependencyFreeTestClasses = listOf(
    "TomlIntegerTypeTest",
    "TomlFloatTypeTest",
    "TomlStringTypeTest",
    "TomlTemporalTypeTest",
    "TomlArrayCollectionTest",
    "TomlTableScenarioTest",
    "TomlObjectMappingTest",
    "TomlWriterTest",
    "TomlConfigTest",
    "TomlScriptRunnerTest",
    "TomlConformanceTest",
    "TomlStabilityTest",
    "TomlAstTest",
    "TomlCrashTest"
)

dependencyFreeTestClasses.forEach { testClass ->
    tasks.register<JavaExec>(testClass.removePrefix("Toml").removeSuffix("Test").replaceFirstChar { it.lowercase() } + "Test") {
        group = "verification"
        description = "Runs $testClass."
        classpath = sourceSets["test"].runtimeClasspath
        mainClass.set("dev.mapletree.aceris.$testClass")
    }
}

tasks.register<JavaExec>("benchmark") {
    group = "verification"
    description = "Runs dependency-free parser and mapping microbenchmarks."
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("dev.mapletree.aceris.TomlBenchmark")
    System.getProperty("aceris.benchmark.warmup")?.let {
        systemProperty("aceris.benchmark.warmup", it)
    }
    System.getProperty("aceris.benchmark.iterations")?.let {
        systemProperty("aceris.benchmark.iterations", it)
    }
}

tasks.named("check") {
    dependsOn("acceptanceTest")
}

tasks.withType<GenerateModuleMetadata>().configureEach {
    enabled = false
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            pom {
                name.set("Aceris")
                description.set("Dependency-free TOML toolkit for Java and Kotlin.")
                url.set("https://github.com/MapleTreeDevelopment/aceris")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/license/mit/")
                        distribution.set("repo")
                    }
                }

                developers {
                    developer {
                        id.set("mapletreedevelopment")
                        name.set("MapleTreeDevelopment")
                        url.set("https://github.com/MapleTreeDevelopment")
                    }
                }

                scm {
                    connection.set("scm:git:https://github.com/MapleTreeDevelopment/aceris.git")
                    developerConnection.set("scm:git:ssh://git@github.com/MapleTreeDevelopment/aceris.git")
                    url.set("https://github.com/MapleTreeDevelopment/aceris")
                    tag.set("HEAD")
                }
            }
        }
    }

    repositories {
        maven {
            name = "CentralStaging"
            url = layout.buildDirectory.dir("central-staging").get().asFile.toURI()
        }
    }
}

val signingKey = providers.gradleProperty("signingInMemoryKey")
    .orElse(providers.environmentVariable("SIGNING_KEY"))
val signingPassword = providers.gradleProperty("signingInMemoryKeyPassword")
    .orElse(providers.environmentVariable("SIGNING_PASSWORD"))
val isReleaseVersion = !acerisVersion.endsWith("-SNAPSHOT")

signing {
    isRequired = isReleaseVersion
    if (signingKey.isPresent) {
        useInMemoryPgpKeys(signingKey.get(), signingPassword.orNull)
    }
    sign(publishing.publications["mavenJava"])
}

val validateCentralRelease = tasks.register("validateCentralRelease") {
    group = "publishing"
    description = "Validates release inputs required for Maven Central."

    doLast {
        if (acerisVersion.endsWith("-SNAPSHOT")) {
            throw GradleException("Maven Central releases cannot use -SNAPSHOT versions. Run with -Pversion=0.1.0.")
        }
        if (!signingKey.isPresent) {
            throw GradleException("Missing PGP signing key. Set SIGNING_KEY or signingInMemoryKey.")
        }
    }
}

tasks.named("publishMavenJavaPublicationToCentralStagingRepository") {
    mustRunAfter(validateCentralRelease)
}

tasks.register<Zip>("centralBundle") {
    group = "publishing"
    description = "Builds a signed Maven Central upload bundle in build/distributions."
    dependsOn(validateCentralRelease)
    dependsOn("publishMavenJavaPublicationToCentralStagingRepository")
    archiveFileName.set("aceris-$acerisVersion-central-bundle.zip")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
    from(layout.buildDirectory.dir("central-staging"))
    exclude("**/maven-metadata.xml*")
}
