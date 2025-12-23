package com.example.llamadroid.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.ui.res.stringResource
import com.example.llamadroid.R
import com.example.llamadroid.ui.navigation.Screen

data class SettingsItem(
    val emoji: String,
    val title: String,
    val description: String,
    val gradientColors: List<Color>,
    val route: String
)

/**
 * Settings Hub - 2-column square grid layout
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsHubScreen(navController: NavController) {
    val settingsItems = listOf(
        SettingsItem(
            emoji = "📂",
            title = stringResource(R.string.settings_general),
            description = stringResource(R.string.settings_general_desc),
            gradientColors = listOf(
                Color(0xFF607D8B).copy(alpha = 0.15f),
                Color(0xFF455A64).copy(alpha = 0.3f)
            ),
            route = "settings_general"
        ),
        SettingsItem(
            emoji = "💬",
            title = stringResource(R.string.settings_llm),
            description = stringResource(R.string.settings_llm_desc),
            gradientColors = listOf(
                Color(0xFF4CAF50).copy(alpha = 0.15f),
                Color(0xFF388E3C).copy(alpha = 0.3f)
            ),
            route = "settings_llm"
        ),
        SettingsItem(
            emoji = "🎨",
            title = stringResource(R.string.settings_imagegen),
            description = stringResource(R.string.settings_imagegen_desc),
            gradientColors = listOf(
                Color(0xFF2196F3).copy(alpha = 0.15f),
                Color(0xFF1976D2).copy(alpha = 0.3f)
            ),
            route = "settings_imagegen"
        ),
        SettingsItem(
            emoji = "🎤",
            title = stringResource(R.string.settings_whisper),
            description = stringResource(R.string.settings_whisper_desc),
            gradientColors = listOf(
                Color(0xFFFF9800).copy(alpha = 0.15f),
                Color(0xFFF57C00).copy(alpha = 0.3f)
            ),
            route = "settings_whisper"
        ),
        SettingsItem(
            emoji = "🎬",
            title = stringResource(R.string.settings_upscaler),
            description = stringResource(R.string.settings_upscaler_desc),
            gradientColors = listOf(
                Color(0xFF9C27B0).copy(alpha = 0.15f),
                Color(0xFF7B1FA2).copy(alpha = 0.3f)
            ),
            route = "settings_upscaler"
        ),
        SettingsItem(
            emoji = "📝",
            title = stringResource(R.string.settings_prompts),
            description = stringResource(R.string.settings_prompts_desc),
            gradientColors = listOf(
                Color(0xFF009688).copy(alpha = 0.15f),
                Color(0xFF00796B).copy(alpha = 0.3f)
            ),
            route = "settings_prompts"
        ),
        SettingsItem(
            emoji = "🔧",
            title = stringResource(R.string.settings_debug),
            description = stringResource(R.string.settings_debug_desc),
            gradientColors = listOf(
                Color(0xFF795548).copy(alpha = 0.15f),
                Color(0xFF5D4037).copy(alpha = 0.3f)
            ),
            route = Screen.Logs.route
        ),
        SettingsItem(
            emoji = "ℹ️",
            title = stringResource(R.string.settings_about),
            description = stringResource(R.string.settings_about_desc),
            gradientColors = listOf(
                Color(0xFF3F51B5).copy(alpha = 0.15f),
                Color(0xFF303F9F).copy(alpha = 0.3f)
            ),
            route = "about"
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                )
            )
    ) {
        // Header
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "⚙️ " + stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                )
            )
            Text(
                stringResource(R.string.settings_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // 2-column grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(settingsItems) { item ->
                SettingsSquareCard(
                    emoji = item.emoji,
                    title = item.title,
                    description = item.description,
                    gradientColors = item.gradientColors,
                    onClick = { navController.navigate(item.route) }
                )
            }
        }
    }
}

@Composable
private fun SettingsSquareCard(
    emoji: String,
    title: String,
    description: String,
    gradientColors: List<Color>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .aspectRatio(1f) // Square
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = Brush.verticalGradient(gradientColors))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Emoji
                Text(
                    emoji,
                    style = MaterialTheme.typography.displaySmall,
                    fontSize = 40.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Title
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Description
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
