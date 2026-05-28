val isVercel = System.getenv("VERCEL") != null

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication) apply (System.getenv("VERCEL") == null)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvmToolchain(17)
    if (!isVercel) {
        androidTarget {
            compilerOptions {
                jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
            }
        }

        listOf(
            iosArm64(),
            iosSimulatorArm64()
        ).forEach { iosTarget ->
            iosTarget.binaries.framework {
                baseName = "ComposeApp"
                isStatic = true
            }
        }
    }

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        compilerOptions {
            outputModuleName.set("composeApp")
        }
        browser {
            commonWebpackConfig {
                devServer = (devServer ?: org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.DevServer()).apply {
                    port = 8080
                }
            }
        }
        binaries.executable()
    }

    sourceSets {
        findByName("wasmJsMain")?.apply {
            dependencies {
                implementation(libs.ktor.client.js)
                implementation(libs.supabase.auth)
                implementation(libs.sqldriver.web)
                implementation(libs.kotlinx.browser)
                implementation(libs.kotlinx.datetime)
                implementation(compose.ui)
                implementation(compose.runtime)
            }
        }

        findByName("androidMain")?.apply {
            dependencies {
                implementation(libs.compose.uiToolingPreview)
                implementation(libs.androidx.activity.compose)
                implementation(libs.koin.android)
                implementation(libs.supabase.auth)
                implementation(libs.ktor.client.cio)
                implementation(libs.androidx.browser)
            }
        }

        commonMain.dependencies {
            implementation(project(":shared"))
            implementation(libs.supabase.auth)
            implementation(libs.supabase.postgrest)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.resources)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.voyager.navigator)
            implementation(libs.voyager.transitions)
            implementation(libs.koin.compose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

// Configuración segura de Android
if (!isVercel) {
    configure<com.android.build.gradle.internal.dsl.BaseAppModuleExtension> {
        namespace = "org.oriundo"
        compileSdk = libs.versions.android.compileSdk.get().toInt()

        defaultConfig {
            applicationId = "org.oriundo"
            minSdk = libs.versions.android.minSdk.get().toInt()
            targetSdk = libs.versions.android.targetSdk.get().toInt()
            versionCode = 1
            versionName = "1.0"
        }
        packaging {
            resources {
                excludes += "/META-INF/{AL2.0,LGPL2.1}"
            }
        }
        buildTypes {
            getByName("release") {
                isMinifyEnabled = true
                isShrinkResources = true
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )
            }
        }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
        }
    }
}

plugins.withId("com.android.application") {
    dependencies {
        add("debugImplementation", libs.compose.uiTooling)
    }
}

// El worker SQLDelight se distribuye como un asset estático en
// composeApp/src/wasmJsMain/resources/sqldelight-worker.js — no se extrae del klib
// porque web-worker-driver 2.x no incluye un worker por defecto.


