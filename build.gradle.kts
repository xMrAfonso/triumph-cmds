plugins {
    kotlin("jvm") version "1.4.31"
}


allprojects {
    apply {
        plugin("java-library")
        plugin("maven-publish")
        plugin("org.jetbrains.kotlin.jvm")
    }

    group = "dev.triumphteam"
    version = "2.0.0-SNAPSHOT"

    repositories {
        mavenCentral()
        mavenLocal()
    }

    dependencies {
        compileOnly("org.jetbrains:annotations:20.1.0")
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    // TODO Add publication

}