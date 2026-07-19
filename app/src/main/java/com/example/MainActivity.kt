package com.example

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.game.GameViewModel
import com.example.ui.GameAppContent
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    
    // Lazy initialize our GameViewModel
    private val gameViewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Force Landscape orientation programmatically
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        
        // 2. Enable Edge-to-Edge and hide System Bars (Fullscreen Mode)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())

        setContent {
            MyApplicationTheme {
                GameAppContent(viewModel = gameViewModel)
            }
        }
    }

    /**
     * Listen for key events to support PC/emulator players using keyboards.
     * Spacebar triggers the bird flap jump!
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_SPACE) {
            gameViewModel.jump()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onResume() {
        super.onResume()
        // Ensure immersive fullscreen continues on resume
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        
        // Restart gamelan background music if it was running and on
        if (gameViewModel.isBgmOn) {
            gameViewModel.soundManager.startMusic()
        }
    }

    override fun onPause() {
        super.onPause()
        // Pause music on minimize
        gameViewModel.soundManager.stopMusic()
    }
}
