package com.painterai.app.data.remote

import com.painterai.app.BuildConfig
import com.painterai.app.data.remote.dto.AnalyzeRequest
import com.painterai.app.data.remote.dto.AnalyzeResponse
import io.github.jan.supabase.auth.Auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClaudeApiService @Inject constructor(
    private val auth: Auth
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun analyzeColor(request: AnalyzeRequest): Result<AnalyzeResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val token = auth.currentAccessTokenOrNull()

                val requestBody = json.encodeToString(AnalyzeRequest.serializer(), request)
                    .toRequestBody("application/json".toMediaType())

                val authHeader = if (token != null) "Bearer $token" else "Bearer ${BuildConfig.SUPABASE_ANON_KEY}"

                val httpRequest = Request.Builder()
                    .url("${BuildConfig.SUPABASE_URL}/functions/v1/analyze-color")
                    .post(requestBody)
                    .addHeader("Authorization", authHeader)
                    .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                    .build()

                val response = client.newCall(httpRequest).execute()
                val body = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("서버 오류: ${response.code}"))
                }

                val parsed = json.decodeFromString(AnalyzeResponse.serializer(), body)
                if (parsed.error != null) {
                    Result.failure(Exception(parsed.error))
                } else {
                    Result.success(parsed)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
