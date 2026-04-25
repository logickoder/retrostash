package dev.logickoder.retrostash.example.domain

import dev.logickoder.retrostash.CacheMutate
import dev.logickoder.retrostash.CacheQuery
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

interface RetrofitPostsApi {

    @CacheQuery("posts/{id}", maxAgeSeconds = 60)
    @GET("posts/{id}")
    suspend fun getPost(@Path("id") id: Int): Response<String>

    @CacheMutate(invalidate = ["posts/{id}"])
    @PUT("posts/{id}")
    suspend fun updatePost(@Path("id") id: Int, @Body body: RequestBody): Response<String>
}
