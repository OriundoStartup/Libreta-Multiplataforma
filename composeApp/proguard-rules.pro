# Kotlin serialization — necesario para DTOs de Supabase
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class com.tuapp.libreta.**$$serializer { *; }
-keepclassmembers class com.tuapp.libreta.** {
    *** Companion;
}
-keepclasseswithmembers class com.tuapp.libreta.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers class kotlinx.serialization.** { *; }

# Paquetes propios de la app
-keep class com.tuapp.libreta.** { *; }
-keep class org.orinundo.** { *; }

# SQLDelight — generadores de queries
-keep class app.cash.sqldelight.** { *; }
-dontwarn app.cash.sqldelight.**

# Ktor — networking
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Supabase client
-keep class io.github.jan.supabase.** { *; }
-dontwarn io.github.jan.supabase.**

# Koin
-keep class org.koin.** { *; }
-dontwarn org.koin.**

# Voyager navigation
-keep class cafe.adriel.voyager.** { *; }
-dontwarn cafe.adriel.voyager.**

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# kotlinx.datetime — usa reflexión internamente
-keep class kotlinx.datetime.** { *; }
-dontwarn kotlinx.datetime.**

# AndroidX
-keep class androidx.** { *; }
-dontwarn androidx.**
-keepclassmembers class androidx.compose.** { *; }

# Kotlin metadata (reflection mínima)
-keep class kotlin.Metadata { *; }
-keep class kotlin.reflect.** { *; }
-dontwarn kotlin.reflect.jvm.internal.**

# Enum values/valueOf — necesario para mapear roles/status desde strings
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Interfaces con lambdas Kotlin (necesario para Compose)
-keepclasseswithmembernames class * {
    native <methods>;
}
