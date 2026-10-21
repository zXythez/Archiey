package com.shattercore.archiey

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.shattercore.archiey.onboarding.WelcomeScreen
import com.shattercore.archiey.ui.ArchieyApp
import com.shattercore.archiey.ui.theme.ArchieyTheme

class MainActivity : ComponentActivity() {

    private var storageGranted by mutableStateOf(false)

    private val legacyPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            storageGranted = hasStorageAccess()
        }

    private fun hasStorageAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            val readGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED

            val writeGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                readGranted
            } else {
                readGranted && writeGranted
            }
        }
    }

    private fun requestStorageAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
            } catch (e: Exception) {
                startActivity(
                    Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                )
            }
        } else {
            val permissions =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                } else {
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                }

            legacyPermissionLauncher.launch(permissions)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences(
            "archiey_settings",
            MODE_PRIVATE
        )

        storageGranted = hasStorageAccess()

        setContent {
            var onboardingCompleted by remember {
                mutableStateOf(
                    prefs.getBoolean("onboarding_completed", false)
                )
            }

            val lifecycleOwner = LocalLifecycleOwner.current

            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        storageGranted = hasStorageAccess()
                    }
                }

                lifecycleOwner.lifecycle.addObserver(observer)

                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            ArchieyTheme {
                if (onboardingCompleted) {
                    ArchieyApp()
                } else {
                    WelcomeScreen(
                        hasStorageAccess = storageGranted,
                        onRequestAccess = {
                            requestStorageAccess()
                        },
                        onContinue = {
                            if (hasStorageAccess()) {
                                prefs.edit()
                                    .putBoolean(
                                        "onboarding_completed",
                                        true
                                    )
                                    .apply()

                                onboardingCompleted = true
                            }
                        }
                    )
                }
            }
        }
    }
}
