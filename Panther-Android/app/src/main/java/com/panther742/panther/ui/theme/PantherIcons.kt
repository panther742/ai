package com.panther742.panther.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Custom icon set. Keeps the app lean (no icons-extended dependency)
 * while still giving Panther its own glyphs.
 * Icon() applies its tint on top of these black shapes.
 */
object PantherIcons {

    val Mic: ImageVector by lazy {
        ImageVector.Builder(
            name = "Mic", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).path(fill = SolidColor(Color.Black)) {
            addPathNodes(
                "M12,14c1.66,0 2.99,-1.34 2.99,-3L15,5c0,-1.66 -1.34,-3 -3,-3S9,3.34 9,5v6c0,1.66 1.34,3 3,3z" +
                    "M17.3,11c0,3 -2.54,5.1 -5.3,5.1S6.7,14 6.7,11L5,11c0,3.41 2.72,6.23 6,6.72L11,21h2v-3.28c3.28,-0.48 6,-3.3 6,-6.72L17.3,11z",
            )
        }.build()
    }

    val Battery: ImageVector by lazy {
        ImageVector.Builder(
            name = "Battery", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).path(fill = SolidColor(Color.Black)) {
            addPathNodes(
                "M15.67,4L14,4L14,2h-4v2L8.33,4C7.6,4 7,4.6 7,5.33v15.33C7,21.4 7.6,22 8.33,22h7.33c0.74,0 1.34,-0.6 1.34,-1.33L17,5.33C17,4.6 16.4,4 15.67,4z",
            )
        }.build()
    }

    val Wifi: ImageVector by lazy {
        ImageVector.Builder(
            name = "Wifi", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).path(fill = SolidColor(Color.Black)) {
            addPathNodes(
                "M1,9l2,2c4.97,-4.97 13.03,-4.97 18,0l2,-2C16.93,2.93 7.08,2.93 1,9zM9,17l3,3 3,-3c-1.65,-1.66 -4.34,-1.66 -6,0zM5,13l2,2c2.76,-2.76 7.24,-2.76 10,0l2,-2C15.14,9.14 8.87,9.14 5,13z",
            )
        }.build()
    }

    val Send: ImageVector by lazy {
        ImageVector.Builder(
            name = "Send", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).path(fill = SolidColor(Color.Black)) {
            addPathNodes("M2.01,21L23,12 2.01,3 2,10l15,2 -15,2z")
        }.build()
    }

    val Bolt: ImageVector by lazy {
        ImageVector.Builder(
            name = "Bolt", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).path(fill = SolidColor(Color.Black)) {
            addPathNodes("M11,21h-1l1,-7L7.5,14c-0.88,0 -0.33,-0.75 -0.31,-0.78C8.48,10.94 10.42,7.54 13.01,3h1l-1,7 3.51,0c0.88,0 0.33,0.75 0.31,0.78C15.52,13.06 13.58,16.46 11,21z")
        }.build()
    }
}
