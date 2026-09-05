# ProGuard / R8 Rules for Study Magic App

# Keep Gson models and fields so JSON serialization/deserialization works in release builds
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Preserve model classes
-keep class com.amstudio.studymagic.models.** { *; }
-keepclassmembers class com.amstudio.studymagic.models.** { *; }

# Preserve Gson annotations and serialized fields
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Retrofit 2 rules
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# OkHttp rules
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Markwon rules
-dontwarn io.noties.markwon.**
-keep class io.noties.markwon.** { *; }

# AndroidPdfViewer rules
-keep class com.github.barteksc.pdfviewer.** { *; }
-dontwarn com.github.barteksc.pdfviewer.**

# Keep activity classes and app components referenced in manifest
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Fragment
-keep public class * extends androidx.fragment.app.Fragment
