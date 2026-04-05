package com.painterai.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.painterai.app.ui.screen.analysis.AnalysisScreen
import com.painterai.app.ui.screen.chat.ChatScreen
import com.painterai.app.ui.screen.home.HomeScreen
import com.painterai.app.ui.screen.newjob.NewJobScreen
import com.painterai.app.ui.screen.newjob.SelectModeScreen
import com.painterai.app.ui.screen.archive.ArchiveScreen

object Routes {
    const val HOME = "home"
    const val NEW_JOB = "new_job"
    const val SELECT_MODE = "select_mode/{jobId}"
    const val ANALYSIS = "analysis/{jobId}"
    const val CHAT = "chat/{jobId}"
    const val ARCHIVE = "archive/{jobId}"

    fun selectMode(jobId: String) = "select_mode/$jobId"
    fun analysis(jobId: String) = "analysis/$jobId"
    fun chat(jobId: String) = "chat/$jobId"
    fun archive(jobId: String) = "archive/$jobId"
}

@Composable
fun PainterAINavGraph(
    navController: NavHostController,
    startDestination: String = Routes.HOME
) {
    NavHost(navController = navController, startDestination = startDestination) {

        composable(Routes.HOME) {
            HomeScreen(
                onNewJob = { navController.navigate(Routes.NEW_JOB) },
                onJobClick = { jobId -> navController.navigate(Routes.analysis(jobId)) }
            )
        }

        composable(Routes.NEW_JOB) {
            NewJobScreen(
                onJobCreated = { jobId ->
                    navController.navigate(Routes.selectMode(jobId)) {
                        popUpTo(Routes.HOME)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            Routes.SELECT_MODE,
            arguments = listOf(navArgument("jobId") { type = NavType.StringType })
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId") ?: return@composable
            SelectModeScreen(
                onPhotoMode = {
                    navController.navigate(Routes.analysis(jobId)) {
                        popUpTo(Routes.HOME)
                    }
                },
                onDbMode = { /* 추후 구현 */ },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            Routes.ANALYSIS,
            arguments = listOf(navArgument("jobId") { type = NavType.StringType })
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId") ?: return@composable
            AnalysisScreen(
                jobId = jobId,
                onOpenChat = { navController.navigate(Routes.chat(jobId)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            Routes.CHAT,
            arguments = listOf(navArgument("jobId") { type = NavType.StringType })
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId") ?: return@composable
            ChatScreen(
                jobId = jobId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            Routes.ARCHIVE,
            arguments = listOf(navArgument("jobId") { type = NavType.StringType })
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId") ?: return@composable
            ArchiveScreen(
                jobId = jobId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
