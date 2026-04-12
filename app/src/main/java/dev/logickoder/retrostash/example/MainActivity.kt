@file:SuppressLint("SetTextI18n")

package dev.logickoder.retrostash.example

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.google.gson.annotations.SerializedName
import dev.logickoder.retrostash.CacheMutate
import dev.logickoder.retrostash.CacheQuery
import dev.logickoder.retrostash.ktor.RetrostashKtorMetadata
import dev.logickoder.retrostash.ktor.RetrostashKtorRuntime
import dev.logickoder.retrostash.ktor.RetrostashPlugin
import dev.logickoder.retrostash.okhttp.AndroidRetrostashStore
import dev.logickoder.retrostash.okhttp.RetrostashOkHttpBridge
import dev.logickoder.retrostash.okhttp.RetrostashOkHttpConfig
import okhttp3.Cache
import okhttp3.OkHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import java.io.File

class MainActivity : AppCompatActivity() {

    private val eventLog = ArrayDeque<String>()
    private val gson = GsonBuilder().create()

    private lateinit var statusText: TextView
    private lateinit var switchTransportButton: Button
    private lateinit var api: ExampleApi
    private lateinit var ktorClient: HttpClient
    private lateinit var ktorRuntime: RetrostashKtorRuntime
    private val transportState = TransportModeState()
    private var currentStatus = "Ready to load posts, create a mutation, or trigger cache replay."

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        statusText = findViewById(R.id.statusText)
        switchTransportButton = findViewById(R.id.switchTransportButton)
        val loadPostsButton: Button = findViewById(R.id.loadPostsButton)
        val createPostButton: Button = findViewById(R.id.createPostButton)
        val searchPostsButton: Button = findViewById(R.id.searchPostsButton)
        val refreshPostsButton: Button = findViewById(R.id.refreshPostsButton)

        val sharedStore = AndroidRetrostashStore(applicationContext, RetrostashOkHttpConfig())
        ktorRuntime = RetrostashKtorRuntime.create(sharedStore)

        val okHttpBuilder = OkHttpClient.Builder()
            .cache(
                Cache(
                    directory = File(cacheDir, "http-cache"),
                    maxSize = 10L * 1024L * 1024L,
                )
            )
        RetrostashOkHttpBridge.install(
            okHttpBuilder,
            applicationContext,
            RetrostashOkHttpConfig(logger = ::recordEvent),
        )

        val retrofit = Retrofit.Builder()
            .baseUrl("https://jsonplaceholder.typicode.com/")
            .client(okHttpBuilder.build())
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
            .build()

        api = retrofit.create(ExampleApi::class.java)
        ktorClient = HttpClient(OkHttp) {
            install(RetrostashPlugin) {
                store = sharedStore
                timeoutMs = 250L
            }
        }

        renderStatus()
        switchTransportButton.setOnClickListener { toggleTransport() }
        loadPostsButton.setOnClickListener { loadPosts() }
        createPostButton.setOnClickListener { createPost() }
        searchPostsButton.setOnClickListener { searchPostsViaPost() }
        refreshPostsButton.setOnClickListener { refreshPostsViaGet() }
    }

    private fun loadPosts() {
        if (transportState.mode == TransportMode.KTOR) {
            loadPostsKtor()
            return
        }
        updateStatus("Loading posts for userId=1...")
        api.getPosts(1).enqueue(object : Callback<List<Post>> {
            override fun onResponse(call: Call<List<Post>>, response: Response<List<Post>>) {
                if (!response.isSuccessful || response.body() == null) {
                    updateStatus("Load failed: HTTP ${response.code()}")
                    return
                }
                val posts = response.body().orEmpty()
                val firstTitle = posts.firstOrNull()?.title ?: "<none>"
                updateStatus("Loaded ${posts.size} posts. First title: $firstTitle")
            }

            override fun onFailure(call: Call<List<Post>>, t: Throwable) {
                updateStatus("Load failed: ${t.message}")
            }
        })
    }

    private fun createPost() {
        if (transportState.mode == TransportMode.KTOR) {
            createPostKtor()
            return
        }
        updateStatus("Creating post for userId=1...")
        val request = CreatePostRequest(1, "Retrostash", "Mutation that invalidates query key")
        api.createPost(request).enqueue(object : Callback<Post> {
            override fun onResponse(call: Call<Post>, response: Response<Post>) {
                if (!response.isSuccessful || response.body() == null) {
                    updateStatus("Create failed: HTTP ${response.code()}")
                    return
                }
                val created = response.body()!!
                updateStatus(
                    "Created post id=${created.id}. Query key for userId=1 was marked dirty.",
                )
            }

            override fun onFailure(call: Call<Post>, t: Throwable) {
                updateStatus("Create failed: ${t.message}")
            }
        })
    }

    private fun searchPostsViaPost() {
        if (transportState.mode == TransportMode.KTOR) {
            searchPostsKtor()
            return
        }
        updateStatus("Searching posts with POST @CacheQuery...")
        val request = CreatePostRequest(1, "Retrostash Query", "POST query example")
        api.searchPosts(request).enqueue(object : Callback<Post> {
            override fun onResponse(call: Call<Post>, response: Response<Post>) {
                if (!response.isSuccessful || response.body() == null) {
                    updateStatus("Search failed: HTTP ${response.code()}")
                    return
                }
                val result = response.body()!!
                updateStatus("POST query returned post id=${result.id}.")
            }

            override fun onFailure(call: Call<Post>, t: Throwable) {
                updateStatus("Search failed: ${t.message}")
            }
        })
    }

    private fun refreshPostsViaGet() {
        if (transportState.mode == TransportMode.KTOR) {
            refreshPostsKtor()
            return
        }
        updateStatus("Refreshing posts with GET @CacheMutate...")
        api.refreshPosts(1).enqueue(object : Callback<Post> {
            override fun onResponse(call: Call<Post>, response: Response<Post>) {
                if (!response.isSuccessful || response.body() == null) {
                    updateStatus("Refresh failed: HTTP ${response.code()}")
                    return
                }
                val result = response.body()!!
                updateStatus(
                    "GET mutation returned post id=${result.id} and marked keys dirty.",
                )
            }

            override fun onFailure(call: Call<Post>, t: Throwable) {
                updateStatus("Refresh failed: ${t.message}")
            }
        })
    }

    private fun toggleTransport() {
        transportState.toggle()
        updateStatus("Switched transport to ${transportLabel()}")
    }

    private fun loadPostsKtor() {
        lifecycleScope.launch {
            val metadata = RetrostashKtorMetadata(
                scopeName = "ExampleApi",
                queryTemplate = "posts?userId={userId}",
                bindings = mapOf("userId" to "1"),
                maxAgeMs = 60_000L,
            )

            val cached = ktorRuntime.resolveFromCache(metadata)
            if (cached != null) {
                val listType = object : TypeToken<List<Post>>() {}.type
                val posts: List<Post> = gson.fromJson(String(cached), listType)
                val firstTitle = posts.firstOrNull()?.title ?: "<none>"
                updateStatus("[Ktor cache] Loaded ${posts.size} posts. First title: $firstTitle")
                return@launch
            }

            runCatching {
                val body = ktorClient.get("https://jsonplaceholder.typicode.com/posts") {
                    parameter("userId", 1)
                }.bodyAsText()
                ktorRuntime.persistQueryResult(metadata, body.encodeToByteArray())
                val listType = object : TypeToken<List<Post>>() {}.type
                val posts: List<Post> = gson.fromJson(body, listType)
                val firstTitle = posts.firstOrNull()?.title ?: "<none>"
                updateStatus("[Ktor network] Loaded ${posts.size} posts. First title: $firstTitle")
            }.onFailure {
                updateStatus("Ktor load failed: ${it.message}")
            }
        }
    }

    private fun createPostKtor() {
        lifecycleScope.launch {
            val request = CreatePostRequest(1, "Retrostash", "Mutation that invalidates query key")
            runCatching {
                val body = ktorClient.post("https://jsonplaceholder.typicode.com/posts") {
                    contentType(ContentType.Application.Json)
                    setBody(gson.toJson(request))
                }.bodyAsText()
                ktorRuntime.invalidate(
                    RetrostashKtorMetadata(
                        scopeName = "ExampleApi",
                        bindings = mapOf("userId" to request.userId.toString()),
                        invalidateTemplates = listOf(
                            "posts?userId=1",
                            "posts/search?userId=1",
                        ),
                    )
                )
                val created = gson.fromJson(body, Post::class.java)
                updateStatus("[Ktor] Created post id=${created.id}. Query key was marked dirty.")
            }.onFailure {
                updateStatus("Ktor create failed: ${it.message}")
            }
        }
    }

    private fun searchPostsKtor() {
        lifecycleScope.launch {
            val request = CreatePostRequest(1, "Retrostash Query", "POST query example")
            val metadata = RetrostashKtorMetadata(
                scopeName = "ExampleApi",
                queryTemplate = "posts/search?userId={userId}",
                bindings = mapOf("userId" to request.userId.toString()),
                bodyBytes = gson.toJson(request).encodeToByteArray(),
                maxAgeMs = 60_000L,
            )

            val cached = ktorRuntime.resolveFromCache(metadata)
            if (cached != null) {
                val result = gson.fromJson(String(cached), Post::class.java)
                updateStatus("[Ktor cache] POST query returned post id=${result.id}.")
                return@launch
            }

            runCatching {
                val body = ktorClient.post("https://jsonplaceholder.typicode.com/posts") {
                    contentType(ContentType.Application.Json)
                    setBody(gson.toJson(request))
                }.bodyAsText()
                ktorRuntime.persistQueryResult(metadata, body.encodeToByteArray())
                val result = gson.fromJson(body, Post::class.java)
                updateStatus("[Ktor network] POST query returned post id=${result.id}.")
            }.onFailure {
                updateStatus("Ktor search failed: ${it.message}")
            }
        }
    }

    private fun refreshPostsKtor() {
        lifecycleScope.launch {
            runCatching {
                val body = ktorClient.get("https://jsonplaceholder.typicode.com/posts/1") {
                    parameter("userId", 1)
                }.bodyAsText()
                ktorRuntime.invalidate(
                    RetrostashKtorMetadata(
                        scopeName = "ExampleApi",
                        bindings = mapOf("userId" to "1"),
                        invalidateTemplates = listOf(
                            "posts?userId=1",
                            "posts/search?userId=1",
                        ),
                    )
                )
                val result = gson.fromJson(body, Post::class.java)
                updateStatus("[Ktor] GET mutation returned post id=${result.id} and marked keys dirty.")
            }.onFailure {
                updateStatus("Ktor refresh failed: ${it.message}")
            }
        }
    }

    private fun updateStatus(message: String) {
        currentStatus = message
        renderStatus()
    }

    private fun recordEvent(message: String) {
        Log.d("RetrostashSample", message)
        statusText.post {
            synchronized(eventLog) {
                eventLog.addLast(message)
                while (eventLog.size > 5) {
                    eventLog.removeFirst()
                }
            }
            renderStatus()
        }
    }

    private fun renderStatus() {
        val transport = transportLabel()
        val recentEvents = synchronized(eventLog) { eventLog.toList() }
        switchTransportButton.text = transport
        statusText.text = buildString {
            append(transport)
            append("\n\n")
            append(currentStatus)
            if (recentEvents.isNotEmpty()) {
                append("\n\nRecent events:\n")
                recentEvents.forEach { event ->
                    append("- ")
                    append(event)
                    append('\n')
                }
            }
        }.trimEnd()
    }

    private fun transportLabel(): String = when (transportState.mode) {
        TransportMode.OKHTTP -> getString(R.string.transport_okhttp)
        TransportMode.KTOR -> getString(R.string.transport_ktor)
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching { ktorClient.close() }
    }

    interface ExampleApi {
        @CacheQuery(key = "posts?userId={userId}")
        @GET("posts")
        fun getPosts(@Query("userId") userId: Int): Call<List<Post>>

        @CacheQuery(key = "posts/search?userId={userId}")
        @POST("posts")
        fun searchPosts(@Body request: CreatePostRequest): Call<Post>

        @CacheMutate(
            invalidate = [
                "posts?userId={userId}",
                "posts/search?userId={userId}",
            ]
        )
        @POST("posts")
        fun createPost(@Body request: CreatePostRequest): Call<Post>

        @CacheMutate(
            invalidate = [
                "posts?userId={userId}",
                "posts/search?userId={userId}",
            ]
        )
        @GET("posts/1")
        fun refreshPosts(@Query("userId") userId: Int): Call<Post>
    }

    data class CreatePostRequest(
        @SerializedName("userId") val userId: Int,
        @SerializedName("title") val title: String,
        @SerializedName("body") val body: String,
    )

    data class Post(
        @SerializedName("id") val id: Int = 0,
        @SerializedName("userId") val userId: Int = 0,
        @SerializedName("title") val title: String = "",
        @SerializedName("body") val body: String = "",
    )
}
