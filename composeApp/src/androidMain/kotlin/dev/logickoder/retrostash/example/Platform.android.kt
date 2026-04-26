package dev.logickoder.retrostash.example

import dev.logickoder.retrostash.core.RetrostashStore
import dev.logickoder.retrostash.example.domain.OkHttpDemoEngine
import dev.logickoder.retrostash.example.domain.RetrofitDemoEngine
import dev.logickoder.retrostash.example.domain.RetrofitPostsApi
import dev.logickoder.retrostash.okhttp.RetrostashOkHttpBridge
import dev.logickoder.retrostash.okhttp.RetrostashOkHttpConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.OkHttpClient
import retrofit2.Retrofit

actual object Platform {
    actual val name: String = "Android"

    actual val ktorEngine: HttpClientEngineFactory<*> = OkHttp

    actual fun createOkHttpEngine(
        store: RetrostashStore,
        onLog: (String) -> Unit,
    ): DemoEngine? {
        val bridge = RetrostashOkHttpBridge(
            store = store,
            config = RetrostashOkHttpConfig(logger = onLog),
        )
        val client = OkHttpClient.Builder().also(bridge::install).build()
        return OkHttpDemoEngine(client = client, onLog = onLog)
    }

    actual fun createRetrofitEngine(
        store: RetrostashStore,
        onLog: (String) -> Unit,
    ): DemoEngine? {
        val bridge = RetrostashOkHttpBridge(
            store = store,
            config = RetrostashOkHttpConfig(logger = onLog),
        )
        val client = OkHttpClient.Builder().also(bridge::install).build()
        val retrofit = Retrofit.Builder()
            .baseUrl("https://jsonplaceholder.typicode.com/")
            .client(client)
            .build()
        val api = retrofit.create(RetrofitPostsApi::class.java)
        return RetrofitDemoEngine(api = api, client = client, onLog = onLog)
    }
}
