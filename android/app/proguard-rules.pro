# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class dev.mirzohidkhon.khonfitness.**$$serializer { *; }
-keepclassmembers class dev.mirzohidkhon.khonfitness.** { *** Companion; }
-keepclasseswithmembers class dev.mirzohidkhon.khonfitness.** { kotlinx.serialization.KSerializer serializer(...); }
