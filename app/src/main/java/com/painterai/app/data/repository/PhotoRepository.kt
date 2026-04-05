package com.painterai.app.data.repository

import android.content.Context
import android.net.Uri
import com.painterai.app.data.remote.dto.PhotoDto
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.upload
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotoRepository @Inject constructor(
    private val postgrest: Postgrest,
    private val storage: Storage
) {
    suspend fun uploadPhoto(
        context: Context,
        jobId: String,
        userId: String,
        uri: Uri,
        type: String,
        angle: String? = null
    ): Result<String> {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return Result.failure(Exception("Cannot open file"))
            val bytes = inputStream.readBytes()
            inputStream.close()

            val fileName = "${type}_${angle ?: "full"}_${System.currentTimeMillis()}.jpg"
            val path = "$userId/$jobId/$fileName"

            storage.from("photos").upload(path, bytes) {
                upsert = false
            }

            // Save photo record
            val photoDto = PhotoDto(
                jobId = jobId,
                type = type,
                storagePath = path,
                angle = angle
            )
            postgrest.from("photos").insert(photoDto)

            Result.success(path)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPhotoUrl(path: String): String {
        return storage.from("photos").publicUrl(path)
    }

    suspend fun getPhotosForJob(jobId: String): Result<List<PhotoDto>> {
        return try {
            val result = postgrest.from("photos")
                .select {
                    filter { eq("job_id", jobId) }
                }
                .decodeList<PhotoDto>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
