package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.core.appcheck.configureAppCheck
import com.example.core.audio.AudioEffectPlayer
import com.example.core.audio.AudioFileManager
import com.example.core.audio.AudioRecorder
import com.example.core.call.AiCallEngine
import com.example.core.call.CallStatus
import com.example.ui.navigation.Screen
import com.example.ui.screens.ActiveCallScreen
import com.example.ui.screens.AiCallScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.IncomingCallScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VoiceChangerScreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.MagicVoiceTheme
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

class MainActivity : ComponentActivity() {

    private lateinit var audioRecorder: AudioRecorder
    private lateinit var audioPlayer: AudioEffectPlayer
    private lateinit var audioFileManager: AudioFileManager
    private lateinit var aiCallEngine: AiCallEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Mandatory App Check initialization with launch intent before any UI/Firebase operations
        configureAppCheck(this, intent)

        // Initialize Audio Engines
        audioRecorder = AudioRecorder(this)
        audioPlayer = AudioEffectPlayer(this)
        audioFileManager = AudioFileManager(this)
        aiCallEngine = AiCallEngine(this)

        enableEdgeToEdge()

        setContent {
            MagicVoiceTheme {
                MainAppContent(
                    audioRecorder = audioRecorder,
                    audioPlayer = audioPlayer,
                    audioFileManager = audioFileManager,
                    aiCallEngine = aiCallEngine
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioRecorder.stopRecording()
        audioPlayer.release()
        aiCallEngine.release()
    }
}

@Composable
fun MainAppContent(
    audioRecorder: AudioRecorder,
    audioPlayer: AudioEffectPlayer,
    audioFileManager: AudioFileManager,
    aiCallEngine: AiCallEngine
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    val activeCallSession by aiCallEngine.callSession.collectAsState()

    // Global Back Handler
    BackHandler(enabled = activeCallSession != null || currentScreen != Screen.Home) {
        if (activeCallSession != null && activeCallSession?.status != CallStatus.CALL_ENDED) {
            aiCallEngine.endCall(saveHistory = true)
        } else if (activeCallSession?.status == CallStatus.CALL_ENDED) {
            aiCallEngine.resetCallSession()
        } else if (currentScreen != Screen.Home) {
            currentScreen = Screen.Home
        }
    }

    // Check if an active call or incoming call overlay should take over the full screen
    val session = activeCallSession
    if (session != null && session.status != CallStatus.CALL_ENDED) {
        if (session.status == CallStatus.INCOMING_RINGING) {
            IncomingCallScreen(
                session = session,
                onAccept = { aiCallEngine.acceptIncomingCall() },
                onDecline = { aiCallEngine.declineIncomingCall() }
            )
            return
        } else {
            ActiveCallScreen(
                callEngine = aiCallEngine,
                session = session,
                onEndCall = {
                    aiCallEngine.endCall(saveHistory = true)
                }
            )
            return
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("main_bottom_nav_bar"),
                containerColor = SurfaceDark,
                contentColor = TextSecondary
            ) {
                Screen.bottomNavItems.forEach { screen ->
                    val isSelected = currentScreen == screen
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentScreen = screen },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                contentDescription = screen.title
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonPurple,
                            selectedTextColor = NeonPurple,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor = NeonPurple.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.testTag("nav_item_${screen.route}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screen_transition"
            ) { target ->
                when (target) {
                    Screen.Home -> HomeScreen(
                        onNavigateToVoiceChanger = { currentScreen = Screen.VoiceChanger },
                        onNavigateToAiCall = { currentScreen = Screen.AiCall },
                        onStartCallWithCharacter = { char ->
                            aiCallEngine.startOutgoingCall(char, char.defaultLanguage, isDemoMode = true)
                        },
                        onSimulateIncomingCallWithCharacter = { char ->
                            aiCallEngine.prepareIncomingCall(char, char.defaultLanguage, isDemoMode = true)
                        }
                    )

                    Screen.VoiceChanger -> VoiceChangerScreen(
                        audioRecorder = audioRecorder,
                        audioPlayer = audioPlayer,
                        audioFileManager = audioFileManager
                    )

                    Screen.AiCall -> AiCallScreen(
                        onStartOutgoingCall = { char, lang, isDemo ->
                            aiCallEngine.startOutgoingCall(char, lang, isDemo)
                        },
                        onSimulateIncomingCall = { char, lang, isDemo ->
                            aiCallEngine.prepareIncomingCall(char, lang, isDemo)
                        }
                    )

                    Screen.History -> HistoryScreen(
                        audioFileManager = audioFileManager,
                        audioPlayer = audioPlayer,
                        callEngine = aiCallEngine,
                        onNavigateToChanger = { currentScreen = Screen.VoiceChanger },
                        onNavigateToAiCall = { currentScreen = Screen.AiCall }
                    )

                    Screen.Settings -> SettingsScreen()
                }
            }
        }
    }
}
