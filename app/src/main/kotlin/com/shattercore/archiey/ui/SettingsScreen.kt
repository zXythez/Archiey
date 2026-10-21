package com.shattercore.archiey.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen() {
    val context = LocalContext.current

    val prefs = remember(context) {
        context.getSharedPreferences(
            "archiey_settings",
            android.content.Context.MODE_PRIVATE
        )
    }

    var advancedEnabled by remember {
        mutableStateOf(
            prefs.getBoolean("advanced_file_operations", false)
        )
    }

    var showWarning by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Advanced",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleSmall
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Advanced file operations",
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "Allow operations in protected Android directories.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = advancedEnabled,
                onCheckedChange = { enabled ->
                    if (enabled) {
                        showWarning = true
                    } else {
                        advancedEnabled = false
                        prefs.edit()
                            .putBoolean("advanced_file_operations", false)
                            .apply()
                    }
                }
            )
        }
    }

    if (showWarning) {
        AlertDialog(
            onDismissRequest = { showWarning = false },
            title = { Text("Enable advanced operations?") },
            text = {
                Text(
                    "Modifying Android-managed directories may cause " +
                    "application failures or permanent data loss. " +
                    "Android storage restrictions still apply."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        advancedEnabled = true
                        prefs.edit()
                            .putBoolean("advanced_file_operations", true)
                            .apply()
                        showWarning = false
                    }
                ) {
                    Text("Enable")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showWarning = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
