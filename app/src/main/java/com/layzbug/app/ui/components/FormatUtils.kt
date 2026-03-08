package com.layzbug.app.ui.components

fun formatDistance(km: Double): String {
    return if (km >= 10) {
        "${Math.round(km)} km"
    } else if (km >= 0.1) {
        "${"%.1f".format(km)} km"
    } else {
        "0 km"
    }
}
