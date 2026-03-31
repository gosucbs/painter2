package com.painterai.app.data.repository

import com.painterai.app.data.remote.dto.JobDto
import com.painterai.app.domain.model.Job
import com.painterai.app.domain.model.JobResult
import com.painterai.app.domain.model.PaintBrand
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JobRepository @Inject constructor(
    private val postgrest: Postgrest
) {
    suspend fun getJobs(): Result<List<Job>> {
        return try {
            val result = postgrest.from("jobs")
                .select {
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<JobDto>()
            Result.success(result.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getJob(id: String): Result<Job> {
        return try {
            val result = postgrest.from("jobs")
                .select {
                    filter { eq("id", id) }
                }
                .decodeSingle<JobDto>()
            Result.success(result.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createJob(job: JobDto): Result<Job> {
        return try {
            val result = postgrest.from("jobs")
                .insert(job) {
                    select()
                }
                .decodeSingle<JobDto>()
            Result.success(result.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteJob(jobId: String): Result<Unit> {
        return try {
            postgrest.from("jobs")
                .delete {
                    filter { eq("id", jobId) }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateJobResult(jobId: String, result: String): Result<Unit> {
        return try {
            postgrest.from("jobs")
                .update({
                    set("result", result)
                    set("updated_at", Instant.now().toString())
                }) {
                    filter { eq("id", jobId) }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchByColorCode(colorCode: String): Result<List<Job>> {
        return try {
            val result = postgrest.from("jobs")
                .select {
                    filter { eq("color_code", colorCode) }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<JobDto>()
            Result.success(result.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun JobDto.toDomain() = Job(
        id = id ?: "",
        userId = userId ?: "",
        vehicleModel = vehicleModel,
        vehicleYear = vehicleYear,
        colorCode = colorCode,
        paintBrand = PaintBrand.fromString(paintBrand),
        workArea = workArea,
        result = JobResult.fromString(result),
        notes = notes,
        createdAt = createdAt?.let { Instant.parse(it) } ?: Instant.now(),
        updatedAt = updatedAt?.let { Instant.parse(it) } ?: Instant.now()
    )
}
