# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep WebView JavaScript interface methods
-keepclassmembers class com.moviepilot.app.WebAppInterface {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep model classes
-keep class com.moviepilot.app.** { *; }
