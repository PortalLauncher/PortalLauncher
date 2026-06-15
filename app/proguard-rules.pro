# Room and WorkManager rules to prevent crashes in obfuscated builds
# These libraries are used by Ads SDKs and need to be kept from being obfuscated or stripped.

-keep class androidx.work.** { *; }
-keep interface androidx.work.** { *; }
-dontwarn androidx.work.**

-keep class androidx.room.** { *; }
-keep interface androidx.room.** { *; }
-dontwarn androidx.room.**

-keep class * extends androidx.room.RoomDatabase {
    <init>(...);
}

-keep class androidx.work.impl.WorkDatabase_Impl {
    public <init>(...);
}

-keep class androidx.work.impl.WorkManagerInitializer {
    public <init>();
}

-keep class androidx.work.WorkManagerInitializer {
    public <init>();
}

-keep class androidx.startup.InitializationProvider
-keep class * implements androidx.startup.Initializer {
    public <init>();
}

# App Specific Keep Rules
-keep class de.szalkowski.activitylauncher.** { *; }
-keep interface de.szalkowski.activitylauncher.** { *; }

# General keep rules for reflection used by libraries
-keepattributes Signature,AnnotationDefault,EnclosingMethod,InnerClasses,SourceFile,LineNumberTable

-keep class androidx.tracing.** { *; }
-dontwarn androidx.tracing.**

-keep class kotlin.** { *; }
-dontwarn kotlin.**
-keep class kotlinx.** { *; }
-dontwarn kotlinx.**
