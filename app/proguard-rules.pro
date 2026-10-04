# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\...\AppData\Local\Android\sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.

-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}

-keep class com.flamewidget.app.data.** { *; }
-keep class com.flamewidget.app.widget.** { *; }
-keep class com.flamewidget.app.widget.WidgetRefreshWorker { *; }
-keep class com.flamewidget.app.widget.FlameWidgetProvider { *; }
-keep class com.flamewidget.app.widget.BootReceiver { *; }

