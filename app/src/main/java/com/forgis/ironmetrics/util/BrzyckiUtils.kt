package com.forgis.ironmetrics.util

object BrzyckiUtils {

    /** 1RM teórico (Brzycki): peso / (1.0278 - 0.0278 * reps). Válido para reps en 1..36. */
    fun calculateTheoreticalOneRm(weight: Double, reps: Int): Double {
        require(weight >= 0.0) { "weight must be >= 0" }
        require(reps in 1..36) { "reps must be between 1 and 36" }
        return weight / (1.0278 - (0.0278 * reps))
    }

    /** Pares (porcentaje, peso) desde 100% hasta 50% en pasos de 5%. */
    fun percentageTable(oneRm: Double): List<Pair<Int, Double>> =
        (100 downTo 50 step 5).map { percent -> percent to oneRm * percent / 100.0 }
}
