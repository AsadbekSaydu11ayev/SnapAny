package com.snapany.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.snapany.app.ui.components.AnimatedCosmicBackground
import com.snapany.app.ui.screens.DownloadsScreen
import com.snapany.app.ui.screens.HomeScreen
import com.snapany.app.ui.screens.SettingsScreen
import com.snapany.app.ui.theme.IndigoAccent
import com.snapany.app.ui.theme.SnapAnyTheme
import com.snapany.app.ui.theme.SurfaceGlass
import com.snapany.app.ui.theme.TextPrimary
import com.snapany.app.ui.theme.TextSecondary
import com.snapany.app.ui.viewmodel.SnapAnyViewModel
import java.util.regex.Pattern

class MainActivity : ComponentActivity() {

    private val viewModel: SnapAnyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNeededPermissions()
        handleIncomingIntent(intent)

        setContent {
            SnapAnyTheme {
                var selectedTab by rememberSaveable { mutableIntStateOf(0) }

                Box(modifier = Modifier.fillMaxSize()) {
                    AnimatedCosmicBackground()

                    Scaffold(
                        containerColor = Color.Transparent,
                        bottomBar = {
                            NavigationBar(containerColor = SurfaceGlass.copy(alpha = 0.94f)) {
                                NavigationBarItem(
                                    selected = selectedTab == 0,
                                    onClick = { selectedTab = 0 },
                                    icon = { Icon(Icons.Rounded.Home, contentDescription = null) },
                                    label = { Text("Bosh sahifa") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = IndigoAccent,
                                        selectedTextColor = TextPrimary,
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextSecondary,
                                        indicatorColor = IndigoAccent.copy(alpha = 0.16f)
                                    )
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 1,
                                    onClick = { selectedTab = 1 },
                                    icon = { Icon(Icons.Rounded.Download, contentDescription = null) },
                                    label = { Text("Yuklamalar") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = IndigoAccent,
                                        selectedTextColor = TextPrimary,
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextSecondary,
                                        indicatorColor = IndigoAccent.copy(alpha = 0.16f)
                                    )
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 2,
                                    onClick = { selectedTab = 2 },
                                    icon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                                    label = { Text("Sozlamalar") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = IndigoAccent,
                                        selectedTextColor = TextPrimary,
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextSecondary,
                                        indicatorColor = IndigoAccent.copy(alpha = 0.16f)
                                    )
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding)) {
                            when (selectedTab) {
                                0 -> HomeScreen(viewModel = viewModel)
                                1 -> DownloadsScreen(viewModel = viewModel)
                                else -> SettingsScreen(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
            val matcher = Pattern.compile("(https?://\\S+)").matcher(sharedText)
            if (matcher.find()) {
                val url = matcher.group(1)
                if (!url.isNullOrBlank()) {
                    viewModel.analyzeUrl(url)
                }
            }
        }
    }

    private fun requestNeededPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1001)
            }
        }
    }
}
