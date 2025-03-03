buildscript {
    repositories {
    }
}

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    kotlin("jvm") version providers.gradleProperty("kotlinVersion").get()
    id("electionguard.common-conventions")
    alias(libs.plugins.ktor)
    alias(libs.plugins.serialization)
}

group = "webapps.electionguard"
version = "0.2"
application {
    mainClass.set("webapps.electionguard.KeyCeremonyRemoteTrusteeKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
    implementation(project(path = ":egklib", configuration = "jvmRuntimeElements"))
    implementation(libs.kotlin.result)
    implementation(libs.bundles.ktor.server)
    implementation(libs.bundles.logging.server)

    testImplementation(libs.ktor.server.tests.jvm)
    testImplementation(libs.kotlin.test.junit) // for some reason, we cant use junit5
}