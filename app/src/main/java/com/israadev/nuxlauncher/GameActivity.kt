package com.israadev.nuxlauncher

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.system.Os
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.israadev.nuxlauncher.core.controls.ControlLayoutManager
import com.israadev.nuxlauncher.core.controls.models.CustomControlButton
import com.israadev.nuxlauncher.core.crash.CrashManager
import com.israadev.nuxlauncher.core.device.PhysicalMouseChecker
import com.israadev.nuxlauncher.core.game.MCOptions
import com.israadev.nuxlauncher.core.game.input.EfficientAndroidLWJGLKeycode
import com.israadev.nuxlauncher.core.game.input.HidableInputLayout
import com.israadev.nuxlauncher.core.game.input.TouchCharInput
import com.israadev.nuxlauncher.core.recorder.ScreenRecordConfig
import com.israadev.nuxlauncher.core.recorder.ScreenRecordManager
import com.israadev.nuxlauncher.core.renderer.NuxRendererRegistry
import com.israadev.nuxlauncher.core.settings.SettingsManager
import com.israadev.nuxlauncher.ui.activities.ErrorActivity
import com.israadev.nuxlauncher.ui.components.GameLoadingOverlay
import com.israadev.nuxlauncher.ui.components.NuxBadge
import com.israadev.nuxlauncher.ui.components.NuxButton
import com.israadev.nuxlauncher.ui.components.NuxCard
import com.israadev.nuxlauncher.ui.control.MouseControlMode
import com.israadev.nuxlauncher.ui.control.SwitchableMouseLayout
import com.israadev.nuxlauncher.ui.dialogs.InGameSettingsDialog
import com.israadev.nuxlauncher.ui.dialogs.NuxPremiumDialog
import com.israadev.nuxlauncher.ui.screens.CustomGuiEditorScreen
import com.israadev.nuxlauncher.ui.theme.NuxColors
import com.israadev.nuxlauncher.ui.theme.NuxSizes
import com.movtery.inputmap.keycodes.LwjglGlfwKeycode
import com.movtery.zalithlauncher.bridge.CURSOR_DISABLED
import com.movtery.zalithlauncher.bridge.CURSOR_ENABLED
import com.movtery.zalithlauncher.bridge.LoggerBridge
import com.movtery.zalithlauncher.bridge.ZLBridge
import com.movtery.zalithlauncher.bridge.ZLBridgeStates
import com.movtery.zalithlauncher.bridge.ZLNativeInvoker
import com.movtery.zalithlauncher.game.sdl.SdlBridge
import com.oracle.dalvik.VMLauncher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.libsdl.app.SDLActivity
import org.libsdl.app.SDLSurface
import org.lwjgl.glfw.CallbackBridge
import java.io.File
import kotlin.concurrent.thread
import kotlin.math.roundToInt

class GameActivity : ComponentActivity(), SurfaceHolder.Callback {

    companion object {
        const val EXTRA_INSTANCE_NAME = "extra_instance_name"
        const val EXTRA_MC_VERSION = "extra_mc_version"
        const val EXTRA_USERNAME = "extra_username"
        const val EXTRA_UUID = "extra_uuid"
        const val EXTRA_GAME_DIR = "extra_game_dir"
        const val EXTRA_ASSETS_DIR = "extra_assets_dir"
        const val EXTRA_ASSET_INDEX = "extra_asset_index"
        const val EXTRA_MAIN_CLASS = "extra_main_class"
        const val EXTRA_CLASSPATH = "extra_classpath"
        const val EXTRA_RUNTIME_NAME = "extra_runtime_name"
        const val EXTRA_RUNTIME_HOME = "extra_runtime_home"
        const val EXTRA_LWJGL_NATIVES_DIR = "extra_lwjgl_natives_dir"
        const val EXTRA_ACCESS_TOKEN = "extra_access_token"
        const val EXTRA_USER_TYPE = "extra_user_type"
        const val EXTRA_AUTHLIB_INJECTOR_PATH = "extra_authlib_injector_path"
        const val EXTRA_AUTHLIB_URL = "extra_authlib_url"
        const val EXTRA_SKIN_PATH = "extra_skin_path"
        const val EXTRA_CAPE_PATH = "extra_cape_path"
        const val EXTRA_SKIN_MODEL = "extra_skin_model"
        const val EXTRA_USE_WRAPPER = "extra_use_wrapper"
        const val EXTRA_LOADER = "extra_loader"
        const val EXTRA_LOADER_VERSION = "extra_loader_version"
        const val EXTRA_INSTALLED_MODS = "extra_installed_mods"
        const val EXTRA_LAUNCHER_UID = "extra_launcher_uid"

        private const val MAX_LIVE_LOG_LINES = 80
    }

    private var pendingRecordConfig: ScreenRecordConfig = ScreenRecordConfig()
    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            ScreenRecordManager.startRecording(
                context = this,
                resultCode = result.resultCode,
                resultData = result.data!!,
                config = pendingRecordConfig
            )
            Toast.makeText(this, "Screen recording started!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Screen recording permission was denied", Toast.LENGTH_SHORT).show()
        }
    }

    private val audioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            pendingRecordConfig = pendingRecordConfig.copy(recordAudio = false)
            Toast.makeText(this, "Audio permission denied. Recording without mic audio.", Toast.LENGTH_SHORT).show()
        }
        try {
            val mpManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            screenCaptureLauncher.launch(mpManager.createScreenCaptureIntent())
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to launch screen recorder: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun requestStartScreenRecording(config: ScreenRecordConfig, onNeedPremiumDialog: () -> Unit = {}) {
        pendingRecordConfig = config
        if (config.recordAudio && ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            try {
                val mpManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                screenCaptureLauncher.launch(mpManager.createScreenCaptureIntent())
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to launch screen recorder: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun requestStopScreenRecording() {
        ScreenRecordManager.stopRecording(this)
        Toast.makeText(this, "Recording stopped and saved to Movies/NUX-Launcher!", Toast.LENGTH_SHORT).show()
    }

    private val liveLogs = mutableStateListOf<String>()
    private val latestLogState = mutableStateOf("")
    private var isGameStarted = false
    private var surfaceHolderRef: SurfaceHolder? = null
    private val isControlVisibleState = mutableStateOf(true)
    private val isGameRenderingState = mutableStateOf(false)

    private var isManualExit = false
    private var sessionStartTime = System.currentTimeMillis()

    private fun handleGameExit(exitCode: Int, isSignal: Boolean, errorDetail: String? = null) {
        if (ScreenRecordManager.recordState.value.isRecording) {
            ScreenRecordManager.stopRecording(this)
        }
        val isNormalExit = isManualExit || (exitCode == 0 && !isSignal)

        if (!isNormalExit) {
            val crashReportsDir = File(gameDirPath, "crash-reports")
            val latestCrashReport = if (crashReportsDir.exists() && crashReportsDir.isDirectory) {
                crashReportsDir.listFiles { f -> f.isFile && f.name.startsWith("crash-") && f.name.endsWith(".txt") }
                    ?.maxByOrNull { it.lastModified() }
            } else null

            val hasRecentCrashReport = latestCrashReport != null && (latestCrashReport.lastModified() >= sessionStartTime - 10000L)
            val logFile = File(filesDir, "latestlog.txt")
            val effectiveLogPath = if (hasRecentCrashReport && latestCrashReport != null) latestCrashReport.absolutePath else if (logFile.exists()) logFile.absolutePath else ""

            val finalExitCode = if (exitCode != 0) exitCode else 1

            CrashManager.recordCrash(
                context = this,
                instanceName = instanceName,
                mcVersion = mcVersion,
                exitCode = finalExitCode,
                isSignal = isSignal,
                gameDirPath = gameDirPath,
                liveLogs = liveLogs.toList(),
                exceptionDetail = errorDetail,
                loader = loader,
                loaderVersion = loaderVersion,
                installedMods = installedMods
            )

            try {
                ErrorActivity.showGameCrash(
                    context = this,
                    exitCode = finalExitCode,
                    isSignal = isSignal,
                    logPath = effectiveLogPath,
                    instanceName = instanceName,
                    mcVersion = mcVersion
                )
            } catch (e: Throwable) {
                try {
                    val launcherIntent = Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    startActivity(launcherIntent)
                } catch (_: Throwable) {}
            }
        } else {
            CrashManager.onGameSessionEnded(this)
            try {
                val launcherIntent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                startActivity(launcherIntent)
            } catch (_: Throwable) {}
        }

        terminateGameProcess()
    }

    private var launcherUid: String = ""
    private var presenceHeartbeatJob: Job? = null
    private val presenceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private fun startInGamePresenceHeartbeat() {
        if (launcherUid.isBlank()) return
        presenceHeartbeatJob?.cancel()
        presenceHeartbeatJob = presenceScope.launch {
            while (isActive) {
                try {
                    val payload = org.json.JSONObject().apply {
                        put("uid", launcherUid)
                        put("status", "in_game")
                        put("platform", "android")
                        put("instanceName", instanceName)
                    }
                    com.israadev.nuxlauncher.core.auth.AuthService.postApiSync("/api/presence", payload.toString(), timeoutSec = 2)
                } catch (_: Throwable) {}
                delay(25_000L)
            }
        }
    }

    private fun notifyPresenceGameExit() {
        try {
            presenceHeartbeatJob?.cancel()
            presenceHeartbeatJob = null
            if (launcherUid.isNotBlank()) {
                val payload = org.json.JSONObject().apply {
                    put("uid", launcherUid)
                    put("status", "online")
                    put("platform", "android")
                }
                com.israadev.nuxlauncher.core.auth.AuthService.postApiSync("/api/presence", payload.toString(), timeoutSec = 2)
            }
            com.israadev.nuxlauncher.core.social.NuxSocialManager.notifyGameExitSync(launcherUid.ifBlank { null })
        } catch (_: Throwable) {}
    }

    private fun terminateGameProcess() {
        try {
            isManualExit = true
            notifyPresenceGameExit()
            CrashManager.onGameSessionEnded(this)
            ZLBridge.releaseBridgeWindow()
            com.israadev.nuxlauncher.core.skin.OfflineSkinServerManager.stopServer()
            com.israadev.nuxlauncher.core.account.offline.OfflineYggdrasilServer.activeInstance?.stop()
        } catch (_: Throwable) {}
        finish()
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            isManualExit = true
            notifyPresenceGameExit()
            CrashManager.onGameSessionEnded(this)
            ZLBridge.releaseBridgeWindow()
            com.israadev.nuxlauncher.core.skin.OfflineSkinServerManager.stopServer()
            com.israadev.nuxlauncher.core.account.offline.OfflineYggdrasilServer.activeInstance?.stop()
        } catch (_: Throwable) {}
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    private var instanceName = "Minecraft"
    private var mcVersion = "Unknown"
    private var username = "Player"
    private var uuid = "00000000-0000-0000-0000-000000000000"
    private var gameDirPath = ""
    private var assetsDirPath = ""
    private var assetIndexId = ""
    private var mainClass = "net.minecraft.client.main.Main"
    private var classpath = ""
    private var runtimeName = "jre-17"
    private var runtimeHomePath = ""
    private var lwjglNativesDirPath = ""
    private var accessToken = "0"
    private var userType = "mojang"
    private var authlibInjectorPath: String? = null
    private var authlibUrl: String? = null
    private var skinPath: String? = null
    private var capePath: String? = null
    private var skinModel = "default"
    private var useWrapper = false
    private var loader = "vanilla"
    private var loaderVersion: String? = null
    private var installedMods: List<String> = emptyList()

    private fun isVulkanOrZinkRenderer(): Boolean {
        val settings = SettingsManager.settings.value
        val sel = settings.selectedRenderer.lowercase()
        val hasVulkanMod = installedMods.any { it.contains("vulkan", ignoreCase = true) }
        return hasVulkanMod ||
                sel.contains("vulkan") ||
                sel.contains("zink") ||
                settings.graphicsApi.equals("VULKAN", ignoreCase = true)
    }

    private fun isLowEndDevice(): Boolean {
        return try {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            am?.getMemoryInfo(memInfo)
            val totalRamMb = memInfo.totalMem / (1024L * 1024L)
            totalRamMb in 1..4600 || (am?.isLowRamDevice == true)
        } catch (_: Throwable) {
            false
        }
    }

    private fun calculateSafeHeapAllocation(requestedInitialMb: Int, requestedMaxMb: Int): Pair<Int, Int> {
        return try {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            am?.getMemoryInfo(memInfo)

            val totalRamMb = (memInfo.totalMem / (1024L * 1024L)).toInt()
            val availRamMb = (memInfo.availMem / (1024L * 1024L)).toInt()

            if (totalRamMb <= 0) {
                return Pair(requestedInitialMb.coerceAtLeast(256), requestedMaxMb.coerceAtLeast(512))
            }

            val deviceSafeCeilingMb = when {
                totalRamMb <= 2200 -> 768
                totalRamMb <= 3300 -> 1150
                totalRamMb <= 4400 -> 1550
                totalRamMb <= 6400 -> 2400
                else -> 4096
            }

            val dynamicAvailCapMb = if (availRamMb > 900) {
                (availRamMb * 0.72f).toInt()
            } else {
                deviceSafeCeilingMb
            }

            val safeMaxMb = minOf(requestedMaxMb, deviceSafeCeilingMb, maxOf(deviceSafeCeilingMb, dynamicAvailCapMb))
                .coerceAtLeast(512)

            val safeInitialMb = minOf(requestedInitialMb, safeMaxMb / 2, 512)
                .coerceAtLeast(256)

            Pair(safeInitialMb, safeMaxMb)
        } catch (_: Throwable) {
            Pair(requestedInitialMb.coerceAtLeast(256), requestedMaxMb.coerceAtLeast(512))
        }
    }

    private fun getNativeLandscapeDimensions(): Pair<Int, Int> {
        val (rawW, rawH) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            Pair(bounds.width(), bounds.height())
        } else {
            val dm = android.util.DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(dm)
            Pair(dm.widthPixels, dm.heightPixels)
        }
        val screenW = (maxOf(rawW, rawH) / 2) * 2
        val screenH = (minOf(rawW, rawH) / 2) * 2
        return Pair(screenW.coerceAtLeast(640), screenH.coerceAtLeast(360))
    }

    private fun getScaledDisplayDimensions(): Pair<Int, Int> {
        val settings = SettingsManager.settings.value
        val (screenW, screenH) = getNativeLandscapeDimensions()

        // Saat menggunakan Vulkan/Zink, gunakan dimensi native penuh agar Surface tidak mengecil di pojok
        if (isVulkanOrZinkRenderer()) {
            return Pair(screenW, screenH)
        }

        val effectiveRatio = if (settings.resolutionRatio == 100 && settings.autoOptimizeMinecraft && isLowEndDevice()) {
            75
        } else {
            settings.resolutionRatio
        }
        val ratio = (effectiveRatio / 100f).coerceIn(0.40f, 1.5f)
        val width = ((screenW * ratio).roundToInt() / 2) * 2
        val height = ((screenH * ratio).roundToInt() / 2) * 2
        return Pair(width.coerceAtLeast(640), height.coerceAtLeast(360))
    }

    private fun applyDynamicResolution(newRatio: Int) {
        val current = SettingsManager.settings.value
        val updated = current.copy(resolutionRatio = newRatio)
        SettingsManager.updateSettings(this, updated)

        if (!isVulkanOrZinkRenderer()) {
            val (targetW, targetH) = getScaledDisplayDimensions()
            surfaceHolderRef?.setFixedSize(targetW, targetH)
            CallbackBridge.windowWidth = targetW
            CallbackBridge.windowHeight = targetH
            CallbackBridge.sendUpdateWindowSize(targetW, targetH)
            LoggerBridge.append("▷ [In-Game Resolution] Updated to $newRatio% (${targetW}x${targetH})")
        } else {
            val (nativeW, nativeH) = getNativeLandscapeDimensions()
            CallbackBridge.windowWidth = nativeW
            CallbackBridge.windowHeight = nativeH
            CallbackBridge.sendUpdateWindowSize(nativeW, nativeH)
            LoggerBridge.append("▷ [In-Game Resolution] Vulkan active: keeping fullscreen native viewport (${nativeW}x${nativeH})")
        }
    }

    private fun applyImmersiveFullscreen() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val params = window.attributes
                params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                window.attributes = params
            }
            WindowCompat.setDecorFitsSystemWindows(window, false)

            val decor = window.peekDecorView() ?: try { window.decorView } catch (_: Throwable) { null }
            if (decor != null) {
                val insetsController = WindowCompat.getInsetsController(window, decor)
                insetsController.hide(
                    WindowInsetsCompat.Type.systemBars() or
                    WindowInsetsCompat.Type.displayCutout()
                )
                insetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

                @Suppress("DEPRECATION")
                decor.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                )
            }
        } catch (_: Throwable) {}
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            applyImmersiveFullscreen()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        super.onCreate(savedInstanceState)

        PhysicalMouseChecker.initChecker(this)
        SettingsManager.init(this)
        com.israadev.nuxlauncher.core.lang.NuxLanguageManager.init(this)
        ControlLayoutManager.init(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && SettingsManager.settings.value.sustainedPerformanceMode) {
            window.setSustainedPerformanceMode(true)
        }
        CallbackBridge.sContext = this
        ZLNativeInvoker.appContext = this

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val params = window.attributes
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
            window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
            window.attributes = params
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)

        instanceName = intent.getStringExtra(EXTRA_INSTANCE_NAME) ?: "Minecraft"
        mcVersion = intent.getStringExtra(EXTRA_MC_VERSION) ?: "Unknown"
        username = intent.getStringExtra(EXTRA_USERNAME) ?: "Player"
        uuid = intent.getStringExtra(EXTRA_UUID) ?: "00000000-0000-0000-0000-000000000000"
        gameDirPath = intent.getStringExtra(EXTRA_GAME_DIR) ?: filesDir.absolutePath
        assetsDirPath = intent.getStringExtra(EXTRA_ASSETS_DIR) ?: File(filesDir, "minecraft/assets").absolutePath
        assetIndexId = intent.getStringExtra(EXTRA_ASSET_INDEX) ?: mcVersion
        mainClass = intent.getStringExtra(EXTRA_MAIN_CLASS) ?: "net.minecraft.client.main.Main"
        classpath = intent.getStringExtra(EXTRA_CLASSPATH) ?: ""
        runtimeName = intent.getStringExtra(EXTRA_RUNTIME_NAME) ?: "jre-17"
        runtimeHomePath = intent.getStringExtra(EXTRA_RUNTIME_HOME) ?: ""
        lwjglNativesDirPath = intent.getStringExtra(EXTRA_LWJGL_NATIVES_DIR) ?: ""
        accessToken = intent.getStringExtra(EXTRA_ACCESS_TOKEN) ?: "0"
        userType = intent.getStringExtra(EXTRA_USER_TYPE) ?: "mojang"
        authlibInjectorPath = intent.getStringExtra(EXTRA_AUTHLIB_INJECTOR_PATH)
        authlibUrl = intent.getStringExtra(EXTRA_AUTHLIB_URL)
        skinPath = intent.getStringExtra(EXTRA_SKIN_PATH)
        capePath = intent.getStringExtra(EXTRA_CAPE_PATH)
        skinModel = intent.getStringExtra(EXTRA_SKIN_MODEL) ?: "default"
        useWrapper = intent.getBooleanExtra(EXTRA_USE_WRAPPER, false)
        loader = intent.getStringExtra(EXTRA_LOADER) ?: "vanilla"
        loaderVersion = intent.getStringExtra(EXTRA_LOADER_VERSION)
        installedMods = intent.getStringArrayListExtra(EXTRA_INSTALLED_MODS) ?: CrashManager.getInstalledMods(gameDirPath)
        launcherUid = intent.getStringExtra(EXTRA_LAUNCHER_UID) ?: ""
        startInGamePresenceHeartbeat()

        setupLogger()

        ZLNativeInvoker.onExitCallback = { exitCode, isSignal ->
            runOnUiThread {
                appendLiveLog("[NUX Engine] Game process ended (code: $exitCode, signal: $isSignal)")
                handleGameExit(exitCode, isSignal)
            }
        }

        CallbackBridge.setGraphicOutputListener {
            runOnUiThread {
                isGameRenderingState.value = true
                appendLiveLog("🎨 [Render Engine] First graphical frame rendered on Surface!")
                val (winW, winH) = getScaledDisplayDimensions()
                CallbackBridge.sendCursorPos(winW / 2f, winH / 2f)
            }
        }

        val composeView = ComposeView(this).apply {
            setContent {
                GameScreen(
                    instanceName = instanceName,
                    mcVersion = mcVersion,
                    username = username,
                    mainClass = mainClass,
                    runtimeName = runtimeName,
                    liveLogs = liveLogs,
                    latestLogState = latestLogState,
                    isControlVisibleState = isControlVisibleState,
                    isGameRenderingState = isGameRenderingState,
                    onSurfaceReady = { surfaceHolder ->
                        surfaceHolderRef = surfaceHolder
                        if (!isVulkanOrZinkRenderer()) {
                            val (targetWidth, targetHeight) = getScaledDisplayDimensions()
                            val (nativeWidth, nativeHeight) = getNativeLandscapeDimensions()
                            if (targetWidth != nativeWidth || targetHeight != nativeHeight) {
                                surfaceHolder.setFixedSize(targetWidth, targetHeight)
                            }
                        }
                        surfaceHolder.addCallback(this@GameActivity)
                    },
                    onResolutionChange = { newRatio ->
                        applyDynamicResolution(newRatio)
                    },
                    onExit = {
                        isManualExit = true
                        terminateGameProcess()
                    },
                    onRequestStartRecord = { config ->
                        requestStartScreenRecording(config)
                    },
                    onRequestStopRecord = {
                        requestStopScreenRecording()
                    }
                )
            }
        }

        setContentView(composeView)
        applyImmersiveFullscreen()
    }

    private fun appendLiveLog(line: String) {
        latestLogState.value = line
        liveLogs.add(line)
        if (liveLogs.size > MAX_LIVE_LOG_LINES) {
            liveLogs.removeAt(0)
        }
    }

    private fun setupLogger() {
        val logFile = File(filesDir, "latestlog.txt")
        if (logFile.exists()) logFile.delete()
        logFile.createNewFile()

        LoggerBridge.setListener { text ->
            runOnUiThread {
                appendLiveLog(text)
                if (!isGameRenderingState.value) {
                    if (text.contains("OpenAL initialized") ||
                        text.contains("Reloading ResourceManager") ||
                        text.contains("Sound engine started") ||
                        text.contains("Setting user: ")) {
                        isGameRenderingState.value = true
                    }
                }
            }
        }

        try {
            LoggerBridge.start(logFile.absolutePath)
            LoggerBridge.appendTitle("BELL Launcher Game Session")
            LoggerBridge.append("▷ Starting $instanceName ($mcVersion) on $runtimeName")
        } catch (e: Throwable) {
            appendLiveLog("[LoggerBridge Init] ${e.message}")
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        val (nativeW, nativeH) = getNativeLandscapeDimensions()
        val (targetWidth, targetHeight) = if (!isVulkanOrZinkRenderer()) {
            getScaledDisplayDimensions().also { (w, h) ->
                if (w != nativeW || h != nativeH) {
                    holder.setFixedSize(w, h)
                }
            }
        } else {
            Pair(nativeW, nativeH)
        }

        CallbackBridge.windowWidth = targetWidth
        CallbackBridge.windowHeight = targetHeight
        CallbackBridge.sendUpdateWindowSize(targetWidth, targetHeight)

        val surface = holder.surface
        val rootLayout = window.decorView as? ViewGroup
        SdlBridge.prepareSurface(this, surface, rootLayout, holder)
        try {
            ZLBridge.setupBridgeWindow(surface)
            appendLiveLog("[Render Bridge] Native window bound (${targetWidth}x${targetHeight}, Vulkan=${isVulkanOrZinkRenderer()}).")
        } catch (e: Throwable) {
            appendLiveLog("[Render Bridge Error] ${e.message}")
        }

        if (!isGameStarted) {
            isGameStarted = true
            startGameJVM()
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        if (width < height && !isGameStarted) {
            return
        }
        val landscapeW = maxOf(width, height).coerceAtLeast(640)
        val landscapeH = minOf(width, height).coerceAtLeast(360)

        CallbackBridge.windowWidth = landscapeW
        CallbackBridge.windowHeight = landscapeH
        CallbackBridge.sendUpdateWindowSize(landscapeW, landscapeH)
        LoggerBridge.append("▷ [SurfaceChanged] Native surface buffer: ${landscapeW}x${landscapeH}")
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        try {
            val nativeSurface = SDLSurface.getNativeSurface()
            if (SdlBridge.beginSurfaceDestroy(holder, nativeSurface)) {
                if (SdlBridge.sdlEnabled) {
                    SDLActivity.getSDLSurface()?.surfaceDestroyed()
                }
                SdlBridge.unregisterSurface(nativeSurface)
            }
            ZLBridge.releaseBridgeWindow()
        } catch (_: Throwable) {}
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (TouchCharInput.isActive()) {
            return super.dispatchKeyEvent(event)
        }

        val source = event.source
        if (source and InputDevice.SOURCE_MOUSE == InputDevice.SOURCE_MOUSE ||
            source and InputDevice.SOURCE_MOUSE_RELATIVE == InputDevice.SOURCE_MOUSE_RELATIVE) {
            if (event.keyCode == KeyEvent.KEYCODE_BACK) {
                val isDown = event.action == KeyEvent.ACTION_DOWN
                CallbackBridge.sendMouseButton(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_RIGHT, isDown)
                return true
            }
        }

        if (event.action == KeyEvent.ACTION_DOWN) {
            isControlVisibleState.value = false
        }

        val index = EfficientAndroidLWJGLKeycode.getIndexByKey(event.keyCode)
        if (index >= 0) {
            EfficientAndroidLWJGLKeycode.execKey(event, index)
            return true
        }

        return super.dispatchKeyEvent(event)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.isFromSource(InputDevice.SOURCE_MOUSE) || event.isFromSource(InputDevice.SOURCE_MOUSE_RELATIVE)) {
            isControlVisibleState.value = false
        }
        return super.dispatchGenericMotionEvent(event)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.isFromSource(InputDevice.SOURCE_MOUSE) || event.isFromSource(InputDevice.SOURCE_MOUSE_RELATIVE)) {
            isControlVisibleState.value = false
        }
        return super.dispatchTouchEvent(event)
    }

    private fun startGameJVM() {
        thread(name = "BELL-JVM-Thread") {
            try {
                val nativeLibDir = applicationInfo.nativeLibraryDir
                val runtimeHome = File(runtimeHomePath)
                val gameDir = File(gameDirPath)
                val assetsDir = File(assetsDirPath)
                val lowEnd = isLowEndDevice()
                val vulkanMode = isVulkanOrZinkRenderer()

                runOnUiThread { appendLiveLog("[BELL Launcher] Menyiapkan JVM (LowEnd=$lowEnd, Vulkan=$vulkanMode)...") }

                val activeSettings = SettingsManager.settings.value
                val (fallbackW, fallbackH) = getScaledDisplayDimensions()
                val targetWidth = CallbackBridge.windowWidth.takeIf { it > 0 } ?: fallbackW
                val targetHeight = CallbackBridge.windowHeight.takeIf { it > 0 } ?: fallbackH

                MCOptions.setup(this@GameActivity, gameDir)
                MCOptions.apply {
                    set("fullscreen", "false")
                    set("touchscreen", "false")
                    set("options.narrator", "0")
                    set("narrator", "0")

                    if (activeSettings.autoOptimizeMinecraft || lowEnd) {
                        set("clouds", "false")
                        set("renderClouds", "\"false\"")
                        set("entityShadows", "false")
                        set("enableVsync", "false")
                        set("ao", "false")
                        set("graphicsMode", "0")
                        set("particles", "2")
                        set("mipmapLevels", "1")
                        set("biomeBlendRadius", "1")
                        if (!containsKey("renderDistance")) {
                            set("renderDistance", if (lowEnd) "4" else "6")
                        }
                        if (!containsKey("simulationDistance")) {
                            set("simulationDistance", "5")
                        }
                    }

                    // Sinkronkan overrideWidth/Height dengan buffer SurfaceView agar selalu fullscreen penuh
                    set("overrideWidth", "$targetWidth")
                    set("overrideHeight", "$targetHeight")
                    save()
                }
                LoggerBridge.append("▷ [MCOptions] Minecraft options verified (res=${targetWidth}x${targetHeight}, vulkan=$vulkanMode)")

                if (!com.israadev.nuxlauncher.core.runtime.JavaRuntimeManager.isRuntimeInstalled(this@GameActivity, runtimeName)) {
                    runOnUiThread { appendLiveLog("[BELL Runtime] Mempersiapkan OpenJDK ($runtimeName)...") }
                    val extRes = kotlinx.coroutines.runBlocking {
                        com.israadev.nuxlauncher.core.runtime.JavaRuntimeManager.extractRuntime(this@GameActivity, runtimeName) { msg ->
                            runOnUiThread { appendLiveLog("[BELL Runtime] $msg") }
                        }
                    }
                    if (extRes.isFailure) {
                        val errMsg = extRes.exceptionOrNull()?.message ?: "Extraction failed"
                        throw Exception("Failed to prepare OpenJDK ($runtimeName): $errMsg")
                    }
                }

                val javaLibDir = File(runtimeHome, "lib")
                try {
                    javaLibDir.walkTopDown().filter { it.extension == "so" }.forEach {
                        it.setReadable(true, false)
                        it.setExecutable(true, false)
                    }
                    File(runtimeHome, "bin").walkTopDown().forEach {
                        it.setReadable(true, false)
                        it.setExecutable(true, false)
                    }
                } catch (_: Throwable) {}

                try {
                    com.israadev.nuxlauncher.core.renderer.NuxRendererPluginManager.scanPlugins(this)
                } catch (_: Throwable) {}

                val targetRenderer = NuxRendererRegistry.resolveRenderer(
                    selectedId = activeSettings.selectedRenderer,
                    mcVersion = mcVersion,
                    nativeLibDir = File(nativeLibDir),
                    context = this
                )
                val rendererId = targetRenderer.rendererId
                val rendererSoName = targetRenderer.libraryName

                LoggerBridge.append("▷ [Renderer] Active Backend: ${targetRenderer.displayName} ($rendererId)")

                val runtimeHomeStr = runtimeHome.absolutePath
                val libDirName = if (Build.SUPPORTED_ABIS.any { it.contains("64") }) "lib64" else "lib"

                val runtimeLdPaths = mutableListOf<String>()
                if (targetRenderer.isPlugin && !targetRenderer.pluginNativePath.isNullOrBlank()) {
                    runtimeLdPaths.add(targetRenderer.pluginNativePath!!)
                }
                val archSubDir = listOf("aarch64", "aarch32", "arm", "i386", "amd64", "x86_64")
                    .map { File("$runtimeHomeStr/lib", it) }
                    .firstOrNull { it.exists() && it.isDirectory }

                if (archSubDir != null) {
                    runtimeLdPaths.add("${archSubDir.absolutePath}/jli")
                    runtimeLdPaths.add("${archSubDir.absolutePath}/server")
                    runtimeLdPaths.add("${archSubDir.absolutePath}/client")
                    runtimeLdPaths.add(archSubDir.absolutePath)
                }

                runtimeLdPaths.add("$runtimeHomeStr/lib/jli")
                if (File("$runtimeHomeStr/jre").exists()) {
                    runtimeLdPaths.add("$runtimeHomeStr/jre/lib/server")
                    runtimeLdPaths.add("$runtimeHomeStr/jre/lib")
                } else {
                    runtimeLdPaths.add("$runtimeHomeStr/lib/server")
                    runtimeLdPaths.add("$runtimeHomeStr/lib")
                }
                if (lwjglNativesDirPath.isNotBlank()) runtimeLdPaths.add(lwjglNativesDirPath)
                runtimeLdPaths.add(nativeLibDir)
                runtimeLdPaths.add("/system/$libDirName")
                runtimeLdPaths.add("/vendor/$libDirName")
                runtimeLdPaths.add("/vendor/$libDirName/hw")
                runtimeLdPaths.add("/system_ext/$libDirName")

                val runtimeLdLibraryPath = runtimeLdPaths.distinct().joinToString(":")
                ZLBridge.setLdLibraryPath(runtimeLdLibraryPath)

                val gameLdPaths = mutableListOf<String>()
                if (lwjglNativesDirPath.isNotBlank()) gameLdPaths.add(lwjglNativesDirPath)
                gameLdPaths.add("/system/$libDirName")
                gameLdPaths.add("/vendor/$libDirName")
                gameLdPaths.add("/vendor/$libDirName/hw")
                gameLdPaths.add("/system_ext/$libDirName")
                if (targetRenderer.isPlugin && !targetRenderer.pluginNativePath.isNullOrBlank()) {
                    gameLdPaths.add(targetRenderer.pluginNativePath!!)
                }
                gameLdPaths.add(nativeLibDir)
                val gameLdLibraryPath = gameLdPaths.distinct().joinToString(":")

                val glLibPath = if (targetRenderer.isPlugin && !targetRenderer.pluginNativePath.isNullOrBlank()) {
                    if (rendererSoName.startsWith("/")) rendererSoName else "${targetRenderer.pluginNativePath}/$rendererSoName"
                } else {
                    "$nativeLibDir/$rendererSoName"
                }

                val isPlugin = targetRenderer.isPlugin
                val isMobileGlues = targetRenderer.id == "mobileglues" ||
                        targetRenderer.rendererId.contains("mobileglue", ignoreCase = true) ||
                        (targetRenderer.pluginPackageName?.contains("mobileglue", ignoreCase = true) == true)

                Os.setenv("POJAV_NATIVEDIR", nativeLibDir, true)
                Os.setenv("POJAV_RENDERER", rendererId, true)
                Os.setenv("POJAV_SDL_REUSE_WINDOW", "1", true)
                Os.setenv("SDL_OPENGL_LIBRARY", rendererId, true)

                if (vulkanMode || rendererId.contains("zink") || rendererId.contains("vulkan")) {
                    Os.setenv("ZINK_DESCRIPTORS", "lazy", true)
                    Os.setenv("MESA_VK_WSI_PRESENT_MODE", "mailbox", true)
                    Os.setenv("MESA_NO_ERROR", "1", true)
                    Os.setenv("vblank_mode", "0", true)
                }

                if (rendererId.startsWith("opengles2") || targetRenderer.id == "gl4es") {
                    Os.setenv("LIBGL_ES", "2", true)
                    Os.setenv("LIBGL_MIPMAP", "3", true)
                    Os.setenv("LIBGL_NOERROR", "1", true)
                    Os.setenv("LIBGL_NOINTOVLHACK", "1", true)
                    Os.setenv("LIBGL_NORMALIZE", "1", true)
                    Os.setenv("LIBGL_FASTEX", "1", true)
                    if (lowEnd) {
                        Os.setenv("LIBGL_SHRINK", "2", true)
                    }
                }

                targetRenderer.envVariables.forEach { (k, v) ->
                    Os.setenv(k, v, true)
                }

                try {
                    val v2Envs = com.israadev.nuxlauncher.core.renderer.v2.NuxRendererV2Manager.getEffectiveEnv(targetRenderer)
                    v2Envs.forEach { (k, v) ->
                        Os.setenv(k, v, true)
                    }
                } catch (_: Exception) {}

                val eglPath = if (!targetRenderer.eglName.isNullOrBlank()) {
                    val rawEgl = targetRenderer.eglName!!
                    if (rawEgl.startsWith("/")) rawEgl
                    else if (targetRenderer.isPlugin && !targetRenderer.pluginNativePath.isNullOrBlank()) "${targetRenderer.pluginNativePath}/$rawEgl"
                    else "$nativeLibDir/$rawEgl"
                } else null

                if (!eglPath.isNullOrBlank()) {
                    Os.setenv("POJAVEXEC_EGL", targetRenderer.eglName ?: eglPath, true)
                    Os.setenv("SDL_EGL_LIBRARY", eglPath, true)
                }

                val skipMesaOverrides = isPlugin || isMobileGlues || targetRenderer.id == "krypton" || targetRenderer.id == "gl4es" || rendererId.startsWith("opengles")
                if (!skipMesaOverrides) {
                    Os.setenv("MESA_LOADER_DRIVER_OVERRIDE", "zink", true)
                    Os.setenv("MESA_GL_VERSION_OVERRIDE", "4.6", true)
                    Os.setenv("MESA_GLSL_VERSION_OVERRIDE", "460", true)
                    Os.setenv("allow_higher_compat_version", "true", true)
                    Os.setenv("allow_glsl_extension_directive_midshader", "true", true)
                    Os.setenv("force_glsl_extensions_warn", "true", true)
                }

                if (activeSettings.zinkPreferSystemDriver) {
                    Os.setenv("POJAV_ZINK_PREFER_SYSTEM_DRIVER", "1", true)
                }
                if (activeSettings.vsyncInZink) {
                    Os.setenv("POJAV_VSYNC_IN_ZINK", "1", true)
                }
                if (activeSettings.graphicsApi != "DEFAULT") {
                    Os.setenv("POJAV_GRAPHICS_API", activeSettings.graphicsApi, true)
                }
                if (activeSettings.vulkanDriver != "auto" && activeSettings.vulkanDriver.isNotBlank()) {
                    Os.setenv("POJAV_VULKAN_DRIVER", activeSettings.vulkanDriver, true)
                }

                if (targetRenderer.isPlugin && !targetRenderer.pluginNativePath.isNullOrBlank()) {
                    val pluginDir = File(targetRenderer.pluginNativePath)
                    targetRenderer.dlopenLibs.forEach { dlName ->
                        val dlFile = File(pluginDir, dlName)
                        if (dlFile.exists()) {
                            try { ZLBridge.dlopen(dlFile.absolutePath) } catch (_: Throwable) {}
                        }
                    }
                    val targetSo = File(glLibPath)
                    if (targetSo.exists()) {
                        try { ZLBridge.dlopen(targetSo.absolutePath) } catch (_: Throwable) {}
                    }
                    if (!eglPath.isNullOrBlank() && eglPath != glLibPath) {
                        val eglFile = File(eglPath)
                        if (eglFile.exists()) {
                            try { ZLBridge.dlopen(eglFile.absolutePath) } catch (_: Throwable) {}
                        }
                    }
                }

                if (rendererId == "gallium_virgl") {
                    Os.setenv("VTEST_SOCKET_NAME", File(cacheDir, ".virgl_test").absolutePath, true)
                }

                if (rendererId.startsWith("gallium") || rendererId.contains("zink") || vulkanMode) {
                    Os.setenv("MESA_GLSL_CACHE_DIR", cacheDir.absolutePath, true)
                    Os.setenv("MESA_GLSL_CACHE_MAX_SIZE", if (lowEnd) "32M" else "128M", true)
                }
                Os.setenv("JAVA_HOME", runtimeHome.absolutePath, true)
                Os.setenv("HOME", gameDir.absolutePath, true)
                Os.setenv("TMPDIR", cacheDir.absolutePath, true)
                Os.setenv("PATH", "${runtimeHome.absolutePath}/bin:" + (Os.getenv("PATH") ?: "/system/bin"), true)
                Os.setenv("LD_LIBRARY_PATH", gameLdLibraryPath, true)
                Os.setenv("AWTSTUB_WIDTH", "$targetWidth", true)
                Os.setenv("AWTSTUB_HEIGHT", "$targetHeight", true)
                Os.setenv("ALSOFT_DRIVERS", "opensl", true)
                if (lowEnd) {
                    Os.setenv("ALSOFT_PERIODS", "3", true)
                    Os.setenv("ALSOFT_PERIOD_SIZE", "512", true)
                }

                val resolvConf = File(gameDir, "resolv.conf")
                val dnsServers = buildSet {
                    try {
                        val cm = getSystemService(android.net.ConnectivityManager::class.java)
                        val activeNet = cm?.activeNetwork
                        if (activeNet != null) {
                            val lp = cm.getLinkProperties(activeNet)
                            lp?.dnsServers
                                ?.mapNotNull { it.hostAddress?.takeIf(String::isNotEmpty) }
                                ?.filterNot { it.contains(':') }
                                ?.let { addAll(it) }
                        }
                    } catch (_: Throwable) {}
                    add("1.1.1.1")
                    add("1.0.0.1")
                }
                val configText = dnsServers.joinToString(separator = "\n") { "nameserver $it" }
                runCatching {
                    if (!resolvConf.exists() || resolvConf.readText().trim() != configText.trim()) {
                        resolvConf.writeText(configText)
                    }
                    val altResolv = File(filesDir, "resolv.conf")
                    if (!altResolv.exists() || altResolv.readText().trim() != configText.trim()) {
                        altResolv.writeText(configText)
                    }
                }
                try { Os.setenv("RESOLV_CONF", resolvConf.absolutePath, true) } catch (_: Throwable) {}

                val isJava8 = File(runtimeHome, "jre").exists() || File(runtimeHome, "lib/rt.jar").exists() || File(runtimeHome, "lib/rt.jar.pack").exists()
                val rtLibDir = if (File(runtimeHome, "jre/lib").exists()) File(runtimeHome, "jre/lib") else File(runtimeHome, "lib")
                val archLibDir = listOf("aarch64", "aarch32", "arm", "i386", "amd64", "x86_64")
                    .map { File(rtLibDir, it) }
                    .firstOrNull { it.exists() && it.isDirectory } ?: rtLibDir

                val rtJliDir = when {
                    File(rtLibDir, "jli/libjli.so").exists() -> File(rtLibDir, "jli")
                    File(archLibDir, "jli/libjli.so").exists() -> File(archLibDir, "jli")
                    else -> rtLibDir
                }
                val rtJvmDir = when {
                    File(rtLibDir, "server/libjvm.so").exists() -> File(rtLibDir, "server")
                    File(rtLibDir, "client/libjvm.so").exists() -> File(rtLibDir, "client")
                    File(archLibDir, "server/libjvm.so").exists() -> File(archLibDir, "server")
                    File(archLibDir, "client/libjvm.so").exists() -> File(archLibDir, "client")
                    else -> rtLibDir
                }

                val essentialLibs = listOf(
                    File(rtJliDir, "libjli.so"),
                    File(rtJvmDir, "libjvm.so"),
                    File(rtLibDir, "libfreetype.so"),
                    File(archLibDir, "libfreetype.so"),
                    File(rtLibDir, "libverify.so"),
                    File(archLibDir, "libverify.so"),
                    File(rtLibDir, "libjava.so"),
                    File(archLibDir, "libjava.so"),
                    File(rtLibDir, "libnet.so"),
                    File(archLibDir, "libnet.so"),
                    File(rtLibDir, "libnio.so"),
                    File(archLibDir, "libnio.so"),
                    File(rtLibDir, "libawt.so"),
                    File(archLibDir, "libawt.so"),
                    File(rtLibDir, "libawt_headless.so"),
                    File(archLibDir, "libawt_headless.so"),
                    File(rtLibDir, "libfontmanager.so"),
                    File(archLibDir, "libfontmanager.so")
                )
                essentialLibs.forEach { so ->
                    if (so.exists()) {
                        try { ZLBridge.dlopen(so.absolutePath) } catch (_: Throwable) {}
                    }
                }

                runtimeHome.walkTopDown().filter { it.isFile && it.name.endsWith(".so") }.forEach { so ->
                    try { ZLBridge.dlopen(so.absolutePath) } catch (_: Throwable) {}
                }

                val openalFile = File(nativeLibDir, "libopenal.so")
                if (openalFile.exists()) {
                    try { ZLBridge.dlopen(openalFile.absolutePath) } catch (_: Throwable) {}
                }

                if (!targetRenderer.isPlugin) {
                    try {
                        val rendererFile = File(nativeLibDir, rendererSoName)
                        if (rendererFile.exists()) {
                            ZLBridge.dlopen(rendererFile.absolutePath)
                        }
                    } catch (_: Throwable) {}

                    if (!eglPath.isNullOrBlank() && eglPath != glLibPath) {
                        val eglFile = File(eglPath)
                        if (eglFile.exists()) {
                            try { ZLBridge.dlopen(eglFile.absolutePath) } catch (_: Throwable) {}
                        }
                    }
                }

                val spirvLibFile = File(nativeLibDir, "libspirv-cross-c-shared.so")
                if (spirvLibFile.exists()) {
                    try { ZLBridge.dlopen(spirvLibFile.absolutePath) } catch (_: Throwable) {}
                }

                if (lwjglNativesDirPath.isNotBlank()) {
                    val lwjglDir = File(lwjglNativesDirPath)
                    listOf("liblwjgl.so", "liblwjgl_opengl.so", "liblwjgl_stb.so", "liblwjgl_tinyfd.so", "libfreetype.so", "libshaderc.so", "liblwjgl_vma.so", "libspirv-cross.so").forEach { libName ->
                        val so = File(lwjglDir, libName)
                        if (so.exists()) {
                            try { ZLBridge.dlopen(so.absolutePath) } catch (_: Throwable) {}
                        }
                    }
                }

                ZLBridge.setupExitMethod(this@GameActivity)
                ZLBridge.initializeGameExitHook()
                ZLBridge.chdir(gameDir.absolutePath)

                val effectiveLwjglDir = if (lwjglNativesDirPath.isNotBlank()) lwjglNativesDirPath else nativeLibDir
                val freetypeLib = if (lwjglNativesDirPath.isNotBlank() && File(lwjglNativesDirPath, "libfreetype.so").exists()) {
                    File(lwjglNativesDirPath, "libfreetype.so").absolutePath
                } else {
                    "$nativeLibDir/libfreetype.so"
                }

                val spirvLibPath = if (lwjglNativesDirPath.isNotBlank() && File(lwjglNativesDirPath, "libspirv-cross.so").exists()) {
                    File(lwjglNativesDirPath, "libspirv-cross.so").absolutePath
                } else if (File(nativeLibDir, "libspirv-cross-c-shared.so").exists()) {
                    File(nativeLibDir, "libspirv-cross-c-shared.so").absolutePath
                } else {
                    "libspirv-cross-c-shared.so"
                }

                com.israadev.nuxlauncher.core.runtime.JavaRuntimeManager.ensureExecutablePermissions(runtimeHome)

                val (safeXmsMb, safeXmxMb) = calculateSafeHeapAllocation(
                    requestedInitialMb = activeSettings.initialHeapMb,
                    requestedMaxMb = activeSettings.ramMb
                )

                val cpuCores = Runtime.getRuntime().availableProcessors().coerceIn(2, 8)

                val jvmArgs = mutableListOf<String>()
                jvmArgs.add("${runtimeHome.absolutePath}/bin/java")
                jvmArgs.add("-Djava.home=${runtimeHome.absolutePath}")
                jvmArgs.add("-Djava.io.tmpdir=${cacheDir.absolutePath}")
                jvmArgs.add("-Djava.library.path=$gameLdLibraryPath")
                jvmArgs.add("-Dorg.lwjgl.librarypath=$effectiveLwjglDir")
                jvmArgs.add("-Dorg.lwjgl.opengl.libname=$glLibPath")
                jvmArgs.add("-Dorg.lwjgl.openal.libname=$nativeLibDir/libopenal.so")
                jvmArgs.add("-Dorg.lwjgl.vulkan.libname=libvulkan.so")
                jvmArgs.add("-Dorg.lwjgl.freetype.libname=$freetypeLib")
                jvmArgs.add("-Dorg.lwjgl.spvc.libname=$spirvLibPath")
                jvmArgs.add("-Dorg.lwjgl.spvc.defaultname=spirv-cross-c-shared")
                jvmArgs.add("-Dorg.lwjgl.system.allocator=system")
                jvmArgs.add("-Djna.boot.library.path=$nativeLibDir")
                jvmArgs.add("-Dglfwstub.windowWidth=$targetWidth")
                jvmArgs.add("-Dglfwstub.windowHeight=$targetHeight")
                jvmArgs.add("-Dglfwstub.initEgl=${!vulkanMode}")
                jvmArgs.add("-Dos.name=Linux")
                jvmArgs.add("-Dos.version=Android-${Build.VERSION.RELEASE}")
                jvmArgs.add("-Duser.home=${gameDir.parentFile?.absolutePath ?: gameDir.absolutePath}")
                jvmArgs.add("-Dnet.minecraft.clientmodname=BELL-Launcher")
                jvmArgs.add("-Dlog4j2.formatMsgNoLookups=true")
                jvmArgs.add("-Djava.rmi.server.useCodebaseOnly=true")
                jvmArgs.add("-Dcom.sun.jndi.rmi.object.trustURLCodebase=false")
                jvmArgs.add("-Dcom.sun.jndi.cosnaming.object.trustURLCodebase=false")
                jvmArgs.add("-Dfml.earlyprogresswindow=false")
                jvmArgs.add("-Dfml.ignoreInvalidMinecraftCertificates=true")
                jvmArgs.add("-Dfml.ignorePatchDiscrepancies=true")
                jvmArgs.add("-Dloader.disable_forked_guis=true")
                jvmArgs.add("-Djdk.lang.Process.launchMechanism=FORK")
                jvmArgs.add("-Dsodium.checks.issue2561=false")
                jvmArgs.add("-Dfile.encoding=UTF-8")
                jvmArgs.add("-Dsun.stdout.encoding=UTF-8")
                jvmArgs.add("-Dsun.stderr.encoding=UTF-8")

                jvmArgs.add("-Dext.net.resolvPath=${resolvConf.absolutePath}")
                jvmArgs.add("-Djava.net.preferIPv4Stack=true")
                jvmArgs.add("-Djava.net.preferIPv6Addresses=false")
                jvmArgs.add("-Dio.netty.native.workdir=${cacheDir.absolutePath}")
                jvmArgs.add("-Djna.tmpdir=${cacheDir.absolutePath}")
                jvmArgs.add("-Dorg.lwjgl.system.SharedLibraryExtractPath=${cacheDir.absolutePath}")
                jvmArgs.add("-Dio.netty.tryReflectionSetAccessible=true")

                jvmArgs.add("-XX:ActiveProcessorCount=$cpuCores")
                jvmArgs.add("-Xms${safeXmsMb}M")
                jvmArgs.add("-Xmx${safeXmxMb}M")

                val userCustomArgs = activeSettings.customJvmArgs
                val hasCustomGc = userCustomArgs.contains("-XX:+Use") && userCustomArgs.contains("GC")
                if (!hasCustomGc) {
                    jvmArgs.add("-XX:+UseG1GC")
                    jvmArgs.add("-XX:MaxGCPauseMillis=28")
                    jvmArgs.add("-XX:G1ReservePercent=15")
                    jvmArgs.add("-XX:G1HeapRegionSize=4M")
                    jvmArgs.add("-XX:+UnlockExperimentalVMOptions")
                    jvmArgs.add("-XX:G1NewSizePercent=20")
                    jvmArgs.add("-XX:G1MaxNewSizePercent=35")
                    jvmArgs.add("-XX:SurvivorRatio=32")
                    jvmArgs.add("-XX:+ParallelRefProcEnabled")
                }
                jvmArgs.add("-XX:+DisableExplicitGC")
                jvmArgs.add("-XX:+PerfDisableSharedMem")
                jvmArgs.add("-XX:ReservedCodeCacheSize=${if (lowEnd) "96M" else "160M"}")
                jvmArgs.add("-XX:+UseStringDeduplication")

                if (userCustomArgs.isNotBlank()) {
                    userCustomArgs.split(" ")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() && !it.startsWith("-Xmx") && !it.startsWith("-Xms") }
                        .forEach { jvmArgs.add(it) }
                }

                val nuxPatcher = File(com.israadev.nuxlauncher.core.instance.InstanceManager.getNuxDir(this), "components/launcher/MioLibPatcher.jar")
                val patcherJar = if (nuxPatcher.exists()) nuxPatcher else File(filesDir, "components/launcher/MioLibPatcher.jar")
                if (patcherJar.exists()) {
                    jvmArgs.add("-javaagent:${patcherJar.absolutePath}")
                }

                if (!authlibInjectorPath.isNullOrBlank()) {
                    val authlibJar = File(authlibInjectorPath!!)
                    if (authlibJar.exists()) {
                        var effectiveAuthlibUrl: String? = null

                        if (!authlibUrl.isNullOrBlank() && !authlibUrl!!.contains("127.0.0.1") && !authlibUrl!!.contains("localhost")) {
                            effectiveAuthlibUrl = authlibUrl
                        } else {
                            val skinFile = skinPath?.let { File(it) }?.takeIf { it.exists() }
                            val capeFile = capePath?.let { File(it) }?.takeIf { it.exists() }

                            if (skinFile != null || capeFile != null) {
                                try {
                                    com.israadev.nuxlauncher.core.account.offline.OfflineYggdrasilServer.activeInstance?.stop()
                                    val server = com.israadev.nuxlauncher.core.account.offline.OfflineYggdrasilServer(0)
                                    server.addCharacter(
                                        username = username,
                                        uuid = uuid,
                                        skinFile = skinFile,
                                        capeFile = capeFile,
                                        isSlim = skinModel == "slim"
                                    )
                                    val port = server.start()
                                    if (port > 0 && server.isAliveAndResponding()) {
                                        effectiveAuthlibUrl = "http://127.0.0.1:$port"
                                    } else {
                                        server.stop()
                                    }
                                } catch (_: Exception) {}
                            }
                        }

                        if (!effectiveAuthlibUrl.isNullOrBlank()) {
                            jvmArgs.add("-javaagent:${authlibJar.absolutePath}=$effectiveAuthlibUrl")
                            jvmArgs.add("-Dauthlibinjector.side=client")
                        }
                    }
                }

                if (!isJava8) {
                    jvmArgs.add("--add-opens=java.base/java.lang=ALL-UNNAMED")
                    jvmArgs.add("--add-opens=java.base/java.lang.reflect=ALL-UNNAMED")
                    jvmArgs.add("--add-opens=java.base/java.util=ALL-UNNAMED")
                    jvmArgs.add("--add-opens=java.base/java.text=ALL-UNNAMED")
                    jvmArgs.add("--add-opens=java.base/java.net=ALL-UNNAMED")
                    jvmArgs.add("--add-opens=jdk.naming.dns/com.sun.jndi.dns=ALL-UNNAMED")
                    jvmArgs.add("--add-exports=jdk.naming.dns/com.sun.jndi.dns=ALL-UNNAMED")
                    jvmArgs.add("--add-opens=java.desktop/sun.awt=ALL-UNNAMED")
                    jvmArgs.add("--add-opens=java.desktop/java.awt=ALL-UNNAMED")
                    jvmArgs.add("--add-opens=java.desktop/sun.font=ALL-UNNAMED")
                    jvmArgs.add("--add-opens=java.desktop/sun.java2d=ALL-UNNAMED")

                    if (mainClass.contains(".")) {
                        val pkg = mainClass.substring(0, mainClass.lastIndexOf("."))
                        jvmArgs.add("--add-exports")
                        jvmArgs.add("$pkg/$pkg=ALL-UNNAMED")
                    }
                }

                val effectiveClasspath = if (mainClass.contains("knot") || mainClass.contains("fabric")) {
                    val entries = classpath.split(File.pathSeparator).filter { it.isNotBlank() }
                    val hasModernAsm = entries.any { it.contains("org/ow2/asm") && !it.contains("/9.6/") }
                    if (hasModernAsm) {
                        entries.filterNot { it.contains("org/ow2/asm") && it.contains("/9.6/") }
                    } else {
                        entries
                    }.distinct().joinToString(File.pathSeparator)
                } else {
                    classpath.split(File.pathSeparator).filter { it.isNotBlank() }.distinct().joinToString(File.pathSeparator)
                }

                jvmArgs.add("-cp")
                jvmArgs.add(effectiveClasspath)
                if (useWrapper) {
                    jvmArgs.add("mio.Wrapper")
                }
                jvmArgs.add(mainClass)
                jvmArgs.add("--username")
                jvmArgs.add(username)
                jvmArgs.add("--version")
                jvmArgs.add(mcVersion)
                jvmArgs.add("--gameDir")
                jvmArgs.add(gameDir.absolutePath)
                jvmArgs.add("--assetsDir")
                jvmArgs.add(assetsDir.absolutePath)
                jvmArgs.add("--assetIndex")
                jvmArgs.add(assetIndexId)
                jvmArgs.add("--uuid")
                jvmArgs.add(uuid)
                jvmArgs.add("--accessToken")
                jvmArgs.add(accessToken)
                jvmArgs.add("--userType")
                jvmArgs.add(userType)
                jvmArgs.add("--versionType")
                jvmArgs.add("release")

                CrashManager.onGameSessionStarted(
                    context = this,
                    instanceName = instanceName,
                    mcVersion = mcVersion,
                    rendererId = activeSettings.selectedRenderer,
                    gameDirPath = gameDir.absolutePath,
                    loader = loader,
                    loaderVersion = loaderVersion,
                    installedMods = installedMods
                )
                runOnUiThread { appendLiveLog("[BELL Engine] Memulai eksekusi VMLauncher.launchJVM()...") }
                val exitCode = VMLauncher.launchJVM(jvmArgs.toTypedArray())
                runOnUiThread {
                    appendLiveLog("[BELL Engine] JVM terminated with exit code: $exitCode")
                    handleGameExit(exitCode, false)
                }
            } catch (e: Throwable) {
                LoggerBridge.append("[ERROR JVM Launch] ${e.message}")
                e.printStackTrace()
                runOnUiThread {
                    handleGameExit(-1, false, e.stackTraceToString())
                }
            }
        }
    }
}

enum class FpsMode {
    NORMAL,
    PINNED,
    SHOW_LOG
}

@Composable
fun GameScreen(
    instanceName: String,
    mcVersion: String,
    username: String,
    mainClass: String,
    runtimeName: String,
    liveLogs: List<String>,
    latestLogState: State<String>,
    isControlVisibleState: MutableState<Boolean>,
    isGameRenderingState: MutableState<Boolean>,
    onSurfaceReady: (SurfaceHolder) -> Unit,
    onResolutionChange: (Int) -> Unit = {},
    onExit: () -> Unit,
    onRequestStartRecord: (ScreenRecordConfig) -> Unit = {},
    onRequestStopRecord: () -> Unit = {}
) {
    var isControlVisible by isControlVisibleState
    val isGameRendering by isGameRenderingState
    var isManualLoadingDismissed by remember { mutableStateOf(false) }
    val showLoadingOverlay = !isGameRendering && !isManualLoadingDismissed

    var showInGameSettingsDialog by remember { mutableStateOf(false) }
    var inGameSettingsInitialTab by remember { mutableIntStateOf(0) }
    var showPremiumDialog by remember { mutableStateOf(false) }
    val recordState by ScreenRecordManager.recordState.collectAsState()
    var isCustomGuiEditorActive by remember { mutableStateOf(false) }
    var isKeyboardRequested by remember { mutableStateOf(false) }
    var isConsoleVisible by remember { mutableStateOf(false) }
    var isConsoleExpanded by remember { mutableStateOf(false) }
    var showExitConfirmDialog by remember { mutableStateOf(false) }
    var currentFps by remember { mutableIntStateOf(0) }
    var fpsMode by remember { mutableStateOf(FpsMode.NORMAL) }
    val listState = rememberLazyListState()
    val context = LocalContext.current

    LaunchedEffect(showInGameSettingsDialog) {
        while (isActive && !showInGameSettingsDialog) {
            try {
                currentFps = CallbackBridge.getCurrentFps()
            } catch (_: Throwable) {}
            delay(750)
        }
    }

    val gameCursorMode by ZLBridgeStates.cursorMode.collectAsState()
    val launcherSettings by SettingsManager.settings.collectAsState()
    val customButtons by ControlLayoutManager.buttons.collectAsState()
    val mouseControlMode = if (launcherSettings.mouseControlMode == "CLICK") MouseControlMode.CLICK else MouseControlMode.SLIDE

    LaunchedEffect(liveLogs.size, isConsoleVisible, isConsoleExpanded) {
        if (isConsoleVisible && isConsoleExpanded && liveLogs.isNotEmpty()) {
            listState.scrollToItem(liveLogs.size - 1)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val screenWidth = constraints.maxWidth.toFloat()
        val screenHeight = constraints.maxHeight.toFloat()

        var cursorX by remember { mutableFloatStateOf(0f) }
        var cursorY by remember { mutableFloatStateOf(0f) }
        var isMousePressed by remember { mutableStateOf(false) }
        var previousCursorMode by remember { mutableIntStateOf(gameCursorMode) }

        LaunchedEffect(gameCursorMode, screenWidth, screenHeight) {
            if (screenWidth > 0f && screenHeight > 0f) {
                val isInitial = (cursorX == 0f && cursorY == 0f)
                val isReturningToMenu = (gameCursorMode == CURSOR_ENABLED && previousCursorMode == CURSOR_DISABLED)
                if (isInitial || isReturningToMenu) {
                    cursorX = screenWidth / 2f
                    cursorY = screenHeight / 2f
                    val winW = CallbackBridge.windowWidth
                    val winH = CallbackBridge.windowHeight
                    val targetX = if (screenWidth > 0 && winW > 0) cursorX * (winW.toFloat() / screenWidth) else cursorX
                    val targetY = if (screenHeight > 0 && winH > 0) cursorY * (winH.toFloat() / screenHeight) else cursorY
                    CallbackBridge.sendCursorPos(targetX, targetY)
                }
            }
            previousCursorMode = gameCursorMode
        }

        AndroidView(
            factory = { ctx ->
                SurfaceView(ctx).apply {
                    onSurfaceReady(holder)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isCustomGuiEditorActive) {
            CustomGuiEditorScreen(
                isIngame = true,
                onNavigateBack = { isCustomGuiEditorActive = false }
            )
        } else {
            var isPhysicalMouseMode by remember {
                mutableStateOf(
                    if (PhysicalMouseChecker.physicalMouseConnected) {
                        launcherSettings.physicalMouseMode
                    } else {
                        false
                    }
                )
            }

            SwitchableMouseLayout(
                modifier = Modifier.fillMaxSize(),
                screenWidth = screenWidth,
                screenHeight = screenHeight,
                cursorMode = gameCursorMode,
                controlMode = mouseControlMode,
                cursorPosition = Offset(cursorX, cursorY),
                cursorSensitivity = launcherSettings.cursorSensitivity / 100f,
                requestPointerCapture = !launcherSettings.physicalMouseMode,
                onCursorPositionChange = { newPos ->
                    cursorX = newPos.x
                    cursorY = newPos.y
                },
                onPhysicalMouseModeChange = { isPhysical -> isPhysicalMouseMode = isPhysical },
                onMouse = {
                    isPhysicalMouseMode = true
                    isControlVisible = false
                },
