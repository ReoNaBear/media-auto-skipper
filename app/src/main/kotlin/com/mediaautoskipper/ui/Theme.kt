package com.mediaautoskipper.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Design tokens: an 8 dp grid, a 12 dp card radius and a 48 dp minimum touch target. */
object Dimens {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val cardRadius = 12.dp
    val touchTarget = 48.dp
}

// One teal brand colour (the app icon) plus neutrals; text pairs are chosen for at least 4.5:1 contrast.
private val LightColors = lightColorScheme(
    primary = Color(0xFF0F766E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCDEFEA),
    onPrimaryContainer = Color(0xFF00201D),
    secondary = Color(0xFF3F5754),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD8E8E5),
    onSecondaryContainer = Color(0xFF0F1F1D),
    tertiaryContainer = Color(0xFFFFE2B8),
    onTertiaryContainer = Color(0xFF2B1700),
    background = Color(0xFFF5FAF9),
    onBackground = Color(0xFF161D1C),
    surface = Color(0xFFF5FAF9),
    onSurface = Color(0xFF161D1C),
    surfaceVariant = Color(0xFFDAE5E2),
    onSurfaceVariant = Color(0xFF3F4947),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFEAF2F0),
    outline = Color(0xFF6F7977),
    outlineVariant = Color(0xFFBEC9C6),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF4FDBCB),
    onPrimary = Color(0xFF003733),
    primaryContainer = Color(0xFF00504A),
    onPrimaryContainer = Color(0xFFB2F2EA),
    secondary = Color(0xFFB0CCC8),
    onSecondary = Color(0xFF1A3532),
    secondaryContainer = Color(0xFF2C4A47),
    onSecondaryContainer = Color(0xFFCCE8E4),
    tertiaryContainer = Color(0xFF5A3A00),
    onTertiaryContainer = Color(0xFFFFE2B8),
    background = Color(0xFF0E1514),
    onBackground = Color(0xFFDDE4E2),
    surface = Color(0xFF0E1514),
    onSurface = Color(0xFFDDE4E2),
    surfaceVariant = Color(0xFF3F4947),
    onSurfaceVariant = Color(0xFFBEC9C6),
    surfaceContainerLow = Color(0xFF161D1C),
    surfaceContainer = Color(0xFF1A2120),
    outline = Color(0xFF899390),
    outlineVariant = Color(0xFF3F4947),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

@Composable
fun MasTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
}

/** A titled group of settings on a card. */
@Composable
fun SectionCard(
    title: String?,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.cardRadius),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(Dimens.md), verticalArrangement = Arrangement.spacedBy(Dimens.sm)) {
            if (title != null) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() },
                )
            }
            content()
        }
    }
}
