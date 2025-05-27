package com.example.jellyfinnew.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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

@Composable
fun JellyfinNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route,
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
                }
            )
        }
        
        composable(Screen.Home.route) {
            val viewModel: HomeViewModel = viewModel()
            val loginViewModel: LoginViewModel = viewModel()
            HomeScreen(
                viewModel = viewModel,
                onPlayMedia = { itemId: String ->
                    navController.navigate(Screen.Player.createRoute(itemId))
                },
                onNavigateToTvShows = { libraryId: String ->
                    navController.navigate(Screen.TvShows.createRoute(libraryId))
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
