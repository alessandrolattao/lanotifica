# ML Kit Barcode Scanning
# play-services-mlkit-barcode-scanning has no bundled ProGuard consumer rules.
# R8 removes internal implementation classes used via service locator pattern,
# causing BarcodeScanning.getClient() to NPE at runtime in release builds.
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_** { *; }

# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Gson
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep data classes for serialization
-keep class com.alessandrolattao.lanotifica.network.** { *; }