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
    object Movies : Screen("movies/{libraryId}") {
        fun createRoute(libraryId: String) = "movies/$libraryId"
    }
    object Music : Screen("music/{libraryId}") {
        fun createRoute(libraryId: String) = "music/$libraryId"
    }
    object Artists : Screen("artists/{libraryId}") {
        fun createRoute(libraryId: String) = "artists/$libraryId"
    }
    object Albums : Screen("albums/{artistId}") {
        fun createRoute(artistId: String) = "albums/$artistId"
    }
    object Songs : Screen("songs/{albumId}") {
        fun createRoute(albumId: String) = "songs/$albumId"
    }
    object GeneralMedia : Screen("general/{libraryId}") {
        fun createRoute(libraryId: String) = "general/$libraryId"
    }
}
