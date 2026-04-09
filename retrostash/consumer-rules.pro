# Preserve annotation metadata required by Retrofit Invocation + Retrostash reflection.
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,AnnotationDefault

# Ensure Retrostash annotations remain available at runtime.
-keep @interface dev.logickoder.retrostash.CacheQuery
-keep @interface dev.logickoder.retrostash.CacheMutate

# Keep service methods carrying Retrostash annotations while still allowing shrinking/obfuscation.
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @dev.logickoder.retrostash.CacheQuery <methods>;
    @dev.logickoder.retrostash.CacheMutate <methods>;
}