# Add project specific ProGuard rules here.

# Keep Gson serialized classes
-keepattributes Signature
-keepattributes *Annotation*

# Gson specific classes
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }

# Keep data classes for Gson
-keep class com.chibaminto.compactalarm.data.** { *; }

# Keep Hilt classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep Compose classes
-keep class androidx.compose.** { *; }

# Keep LocalTime serialization
-keep class java.time.** { *; }
