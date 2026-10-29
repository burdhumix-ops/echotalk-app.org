package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.data.repository.AppTheme
import com.example.data.repository.EchoTalkRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * ThemeCustomColors: Extended dynamic colors injected into Compose UI components
 * beyond standard Material 3 colorScheme (e.g. ambient glows, gradient brushes, unread dots).
 */
@Stable
data class ThemeCustomColors(
    val theme: AppTheme,
    val primaryAccent: Color,
    val secondaryAccent: Color,
    val gradientBrush: Brush,
    val cardBackground: Color,
    val surfaceTranslucent: Color,
    val ambientGlowColor: Color,
    val unreadBadgeColor: Color,
    val borderStroke: Color,
    val isDark: Boolean
)

val LocalThemeManager = staticCompositionLocalOf<ThemeManager> {
    ThemeManager
}

val LocalThemeCustomColors = staticCompositionLocalOf {
    ThemeManager.getCustomColors(AppTheme.IOS_GLASS)
}

/**
 * ThemeManager: Centralized manager handling application color presets,
 * specifically providing dedicated support for 'Velvet Rose Gold' and 'Midnight Slate'
 * themes alongside Classic, iOS Glass, Warm Sand, Midnight Emerald, and Obsidian,
 * with dynamic color injection into Compose UI components.
 */
object ThemeManager {

    private val repository by lazy { EchoTalkRepository.getInstance() }

    val currentTheme: StateFlow<AppTheme>
        get() = repository.theme

    /**
     * Updates the application theme and triggers dynamic recomposition.
     */
    fun setTheme(theme: AppTheme) {
        repository.setTheme(theme)
    }

    /**
     * Resolves the full M3 ColorScheme for any theme preset.
     */
    fun getColorScheme(theme: AppTheme = currentTheme.value): ColorScheme {
        return getColorSchemeForTheme(theme)
    }

    /**
     * Returns whether the given theme is a dark theme.
     */
    fun isDark(theme: AppTheme = currentTheme.value): Boolean {
        return theme != AppTheme.CLASSIC && theme != AppTheme.WARM_SAND
    }

    /**
     * Returns rich extended theme colors for dynamic injection into components.
     */
    fun getCustomColors(theme: AppTheme = currentTheme.value): ThemeCustomColors {
        return when (theme) {
            AppTheme.VELVET_ROSE_GOLD -> ThemeCustomColors(
                theme = theme,
                primaryAccent = VelvetRoseGoldPrimary, // #E8A5C8
                secondaryAccent = VelvetRoseGoldSecondary, // #E287B2
                gradientBrush = Brush.horizontalGradient(
                    listOf(VelvetRoseGoldPrimary, Color(0xFF6366F1))
                ),
                cardBackground = VelvetCharcoalCard,
                surfaceTranslucent = VelvetCharcoalSurface.copy(alpha = 0.85f),
                ambientGlowColor = Color(0x33E8A5C8),
                unreadBadgeColor = VelvetRoseGoldPrimary,
                borderStroke = VelvetBorder,
                isDark = true
            )
            AppTheme.MIDNIGHT_SLATE -> ThemeCustomColors(
                theme = theme,
                primaryAccent = MidnightSlatePrimary, // #38BDF8
                secondaryAccent = MidnightSlateSecondary, // #818CF8
                gradientBrush = Brush.horizontalGradient(
                    listOf(MidnightSlatePrimary, MidnightSlateSecondary)
                ),
                cardBackground = MidnightSlateCard,
                surfaceTranslucent = MidnightSlateSurface.copy(alpha = 0.85f),
                ambientGlowColor = Color(0x3338BDF8),
                unreadBadgeColor = MidnightSlatePrimary,
                borderStroke = MidnightSlateBorder,
                isDark = true
            )
            AppTheme.IOS_GLASS -> ThemeCustomColors(
                theme = theme,
                primaryAccent = IosGlassAccent,
                secondaryAccent = Color(0xFF38BDF8),
                gradientBrush = Brush.horizontalGradient(
                    listOf(Color(0xFF6366F1), Color(0xFF38BDF8))
                ),
                cardBackground = IosGlassCard,
                surfaceTranslucent = IosGlassSurface.copy(alpha = 0.85f),
                ambientGlowColor = Color(0x266366F1),
                unreadBadgeColor = Color(0xFFE8A5C8),
                borderStroke = IosGlassBorder,
                isDark = true
            )
            AppTheme.CLASSIC -> ThemeCustomColors(
                theme = theme,
                primaryAccent = ClassicAccent,
                secondaryAccent = Color(0xFF3B82F6),
                gradientBrush = Brush.horizontalGradient(
                    listOf(ClassicAccent, Color(0xFF60A5FA))
                ),
                cardBackground = ClassicCard,
                surfaceTranslucent = ClassicSurface.copy(alpha = 0.95f),
                ambientGlowColor = Color(0x1A2563EB),
                unreadBadgeColor = ClassicAccent,
                borderStroke = ClassicBorder,
                isDark = false
            )
            AppTheme.WARM_SAND -> ThemeCustomColors(
                theme = theme,
                primaryAccent = WarmSandAccent,
                secondaryAccent = Color(0xFFD97706),
                gradientBrush = Brush.horizontalGradient(
                    listOf(WarmSandAccent, Color(0xFFF59E0B))
                ),
                cardBackground = WarmSandCard,
                surfaceTranslucent = WarmSandSurface.copy(alpha = 0.95f),
                ambientGlowColor = Color(0x26C86448),
                unreadBadgeColor = WarmSandAccent,
                borderStroke = WarmSandBorder,
                isDark = false
            )
            AppTheme.MIDNIGHT_EMERALD -> ThemeCustomColors(
                theme = theme,
                primaryAccent = EmeraldAccent,
                secondaryAccent = Color(0xFF34D399),
                gradientBrush = Brush.horizontalGradient(
                    listOf(EmeraldAccent, Color(0xFF059669))
                ),
                cardBackground = EmeraldCard,
                surfaceTranslucent = EmeraldSurface.copy(alpha = 0.85f),
                ambientGlowColor = Color(0x3310B981),
                unreadBadgeColor = EmeraldAccent,
                borderStroke = EmeraldBorder,
                isDark = true
            )
            AppTheme.OBSIDIAN -> ThemeCustomColors(
                theme = theme,
                primaryAccent = ObsidianAccent,
                secondaryAccent = Color(0xFFA78BFA),
                gradientBrush = Brush.horizontalGradient(
                    listOf(ObsidianAccent, Color(0xFFC084FC))
                ),
                cardBackground = ObsidianCard,
                surfaceTranslucent = ObsidianSurface.copy(alpha = 0.85f),
                ambientGlowColor = Color(0x338B5CF6),
                unreadBadgeColor = ObsidianAccent,
                borderStroke = ObsidianBorder,
                isDark = true
            )
        }
    }

    /**
     * Accessor for currently injected theme custom colors in any Composable.
     */
    val colors: ThemeCustomColors
        @Composable
        @ReadOnlyComposable
        get() = LocalThemeCustomColors.current
}

/**
 * DynamicThemeProvider: Injects ThemeManager and dynamic ThemeCustomColors into the Compose tree.
 */
@Composable
fun DynamicThemeProvider(
    theme: AppTheme,
    content: @Composable () -> Unit
) {
    val customColors = ThemeManager.getCustomColors(theme)
    val colorScheme = ThemeManager.getColorScheme(theme)

    CompositionLocalProvider(
        LocalThemeManager provides ThemeManager,
        LocalThemeCustomColors provides customColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
