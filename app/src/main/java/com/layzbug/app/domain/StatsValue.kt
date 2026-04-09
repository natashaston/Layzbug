package com.layzbug.app.domain

data class StatsValue(
    val value: Int,
    val label: String,
    val distanceKm: Double = 0.0,
    val totalMinutes: Long = 0L
)