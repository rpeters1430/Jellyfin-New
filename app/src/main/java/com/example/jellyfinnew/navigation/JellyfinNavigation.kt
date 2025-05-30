package com.example.jellyfinnew.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.jellyfinnew.ui.home.HomeScreen
import com.example.jellyfinnew.ui.home.HomeViewModel
import com.example.jellyfinnew.ui.login.LoginScreen
import com.example.jellyfinnew.ui.login.LoginViewModel
import com.example.jellyfinnew.ui.player.PlayerScreen
import com.example.jellyfinnew.ui.tvshows.TvShowsScreen
import com.example.jellyfinnew.ui.tvshows.TvShowsViewModel
import com.example.jellyfinnew.ui.tvshows.TvSeasonsScreen
import com.example.jellyfinnew.ui.tvshows.TvSeasonsViewModel
import com.example.jellyfinnew.ui.tvshows.TvEpisodesScreen
import com.example.jellyfinnew.ui.tvshows.TvEpisodesViewModel
import com.example.jellyfinnew.ui.movies.MoviesScreen
import com.example.jellyfinnew.ui.movies.MoviesViewModel
import com.example.jellyfinnew.ui.music.ArtistsScreen
import com.example.jellyfinnew.ui.music.AlbumsScreen
import com.example.jellyfinnew.ui.music.SongsScreen
import com.example.jellyfinnew.ui.music.MusicViewModel
import com.example.jellyfinnew.ui.general.GeneralMediaScreen
import com.example.jellyfinnew.ui.general.GeneralMediaViewModel
import com.example.jellyfinnew.di.ServiceLocator
import androidx.tv.material3.ExperimentalTvMaterial3Api

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun JellyfinNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { ServiceLocator.provideRepository(context) }
    
    // Check connection state to determine start destination
    val connectionState by repository.connectionState.collectAsState()
    val startDestination = if (connectionState.isConnected) Screen.Home.route else Screen.Login.route

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Login.route) {
            val viewModel: LoginViewModel = viewModel()
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }            )
        }

        composable(Screen.Home.route) {
            val viewModel: HomeViewModel = viewModel()
            val loginViewModel: LoginViewModel = viewModel()
            HomeScreen(
                viewModel = viewModel,
                onPlayMedia = { itemId: String ->
                    navController.navigate(Screen.Player.createRoute(itemId))
                },
                onDisconnect = {
                    loginViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.TvShows.route,
            arguments = listOf(navArgument("libraryId") { type = NavType.StringType })
        ) { backStackEntry ->
            val libraryId = backStackEntry.arguments?.getString("libraryId") ?: return@composable
            val viewModel: TvShowsViewModel = viewModel()
            viewModel.setLibraryId(libraryId)
            TvShowsScreen(
                viewModel = viewModel,
                onSeriesClick = { seriesId ->
                    navController.navigate(Screen.TvSeasons.createRoute(seriesId))
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.TvSeasons.route,
            arguments = listOf(navArgument("seriesId") { type = NavType.StringType })
        ) { backStackEntry ->
            val seriesId = backStackEntry.arguments?.getString("seriesId") ?: return@composable
            val viewModel: TvSeasonsViewModel = viewModel()
            viewModel.setSeriesId(seriesId)
            TvSeasonsScreen(
                viewModel = viewModel,
                onSeasonClick = { _, seasonId ->
                    navController.navigate(Screen.TvEpisodes.createRoute(seriesId, seasonId))
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.TvEpisodes.route,
            arguments = listOf(
                navArgument("seriesId") { type = NavType.StringType },
                navArgument("seasonId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val seriesId = backStackEntry.arguments?.getString("seriesId") ?: return@composable
            val seasonId = backStackEntry.arguments?.getString("seasonId") ?: return@composable
            val viewModel: TvEpisodesViewModel = viewModel()
            viewModel.setSeriesAndSeasonId(seriesId, seasonId)
            TvEpisodesScreen(
                viewModel = viewModel,
                onEpisodeClick = { episodeId ->
                    navController.navigate(Screen.Player.createRoute(episodeId))
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // Movies screen with proper ViewModel factory
        composable(
            route = Screen.Movies.route,
            arguments = listOf(navArgument("libraryId") { type = NavType.StringType })
        ) { backStackEntry ->
            val libraryId = backStackEntry.arguments?.getString("libraryId") ?: return@composable
            val viewModelFactory = remember { ServiceLocator.provideMoviesViewModelFactory(context) }
            val viewModel: MoviesViewModel = viewModel(factory = viewModelFactory)

            MoviesScreen(
                viewModel = viewModel,
                libraryId = libraryId,
                onMovieClick = { movieId ->
                    navController.navigate(Screen.Player.createRoute(movieId))
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // Music screen with proper ViewModel factory
        composable(
            route = Screen.Music.route,
            arguments = listOf(navArgument("libraryId") { type = NavType.StringType })
        ) { backStackEntry ->
            val libraryId = backStackEntry.arguments?.getString("libraryId") ?: return@composable
            val viewModelFactory = remember { ServiceLocator.provideMusicViewModelFactory(context) }
            val viewModel: MusicViewModel = viewModel(factory = viewModelFactory)

            ArtistsScreen(
                viewModel = viewModel,
                libraryId = libraryId,
                onArtistClick = { artistId ->
                    navController.navigate(Screen.Albums.createRoute(artistId))
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // Albums screen
        composable(
            route = Screen.Albums.route,
            arguments = listOf(navArgument("artistId") { type = NavType.StringType })
        ) { backStackEntry ->
            val artistId = backStackEntry.arguments?.getString("artistId") ?: return@composable
            val viewModelFactory = remember { ServiceLocator.provideMusicViewModelFactory(context) }
            val viewModel: MusicViewModel = viewModel(factory = viewModelFactory)

            AlbumsScreen(
                viewModel = viewModel,
                artistId = artistId,
                onAlbumClick = { albumId ->
                    navController.navigate(Screen.Songs.createRoute(albumId))
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // Songs screen
        composable(
            route = Screen.Songs.route,
            arguments = listOf(navArgument("albumId") { type = NavType.StringType })
        ) { backStackEntry ->
            val albumId = backStackEntry.arguments?.getString("albumId") ?: return@composable
            val viewModelFactory = remember { ServiceLocator.provideMusicViewModelFactory(context) }
            val viewModel: MusicViewModel = viewModel(factory = viewModelFactory)

            SongsScreen(
                viewModel = viewModel,
                albumId = albumId,
                onSongClick = { songId ->
                    navController.navigate(Screen.Player.createRoute(songId))
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // General Media screen
        composable(
            route = Screen.GeneralMedia.route,
            arguments = listOf(navArgument("libraryId") { type = NavType.StringType })
        ) { backStackEntry ->
            val libraryId = backStackEntry.arguments?.getString("libraryId") ?: return@composable
            val viewModel: GeneralMediaViewModel = viewModel()
            GeneralMediaScreen(
                viewModel = viewModel,
                libraryId = libraryId,
                onItemClick = { itemId ->
                    navController.navigate(Screen.Player.createRoute(itemId))
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.Player.route,
            arguments = listOf(navArgument("itemId") { type = NavType.StringType })
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId") ?: return@composable
            // Fix memory leak: use remember with backStackEntry as key
            val homeEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.Home.route)
            }
            val homeViewModel: HomeViewModel = viewModel(
                viewModelStoreOwner = homeEntry
            )

            val streamUrl = homeViewModel.getStreamUrl(itemId)
            if (streamUrl != null) {
                PlayerScreen(
                    streamUrl = streamUrl,
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}