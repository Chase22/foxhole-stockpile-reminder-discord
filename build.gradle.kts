import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.konan.properties.loadProperties

plugins {
    kotlin("jvm") version "1.8.10"
}

group = "de.chasenet"
version = "1.0"

val repository = loadProperties("local.properties")["repository"]

repositories {
    mavenCentral()
}

dependencies {
    implementation("dev.kord:kord-core:0.8.3")
    implementation("dev.kord:kord-gateway:0.8.3")
    implementation("dev.kord:kord-rest:0.8.3")

    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")

    implementation("io.insert-koin:koin-core-jvm:3.4.0")

    implementation("ch.qos.logback:logback-classic:1.4.6")

    testImplementation(kotlin("test"))
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "de.chasenet.foxhole.MainKt"
    }
    archiveVersion.set("")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}

val buildDocker = task("buildDocker") {
    dependsOn(tasks.assemble)
    exec {
        commandLine("docker", "build", "-t", "$repository/${project.name}:${project.version}", ".")
    }
}

task("pushDocker") {
    dependsOn(buildDocker)
    doLast {
        exec {
            commandLine("docker", "push", "$repository/${project.name}:${project.version}")
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_17.majorVersion
}