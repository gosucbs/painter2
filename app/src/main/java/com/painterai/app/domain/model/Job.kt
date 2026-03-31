package com.painterai.app.domain.model

import kotlinx.serialization.Serializable
import java.time.Instant

data class Job(
    val id: String = "",
    val userId: String = "",
    val vehicleModel: String = "",
    val vehicleYear: Int? = null,
    val colorCode: String = "",
    val paintBrand: PaintBrand = PaintBrand.KCC_SUMIX,
    val workArea: String? = null,
    val result: JobResult = JobResult.IN_PROGRESS,
    val notes: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)

enum class PaintBrand(val displayName: String) {
    KCC_SUMIX("KCC 수믹스");

    companion object {
        fun fromString(value: String): PaintBrand =
            entries.find { it.name == value } ?: KCC_SUMIX
    }
}

enum class JobResult(val displayName: String) {
    IN_PROGRESS("진행중"),
    SUCCESS("성공"),
    FAIL("실패");

    companion object {
        fun fromString(value: String): JobResult =
            entries.find { it.name.equals(value, ignoreCase = true) } ?: IN_PROGRESS
    }
}
