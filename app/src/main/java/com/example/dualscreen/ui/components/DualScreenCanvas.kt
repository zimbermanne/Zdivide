package com.example.dualscreen.ui.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.dualscreen.network.CalibrationPayload
import com.example.dualscreen.network.ConfigPayload
import com.example.dualscreen.network.DeviceRole
import com.example.dualscreen.network.TouchPayload
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DualScreenCanvas(
    modifier: Modifier = Modifier,
    role: DeviceRole,
    config: ConfigPayload,
    calibration: CalibrationPayload,
    showAlignmentGrid: Boolean = true,
    activeImageId: String = "panoramic",
    touchPointer: TouchPayload? = null,
    onTouchEvent: ((action: String, xRatio: Float, yRatio: Float) -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)) // Deep dark canvas
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        val xRatio = offset.x / size.width
                        val yRatio = offset.y / size.height
                        onTouchEvent?.invoke("DOWN", xRatio, yRatio)
                        tryAwaitRelease()
                        onTouchEvent?.invoke("UP", xRatio, yRatio)
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Calculate split bounds depending on whether we are Host (Left/Top) or Client (Right/Bottom)
            val isHost = role == DeviceRole.HOST
            val isHorizontal = config.splitDirection.equals("HORIZONTAL", ignoreCase = true)
            val ratio = config.splitRatio.coerceIn(0.1f, 0.9f)

            // Calculate viewport crop coordinates in virtual full canvas space [0..1, 0..1]
            val leftCrop: Float
            val topCrop: Float
            val rightCrop: Float
            val bottomCrop: Float

            if (isHorizontal) {
                if (isHost) {
                    leftCrop = 0f
                    topCrop = 0f
                    rightCrop = ratio
                    bottomCrop = 1f
                } else {
                    leftCrop = ratio
                    topCrop = 0f
                    rightCrop = 1f
                    bottomCrop = 1f
                }
            } else {
                if (isHost) {
                    leftCrop = 0f
                    topCrop = 0f
                    rightCrop = 1f
                    bottomCrop = ratio
                } else {
                    leftCrop = 0f
                    topCrop = ratio
                    rightCrop = 1f
                    bottomCrop = 1f
                }
            }

            // Apply Calibration Transformations
            val gapOffsetPx = calibration.gapCompensationDp.dp.toPx()
            val xOffsetPx = calibration.xOffsetDp.dp.toPx() + (if (!isHost && isHorizontal) gapOffsetPx else 0f)
            val yOffsetPx = calibration.yOffsetDp.dp.toPx() + (if (!isHost && !isHorizontal) gapOffsetPx else 0f)
            val scaleVal = calibration.scaleFactor
            val rotDeg = calibration.rotationDegrees

            translate(left = xOffsetPx, top = yOffsetPx) {
                scale(scaleVal, pivot = Offset(canvasWidth / 2f, canvasHeight / 2f)) {
                    rotate(rotDeg, pivot = Offset(canvasWidth / 2f, canvasHeight / 2f)) {

                        // Draw Image artwork / test pattern half
                        drawTestPatternOrArtwork(
                            canvasWidth = canvasWidth,
                            canvasHeight = canvasHeight,
                            leftCrop = leftCrop,
                            topCrop = topCrop,
                            rightCrop = rightCrop,
                            bottomCrop = bottomCrop,
                            activeImageId = activeImageId,
                            isHost = isHost,
                            scaleMode = config.scaleMode
                        )

                        // Alignment Target Grid
                        if (showAlignmentGrid) {
                            drawAlignmentGrid(
                                canvasWidth = canvasWidth,
                                canvasHeight = canvasHeight,
                                isHost = isHost,
                                isHorizontal = isHorizontal
                            )
                        }
                    }
                }
            }

            // Touch Pointer Indicator Overlay
            touchPointer?.let { touch ->
                val px = touch.xRatio * canvasWidth
                val py = touch.yRatio * canvasHeight
                drawCircle(
                    color = Color(0xFF38BDF8),
                    radius = 24.dp.toPx(),
                    center = Offset(px, py),
                    style = Stroke(width = 4.dp.toPx())
                )
                drawCircle(
                    color = Color(0xFF0EA5E9),
                    radius = 8.dp.toPx(),
                    center = Offset(px, py)
                )
            }
        }
    }
}

private fun DrawScope.drawTestPatternOrArtwork(
    canvasWidth: Float,
    canvasHeight: Float,
    leftCrop: Float,
    topCrop: Float,
    rightCrop: Float,
    bottomCrop: Float,
    activeImageId: String,
    isHost: Boolean,
    scaleMode: String
) {
    // Virtual full canvas coordinates
    val fullVirtualWidth = canvasWidth / (rightCrop - leftCrop)
    val fullVirtualHeight = canvasHeight / (bottomCrop - topCrop)

    val virtualOriginX = -leftCrop * fullVirtualWidth
    val virtualOriginY = -topCrop * fullVirtualHeight

    translate(left = virtualOriginX, top = virtualOriginY) {
        // Render rich gradient landscape & cyber geometric visual test scene
        drawRect(
            color = Color(0xFF0F172A),
            topLeft = Offset(0f, 0f),
            size = Size(fullVirtualWidth, fullVirtualHeight)
        )

        // Gradient Sky Background
        val skyHeight = fullVirtualHeight * 0.65f
        for (i in 0..20) {
            val y = skyHeight * (i / 20f)
            val alpha = (i / 20f)
            val skyColor = Color(
                red = 0.05f + 0.4f * (1f - alpha),
                green = 0.1f + 0.1f * alpha,
                blue = 0.3f + 0.5f * alpha,
                alpha = 1.0f
            )
            drawRect(
                color = skyColor,
                topLeft = Offset(0f, y),
                size = Size(fullVirtualWidth, skyHeight / 20f)
            )
        }

        // Cyber Sun / Sphere in Center (Split across seam)
        val sunRadius = fullVirtualWidth * 0.15f
        val sunCenterX = fullVirtualWidth * 0.5f
        val sunCenterY = fullVirtualHeight * 0.4f

        drawCircle(
            color = Color(0xFFF43F5E),
            radius = sunRadius * 1.2f,
            center = Offset(sunCenterX, sunCenterY),
            alpha = 0.3f
        )
        drawCircle(
            color = Color(0xFFFB923C),
            radius = sunRadius,
            center = Offset(sunCenterX, sunCenterY)
        )

        // Geometric Mountain Peaks across full panoramic width
        val pathLeft = Path().apply {
            moveTo(0f, fullVirtualHeight * 0.7f)
            lineTo(fullVirtualWidth * 0.25f, fullVirtualHeight * 0.35f)
            lineTo(fullVirtualWidth * 0.5f, fullVirtualHeight * 0.65f)
            lineTo(fullVirtualWidth * 0.75f, fullVirtualHeight * 0.30f)
            lineTo(fullVirtualWidth, fullVirtualHeight * 0.7f)
            lineTo(fullVirtualWidth, fullVirtualHeight)
            lineTo(0f, fullVirtualHeight)
            close()
        }
        drawPath(pathLeft, color = Color(0xFF1E1B4B))

        // Cyber Grid Lines on Ground (Phase 3 alignment verify)
        val groundY = fullVirtualHeight * 0.65f
        val gridLines = 16
        for (i in 0..gridLines) {
            val startX = fullVirtualWidth * (i.toFloat() / gridLines)
            drawLine(
                color = Color(0xFF06B6D4),
                start = Offset(sunCenterX, sunCenterY + sunRadius),
                end = Offset(startX, fullVirtualHeight),
                strokeWidth = 2.dp.toPx(),
                alpha = 0.7f
            )
        }

        // Horizontal perspective lines on ground
        for (j in 1..10) {
            val py = groundY + (fullVirtualHeight - groundY) * (j.toFloat() / 10f) * (j.toFloat() / 10f)
            drawLine(
                color = Color(0xFF06B6D4),
                start = Offset(0f, py),
                end = Offset(fullVirtualWidth, py),
                strokeWidth = 1.5.dp.toPx(),
                alpha = 0.6f
            )
        }

        // Large Center Text Label across seam
        val paint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = fullVirtualWidth * 0.045f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        drawContext.canvas.nativeCanvas.drawText(
            "DUALSCREEN PANORAMA DEMO",
            sunCenterX,
            sunCenterY + sunRadius + 60f,
            paint
        )
    }
}

private fun DrawScope.drawAlignmentGrid(
    canvasWidth: Float,
    canvasHeight: Float,
    isHost: Boolean,
    isHorizontal: Boolean
) {
    val strokePx = 2.dp.toPx()
    val dashedEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)

    // Center Crosshairs
    val cx = canvasWidth / 2f
    val cy = canvasHeight / 2f

    drawLine(
        color = Color(0xFFE2E8F0),
        start = Offset(cx, 0f),
        end = Offset(cx, canvasHeight),
        strokeWidth = strokePx,
        pathEffect = dashedEffect
    )
    drawLine(
        color = Color(0xFFE2E8F0),
        start = Offset(0f, cy),
        end = Offset(canvasWidth, cy),
        strokeWidth = strokePx,
        pathEffect = dashedEffect
    )

    // Concentric calibration rings in center
    drawCircle(
        color = Color(0xFF38BDF8),
        radius = 80.dp.toPx(),
        center = Offset(cx, cy),
        style = Stroke(width = strokePx, pathEffect = dashedEffect)
    )
    drawCircle(
        color = Color(0xFF818CF8),
        radius = 140.dp.toPx(),
        center = Offset(cx, cy),
        style = Stroke(width = strokePx, pathEffect = dashedEffect)
    )

    // Corner alignment ticks
    val tickLength = 30.dp.toPx()
    val tickColor = Color(0xFFF43F5E)

    // Edge seam indicator
    val seamColor = if (isHost) Color(0xFF22C55E) else Color(0xFF3B82F6)
    if (isHorizontal) {
        val seamX = if (isHost) canvasWidth else 0f
        drawLine(
            color = seamColor,
            start = Offset(seamX, 0f),
            end = Offset(seamX, canvasHeight),
            strokeWidth = 6.dp.toPx()
        )
    } else {
        val seamY = if (isHost) canvasHeight else 0f
        drawLine(
            color = seamColor,
            start = Offset(0f, seamY),
            end = Offset(canvasWidth, seamY),
            strokeWidth = 6.dp.toPx()
        )
    }
}
