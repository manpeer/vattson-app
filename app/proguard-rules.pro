# Retrofit 2.x & GSON: Retain attributes needed for reflective generic types and annotations
-keepattributes Signature, InnerClasses, AnnotationDefault
-keepattributes *Annotation*
-dontwarn okio.**
-dontwarn javax.annotation.**

# Keep data models used by Gson deserialization
-keep class ee.vattson.data.model.** { *; }

