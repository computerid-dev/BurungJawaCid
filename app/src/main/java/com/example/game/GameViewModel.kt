package com.example.game

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sound.SoundManager
import kotlinx.coroutines.*
import java.util.Random

enum class ScreenState {
    HOME,
    SETTINGS,
    PLAYING,
    PAUSED,
    FAILED,
    WIN,
    ABOUT
}

data class Obstacle(
    val id: Long,
    var x: Float, // virtual horizontal X position (0..1000)
    val gapY: Float, // virtual vertical center of gap (0..1000)
    val gapHeight: Float, // virtual gap height size (0..1000)
    val width: Float = 130f,
    var passed: Boolean = false
)

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var alpha: Float,
    val colorType: Int, // 0: Gold, 1: Red, 2: Green
    val size: Float
)

class GameViewModel(application: Application) : AndroidViewModel(application) {
    
    // Shared Preferences for saving scores and settings offline
    private val sharedPrefs = application.getSharedPreferences("BurungTerbangPrefs", Context.MODE_PRIVATE)
    val soundManager = SoundManager(application)
    
    // Game state variables
    var screenState by mutableStateOf(ScreenState.HOME)
        private set
    
    // Bird properties (virtual coordinate space 0..1000)
    var birdY by mutableStateOf(500f)
        private set
    var birdVelocity by mutableStateOf(0f)
        private set
    val birdX = 220f
    val birdRadius = 24f
    
    // Scores
    var currentScore by mutableStateOf(0)
        private set
    var highScore by mutableStateOf(0)
        private set
        
    // Settings
    var birdSpeedSetting by mutableStateOf("Normal") // Lambat, Normal, Cepat
    var volumeSetting by mutableStateOf("Normal") // Kecil, Normal, Besar
    var isBgmOn by mutableStateOf(true)
    var isVibrateOn by mutableStateOf(true)
    
    // Win score target
    val targetWinScore = 25
    
    // Obstacles list
    val obstacles = mutableStateListOf<Obstacle>()
    private var nextObstacleId = 0L
    
    // Visual environment
    var bgParallaxFar by mutableStateOf(0f)
        private set
    var bgParallaxNear by mutableStateOf(0f)
        private set
    
    // Interactive particle effects (e.g. trailing feathers, celebration sparks)
    val particles = mutableStateListOf<Particle>()
    
    // Game loop jobs
    private var gameLoopJob: Job? = null
    private val random = Random()
    
    init {
        loadSettings()
        soundManager.startMusic()
    }
    
    private fun loadSettings() {
        highScore = sharedPrefs.getInt("high_score", 0)
        birdSpeedSetting = sharedPrefs.getString("bird_speed", "Normal") ?: "Normal"
        volumeSetting = sharedPrefs.getString("volume", "Normal") ?: "Normal"
        isBgmOn = sharedPrefs.getBoolean("bgm_on", true)
        isVibrateOn = sharedPrefs.getBoolean("vibrate_on", true)
    }
    
    fun saveSettings() {
        sharedPrefs.edit().apply {
            putString("bird_speed", birdSpeedSetting)
            putString("volume", volumeSetting)
            putBoolean("bgm_on", isBgmOn)
            putBoolean("vibrate_on", isVibrateOn)
            apply()
        }
        soundManager.updateSettings(!isBgmOn, isVibrateOn, volumeSetting)
    }
    
    fun resetHighScore() {
        highScore = 0
        sharedPrefs.edit().putInt("high_score", 0).apply()
        soundManager.playScore() // Play pleasant sound on action
    }
    
    // Navigation actions
    fun navigateTo(state: ScreenState) {
        screenState = state
        if (state == ScreenState.HOME) {
            soundManager.startMusic()
        }
    }
    
    // Start game
    fun startGame() {
        birdY = 450f
        birdVelocity = 0f
        currentScore = 0
        obstacles.clear()
        particles.clear()
        nextObstacleId = 0L
        
        // Spawn first obstacle
        spawnObstacle(1100f)
        spawnObstacle(1600f)
        
        navigateTo(ScreenState.PLAYING)
        
        // Start loop
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch(Dispatchers.Default) {
            val tickRateMs = 16L // ~60fps
            
            // Physics constant depending on selected bird speed
            val speedFactor = when (birdSpeedSetting) {
                "Lambat" -> 3.8f
                "Normal" -> 5.2f
                "Cepat" -> 6.8f
                else -> 5.2f
            }
            
            val gapHeight = when (birdSpeedSetting) {
                "Lambat" -> 300f
                "Normal" -> 260f
                "Cepat" -> 220f
                else -> 260f
            }
            
            val gravity = 0.35f
            
            while (isActive && screenState == ScreenState.PLAYING) {
                // 1. Apply gravity to bird
                birdVelocity += gravity
                birdY += birdVelocity
                
                // 2. Parallax background movement
                bgParallaxFar = (bgParallaxFar + speedFactor * 0.15f) % 1000f
                bgParallaxNear = (bgParallaxNear + speedFactor * 0.4f) % 1000f
                
                // 3. Update particles
                updateParticles()
                
                // 4. Update obstacles and check collisions
                var collisionDetected = false
                val iterator = obstacles.iterator()
                val updatedObstacles = mutableListOf<Obstacle>()
                
                var spawnNewAtEnd = false
                
                while (iterator.hasNext()) {
                    val obs = iterator.next()
                    obs.x -= speedFactor
                    
                    // Check score pass
                    if (!obs.passed && obs.x + obs.width < birdX) {
                        obs.passed = true
                        viewModelScope.launch(Dispatchers.Main) {
                            currentScore++
                            soundManager.playScore()
                            // Spawn some score celebration sparkles
                            spawnScoreParticles(birdX, birdY)
                            
                            if (currentScore >= targetWinScore) {
                                triggerWin()
                            }
                        }
                    }
                    
                    // Check collision
                    if (checkCollision(obs, gapHeight)) {
                        collisionDetected = true
                    }
                    
                    if (obs.x + obs.width < -100f) {
                        // Mark for removal
                    } else {
                        updatedObstacles.add(obs)
                    }
                }
                
                // Sync elements
                withContext(Dispatchers.Main) {
                    obstacles.retainAll { it.x + it.width >= -100f }
                }
                
                // If last obstacle is far enough, spawn another
                val lastObs = obstacles.lastOrNull()
                if (lastObs != null && lastObs.x < 650f) {
                    spawnObstacle(1100f)
                }
                
                // Check bounds (ground or ceiling crashes)
                if (birdY - birdRadius <= 50f || birdY + birdRadius >= 950f) {
                    collisionDetected = true
                }
                
                if (collisionDetected) {
                    triggerFail()
                }
                
                delay(tickRateMs)
            }
        }
    }
    
    // Jump controller (called on touch or keyboard space)
    fun jump() {
        if (screenState == ScreenState.PLAYING) {
            // Negative velocity moves bird upward in screen coordinates (0 at top, 1000 at bottom)
            birdVelocity = -6.8f
            soundManager.playFlap()
            
            // Spawn feather particles trailing
            spawnFeatherParticles(birdX, birdY)
        }
    }
    
    private fun spawnObstacle(startX: Float) {
        val gapHeight = when (birdSpeedSetting) {
            "Lambat" -> 300f
            "Normal" -> 260f
            "Cepat" -> 220f
            else -> 260f
        }
        // gapY center can range from 200 to 700 to stay safely away from ceiling/ground
        val minGapY = 250f
        val maxGapY = 750f
        val gapY = minGapY + random.nextFloat() * (maxGapY - minGapY)
        
        viewModelScope.launch(Dispatchers.Main) {
            obstacles.add(
                Obstacle(
                    id = nextObstacleId++,
                    x = startX,
                    gapY = gapY,
                    gapHeight = gapHeight
                )
            )
        }
    }
    
    private fun checkCollision(obs: Obstacle, gapHeight: Float): Boolean {
        // Simple circle-box overlap check
        val birdLeft = birdX - birdRadius
        val birdRight = birdX + birdRadius
        val birdTop = birdY - birdRadius
        val birdBottom = birdY + birdRadius
        
        val obsLeft = obs.x
        val obsRight = obs.x + obs.width
        
        // Gap bounds
        val gapTop = obs.gapY - (gapHeight / 2f)
        val gapBottom = obs.gapY + (gapHeight / 2f)
        
        // If bird is horizontally overlapping with obstacle
        if (birdRight >= obsLeft && birdLeft <= obsRight) {
            // If bird is NOT completely within the vertical gap, it collides!
            if (birdTop < gapTop || birdBottom > gapBottom) {
                return true
            }
        }
        return false
    }
    
    private fun triggerFail() {
        viewModelScope.launch(Dispatchers.Main) {
            soundManager.playCrash()
            delay(150)
            soundManager.playFail()
            screenState = ScreenState.FAILED
            
            // Check high score
            if (currentScore > highScore) {
                highScore = currentScore
                sharedPrefs.edit().putInt("high_score", highScore).apply()
            }
        }
    }
    
    private fun triggerWin() {
        viewModelScope.launch(Dispatchers.Main) {
            soundManager.playWin()
            screenState = ScreenState.WIN
            
            // Check high score
            if (currentScore > highScore) {
                highScore = currentScore
                sharedPrefs.edit().putInt("high_score", highScore).apply()
            }
            
            // Spawn extra victory particles!
            for (i in 0 until 50) {
                spawnVictorySparkle()
            }
        }
    }
    
    private fun updateParticles() {
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.x += p.vx
            p.y += p.vy
            p.alpha -= 0.02f
            if (p.alpha <= 0f) {
                // Remove particle safely on next iteration or inside lock
            }
        }
        
        // Run clean up on Main
        viewModelScope.launch(Dispatchers.Main) {
            particles.removeAll { it.alpha <= 0f }
        }
    }
    
    private fun spawnFeatherParticles(bx: Float, by: Float) {
        viewModelScope.launch(Dispatchers.Main) {
            for (i in 0..2) {
                particles.add(
                    Particle(
                        x = bx - 10f,
                        y = by + (random.nextFloat() * 10f - 5f),
                        vx = -1.5f - random.nextFloat() * 2f,
                        vy = -1.0f + random.nextFloat() * 2f,
                        alpha = 1.0f,
                        colorType = 0, // Gold/feather color
                        size = 6f + random.nextFloat() * 8f
                    )
                )
            }
        }
    }
    
    private fun spawnScoreParticles(bx: Float, by: Float) {
        viewModelScope.launch(Dispatchers.Main) {
            for (i in 0..8) {
                particles.add(
                    Particle(
                        x = bx,
                        y = by,
                        vx = (random.nextFloat() * 6f - 3f),
                        vy = (random.nextFloat() * 6f - 3f),
                        alpha = 1.0f,
                        colorType = random.nextInt(3), // gold, red, green
                        size = 5f + random.nextFloat() * 10f
                    )
                )
            }
        }
    }
    
    private fun spawnVictorySparkle() {
        viewModelScope.launch(Dispatchers.Main) {
            particles.add(
                Particle(
                    x = 200f + random.nextFloat() * 600f,
                    y = 100f + random.nextFloat() * 500f,
                    vx = (random.nextFloat() * 4f - 2f),
                    vy = (random.nextFloat() * 4f - 2f),
                    alpha = 1.0f,
                    colorType = random.nextInt(3),
                    size = 8f + random.nextFloat() * 12f
                )
            )
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        soundManager.release()
    }
}
