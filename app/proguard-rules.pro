-keepattributes *Annotation*
-keepclassmembers class ** {
    @androidx.compose.runtime.Composable <methods>;
}
-keep class com.dpibypass.app.vpn.** { *; }
-dontwarn javax.annotation.**
