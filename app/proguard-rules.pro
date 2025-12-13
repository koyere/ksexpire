# ProGuard rules for KS Expire - Privacy-First Subscription Manager
# Optimized for release build with maximum obfuscation while preserving functionality

# ================================================================================================
# DEBUGGING (Remove in production)
# ================================================================================================
# Keep line numbers for crash reports (can be removed for maximum obfuscation)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ================================================================================================
# ANDROID CORE
# ================================================================================================
# Keep Android framework classes
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends androidx.fragment.app.Fragment

# Keep View constructors
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# ================================================================================================
# ROOM DATABASE
# ================================================================================================
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keep class androidx.room.** { *; }
-dontwarn androidx.room.paging.**

# Keep our entities and DAOs
-keep class com.koyeresolutions.ksexpire.data.entities.** { *; }
-keep class com.koyeresolutions.ksexpire.data.database.** { *; }

# ================================================================================================
# CAMERAX
# ================================================================================================
-keep class androidx.camera.** { *; }
-keep interface androidx.camera.** { *; }
-dontwarn androidx.camera.**

# ================================================================================================
# GOOGLE PLAY SERVICES
# ================================================================================================
-keep class com.google.android.play.core.** { *; }
-keep interface com.google.android.play.core.** { *; }

# Google Play In-App Review
-keep class com.google.android.play.core.review.** { *; }

# ================================================================================================
# KOTLIN & COROUTINES
# ================================================================================================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ================================================================================================
# MATERIAL DESIGN & UI
# ================================================================================================
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**

# ViewBinding
-keep class * implements androidx.viewbinding.ViewBinding {
    public static *** inflate(android.view.LayoutInflater);
    public static *** inflate(android.view.LayoutInflater, android.view.ViewGroup, boolean);
    public static *** bind(android.view.View);
}

# ================================================================================================
# WORKMANAGER
# ================================================================================================
-keep class androidx.work.** { *; }
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.InputMerger
-keepclassmembers class * extends androidx.work.Worker {
    public <init>(android.content.Context,androidx.work.WorkerParameters);
}

# ================================================================================================
# BACKUP SYSTEM
# ================================================================================================
# Keep backup classes for serialization
-keep class com.koyeresolutions.ksexpire.backup.** { *; }

# ================================================================================================
# REMOVE LOGGING (SECURITY)
# ================================================================================================
# Remove all Log calls (security best practice)
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# ================================================================================================
# OPTIMIZATION
# ================================================================================================
# Enable aggressive optimization
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

# ================================================================================================
# WARNINGS TO IGNORE
# ================================================================================================
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**