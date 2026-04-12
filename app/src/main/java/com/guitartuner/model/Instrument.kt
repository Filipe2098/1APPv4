package com.guitartuner.model

/**
 * Supported instrument categories for V4.
 *
 * Each instrument has:
 *  - a localization key for display
 *  - a list of available string counts
 *  - a map of stringCount -> list of note names (low to high)
 *  - a highPrecision flag: bowed instruments want ±20 cents range + finer detection
 */
enum class InstrumentType(
    val id: String,
    val stringCountOptions: List<Int>,
    val defaultStringCount: Int,
    val tunings: Map<Int, List<String>>,
    val highPrecision: Boolean
) {
    GUITARRA(
        id = "guitar",
        stringCountOptions = listOf(6, 7, 8),
        defaultStringCount = 6,
        tunings = mapOf(
            6 to listOf("E2", "A2", "D3", "G3", "B3", "E4"),
            7 to listOf("B1", "E2", "A2", "D3", "G3", "B3", "E4"),
            8 to listOf("F#1", "B1", "E2", "A2", "D3", "G3", "B3", "E4")
        ),
        highPrecision = false
    ),
    BAIXO(
        id = "bass",
        stringCountOptions = listOf(4, 5, 6),
        defaultStringCount = 4,
        tunings = mapOf(
            4 to listOf("E1", "A1", "D2", "G2"),
            5 to listOf("B0", "E1", "A1", "D2", "G2"),
            6 to listOf("B0", "E1", "A1", "D2", "G2", "C3")
        ),
        highPrecision = false
    ),
    VIOLINO(
        id = "violin",
        stringCountOptions = listOf(4, 5),
        defaultStringCount = 4,
        tunings = mapOf(
            4 to listOf("G3", "D4", "A4", "E5"),
            5 to listOf("C3", "G3", "D4", "A4", "E5")
        ),
        highPrecision = true
    ),
    VIOLA(
        id = "viola",
        stringCountOptions = listOf(4, 5),
        defaultStringCount = 4,
        tunings = mapOf(
            4 to listOf("C3", "G3", "D4", "A4"),
            5 to listOf("C3", "G3", "D4", "A4", "E5")
        ),
        highPrecision = true
    ),
    VIOLONCELO(
        id = "cello",
        stringCountOptions = listOf(4, 5),
        defaultStringCount = 4,
        tunings = mapOf(
            4 to listOf("C2", "G2", "D3", "A3"),
            5 to listOf("C2", "G2", "D3", "A3", "E4")
        ),
        highPrecision = true
    ),
    CONTRABAIXO(
        id = "doublebass",
        stringCountOptions = listOf(4, 5),
        defaultStringCount = 4,
        tunings = mapOf(
            4 to listOf("E1", "A1", "D2", "G2"),
            5 to listOf("B0", "E1", "A1", "D2", "G2")
        ),
        highPrecision = true
    );

    fun tuningFor(stringCount: Int): List<String> {
        val clamped = if (stringCount in stringCountOptions) stringCount else defaultStringCount
        return tunings[clamped] ?: tunings.getValue(defaultStringCount)
    }
}
