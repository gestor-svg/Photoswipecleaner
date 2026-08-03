package com.swipecleaner.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.runtime.*
import com.swipecleaner.app.domain.BucketFolder
import com.swipecleaner.app.ui.PermissionGate
import com.swipecleaner.app.ui.screens.FolderSelectionScreen
import com.swipecleaner.app.ui.screens.PhotoDeckScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PermissionGate {
                        var selectedFolder by remember { mutableStateOf<BucketFolder?>(null) }

                        val folder = selectedFolder
                        if (folder == null) {
                            FolderSelectionScreen(
                                onFolderSelected = { selectedFolder = it }
                            )
                        } else {
                            PhotoDeckScreen(
                                folder = folder,
                                onBack = { selectedFolder = null }
                            )
                        }
                    }
                }
            }
        }
    }
}
