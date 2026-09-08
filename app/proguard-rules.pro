# ProGuard / R8 Rules for Study Magic App

# Preserve Attributes required for Gson Generics, Reflection, and Retrofit Annotations
-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses, SourceFile, LineNumberTable

# Preserve all Data Model classes (and inner classes) for Gson and Intent Serialization
-keep class com.amstudio.studymagic.models.** { *; }
-keepclassmembers class com.amstudio.studymagic.models.** { *; }

# Preserve SerializedName fields for Gson
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Preserve Serializable implementation rules for Intent Extras
-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
    <fields>;
}

# Preserve Fragments and Adapters
-keep class com.amstudio.studymagic.fragments.** { *; }
-keep class com.amstudio.studymagic.adapters.** { *; }
-keepclassmembers class com.amstudio.studymagic.adapters.** { *; }

# Preserve Retrofit API Interfaces and Annotations
-keep interface com.amstudio.studymagic.api.** { *; }
-keep class com.amstudio.studymagic.api.** { *; }
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.** <methods>;
}

# Gson Rules
-dontwarn com.google.gson.**
-keep class com.google.gson.** { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken { *; }

# OkHttp and Retrofit Suppressions
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# AndroidPdfViewer & Pdfium Native JNI Keep Rules (Critical for Release APK)
-keep class com.github.barteksc.pdfviewer.** { *; }
-keepclassmembers class com.github.barteksc.pdfviewer.** { *; }
-dontwarn com.github.barteksc.pdfviewer.**

-keep class com.shockwave.pdfium.** { *; }
-keepclassmembers class com.shockwave.pdfium.** { *; }
-dontwarn com.shockwave.pdfium.**

# Libraries Suppressions & Keep Rules
-dontwarn io.noties.markwon.**
-dontwarn com.squareup.picasso.**
-dontwarn nl.dionsegijn.konfetti.**
