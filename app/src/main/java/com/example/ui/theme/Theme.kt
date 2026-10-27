package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.data.repository.AppTheme

fun getColorSchemeForTheme(theme: AppTheme): ColorScheme {
    return when (theme) {
        AppTheme.CLASSIC -> lightColorScheme(
            primary = ClassicAccent,
            onPrimary = Color.White,
            secondary = Color(0xFF3B82F6),
            background = ClassicBg,
            surface = ClassicSurface,
            surfaceVariant = Color(0xFFF1F5F9),
            onBackground = ClassicText,
            onSurface = ClassicText,
            onSurfaceVariant = ClassicTextMuted,
            outline = ClassicBorder,
            outlineVariant = Color(0x0F000000)
        )
        AppTheme.IOS_GLASS -> darkColorScheme(
            primary = IosGlassAccent,
            onPrimary = Color.White,
            secondary = Color(0xFF38BDF8),
            background = IosGlassBg,
            surface = IosGlassSurface,
            surfaceVariant = IosGlassCard,
            onBackground = IosGlassText,
            onSurface = IosGlassText,
            onSurfaceVariant = IosGlassTextMuted,
            outline = IosGlassBorder,
            outlineVariant = Color(0x0DFFFFFF)
        )
        AppTheme.WARM_SAND -> lightColorScheme(
            primary = WarmSandAccent,
            onPrimary = Color.White,
            secondary = Color(0xFFD97706),
            background = WarmSandBg,
            surface = WarmSandSurface,
            surfaceVariant = Color(0xFFEFE3D5),
            onBackground = WarmSandText,
            onSurface = WarmSandText,
            onSurfaceVariant = WarmSandTextMuted,
            outline = WarmSandBorder,
            outlineVariant = Color(0x14C86448)
        )
        AppTheme.MIDNIGHT_EMERALD -> darkColorScheme(
            primary = EmeraldAccent,
            onPrimary = Color.Black,
            secondary = Color(0xFF34D399),
            background = EmeraldBg,
            surface = EmeraldSurface,
            surfaceVariant = EmeraldCard,
            onBackground = EmeraldText,
            onSurface = EmeraldText,
            onSurfaceVariant = EmeraldTextMuted,
            outline = EmeraldBorder,
            outlineVariant = Color(0x1A10B981)
        )
        AppTheme.OBSIDIAN -> darkColorScheme(
            primary = ObsidianAccent,
            onPrimary = Color.White,
            secondary = Color(0xFFA78BFA),
            background = ObsidianBg,
            surface = ObsidianSurface,
            surfaceVariant = ObsidianCard,
            onBackground = ObsidianText,
            onSurface = ObsidianText,
            onSurfaceVariant = ObsidianTextMuted,
            outline = ObsidianBorder,
            outlineVariant = Color(0x1F8B5CF6)
        )
        AppTheme.VELVET_ROSE_GOLD -> darkColorScheme(
            primary = VelvetRoseGoldPrimary,
            onPrimary = VelvetMidnightSlateBg,
            secondary = VelvetRoseGoldSecondary,
            tertiary = VelvetGoldAccent,
            background = VelvetMidnightSlateBg,
            surface = VelvetCharcoalSurface,
            surfaceVariant = VelvetCharcoalCard,
            onBackground = VelvetChampagneText,
            onSurface = VelvetChampagneText,
            onSurfaceVariant = VelvetBlushTextMuted,
            outline = VelvetBorder,
            outlineVariant = Color(0x26E8A5C8)
        )
    }
}

@Composable
fun EchoTalkTheme(
    theme: AppTheme = AppTheme.IOS_GLASS,
    content: @Composable () -> Unit
) {
    val colorScheme = getColorSchemeForTheme(theme)
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
