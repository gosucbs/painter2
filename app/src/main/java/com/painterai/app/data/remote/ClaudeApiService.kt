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
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
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
                    val errorMsg = when (response.code) {
                        429 -> "AI 요청이 너무 많습니다. 잠시 후 다시 시도해주세요."
                        529 -> "현재 Claude AI 서버 트래픽이 많습니다.\n잠시 후 (1~2분) 다시 시도해주세요."
                        500 -> "AI 서버 내부 오류입니다. 잠시 후 다시 시도해주세요."
                        401 -> "API 인증 오류입니다. 관리자에게 문의해주세요."
                        400 -> "요청 형식 오류: $body"
                        else -> "서버 오류 (${response.code}). 잠시 후 다시 시도해주세요."
                    }
                    return@withContext Result.failure(Exception(errorMsg))
                }

                val parsed = json.decodeFromString(AnalyzeResponse.serializer(), body)
                if (parsed.error != null) {
                    val msg = when {
                        parsed.error.contains("Overloaded", ignoreCase = true) ->
                            "현재 Claude AI 서버 트래픽이 많습니다.\n잠시 후 (1~2분) 다시 시도해주세요."
                        else -> "AI 오류: ${parsed.error}"
                    }
                    Result.failure(Exception(msg))
                } else {
                    Result.success(parsed)
                }
            } catch (e: java.net.UnknownHostException) {
                Result.failure(Exception("인터넷 연결을 확인해주세요."))
            } catch (e: java.net.SocketTimeoutException) {
                Result.failure(Exception("서버 응답 시간이 초과되었습니다. 다시 시도해주세요."))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
