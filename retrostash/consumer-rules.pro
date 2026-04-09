# retrostash/consumer-rules.pro

# 1. Preserve annotations in the bytecode so Invocation reflection can read them
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# 2. Prevent R8 from stripping or obfuscating the annotation classes
-keep @interface dev.logickoder.retrostash.CacheQuery
-keep @interface dev.logickoder.retrostash.CacheMutate

# 3. Preserve the interface methods (and their parameters) that use these annotations
-keepclassmembers interface * {
    @dev.logickoder.retrostash.CacheQuery *;
    @dev.logickoder.retrostash.CacheMutate *;
}