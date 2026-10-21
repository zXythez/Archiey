package com.shattercore.archiey.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

enum class ArchieyTab(
    val label: String
) {
    HOME("Home"),
    ARCHIVES("Archives"),
    RECENT("Recent"),
    SETTINGS("Settings")
}

@Composable
fun BottomNavBar(
    selectedTab: ArchieyTab,
    onTabSelected: (ArchieyTab) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = NavigationBarDefaults.Elevation
    ) {
        ArchieyTab.entries.forEach { tab ->

            val icon = when (tab) {
                ArchieyTab.HOME -> Icons.Outlined.Folder
                ArchieyTab.ARCHIVES -> Icons.Outlined.Archive
                ArchieyTab.RECENT -> Icons.Outlined.History
                ArchieyTab.SETTINGS -> Icons.Outlined.Settings
            }

            NavigationBarItem(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = tab.label
                    )
                },
                label = {
                    Text(tab.label)
                },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
