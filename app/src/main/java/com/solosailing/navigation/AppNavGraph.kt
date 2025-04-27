// En com/solosailing/navigation/AppNavGraph.kt
package com.solosailing.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.solosailing.presentation.home.HomeScreen
import com.solosailing.presentation.login.LoginScreen
import com.solosailing.presentation.login.RegisterScreen
// import com.solosailing.ui.components.audio.SoundTestScreen
import com.solosailing.presentation.map.MapScreen
import com.solosailing.presentation.regatta.LiveRegattaScreen
import com.solosailing.presentation.regatta.PastRegattasScreen
import com.solosailing.viewModel.TrackingViewModel

@Composable
fun AppNavGraph() {
    val navController: NavHostController = rememberNavController()
    // Usa la constante para el startDestination
    NavHost(navController = navController, startDestination = Routes.HOME) {

        composable(Routes.HOME) {
            HomeScreen(navController)
        }
        composable(Routes.LOGIN) {
            LoginScreen(navController)
        }
        composable(Routes.REGISTER) {
            RegisterScreen(navController)
        }
        composable(Routes.MAP) {
            MapScreen(navController)
//            val tracking: TrackingViewModel = hiltViewModel()
//            val active = tracking.trackingActive.collectAsState()
//            if (active.value) {
//                MapScreen(navController)
//            } else {
//                LaunchedEffect(Unit) { navController.popBackStack() }
//            }
        }
        composable(Routes.REGATTA_SIMULATION) {
            //RegattaSimulationScreen(navController)
        }
        composable(Routes.PAST_REGATTAS) { // Define constante si la necesitas
            PastRegattasScreen()
        }
        composable(Routes.LIVE_REGATTAS + "/{regattaId}") { backStackEntry ->
            val id = backStackEntry.arguments!!.getString("regattaId")!!
            LiveRegattaScreen(regattaId = id)
        }

        composable(Routes.SOUND_SETTINGS) {
            //SoundSettingsScreen(navController)
        }
        composable(Routes.NAV_SETTINGS) {
            //NavSettingsScreen(navController)
        }
        // composable("aboutUs") { // Define constante si la necesitas
        //     //AboutUsScreen(navController)
        // }
        // composable("soundTest") { // Define constante si la necesitas
        //     // SoundTestScreen(context = navController.context)
        // }
    }
}