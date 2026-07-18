# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.cosmere.companion.**$$serializer { *; }
-keepclassmembers class com.cosmere.companion.** { *** Companion; }
-keepclasseswithmembers class com.cosmere.companion.** { kotlinx.serialization.KSerializer serializer(...); }
