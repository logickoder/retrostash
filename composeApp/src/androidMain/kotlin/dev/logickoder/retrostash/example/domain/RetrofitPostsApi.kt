package dev.logickoder.retrostash.example.domain

import dev.logickoder.retrostash.CacheMutate
import dev.logickoder.retrostash.CacheQuery
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Streaming

interface RetrofitPostsApi {

    @CacheQuery("posts/{id}", maxAgeSeconds = 60, tags = ["post:{id}"])
    @GET("posts/{id}")
    @Streaming
    suspend fun getPost(@Path("id") id: Int): Response<ResponseBody>

    @CacheMutate(invalidate = ["posts/{id}"], invalidateTags = ["post:{id}"])
    @PUT("posts/{id}")
    @Streaming
    suspend fun updatePost(@Path("id") id: Int, @Body body: RequestBody): Response<ResponseBody>
}
