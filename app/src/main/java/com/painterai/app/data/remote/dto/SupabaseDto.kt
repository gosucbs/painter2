package com.painterai.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JobDto(
    val id: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("vehicle_model") val vehicleModel: String,
    @SerialName("vehicle_year") val vehicleYear: Int? = null,
    @SerialName("color_code") val colorCode: String,
    @SerialName("paint_brand") val paintBrand: String = "KCC_SUMIX",
    @SerialName("work_area") val workArea: String? = null,
    val result: String = "in_progress",
    val notes: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class RecipeDto(
    val id: String? = null,
    @SerialName("job_id") val jobId: String,
    val type: String,
    val toners: String, // JSON string: [{"code":"K9001","grams":57.40}]
    @SerialName("total_grams") val totalGrams: Double? = null,
    val source: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class PhotoDto(
    val id: String? = null,
    @SerialName("job_id") val jobId: String,
    val type: String,
    @SerialName("storage_path") val storagePath: String,
    val angle: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class ConversationDto(
    val id: String? = null,
    @SerialName("job_id") val jobId: String,
    val messages: String = "[]", // JSON string
    @SerialName("analysis_summary") val analysisSummary: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class TonerJson(
    val code: String,
    val grams: Double
)
