package com.layzbug.app.ui.navigation

sealed class Routes(val route: String) {
    object Splash : Routes("splash")
    object Permission : Routes("permission")
    object Home : Routes("home")
    object History : Routes("history")
    object MonthDetail : Routes("details/{year}/{month}") {
        fun createRoute(year: Int, month: Int) = "details/$year/$month"
    }
}