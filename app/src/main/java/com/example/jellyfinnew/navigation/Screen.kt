package com.example.jellyfinnew.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Home : Screen("home")
    object Player : Screen("player/{itemId}") {
        fun createRoute(itemId: String) = "player/$itemId"
    }
    object TvShows : Screen("tvshows/{libraryId}") {
        fun createRoute(libraryId: String) = "tvshows/$libraryId"
    }
    object TvSeasons : Screen("tvseasons/{seriesId}") {
        fun createRoute(seriesId: String) = "tvseasons/$seriesId"
    }
    object TvEpisodes : Screen("tvepisodes/{seriesId}/{seasonId}") {
        fun createRoute(seriesId: String, seasonId: String) = "tvepisodes/$seriesId/$seasonId"
    }
}
