buildscript {
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    kotlin("multiplatform") version "1.6.10"
    id("tech.formatter-kt.formatter") version "0.7.9"
}

group = "electionguard-kotlin-multiplatform"
version = "1.0-SNAPSHOT"

repositories {
    google()
    mavenCentral()
}

// Failed attempt to get more output from failed tests
// https://stackoverflow.com/questions/3963708/gradle-how-to-display-test-results-in-the-console-in-real-time

//tasks.withType<AbstractTestTask> {
//    afterSuite(KotlinClosure2({ desc: TestDescriptor, result: TestResult ->
//        if (desc.parent == null) { // will match the outermost suite
//            println("Results: ${result.resultType} (${result.testCount} tests,
// ${result.successfulTestCount} successes, ${result.failedTestCount} failures,
// ${result.skippedTestCount} skipped)")
//        }
//    }))
//    testLogging {
//        events("started", "passed", "skipped", "failed", "standardOut", "standardError")
//    }
//}

// Hack to get us a newer version of Gradle than the default of 14.17.0
rootProject.plugins.withType(org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin::class.java) {
    rootProject.the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension>().download = true
    rootProject.the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension>().nodeVersion = "17.3.0"
}


kotlin {
    jvm {
        compilations.all { kotlinOptions.jvmTarget = "1.8" }
        //        withJava()
        testRuns["test"].executionTask
            .configure {
                useJUnitPlatform()
                minHeapSize = "512m"
                maxHeapSize = "2048m"
                jvmArgs = listOf("-Xss128m")
            }
    }

    js(LEGACY) {
        moduleName = "electionguard"

        useCommonJs()
        binaries.executable()

        //        browser {
        //            commonWebpackConfig { cssSupport.enabled = true }
        //        }
        nodejs {
            version = "17.3.0"

            testTask {
                useMocha {
                    // thirty seconds rather than the default of two seconds
                    timeout = "30000"
                }
            }
        }

        // This should allow things to run in Chrome, but we're getitng weird errors.
        // Error during file loading or preprocessing

        //Error: error:0308010C:digital envelope routines::unsupported
        //    at new Hash (node:internal/crypto/hash:67:19)
        //    at Object.createHash (node:crypto:130:10)
        //    at BulkUpdateDecorator.hashFactory (/Users/dwallach/IdeaProjects/electionguard-kotlin-multiplatform/build/js/node_modules/webpack/lib/util/createHash.js:155:18)

        // This seems like a Kotlin bug, not my bug.

//         browser {
//             testTask {
//                 useKarma {
//                     useChromeHeadless()
//                 }
//             }
//         }

    }

    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget =
        when {
            hostOs == "Mac OS X" -> macosX64("native")
            hostOs == "Linux" -> linuxX64("native")
            isMingwX64 -> mingwX64("native")
            else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
        }

    nativeTarget.apply {
        compilations.getByName("main") {
            cinterops {
                val libhacl by
                    creating {
                        defFile(project.file("src/nativeInterop/libhacl.def"))
                        packageName("hacl")
                        compilerOpts("-Ilibhacl/include")
                        includeDirs.allHeaders("${System.getProperty("user.dir")}/libhacl/include")
                    }
            }
        }

        binaries { executable { entryPoint = "main" } }
        //        binaries {
        //            staticLib {
        //                entryPoint = "main"
        //            }
        //        }
    }

    sourceSets {
        all { languageSettings.optIn("kotlin.RequiresOptIn") }

        val commonMain by
            getting {
                dependencies {
                    implementation(kotlin("stdlib-common", "1.6.10"))
                    implementation(kotlin("stdlib", "1.6.10"))

                    // JSON serialization and DSL
                    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.1")

                    // Coroutines
                    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0-RC3")
                }
            }
        val commonTest by
            getting {
                dependencies {
                    implementation(kotlin("test-common", "1.6.10"))
                    implementation(kotlin("test-annotations-common", "1.6.10"))

                    // runTest() for running suspend functions in tests
                    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.0-RC3")

                    // Fancy property-based testing
                    implementation("io.kotest:kotest-property:5.0.1")
                }
            }
        val jvmMain by
            getting {
                dependencies {
                    implementation(kotlin("stdlib-jdk8", "1.6.10"))

                    // Progress bars
                    implementation("me.tongfei:progressbar:0.9.2")
                }
            }
        val jvmTest by
            getting {
                dependencies {
                    // Unclear if we really need all the extra features of JUnit5, but it would
                    // at least be handy if we could get its parallel test runner to work.
                    implementation(kotlin("test-junit5", "1.6.10"))
                }
            }
        val jsMain by
            getting {
                dependencies {
                    implementation(kotlin("stdlib-js", "1.6.10"))

                    // Portable, Kotlin port of Java's BigInteger; slow but works
//                    implementation("io.github.gciatto:kt-math:0.4.0")

                    // GMP-WASM is probably the fastest bignum package for JavaScript,
                    // but because it relies on WASM, it makes everything more difficult
                    // to use than, say, kt-math.
                    implementation(npm("gmp-wasm", "0.9.4", generateExternals = true))
                }
            }
        val jsTest by getting { dependencies { implementation(kotlin("test-js", "1.6.10")) } }
        val nativeMain by getting { dependencies {} }
        val nativeTest by getting { dependencies {} }
    }
}

tasks.register("libhaclBuild") {
    doLast {
        exec {
            workingDir(".")
            // -p flag will ignore errors if the directory already exists
            commandLine("mkdir", "-p", "build")
        }
        exec {
            workingDir("build")
            commandLine("cmake", "-DCMAKE_BUILD_TYPE=Release", "..")
        }
        exec {
            workingDir("build")
            commandLine("make")
        }
    }
}

// hack to make sure that we've compiled the library prior to running cinterop on it
tasks["cinteropLibhaclNative"].dependsOn("libhaclBuild")

tasks.withType<Test> { testLogging { showStandardStreams = true } }