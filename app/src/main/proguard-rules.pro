-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# preserve several libraries
-keepnames class android.** { *; }
-keepnames class androidx.** { *; }
-keepnames class com.android.** { *; }
-keepnames class com.google.** { *; }
-keepnames class kotlin.** { *; }
-keepnames class kotlinx.** { *; }
