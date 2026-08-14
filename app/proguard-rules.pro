# Prompt Optimizer - ProGuard/R8 rules
# Keep kotlinx.serialization generated serializers
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.promptoptimizer.**$$serializer { *; }
-keepclassmembers enum * { *; }
