// En com/solosailing/navigation/AppNavGraph.kt
package com.solosailing.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.solosailing.presentation.home.HomeScreen
import com.solosailing.presentation.login.LoginScreen
import com.solosailing.presentation.login.RegisterScreen
import com.solosailing.presentation.map.MapScreen
import com.solosailing.presentation.regatta.LiveRegattaScreen
import com.solosailing.presentation.regatta.LiveRegattasListScreen
import com.solosailing.presentation.regatta.PastRegattaScreen
import com.solosailing.presentation.regatta.PastRegattasListScreen
import com.solosailing.presentation.regatta.RegattaSimulationScreen

@Composable
fun AppNavGraph() {
    val navController: NavHostController = rememberNavController()
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
            RegattaSimulationScreen()
        }

        composable(Routes.PAST_REGATTAS) {
            PastRegattasListScreen(navController)
        }

        composable("past/{regattaId}") { backStackEntry ->
            val id = backStackEntry.arguments!!.getString("regattaId")!!
            PastRegattaScreen(regattaId = id)
        }

        composable(Routes.LIVE_REGATTAS) {
            LiveRegattasListScreen(navController)
        }
        composable("${Routes.LIVE_REGATTAS}/{regattaId}") { backStackEntry ->
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