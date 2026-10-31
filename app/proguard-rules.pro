# kotlinx.serialization: keep generated serializers for @Serializable models.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers @kotlinx.serialization.Serializable class com.zennature.wallpaper.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class com.zennature.wallpaper.** {
    kotlinx.serialization.KSerializer serializer(...);
}
