package com.example.whatsappstatussaver.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import android.net.Uri
import android.util.Base64
import androidx.compose.material3.ExperimentalMaterial3Api
import com.example.whatsappstatussaver.data.models.MediaType
import com.example.whatsappstatussaver.data.models.PlatformType
import com.example.whatsappstatussaver.data.models.StatusMedia

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Status : Screen("status/{platform}") {
        fun createRoute(platform: PlatformType) = "status/${platform.name}"
    }
    object DirectChat : Screen("direct_chat")
    object SavedFiles : Screen("saved_files")
    object Reminder : Screen("reminder")
    object Settings : Screen("settings")
    object Splash : Screen("splash")

    object MediaViewer : Screen("viewer?uri={uri}&type={type}&name={name}&platform={platform}") {
        fun createRoute(uri: String, type: MediaType, name: String, platform: PlatformType): String {
            // FIX 1: URI ko Base64 URL_SAFE me encode karein taake spaces (%20) ya slashes route ko break na karein
            val uriBytes = uri.toByteArray(Charsets.UTF_8)
            val base64Uri = Base64.encodeToString(uriBytes, Base64.URL_SAFE or Base64.NO_WRAP)
            val encodedName = Uri.encode(name)
            return "viewer?uri=$base64Uri&type=${type.name}&name=$encodedName&platform=${platform.name}"
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsAppStatusSaverNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        modifier = modifier
    ) {
        composable(Screen.Splash.route) {
            com.example.whatsappstatussaver.ui.splash.SplashScreen(
                onSplashScreenFinished = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Home.route) {
            com.example.whatsappstatussaver.ui.home.HomeScreen(
                onNavigateToStatus = { platform -> navController.navigate(Screen.Status.createRoute(platform)) },
                onNavigateToDirectChat = { navController.navigate(Screen.DirectChat.route) },
                onNavigateToSavedFiles = { navController.navigate(Screen.SavedFiles.route) },
                onNavigateToReminder = { navController.navigate(Screen.Reminder.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(
            route = Screen.Status.route,
            arguments = listOf(navArgument("platform") { type = NavType.StringType })
        ) { backStackEntry ->
            val platformString = backStackEntry.arguments?.getString("platform") ?: PlatformType.WHATSAPP.name
            val platform = try { PlatformType.valueOf(platformString) } catch (e: Exception) { PlatformType.WHATSAPP }
            com.example.whatsappstatussaver.ui.status.StatusScreen(
                initialPlatform = platform,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToViewer = { uri, type, name ->
                    navController.navigate(Screen.MediaViewer.createRoute(uri, type, name, platform))
                }
            )
        }
        composable(Screen.DirectChat.route) {
            com.example.whatsappstatussaver.ui.chat.DirectChatScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.SavedFiles.route) {
            com.example.whatsappstatussaver.ui.saved.SavedFilesScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToViewer = { uri, type, name ->
                    navController.navigate(Screen.MediaViewer.createRoute(uri, type, name, PlatformType.SAVED))
                }
            )
        }
        composable(Screen.Reminder.route) {
            com.example.whatsappstatussaver.ui.reminder.ReminderScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.Settings.route) {
            com.example.whatsappstatussaver.ui.settings.SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.MediaViewer.route,
            arguments = listOf(
                navArgument("uri") { type = NavType.StringType },
                navArgument("type") { type = NavType.StringType },
                navArgument("name") { type = NavType.StringType },
                navArgument("platform") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val base64Uri = backStackEntry.arguments?.getString("uri") ?: ""
            val typeStr = backStackEntry.arguments?.getString("type") ?: MediaType.IMAGE.name
            val name = backStackEntry.arguments?.getString("name") ?: "Unknown"
            val platformStr = backStackEntry.arguments?.getString("platform") ?: PlatformType.WHATSAPP.name

            // FIX 2: Base64 String ko wapas original String URI me convert karein
            val decodedUriStr = try {
                val decodedBytes = Base64.decode(base64Uri, Base64.URL_SAFE or Base64.NO_WRAP)
                String(decodedBytes, Charsets.UTF_8)
            } catch (e: Exception) {
                base64Uri // Fallback incase parsing fails
            }

            val statusMedia = StatusMedia(
                uri = Uri.parse(decodedUriStr),
                name = name,
                type = try { MediaType.valueOf(typeStr) } catch (e: Exception) { MediaType.IMAGE },
                size = 0L,
                dateModified = 0L,
                platform = try { PlatformType.valueOf(platformStr) } catch (e: Exception) { PlatformType.WHATSAPP }
            )

            val viewerViewModel: com.example.whatsappstatussaver.ui.saved.SavedFilesViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            val statusViewModel: com.example.whatsappstatussaver.ui.status.StatusViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            com.example.whatsappstatussaver.ui.viewer.MediaViewerScreen(
                statusMedia = statusMedia,
                onNavigateBack = { navController.popBackStack() },
                onSaveMedia = { media -> statusViewModel.saveMedia(media) },
                onTagUpdate = { tags -> viewerViewModel.updateTags(statusMedia, tags) },
                onCompressVideo = { uri -> viewerViewModel.compressVideo(uri) }
            )
        }
    }
}