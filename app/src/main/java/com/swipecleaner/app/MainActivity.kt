package com.swipecleaner.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.swipecleaner.app.data.OnboardingManager
import com.swipecleaner.app.domain.BucketFolder
import com.swipecleaner.app.ui.PermissionGate
import com.swipecleaner.app.ui.screens.AboutScreen
import com.swipecleaner.app.ui.screens.FolderSelectionScreen
import com.swipecleaner.app.ui.screens.OnboardingScreen
import com.swipecleaner.app.ui.screens.PhotoDeckScreen
import com.swipecleaner.app.ui.theme.PhotoSwipeCleanerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PhotoSwipeCleanerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val context = LocalContext.current
                    val onboardingManager = remember { OnboardingManager(context) }
                    var showOnboarding by remember { mutableStateOf(!onboardingManager.hasSeenTutorial()) }

                    if (showOnboarding) {
                        // Tutorial primero, antes de pedir el permiso de fotos —
                        // así el usuario entiende qué va a hacer la app antes
                        // de que Android le pregunte por el acceso a su galería.
                        OnboardingScreen(onFinish = {
                            onboardingManager.markTutorialSeen()
                            showOnboarding = false
                        })
                    } else {
                        PermissionGate {
                            var selectedFolder by remember { mutableStateOf<BucketFolder?>(null) }
                            var showAbout by remember { mutableStateOf(false) }

                            when {
                                showAbout -> {
                                    AboutScreen(onBack = { showAbout = false })
                                }
                                selectedFolder != null -> {
                                    PhotoDeckScreen(
                                        folder = selectedFolder!!,
                                        onBack = { selectedFolder = null }
                                    )
                                }
                                else -> {
                                    FolderSelectionScreen(
                                        onFolderSelected = { selectedFolder = it },
                                        onAboutClick = { showAbout = true }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
