package dev.logickoder.retrostash

import com.google.gson.JsonElement
import dev.logickoder.retrostash.model.QueryContext
import okhttp3.Request
import retrofit2.Invocation
import retrofit2.http.Body
import retrofit2.http.Path
import retrofit2.http.Query
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

class NetworkCacheKeyResolver {

    fun resolveQueryContext(request: Request): QueryContext? {
        val invocation = request.tag(Invocation::class.java) ?: return null
        val annotation = invocation.method().getAnnotation(CacheQuery::class.java) ?: return null
        val key = resolveKey(invocation, annotation.key) ?: return null
        return QueryContext(key, request.method == "POST")
    }

    fun resolveMutationKeys(request: Request): List<String> {
        val invocation = request.tag(Invocation::class.java) ?: return emptyList()
        val annotation =
            invocation.method().getAnnotation(CacheMutate::class.java) ?: return emptyList()
        return annotation.invalidate.mapNotNull { resolveKey(invocation, it) }
    }

    private fun resolveKey(invocation: Invocation, template: String): String? {
        if (template.isBlank()) return null
        val method = invocation.method()
        val args = invocation.arguments()
        val paramAnnotations = method.parameterAnnotations
        val bindings = mutableMapOf<String, String>()

        paramAnnotations.forEachIndexed { i, annotations ->
            val arg = args.getOrNull(i) ?: return@forEachIndexed
            annotations.forEach { ann ->
                when (ann) {
                    is Path -> bindings[ann.value] = arg.toString()
                    is Query -> bindings[ann.value] = arg.toString()
                }
            }
        }

        val placeholders = PLACEHOLDER_REGEX.findAll(template)
            .map { it.groupValues[1] }.distinct().toList()

        val unresolved = placeholders.filter { !bindings.containsKey(it) }
        if (unresolved.isNotEmpty()) {
            val bodyArgs = paramAnnotations.mapIndexedNotNull { i, anns ->
                if (anns.any { it is Body }) args.getOrNull(i) else null
            }
            unresolved.forEach { placeholder ->
                bodyArgs.firstNotNullOfOrNull { traverseJson(gson.toJsonTree(it), placeholder) }
                    ?.let { bindings[placeholder] = it }
            }
        }

        if (!placeholders.all { bindings.containsKey(it) }) return null

        val resolved = PLACEHOLDER_REGEX.replace(template) { bindings[it.groupValues[1]].orEmpty() }
        val hash =
            sha256(placeholders.sorted().joinToString("|") { "$it=${bindings[it].orEmpty()}" })
        return "${method.declaringClass.simpleName}|$resolved|$hash"
    }

    private fun traverseJson(element: JsonElement, fieldName: String): String? = when {
        element.isJsonObject -> {
            val obj = element.asJsonObject
            val direct = obj.get(fieldName)
            if (direct != null && !direct.isJsonNull && direct.isJsonPrimitive) direct.asString
            else obj.entrySet().firstNotNullOfOrNull { (_, v) -> traverseJson(v, fieldName) }
        }

        element.isJsonArray -> element.asJsonArray.firstNotNullOfOrNull {
            traverseJson(
                it,
                fieldName
            )
        }

        else -> null
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    companion object {
        private val PLACEHOLDER_REGEX = Regex("\\{([^}]+)\\}")
    }
}