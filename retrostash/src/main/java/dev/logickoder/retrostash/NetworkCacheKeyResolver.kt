package dev.logickoder.retrostash

import dev.logickoder.retrostash.model.QueryContext
import okhttp3.Request
import retrofit2.Invocation
import retrofit2.http.Body
import retrofit2.http.Path
import retrofit2.http.Query
import org.json.JSONArray
import org.json.JSONObject
import java.lang.reflect.Array as ReflectArray
import java.lang.reflect.Modifier
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Collections
import java.util.IdentityHashMap

/**
 * Resolves stable cache keys from Retrofit invocation metadata and key templates.
 */
class NetworkCacheKeyResolver(
    private val logger: ((String) -> Unit)? = null,
) {

    /**
     * Builds the internal query key format from a [CacheQuery] template and explicit bindings.
     */
    fun buildQueryKey(
        apiClass: Class<*>,
        template: String,
        bindings: Map<String, Any?>,
    ): String? {
        val stringBindings = bindings.mapNotNull { (name, value) ->
            externalBindingAsString(value)?.let { name to it }
        }.toMap()
        val key = buildKey(apiClass.simpleName, template, stringBindings) ?: return null
        log("query key (external) -> $key")
        return key
    }

    /**
     * Resolves [QueryContext] for requests annotated with [CacheQuery].
     */
    fun resolveQueryContext(request: Request): QueryContext? {
        val invocation = request.tag(Invocation::class.java) ?: return null
        val annotation = invocation.method().getAnnotation(CacheQuery::class.java) ?: return null
        val key = resolveKey(invocation, annotation.key) ?: return null
        log("query key -> $key")
        return QueryContext(key, request.method == "POST")
    }

    /**
     * Resolves all invalidation keys defined by [CacheMutate] for the request.
     */
    fun resolveMutationKeys(request: Request): List<String> {
        val invocation = request.tag(Invocation::class.java) ?: return emptyList()
        val annotation =
            invocation.method().getAnnotation(CacheMutate::class.java) ?: return emptyList()
        val keys = annotation.invalidate.mapNotNull { resolveKey(invocation, it) }
        if (keys.isNotEmpty()) {
            log("found mutation keys -> ${keys.joinToString()}")
        }
        return keys
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
                bodyArgs.firstNotNullOfOrNull { traverseAny(it, placeholder) }
                    ?.let { bindings[placeholder] = it }
            }
        }

        return buildKey(method.declaringClass.simpleName, template, bindings)
    }

    private fun buildKey(scopeName: String, template: String, bindings: Map<String, String>): String? {
        if (template.isBlank()) return null
        val placeholders = PLACEHOLDER_REGEX.findAll(template)
            .map { it.groupValues[1] }
            .distinct()
            .toList()
        if (!placeholders.all { bindings.containsKey(it) }) return null

        val resolved = PLACEHOLDER_REGEX.replace(template) { bindings[it.groupValues[1]].orEmpty() }
        val hash = sha256(
            placeholders.sorted().joinToString("|") { "$it=${bindings[it].orEmpty()}" }
        )
        return "$scopeName|$resolved|$hash"
    }

    private fun traverseAny(root: Any, fieldName: String): String? {
        val visited = Collections.newSetFromMap(IdentityHashMap<Any, Boolean>())
        return traverseAnyInternal(root, fieldName, visited)
    }

    private fun traverseAnyInternal(
        value: Any?,
        fieldName: String,
        visited: MutableSet<Any>
    ): String? {
        if (value == null) return null
        if (primitiveAsString(value) != null) return null
        if (!visited.add(value)) return null

        if (value is Map<*, *>) {
            value[fieldName]?.let { primitiveAsString(it) }?.let { return it }
            return value.values.firstNotNullOfOrNull { traverseAnyInternal(it, fieldName, visited) }
        }

        if (value is JSONObject) {
            value.opt(fieldName)?.let { primitiveAsString(it) }?.let { return it }
            val keys = value.keys()
            while (keys.hasNext()) {
                val nested = traverseAnyInternal(value.opt(keys.next()), fieldName, visited)
                if (nested != null) return nested
            }
            return null
        }

        if (value is JSONArray) {
            for (i in 0 until value.length()) {
                val nested = traverseAnyInternal(value.opt(i), fieldName, visited)
                if (nested != null) return nested
            }
            return null
        }

        if (value is Iterable<*>) {
            return value.firstNotNullOfOrNull { traverseAnyInternal(it, fieldName, visited) }
        }

        if (value.javaClass.isArray) {
            val length = ReflectArray.getLength(value)
            for (i in 0 until length) {
                val found = traverseAnyInternal(ReflectArray.get(value, i), fieldName, visited)
                if (found != null) return found
            }
            return null
        }

        var type: Class<*>? = value.javaClass
        while (type != null && type != Any::class.java) {
            type.declaredFields
                .filterNot { Modifier.isStatic(it.modifiers) || it.isSynthetic }
                .forEach { field ->
                    val fieldValue = runCatching {
                        field.isAccessible = true
                        field.get(value)
                    }.getOrNull()

                    if (field.name == fieldName) {
                        primitiveAsString(fieldValue)?.let { return it }
                    }

                    val nested = traverseAnyInternal(fieldValue, fieldName, visited)
                    if (nested != null) return nested
                }
            type = type.superclass
        }

        return null
    }

    private fun primitiveAsString(value: Any?): String? = when (value) {
        null -> null
        JSONObject.NULL -> null
        is String -> value
        is Number -> value.toString()
        is Boolean -> value.toString()
        is Char -> value.toString()
        is Enum<*> -> value.name
        else -> null
    }

    private fun externalBindingAsString(value: Any?): String? = when (value) {
        null -> null
        is Enum<*> -> value.name
        else -> value.toString()
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private fun log(message: String) {
        logger?.invoke("[Retrostash] $message")
    }

    companion object {
        private val PLACEHOLDER_REGEX = Regex("\\{([^}]+)\\}")
    }
}