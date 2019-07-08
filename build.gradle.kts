import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootJar

buildscript {
    extra.apply {
        set("kotlinJvmPluginVersion", "1.2.71")
        set("kotlinSpringPluginVersion", "1.2.71")
        set("springVersion", "2.1.6.RELEASE")
        set("dependencyMgrVersion", "1.0.7.RELEASE")
    }

}


plugins {
    id("org.springframework.boot") version extra.get("springVersion") as String
    id("io.spring.dependency-management") version extra.get("dependencyMgrVersion") as String
    kotlin("jvm") version extra.get("kotlinJvmPluginVersion") as String
    kotlin("plugin.spring") version extra.get("kotlinSpringPluginVersion") as String
}

group = "net.alfss"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType(Wrapper::class.java).configureEach  {
    gradleVersion = "5.2.1"
    distributionType = Wrapper.DistributionType.BIN
}