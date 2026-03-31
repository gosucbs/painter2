package com.painterai.app.domain.model

data class Recipe(
    val id: String = "",
    val jobId: String = "",
    val type: RecipeType = RecipeType.STD,
    val toners: List<Toner> = emptyList(),
    val totalGrams: Double = 0.0,
    val source: String? = null
)

enum class RecipeType(val displayName: String) {
    STD("STD (표준)"),
    SAMPLE("조색 시편");

    companion object {
        fun fromString(value: String): RecipeType =
            entries.find { it.name.equals(value, ignoreCase = true) } ?: STD
    }
}

data class Toner(
    val code: String = "",
    val grams: Double = 0.0
) {
    fun toDisplayString(): String = "$code: ${String.format("%.2f", grams)}g"
}
