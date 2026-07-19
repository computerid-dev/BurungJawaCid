package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.GameViewModel
import com.example.game.Obstacle
import com.example.game.Particle
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalTextApi::class)
@Composable
fun GameCanvas(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier,
    textMeasurer: TextMeasurer
) {
    val birdY = viewModel.birdY
    val birdX = viewModel.birdX
    val birdRadius = viewModel.birdRadius
    val obstacles = viewModel.obstacles
    val particles = viewModel.particles
    
    // Parallax values
    val bgFar = viewModel.bgParallaxFar
    val bgNear = viewModel.bgParallaxNear

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        viewModel.jump()
                    }
                )
            }
    ) {
        val width = size.width
        val height = size.height
        
        // Scale factors to map 1000x1000 virtual space to screen pixels
        val sx = width / 1000f
        val sy = height / 1000f

        // 1. DRAW SKY GRADIENT (Javanese colors: Golden yellow to Brick Red / Dark Orange)
        val skyGradient = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF800000), // Dark Brick Red / Maroon
                Color(0xFFD2691E), // Chocolate / Ochre
                Color(0xFFFFD700)  // Golden Yellow
            ),
            startY = 0f,
            endY = height * 0.8f
        )
        drawRect(brush = skyGradient, size = size)

        // 2. DRAW PARALLAX BACKGROUND: FAR MOUNTAINS (Siluet Gunung Jawa)
        drawFarMountains(width, height, bgFar)

        // 3. DRAW PARALLAX BACKGROUND: NEAR HILLS & SAWAH (Sawah Terasering)
        drawNearHillsAndSawah(width, height, bgNear)

        // 4. DRAW DECORATIVE BATIK CLOUDS / SUN
        drawJavaSunAndClouds(width, height)

        // 5. DRAW GAPURA OBSTACLES (Candi Bentar Javanese Brick Gate)
        val gapHeight = when (viewModel.birdSpeedSetting) {
            "Lambat" -> 300f
            "Normal" -> 260f
            "Cepat" -> 220f
            else -> 260f
        }
        for (obs in obstacles) {
            drawGapuraObstacle(obs, gapHeight, sx, sy)
        }

        // 6. DRAW GROUND & CEILING DECORATIVE BORDERS
        drawDecorativeBorders(width, height, sx, sy)

        // 7. DRAW PARTICLES (Trailing feathers or score celebration sparkles)
        for (p in particles) {
            drawGameParticle(p, sx, sy)
        }

        // 8. DRAW WAYANG BIRD CHARACTER
        drawWayangBird(birdX * sx, birdY * sy, birdRadius * sx, viewModel.birdVelocity, this)

        // 9. DRAW THE WATERMARK "Nugroho Y.R" (Samar, Low opacity in bottom right)
        drawWatermark(width, height, textMeasurer)
    }
}

private fun DrawScope.drawFarMountains(width: Float, height: Float, parallaxOffset: Float) {
    // We render 2 mountain peaks
    // Mountain shape can be generated procedurally
    val mountColor = Color(0xFF3E2723) // Very deep brown/charcoal silhouetted
    val path = Path()
    
    // Convert parallax (0..1000) to actual pixels
    val offsetPx = (parallaxOffset / 1000f) * width
    
    // Draw two overlapping peaks looping smoothly
    val mountainWidth = width * 1.2f
    
    for (i in 0..1) {
        val startX = (i * mountainWidth) - offsetPx
        
        path.reset()
        path.moveTo(startX, height * 0.75f)
        path.lineTo(startX + mountainWidth * 0.25f, height * 0.45f) // Peak 1
        path.lineTo(startX + mountainWidth * 0.4f, height * 0.65f)  // Valley
        path.lineTo(startX + mountainWidth * 0.65f, height * 0.38f) // Peak 2 (higher)
        path.lineTo(startX + mountainWidth * 0.95f, height * 0.75f) // Valley
        path.lineTo(startX + mountainWidth, height * 0.75f)
        path.lineTo(startX + mountainWidth, height)
        path.lineTo(startX, height)
        path.close()
        
        drawPath(path = path, color = mountColor, alpha = 0.85f)
    }
}

private fun DrawScope.drawNearHillsAndSawah(width: Float, height: Float, parallaxOffset: Float) {
    // Sawah terraces (overlapping round green-brown layers)
    val hillColor = Color(0xFF1B4D3E) // Dark Forest Green
    val hillColor2 = Color(0xFF2E6F40) // Olive Green Sawah
    
    val offsetPx = (parallaxOffset / 1000f) * width
    val chunkWidth = width * 1.1f
    
    val path = Path()
    
    // Draw Background Terraces (slightly darker)
    for (i in 0..1) {
        val startX = (i * chunkWidth) - offsetPx
        path.reset()
        path.moveTo(startX, height * 0.85f)
        path.quadraticTo(startX + chunkWidth * 0.3f, height * 0.68f, startX + chunkWidth * 0.6f, height * 0.78f)
        path.quadraticTo(startX + chunkWidth * 0.85f, height * 0.70f, startX + chunkWidth, height * 0.85f)
        path.lineTo(startX + chunkWidth, height)
        path.lineTo(startX, height)
        path.close()
        drawPath(path = path, color = hillColor, alpha = 0.9f)
    }
    
    // Draw Foreground Terraces with sawah contour lines
    for (i in 0..1) {
        val startX = (i * chunkWidth) - offsetPx + 150f
        path.reset()
        path.moveTo(startX, height * 0.9f)
        path.quadraticTo(startX + chunkWidth * 0.25f, height * 0.75f, startX + chunkWidth * 0.5f, height * 0.82f)
        path.quadraticTo(startX + chunkWidth * 0.75f, height * 0.74f, startX + chunkWidth, height * 0.9f)
        path.lineTo(startX + chunkWidth, height)
        path.lineTo(startX, height)
        path.close()
        drawPath(path = path, color = hillColor2, alpha = 1.0f)
        
        // Draw some golden lines for harvest rice details
        drawContourLine(startX, startX + chunkWidth, height * 0.82f, height * 0.85f)
    }
}

private fun DrawScope.drawContourLine(startX: Float, endX: Float, midY1: Float, midY2: Float) {
    val strokeColor = Color(0xFFD4AF37) // Golden yellow line representing ripe rice fields
    val path = Path()
    path.moveTo(startX, midY2 + 20f)
    path.quadraticTo((startX + endX)/2f - 100f, midY1 + 10f, endX, midY2 + 10f)
    drawPath(path = path, color = strokeColor, style = Stroke(width = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)), alpha = 0.4f)
}

private fun DrawScope.drawJavaSunAndClouds(width: Float, height: Float) {
    // Beautiful massive golden mystical sun
    drawCircle(
        color = Color(0xFFFFD700),
        radius = width * 0.08f,
        center = Offset(width * 0.85f, height * 0.25f),
        alpha = 0.65f
    )
    
    // Sun rays
    drawCircle(
        color = Color(0xFFFFE4B5),
        radius = width * 0.11f,
        center = Offset(width * 0.85f, height * 0.25f),
        alpha = 0.2f
    )
}

private fun DrawScope.drawGapuraObstacle(obs: Obstacle, gapHeight: Float, sx: Float, sy: Float) {
    // Map virtual coordinates to real screen coordinates
    val rx = obs.x * sx
    val rw = obs.width * sx
    
    val gapCenterY = obs.gapY * sy
    val hGap = gapHeight * sy
    
    val topGapLimit = gapCenterY - (hGap / 2f)
    val bottomGapLimit = gapCenterY + (hGap / 2f)
    
    // Styling colors for Javanese Brick Pillars (Candi Bentar Gate style)
    val brickColor = Color(0xFF8B2500) // Deep Terracotta / Red Brick
    val capColor = Color(0xFF4A1500)   // Dark Carved Wood/Stone Cap
    val reliefColor = Color(0xFFEEDC82) // Gold-tint carvings relief
    
    // --- DRAW TOP PILLAR (Hanging Down) ---
    // Pillar trunk
    drawRect(
        color = brickColor,
        topLeft = Offset(rx, 0f),
        size = Size(rw, topGapLimit)
    )
    // Pillar bottom cap (the carved temple edge facing the gap)
    val capHeight = 35f
    drawRect(
        color = capColor,
        topLeft = Offset(rx - 8f, topGapLimit - capHeight),
        size = Size(rw + 16f, capHeight)
    )
    // Stepped trim details on the top pillar
    drawRect(
        color = Color.Black,
        topLeft = Offset(rx, topGapLimit - capHeight),
        size = Size(rw, 3f),
        alpha = 0.5f
    )
    // Vertical Javanese stone carvings / stripes details on top pillar
    for (i in 1..4) {
        val lineX = rx + (rw * 0.2f * i)
        drawLine(
            color = reliefColor,
            start = Offset(lineX, 20f),
            end = Offset(lineX, topGapLimit - capHeight - 10f),
            strokeWidth = 2f,
            alpha = 0.25f
        )
    }

    // --- DRAW BOTTOM PILLAR (Rising Up) ---
    // Pillar trunk
    drawRect(
        color = brickColor,
        topLeft = Offset(rx, bottomGapLimit),
        size = Size(rw, size.height - bottomGapLimit)
    )
    // Pillar top cap (the carved temple edge facing the gap)
    drawRect(
        color = capColor,
        topLeft = Offset(rx - 8f, bottomGapLimit),
        size = Size(rw + 16f, capHeight)
    )
    // Stepped trim details on the bottom pillar
    drawRect(
        color = Color.Black,
        topLeft = Offset(rx, bottomGapLimit + capHeight),
        size = Size(rw, 3f),
        alpha = 0.5f
    )
    // Stone lines decoration on bottom pillar
    var brickRowY = bottomGapLimit + capHeight + 15f
    while (brickRowY < size.height) {
        drawLine(
            color = Color.Black,
            start = Offset(rx, brickRowY),
            end = Offset(rx + rw, brickRowY),
            strokeWidth = 1.5f,
            alpha = 0.3f
        )
        brickRowY += 40f
    }
    // Vertical Javanese stone carvings / stripes on bottom pillar
    for (i in 1..4) {
        val lineX = rx + (rw * 0.2f * i)
        drawLine(
            color = reliefColor,
            start = Offset(lineX, bottomGapLimit + capHeight + 10f),
            end = Offset(lineX, size.height - 20f),
            strokeWidth = 2f,
            alpha = 0.25f
        )
    }
}

private fun DrawScope.drawDecorativeBorders(width: Float, height: Float, sx: Float, sy: Float) {
    // Beautiful Batik-patterned horizontal boundaries at top and bottom (representing Javanese borders)
    val borderGold = Color(0xFFD4AF37) // Golden
    val borderDark = Color(0xFF1A1A1A) // Black/Dark Charcoal
    
    val borderWidth = 12f
    
    // Top Border
    drawRect(color = borderDark, topLeft = Offset.Zero, size = Size(width, borderWidth))
    drawRect(color = borderGold, topLeft = Offset(0f, borderWidth), size = Size(width, 3f))
    
    // Bottom Border (represents ground)
    drawRect(color = borderGold, topLeft = Offset(0f, height - borderWidth - 3f), size = Size(width, 3f))
    drawRect(color = borderDark, topLeft = Offset(0f, height - borderWidth), size = Size(width, borderWidth))
    
    // Draw simple pattern dots along the borders (Batik Kawung / coin visual representation)
    val dotSpacing = 40f
    var currX = 20f
    while (currX < width) {
        drawCircle(color = borderGold, radius = 3f, center = Offset(currX, borderWidth / 2f))
        drawCircle(color = borderGold, radius = 3f, center = Offset(currX, height - borderWidth / 2f))
        currX += dotSpacing
    }
}

private fun DrawScope.drawGameParticle(p: Particle, sx: Float, sy: Float) {
    val px = p.x * sx
    val py = p.y * sy
    val pSize = p.size * sx
    
    val color = when (p.colorType) {
        0 -> Color(0xFFD4AF37) // Gold sparkle / feather
        1 -> Color(0xFFB22222) // Red firework sparkle
        else -> Color(0xFF2E8B57) // Green leaf particle
    }
    
    // Draw diamonds for stars, circles for leaves
    if (p.colorType == 0) {
        // Diamond spark
        val path = Path()
        path.moveTo(px, py - pSize)
        path.lineTo(px + pSize, py)
        path.lineTo(px, py + pSize)
        path.lineTo(px - pSize, py)
        path.close()
        drawPath(path = path, color = color, alpha = p.alpha)
    } else {
        // Round glowing particles
        drawCircle(color = color, radius = pSize / 2f, center = Offset(px, py), alpha = p.alpha)
    }
}

/**
 * Draws a highly stylized Javanese WAYANG KULIT Bird character.
 * Uses a pure black silhouette core with elegant golden ornaments, an arched tail, and fine curved beak.
 */
private fun drawWayangBird(bx: Float, by: Float, br: Float, velocity: Float, scope: DrawScope) {
    scope.apply {
        // Base puppet coloring
        val shadowColor = Color(0xFF111111) // Shadow puppet black
        val goldAccent = Color(0xFFD4AF37)  // Wayang gold leaf detailing
        val whiteEye = Color.White
        
        // Wing rotation based on velocity (makes it look like it is flapping organically)
        // Up/down oscillation
        val wingAngleDeg = -velocity * 4f
        val wingRads = Math.toRadians(wingAngleDeg.toDouble())
        
        // 1. DRAW LONG STYLIZED WAYANG TAIL (Arched feather plume)
        val tailPath = Path()
        tailPath.moveTo(bx - br * 0.4f, by + br * 0.2f)
        // Elegant curlicue backwards and upwards
        tailPath.cubicTo(
            bx - br * 1.8f, by + br * 0.5f,
            bx - br * 1.6f, by - br * 1.5f,
            bx - br * 2.2f, by - br * 1.2f
        )
        tailPath.cubicTo(
            bx - br * 1.4f, by - br * 1.0f,
            bx - br * 1.1f, by - br * 0.2f,
            bx - br * 0.6f, by + br * 0.2f
        )
        tailPath.close()
        drawPath(path = tailPath, color = shadowColor)
        drawPath(path = tailPath, color = goldAccent, style = Stroke(width = 2f), alpha = 0.8f)

        // 2. DRAW MAIN BIRD BODY (Plump Javanese Rooster/Wayang bird shape)
        drawCircle(
            color = shadowColor,
            radius = br,
            center = Offset(bx, by)
        )
        // Gold rim carving lines around the body
        drawCircle(
            color = goldAccent,
            radius = br - 3f,
            center = Offset(bx, by),
            style = Stroke(width = 1.5f),
            alpha = 0.8f
        )

        // 3. DRAW STYLIZED WAYANG BEAK (Thin, long, elegant curved beak)
        val beakPath = Path()
        beakPath.moveTo(bx + br * 0.7f, by - br * 0.2f)
        // Sweeps forward and curves down sharply at the tip
        beakPath.quadraticTo(bx + br * 2.2f, by - br * 0.4f, bx + br * 2.5f, by + br * 0.1f)
        beakPath.quadraticTo(bx + br * 1.8f, by + br * 0.3f, bx + br * 0.7f, by + br * 0.2f)
        beakPath.close()
        drawPath(path = beakPath, color = shadowColor)
        drawPath(path = beakPath, color = goldAccent, style = Stroke(width = 1.5f))

        // 4. DRAW GELUNG WIDENG (Traditional wayang crown curly hair bun at back of head)
        val crownPath = Path()
        crownPath.moveTo(bx - br * 0.3f, by - br * 0.9f)
        crownPath.cubicTo(
            bx - br * 1.2f, by - br * 1.8f,
            bx - br * 0.2f, by - br * 2.0f,
            bx + br * 0.2f, by - br * 1.4f
        )
        crownPath.quadraticTo(bx - br * 0.1f, by - br * 1.1f, bx - br * 0.2f, by - br * 0.9f)
        crownPath.close()
        drawPath(path = crownPath, color = shadowColor)
        drawPath(path = crownPath, color = goldAccent, style = Stroke(width = 1.5f))

        // 5. DRAW THE ALMOND WAYANG EYE (Indra/Liyapan style eye)
        val eyeX = bx + br * 0.4f
        val eyeY = by - br * 0.2f
        val eyePath = Path()
        eyePath.moveTo(eyeX - 8f, eyeY)
        eyePath.quadraticTo(eyeX, eyeY - 5f, eyeX + 8f, eyeY)
        eyePath.quadraticTo(eyeX, eyeY + 5f, eyeX - 8f, eyeY)
        eyePath.close()
        drawPath(path = eyePath, color = whiteEye)
        drawCircle(color = Color.Black, radius = 2.5f, center = Offset(eyeX, eyeY))

        // 6. DRAW THE FLAPPING WING (Articulated shadow puppet wing with gold details)
        val wingPath = Path()
        val wingBaseX = bx - br * 0.1f
        val wingBaseY = by - br * 0.1f
        
        // Compute wing tip offset based on angle
        val wingLen = br * 1.4f
        val tipX = wingBaseX - wingLen * cos(wingRads).toFloat()
        val tipY = wingBaseY - wingLen * sin(wingRads).toFloat() - br * 0.5f
        
        wingPath.moveTo(wingBaseX, wingBaseY)
        wingPath.quadraticTo(wingBaseX - br * 0.6f, wingBaseY - br * 1.3f, tipX, tipY)
        wingPath.quadraticTo(wingBaseX + br * 0.4f, wingBaseY - br * 0.5f, wingBaseX, wingBaseY)
        wingPath.close()
        
        drawPath(path = wingPath, color = shadowColor)
        drawPath(path = wingPath, color = goldAccent, style = Stroke(width = 2f))
        
        // Internal gold lines representing wing feathers
        drawLine(
            color = goldAccent,
            start = Offset(wingBaseX - 10f, wingBaseY - 10f),
            end = Offset(tipX + 10f, tipY + 10f),
            strokeWidth = 1.5f,
            alpha = 0.7f
        )
    }
}

private fun DrawScope.drawWatermark(width: Float, height: Float, textMeasurer: TextMeasurer) {
    // low opacity (0.15f) watermark in the corner (bottom right, slightly above navigation/border)
    val text = "Nugroho Y.R"
    val textLayoutResult = textMeasurer.measure(
        text = AnnotatedString(text),
        style = TextStyle(
            color = Color.White.copy(alpha = 0.15f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    )
    
    drawText(
        textLayoutResult = textLayoutResult,
        topLeft = Offset(
            x = width - textLayoutResult.size.width - 24f,
            y = height - textLayoutResult.size.height - 24f
        )
    )
}
