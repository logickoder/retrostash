@file:SuppressLint("SetTextI18n")

package dev.logickoder.retrostash.example

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import dev.logickoder.retrostash.CacheMutate
import dev.logickoder.retrostash.CacheQuery
import dev.logickoder.retrostash.Retrostash
import dev.logickoder.retrostash.RetrostashConfig
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var api: ExampleApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        val loadPostsButton: Button = findViewById(R.id.loadPostsButton)
        val createPostButton: Button = findViewById(R.id.createPostButton)
        val searchPostsButton: Button = findViewById(R.id.searchPostsButton)
        val refreshPostsButton: Button = findViewById(R.id.refreshPostsButton)

        val okHttpBuilder = OkHttpClient.Builder()
        Retrostash.install(okHttpBuilder, applicationContext, RetrostashConfig())

        val retrofit = Retrofit.Builder()
            .baseUrl("https://jsonplaceholder.typicode.com/")
            .client(okHttpBuilder.build())
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
            .build()

        api = retrofit.create(ExampleApi::class.java)

        loadPostsButton.setOnClickListener { loadPosts() }
        createPostButton.setOnClickListener { createPost() }
        searchPostsButton.setOnClickListener { searchPostsViaPost() }
        refreshPostsButton.setOnClickListener { refreshPostsViaGet() }
    }

    private fun loadPosts() {
        statusText.text = "Loading posts for userId=1..."
        api.getPosts(1).enqueue(object : Callback<List<Post>> {
            override fun onResponse(call: Call<List<Post>>, response: Response<List<Post>>) {
                if (!response.isSuccessful || response.body() == null) {
                    statusText.text = "Load failed: HTTP ${response.code()}"
                    return
                }
                val posts = response.body().orEmpty()
                val firstTitle = posts.firstOrNull()?.title ?: "<none>"
                statusText.text = "Loaded ${posts.size} posts. First title: $firstTitle"
            }

            override fun onFailure(call: Call<List<Post>>, t: Throwable) {
                statusText.text = "Load failed: ${t.message}"
            }
        })
    }

    private fun createPost() {
        statusText.text = "Creating post for userId=1..."
        val request = CreatePostRequest(1, "Retrostash", "Mutation that invalidates query key")
        api.createPost(request).enqueue(object : Callback<Post> {
            override fun onResponse(call: Call<Post>, response: Response<Post>) {
                if (!response.isSuccessful || response.body() == null) {
                    statusText.text = "Create failed: HTTP ${response.code()}"
                    return
                }
                val created = response.body()!!
                statusText.text =
                    "Created post id=${created.id}. Query key for userId=1 was marked dirty."
            }

            override fun onFailure(call: Call<Post>, t: Throwable) {
                statusText.text = "Create failed: ${t.message}"
            }
        })
    }

    private fun searchPostsViaPost() {
        statusText.text = "Searching posts with POST @CacheQuery..."
        val request = CreatePostRequest(1, "Retrostash Query", "POST query example")
        api.searchPosts(request).enqueue(object : Callback<Post> {
            override fun onResponse(call: Call<Post>, response: Response<Post>) {
                if (!response.isSuccessful || response.body() == null) {
                    statusText.text = "Search failed: HTTP ${response.code()}"
                    return
                }
                val result = response.body()!!
                statusText.text = "POST query returned post id=${result.id}."
            }

            override fun onFailure(call: Call<Post>, t: Throwable) {
                statusText.text = "Search failed: ${t.message}"
            }
        })
    }

    private fun refreshPostsViaGet() {
        statusText.text = "Refreshing posts with GET @CacheMutate..."
        api.refreshPosts(1).enqueue(object : Callback<Post> {
            override fun onResponse(call: Call<Post>, response: Response<Post>) {
                if (!response.isSuccessful || response.body() == null) {
                    statusText.text = "Refresh failed: HTTP ${response.code()}"
                    return
                }
                val result = response.body()!!
                statusText.text =
                    "GET mutation returned post id=${result.id} and marked keys dirty."
            }

            override fun onFailure(call: Call<Post>, t: Throwable) {
                statusText.text = "Refresh failed: ${t.message}"
            }
        })
    }

    interface ExampleApi {
        @CacheQuery(key = "posts?userId={userId}")
        @GET("posts")
        fun getPosts(@Query("userId") userId: Int): Call<List<Post>>

        @CacheQuery(key = "posts/search?userId={userId}")
        @POST("posts")
        fun searchPosts(@Body request: CreatePostRequest): Call<Post>

        @CacheMutate(invalidate = ["posts?userId={userId}"])
        @POST("posts")
        fun createPost(@Body request: CreatePostRequest): Call<Post>

        @CacheMutate(invalidate = ["posts?userId={userId}"])
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
