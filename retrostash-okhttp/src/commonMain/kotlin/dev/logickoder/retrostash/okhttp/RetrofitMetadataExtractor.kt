package dev.logickoder.retrostash.okhttp

import dev.logickoder.retrostash.CacheMutate
import dev.logickoder.retrostash.CacheQuery
import retrofit2.Invocation
import retrofit2.http.Body
import retrofit2.http.Path
import retrofit2.http.Query

internal object RetrofitMetadataExtractor {

    fun extract(invocation: Invocation): OkHttpRetrostashMetadata? {
        val method = invocation.method()
        val queryAnn = method.getAnnotation(CacheQuery::class.java)
        val mutateAnn = method.getAnnotation(CacheMutate::class.java)

        if (queryAnn == null && mutateAnn == null) {
            return null
        }

        val bindings = extractBindings(method.parameterAnnotations, invocation.arguments())
        return OkHttpRetrostashMetadata(
            scopeName = method.declaringClass.simpleName,
            queryTemplate = queryAnn?.key,
            maxAgeMs = ((queryAnn?.maxAgeSeconds ?: 0L).coerceAtLeast(0L) * 1000L),
            bindings = bindings,
            invalidateTemplates = resolveInvalidateTemplates(mutateAnn),
            tagTemplates = queryAnn?.tags?.toList().orEmpty(),
            invalidateTagTemplates = mutateAnn?.invalidateTags?.toList().orEmpty(),
        )
    }

    private fun resolveInvalidateTemplates(annotation: CacheMutate?): List<String> {
        if (annotation == null) return emptyList()
        val oldShape = annotation.invalidate.toList()
        if (oldShape.isNotEmpty()) return oldShape
        return annotation.invalidates.toList()
    }

    private fun extractBindings(
        parameterAnnotations: Array<Array<Annotation>>,
        arguments: List<Any?>,
    ): Map<String, String> {
        val result = mutableMapOf<String, String>()
        parameterAnnotations.forEachIndexed { index, annotations ->
            val value = arguments.getOrNull(index)?.toString() ?: return@forEachIndexed
            annotations.forEach { annotation ->
                when (annotation) {
                    is Path -> result[annotation.value] = value
                    is Query -> result[annotation.value] = value
                    is Body -> Unit
                }
            }
        }
        return result
    }
}
