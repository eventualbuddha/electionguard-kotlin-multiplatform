buildscript {
    repositories {
    }
}

@Suppress("DSL_SCOPE_VIOLATION")
// https://youtrack.jetbrains.com/issue/KTIJ-19369
plugins {
    kotlin("jvm") version providers.gradleProperty("kotlinVersion").get()
    id("electionguard.common-conventions")
    alias(libs.plugins.ktor)
    alias(libs.plugins.serialization)
}

group = "webapps.electionguard"
version = "0.1"
application {
    mainClass.set("io.ktor.server.netty.EngineMain")
    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
    implementation(project(path = ":egklib", configuration = "jvmRuntimeElements"))

    implementation(libs.kotlin.result)
    implementation(libs.bundles.ktor.server)
    implementation(libs.bundles.logging.server)

    testImplementation(libs.ktor.server.tests.jvm)
    testImplementation(libs.kotlin.test.junit5)
}