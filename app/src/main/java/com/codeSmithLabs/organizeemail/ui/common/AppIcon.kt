package com.codeSmithLabs.organizeemail.ui.common

import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector

sealed class AppIcon {
    data class Vector(val imageVector: ImageVector) : AppIcon()
    data class PainterIcon(val painter: Painter) : AppIcon()
}