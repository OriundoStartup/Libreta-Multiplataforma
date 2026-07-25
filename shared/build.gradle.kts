import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

val isVercel = System.getenv("VERCEL") != null

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary) apply (System.getenv("VERCEL") == null)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.buildkonfig)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvmToolchain(17)
    if (!isVercel) {
        androidTarget {
            compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
        }
        listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
            iosTarget.binaries.framework {
                baseName = "Shared"
                isStatic = true
            }
        }
    }

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.resources)
            implementation(libs.compose.materialIconsExtended)
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.sqldelight.async.extensions)
            implementation(libs.kotlinx.datetime)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.voyager.navigator)
            implementation(libs.voyager.screenmodel)
            implementation(libs.voyager.transitions)
            implementation(libs.voyager.koin)
            implementation(libs.supabase.postgrest)
            implementation(libs.supabase.auth)
            implementation(libs.supabase.realtime)
            implementation(libs.supabase.storage)
            implementation(libs.ktor.client.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.no.arg)
        }

        // Configuración segura de sourceSets de plataforma
        findByName("androidMain")?.apply {
            dependencies {
                implementation(libs.sqldelight.android)
                implementation(libs.androidx.core.ktx)
                implementation(libs.androidx.browser)
                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.security.crypto)
            }
        }

        findByName("androidUnitTest")?.apply {
            dependencies {
                implementation(libs.mockk)
            }
        }

        findByName("iosMain")?.apply {
            dependencies {
                implementation(libs.sqldelight.native)
            }
        }

        findByName("wasmJsMain")?.apply {
            dependencies {
                implementation(libs.ktor.client.js)
                implementation(libs.sqldriver.web)
                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.browser)
            }
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

sqldelight {
    databases {
        create("LibretaAppDatabase") {
            packageName.set("com.tuapp.libreta.db")
            // Genera API suspend: el web-worker-driver es asíncrono. Los drivers nativos
            // (Android/iOS) son síncronos y await() retorna de inmediato, así que siguen funcionando.
            generateAsync.set(true)
        }
    }
}

// Configuración segura de Android
if (!isVercel) {
    configure<com.android.build.gradle.LibraryExtension> {
        namespace = "com.tuapp.libreta.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        defaultConfig { minSdk = libs.versions.android.minSdk.get().toInt() }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
        }
        lint {
            disable += "FrequentlyChangingValue"
        }
    }
}

// ── BuildKonfig — lee de variables de entorno (CI) o local.properties (Local) ─
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}

fun getProp(name: String): String {
    return System.getenv(name) ?: localProps.getProperty(name, "")
}

buildkonfig {
    packageName = "com.tuapp.libreta"
    defaultConfigs {
        buildConfigField(com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING, "SUPABASE_URL", getProp("SUPABASE_URL"))
        buildConfigField(com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING, "SUPABASE_KEY", getProp("SUPABASE_KEY"))
        buildConfigField(com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING, "SUPABASE_REDIRECT_URL", getProp("SUPABASE_REDIRECT_URL"))
    }
}
