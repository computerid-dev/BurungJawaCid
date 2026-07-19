package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.GameViewModel
import com.example.game.ScreenState

// Javanese cultural color palette tokens
val JavaneseBrickRed = Color(0xFF8B2500)
val JavaneseGold = Color(0xFFD4AF37)
val JavaneseIvory = Color(0xFFFFFDD0)
val JavaneseDarkGreen = Color(0xFF1B4D3E)
val JavaneseCharcoal = Color(0xFF151515)

@OptIn(ExperimentalTextApi::class)
@Composable
fun GameAppContent(viewModel: GameViewModel) {
    val textMeasurer = rememberTextMeasurer()
    val context = LocalContext.current
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(JavaneseCharcoal)
    ) {
        when (viewModel.screenState) {
            ScreenState.HOME -> {
                HomeScreen(viewModel, textMeasurer)
            }
            ScreenState.SETTINGS -> {
                SettingsScreen(viewModel, textMeasurer)
            }
            ScreenState.ABOUT -> {
                AboutScreen(viewModel, textMeasurer)
            }
            ScreenState.PLAYING -> {
                // Main gameplay rendering
                GameCanvas(
                    viewModel = viewModel,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("game_play_area"),
                    textMeasurer = textMeasurer
                )
                
                // HUD scoring displays
                HUDLayer(viewModel)
            }
            ScreenState.PAUSED -> {
                // Game continues to show behind pause screen
                GameCanvas(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize(),
                    textMeasurer = textMeasurer
                )
                PauseOverlay(viewModel, textMeasurer)
            }
            ScreenState.FAILED -> {
                GameCanvas(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize(),
                    textMeasurer = textMeasurer
                )
                FailedScreen(viewModel, textMeasurer)
            }
            ScreenState.WIN -> {
                GameCanvas(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize(),
                    textMeasurer = textMeasurer
                )
                WinScreen(viewModel, textMeasurer)
            }
        }
    }
}

/**
 * Procedural Gunungan Wayang (Tree of Life) Drawing Composable
 */
@Composable
fun GununganWayangLogo(
    modifier: Modifier = Modifier,
    scale: Float = 1.0f,
    angle: Float = 0f
) {
    Canvas(
        modifier = modifier
            .size(240.dp)
            .rotate(angle)
    ) {
        val w = size.width
        val h = size.height
        
        // Leaf shape path
        val path = Path()
        path.moveTo(w / 2f, h * 0.05f) // top tip
        
        // Left side curve
        path.cubicTo(
            w * 0.15f, h * 0.25f,
            w * 0.02f, h * 0.65f,
            w * 0.15f, h * 0.88f
        )
        path.lineTo(w / 2f, h * 0.95f) // bottom center
        
        // Right side curve (symmetrical)
        path.lineTo(w * 0.85f, h * 0.88f)
        path.cubicTo(
            w * 0.98f, h * 0.65f,
            w * 0.85f, h * 0.25f,
            w / 2f, h * 0.05f
        )
        path.close()
        
        // Fill black silhouette
        drawPath(path = path, color = JavaneseCharcoal)
        // Gold outline
        drawPath(path = path, color = JavaneseGold, style = Stroke(width = 4f * scale))
        
        // Inner Javanese motifs (Traditional tree of life and gate)
        // 1. Center Trunk line
        drawLine(
            color = JavaneseGold,
            start = Offset(w / 2f, h * 0.95f),
            end = Offset(w / 2f, h * 0.35f),
            strokeWidth = 3f * scale
        )
        
        // 2. Branch sweeps
        val branchY = floatArrayOf(0.45f, 0.58f, 0.70f)
        for (y in branchY) {
            val by = h * y
            // Left branch
            drawLine(
                color = JavaneseGold,
                start = Offset(w / 2f, by),
                end = Offset(w * 0.28f, by - h * 0.05f),
                strokeWidth = 2f * scale
            )
            // Right branch
            drawLine(
                color = JavaneseGold,
                start = Offset(w / 2f, by),
                end = Offset(w * 0.72f, by - h * 0.05f),
                strokeWidth = 2f * scale
            )
        }
        
        // 3. Gate shape at bottom
        val gw = w * 0.25f
        val gh = h * 0.18f
        val gx = (w - gw) / 2f
        val gy = h * 0.73f
        
        drawRect(
            color = JavaneseBrickRed,
            topLeft = Offset(gx, gy),
            size = Size(gw, gh),
            style = Stroke(width = 2f * scale)
        )
        
        // Gate roof (triangle)
        val roof = Path()
        roof.moveTo(w / 2f, gy - h * 0.05f)
        roof.lineTo(gx - 5f, gy)
        roof.lineTo(gx + gw + 5f, gy)
        roof.close()
        
        drawPath(path = roof, color = JavaneseGold, style = Stroke(width = 2f * scale))
    }
}

/**
 * 1. HOME SCREEN
 */
@Composable
fun HomeScreen(viewModel: GameViewModel, textMeasurer: TextMeasurer) {
    // Parallax background floating animation
    val infiniteTransition = rememberInfiniteTransition(label = "home_float")
    val floatAnim by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gunungan_float"
    )
    
    // Watermark in bottom corner
    WatermarkOverlay(textMeasurer)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(JavaneseDarkGreen, JavaneseCharcoal)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Animated Logo
            Box(
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .offset(y = floatAnim.dp),
                contentAlignment = Alignment.Center
            ) {
                GununganWayangLogo(scale = 1.1f, angle = floatAnim * 0.3f)
            }
            
            // Javanese App Title
            Text(
                text = "BURUNG TERBANG",
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                color = JavaneseGold,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color.Black,
                        offset = Offset(4f, 4f),
                        blurRadius = 6f
                    )
                ),
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Tema Budaya Jawa | Game Arkade",
                fontSize = 15.sp,
                color = JavaneseIvory.copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            // Actions Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play Button
                Button(
                    onClick = { viewModel.startGame() },
                    colors = ButtonDefaults.buttonColors(containerColor = JavaneseGold),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(50.dp)
                        .testTag("play_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Main",
                        tint = JavaneseCharcoal,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(
                        text = "MAIN",
                        fontWeight = FontWeight.Bold,
                        color = JavaneseCharcoal,
                        fontSize = 18.sp
                    )
                }
                
                // Settings Button
                Button(
                    onClick = { viewModel.navigateTo(ScreenState.SETTINGS) },
                    colors = ButtonDefaults.buttonColors(containerColor = JavaneseBrickRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(50.dp)
                        .testTag("settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Pengaturan",
                        tint = JavaneseIvory,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(
                        text = "PENGATURAN",
                        fontWeight = FontWeight.Bold,
                        color = JavaneseIvory,
                        fontSize = 14.sp
                    )
                }

                // About/Info Button
                IconButton(
                    onClick = { viewModel.navigateTo(ScreenState.ABOUT) },
                    modifier = Modifier
                        .size(50.dp)
                        .background(JavaneseIvory.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .testTag("about_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = "Tentang",
                        tint = JavaneseGold
                    )
                }
            }
            
            // Display high score
            if (viewModel.highScore > 0) {
                Text(
                    text = "Skor Tertinggi Lokal: ${viewModel.highScore}",
                    fontSize = 14.sp,
                    color = JavaneseGold,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 16.dp),
                    style = TextStyle(background = JavaneseCharcoal.copy(alpha = 0.6f))
                )
            }
        }
    }
}

/**
 * 2. SETTINGS SCREEN
 */
@Composable
fun SettingsScreen(viewModel: GameViewModel, textMeasurer: TextMeasurer) {
    WatermarkOverlay(textMeasurer)
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(JavaneseCharcoal)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = JavaneseDarkGreen.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.5.dp, JavaneseGold),
            modifier = Modifier
                .widthIn(max = 550.dp)
                .fillMaxHeight(0.85f)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "PENGATURAN GAME",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = JavaneseGold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // --- Speed Selection ---
                Text(
                    text = "Kecepatan Burung",
                    color = JavaneseIvory,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf("Lambat", "Normal", "Cepat").forEach { speed ->
                        val isSelected = viewModel.birdSpeedSetting == speed
                        Button(
                            onClick = { 
                                viewModel.birdSpeedSetting = speed 
                                viewModel.saveSettings()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) JavaneseGold else JavaneseIvory.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("speed_$speed")
                        ) {
                            Text(
                                text = speed,
                                color = if (isSelected) JavaneseCharcoal else JavaneseIvory,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // --- Volume Selector ---
                Text(
                    text = "Volume Suara",
                    color = JavaneseIvory,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf("Kecil", "Normal", "Besar").forEach { vol ->
                        val isSelected = viewModel.volumeSetting == vol
                        Button(
                            onClick = { 
                                viewModel.volumeSetting = vol 
                                viewModel.saveSettings()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) JavaneseGold else JavaneseIvory.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("volume_$vol")
                        ) {
                            Text(
                                text = vol,
                                color = if (isSelected) JavaneseCharcoal else JavaneseIvory,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                
                // --- Toggle Buttons ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Musik Gamelan Latar", color = JavaneseIvory, fontSize = 15.sp)
                    Switch(
                        checked = viewModel.isBgmOn,
                        onCheckedChange = { 
                            viewModel.isBgmOn = it 
                            viewModel.saveSettings()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = JavaneseCharcoal,
                            checkedTrackColor = JavaneseGold,
                            uncheckedThumbColor = JavaneseGold,
                            uncheckedTrackColor = JavaneseCharcoal
                        ),
                        modifier = Modifier.testTag("bgm_switch")
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Getar (Vibration)", color = JavaneseIvory, fontSize = 15.sp)
                    Switch(
                        checked = viewModel.isVibrateOn,
                        onCheckedChange = { 
                            viewModel.isVibrateOn = it 
                            viewModel.saveSettings()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = JavaneseCharcoal,
                            checkedTrackColor = JavaneseGold,
                            uncheckedThumbColor = JavaneseGold,
                            uncheckedTrackColor = JavaneseCharcoal
                        ),
                        modifier = Modifier.testTag("vibrate_switch")
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                // --- Reset High Score ---
                Button(
                    onClick = { viewModel.resetHighScore() },
                    colors = ButtonDefaults.buttonColors(containerColor = JavaneseBrickRed),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("reset_highscore_button")
                ) {
                    Text(text = "RESET SKOR TERTINGGI", color = JavaneseIvory, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                // --- Back Button ---
                Button(
                    onClick = { viewModel.navigateTo(ScreenState.HOME) },
                    colors = ButtonDefaults.buttonColors(containerColor = JavaneseIvory),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("back_button")
                ) {
                    Text(text = "KEMBALI KE MENU", color = JavaneseCharcoal, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * 3. ABOUT / DEVELOPER SCREEN
 */
@Composable
fun AboutScreen(viewModel: GameViewModel, textMeasurer: TextMeasurer) {
    val context = LocalContext.current
    WatermarkOverlay(textMeasurer)
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(JavaneseCharcoal)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = JavaneseDarkGreen.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.5.dp, JavaneseGold),
            modifier = Modifier
                .widthIn(max = 500.dp)
                .fillMaxHeight(0.85f)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "TENTANG DEVELOPER",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = JavaneseGold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Divider(color = JavaneseGold.copy(alpha = 0.4f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // Dev Details
                DeveloperDetailRow(label = "Pembuat", value = "Nugroho Y.R")
                DeveloperDetailRow(label = "Brand", value = "Computer[ID]•GROUP")
                DeveloperDetailRow(label = "Email", value = "nugrohokelyn@gmail.com")
                DeveloperDetailRow(label = "GitHub", value = "ComputerID-Dev")
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Saluran WhatsApp Resmi:",
                    color = JavaneseGold,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
                
                // Clickable WA Link
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://whatsapp.com/channel/0029Vb8In46CMY0HCCQpYr1M"))
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)), // WhatsApp Green
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .testTag("whatsapp_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow, // Just a simple play symbol representing visit
                        contentDescription = "WhatsApp",
                        tint = Color.White,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(text = "Kunjungi Saluran WhatsApp", color = Color.White, fontWeight = FontWeight.Bold)
                }
                
                Text(
                    text = "Info & source resmi hanya tersedia di saluran WhatsApp di atas.",
                    color = JavaneseIvory.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.navigateTo(ScreenState.HOME) },
                    colors = ButtonDefaults.buttonColors(containerColor = JavaneseIvory),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("about_back_button")
                ) {
                    Text(text = "KEMBALI KE MENU", color = JavaneseCharcoal, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun DeveloperDetailRow(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(text = label, color = JavaneseGold.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(text = value, color = JavaneseIvory, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

/**
 * 4. IN-GAME HUD LAYER (Heads-up Display)
 */
@Composable
fun HUDLayer(viewModel: GameViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top-left: Real-time Score
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .background(JavaneseCharcoal.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Skor: ${viewModel.currentScore}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = JavaneseGold
            )
            Text(
                text = " / $viewModel.targetWinScore",
                fontSize = 14.sp,
                color = JavaneseIvory.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 2.dp)
            )
        }
        
        // Top-right: Pause Trigger
        IconButton(
            onClick = { viewModel.navigateTo(ScreenState.PAUSED) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .background(JavaneseCharcoal.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                .testTag("pause_hud_button")
        ) {
            Icon(
                imageVector = Icons.Filled.Settings, // Pause representation in classic aesthetic
                contentDescription = "Jeda",
                tint = JavaneseGold
            )
        }
        
        // Center top: Short instruction tip (tap/space bar to fly)
        if (viewModel.currentScore == 0) {
            Text(
                text = "Sentuh Layar atau Spasi untuk Terbang!",
                color = JavaneseIvory.copy(alpha = 0.7f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-100).dp)
                    .background(JavaneseCharcoal.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            )
        }
    }
}

/**
 * 5. PAUSE OVERLAY
 */
@Composable
fun PauseOverlay(viewModel: GameViewModel, textMeasurer: TextMeasurer) {
    WatermarkOverlay(textMeasurer)
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .width(320.dp)
                .background(JavaneseDarkGreen.copy(alpha = 0.9f), RoundedCornerShape(16.dp))
                .border(2.dp, JavaneseGold, RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            Text(
                text = "GAME DIJEDA",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = JavaneseGold,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            // Resume
            Button(
                onClick = { viewModel.navigateTo(ScreenState.PLAYING) },
                colors = ButtonDefaults.buttonColors(containerColor = JavaneseGold),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("resume_button")
            ) {
                Text(text = "LANJUTKAN", color = JavaneseCharcoal, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Restart
            Button(
                onClick = { viewModel.startGame() },
                colors = ButtonDefaults.buttonColors(containerColor = JavaneseIvory.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, JavaneseIvory),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("pause_restart_button")
            ) {
                Text(text = "ULANGI GAME", color = JavaneseIvory, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Home
            Button(
                onClick = { viewModel.navigateTo(ScreenState.HOME) },
                colors = ButtonDefaults.buttonColors(containerColor = JavaneseBrickRed),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("pause_home_button")
            ) {
                Text(text = "KEMBALI KE MENU", color = JavaneseIvory, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

/**
 * 6. GAME OVER / FAILED SCREEN
 */
@Composable
fun FailedScreen(viewModel: GameViewModel, textMeasurer: TextMeasurer) {
    WatermarkOverlay(textMeasurer)
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .width(320.dp)
                .background(JavaneseCharcoal, RoundedCornerShape(16.dp))
                .border(2.dp, JavaneseBrickRed, RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            Text(
                text = "BURUNG JATUH!",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = JavaneseBrickRed,
                modifier = Modifier.padding(bottom = 12.dp),
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Anda menabrak gapura atau batas tanah.",
                fontSize = 12.sp,
                color = JavaneseIvory.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 16.dp),
                textAlign = TextAlign.Center
            )
            
            // Scores Card
            Card(
                colors = CardDefaults.cardColors(containerColor = JavaneseDarkGreen.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                border = BorderStroke(1.dp, JavaneseGold.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "SKOR AKHIR", fontSize = 12.sp, color = JavaneseIvory.copy(alpha = 0.7f))
                    Text(text = "${viewModel.currentScore}", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = JavaneseGold)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(text = "SKOR TERTINGGI LOKAL", fontSize = 11.sp, color = JavaneseIvory.copy(alpha = 0.7f))
                    Text(text = "${viewModel.highScore}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = JavaneseIvory)
                }
            }
            
            // Try again
            Button(
                onClick = { viewModel.startGame() },
                colors = ButtonDefaults.buttonColors(containerColor = JavaneseGold),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("replay_button")
            ) {
                Text(text = "COBA LAGI", color = JavaneseCharcoal, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Home
            Button(
                onClick = { viewModel.navigateTo(ScreenState.HOME) },
                colors = ButtonDefaults.buttonColors(containerColor = JavaneseIvory.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("failed_home_button")
            ) {
                Text(text = "KEMBALI KE MENU", color = JavaneseIvory, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

/**
 * 7. WIN (VICTORY) SCREEN
 */
@Composable
fun WinScreen(viewModel: GameViewModel, textMeasurer: TextMeasurer) {
    WatermarkOverlay(textMeasurer)
    
    // Rotating golden Gunungan celebration animation
    val infiniteTransition = rememberInfiniteTransition(label = "win_rotation")
    val angle by infiniteTransition.animateFloat(
        initialValue = -15f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gunungan_spin"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .width(360.dp)
                .background(JavaneseDarkGreen, RoundedCornerShape(20.dp))
                .border(2.5.dp, JavaneseGold, RoundedCornerShape(20.dp))
                .padding(24.dp)
        ) {
            // Gunungan waving
            Box(
                modifier = Modifier.offset(y = (-10).dp),
                contentAlignment = Alignment.Center
            ) {
                GununganWayangLogo(scale = 0.9f, angle = angle)
            }
            
            Text(
                text = "MENANG / SUGENG!",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = JavaneseGold,
                modifier = Modifier.padding(bottom = 6.dp),
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Target $viewModel.targetWinScore Gapura berhasil dilalui!",
                fontSize = 13.sp,
                color = JavaneseIvory,
                modifier = Modifier.padding(bottom = 16.dp),
                textAlign = TextAlign.Center
            )
            
            // Celebratory Banner
            Card(
                colors = CardDefaults.cardColors(containerColor = JavaneseBrickRed),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "SKOR TERAKHIR", fontSize = 11.sp, color = JavaneseIvory.copy(alpha = 0.8f))
                    Text(text = "${viewModel.currentScore}", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = JavaneseIvory)
                }
            }
            
            // Play more / restart
            Button(
                onClick = { viewModel.startGame() },
                colors = ButtonDefaults.buttonColors(containerColor = JavaneseGold),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("win_replay_button")
            ) {
                Text(text = "MAIN LAGI", color = JavaneseCharcoal, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Home
            Button(
                onClick = { viewModel.navigateTo(ScreenState.HOME) },
                colors = ButtonDefaults.buttonColors(containerColor = JavaneseIvory.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("win_home_button")
            ) {
                Text(text = "KEMBALI KE MENU", color = JavaneseIvory, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

/**
 * Common overlay to render watermark text "Nugroho Y.R" in bottom-right corner consistently
 */
@Composable
fun WatermarkOverlay(textMeasurer: TextMeasurer) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        val textLayoutResult = textMeasurer.measure(
            text = AnnotatedString("Nugroho Y.R"),
            style = TextStyle(
                color = Color.White.copy(alpha = 0.15f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(
                    x = size.width - textLayoutResult.size.width - 24f,
                    y = size.height - textLayoutResult.size.height - 24f
                )
            )
        }
    }
}
