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

// Token Desain BELL Launcher (Monochrome Obsidian Glass + Crisp White)
private val BellGlassCard = Color(0xCC131318)
private val BellGlassElevated = Color(0xEB1A1A22)
private val BellGlassInput = Color(0xD90A0A0E)
private val BellBorder = Color(0x26FFFFFF)
private val BellBorderActive = Color(0x66FFFFFF)
private val BellPillWhite = Color(0xFFFFFFFF)
private val BellInkDark = Color(0xFF09090B)
private val BellTextPrimary = Color(0xFFFFFFFF)
private val BellTextSecondary = Color(0xFFB4B6BE)
private val BellTextMuted = Color(0xFF787B85)
private val BellDanger = Color(0xFFF87171)

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
        Toast.makeText(this, "Recording stopped and saved to Movies/BELL-Launcher!", Toast.LENGTH_SHORT).show()
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

    private fun shouldBypassSurfaceScaler(): Boolean {
        val settings = SettingsManager.settings.value
        val sel = settings.selectedRenderer.lowercase()
        val hasVulkanMod = installedMods.any { it.contains("vulkan", ignoreCase = true) }
        return hasVulkanMod ||
                sel.contains("vulkan") ||
                sel.contains("zink") ||
                sel.contains("mobileglue") ||
                settings.graphicsApi.equals("VULKAN", ignoreCase = true)
    }

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
        val dm = resources.displayMetrics
        val (rawW, rawH) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            Pair(bounds.width(), bounds.height())
        } else {
            Pair(dm.widthPixels, dm.heightPixels)
        }
        val screenW = maxOf(rawW, rawH)
        val screenH = minOf(rawW, rawH)
        return Pair(screenW.coerceAtLeast(640), screenH.coerceAtLeast(360))
    }

    private fun getScaledDisplayDimensions(): Pair<Int, Int> {
        val settings = SettingsManager.settings.value
        val (screenW, screenH) = getNativeLandscapeDimensions()

        if (shouldBypassSurfaceScaler()) {
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

        if (!shouldBypassSurfaceScaler()) {
            val (targetW, targetH) = getScaledDisplayDimensions()
            surfaceHolderRef?.setFixedSize(targetW, targetH)
            CallbackBridge.windowWidth = targetW
            CallbackBridge.windowHeight = targetH
            CallbackBridge.sendUpdateWindowSize(targetW, targetH)
            LoggerBridge.append("▷ [In-Game Resolution] Dynamically updated to $newRatio% (${targetW}x${targetH})")
        } else {
            val (nativeW, nativeH) = getNativeLandscapeDimensions()
            surfaceHolderRef?.setFixedSize(nativeW, nativeH)
            CallbackBridge.windowWidth = nativeW
            CallbackBridge.windowHeight = nativeH
            CallbackBridge.sendUpdateWindowSize(nativeW, nativeH)
            LoggerBridge.append("▷ [In-Game Resolution] Native surface locked at ${nativeW}x${nativeH}")
        }
    }

    private fun hasActiveShaderPack(gameDir: File): Boolean {
        return try {
            val irisProps = File(gameDir, "config/iris.properties")
            if (irisProps.exists()) {
                val text = irisProps.readText()
                val enabled = text.lines().any { it.trim().equals("enableShaders=true", ignoreCase = true) }
                val packLine = text.lines().firstOrNull { it.trim().startsWith("shaderPack=") }
                val packName = packLine?.substringAfter("=")?.trim().orEmpty()
                if (enabled && packName.isNotEmpty() && !packName.equals("(internal)", ignoreCase = true) && !packName.equals("OFF", ignoreCase = true)) {
                    return true
                }
            }
            val shaderDir = File(gameDir, "shaderpacks")
            shaderDir.exists() && (shaderDir.listFiles()?.any { it.name.endsWith(".zip", ignoreCase = true) || it.isDirectory } == true)
        } catch (_: Throwable) {
            false
        }
    }

    private fun optimizeShaderPipeline(gameDir: File, lowEnd: Boolean): Boolean {
        val shaderActive = hasActiveShaderPack(gameDir)
        if (!shaderActive) return false

        try {
            val configDir = File(gameDir, "config").apply { mkdirs() }
            val irisPropsFile = File(configDir, "iris.properties")

            val lines = if (irisPropsFile.exists()) {
                irisPropsFile.readLines().toMutableList()
            } else {
                mutableListOf()
            }

            fun setProp(key: String, value: String, onlyIfDefault: Boolean = false, defaultValue: String = "") {
                val idx = lines.indexOfFirst { it.trim().startsWith("$key=") }
                if (idx >= 0) {
                    val currentVal = lines[idx].substringAfter("=").trim()
                    if (!onlyIfDefault || currentVal == defaultValue) {
                        lines[idx] = "$key=$value"
                    }
                } else {
                    lines.add("$key=$value")
                }
            }

            val userRatio = SettingsManager.settings.value.resolutionRatio
            val targetRenderQuality = when {
                userRatio in 40..95 -> String.format(java.util.Locale.US, "%.2f", userRatio / 100f)
                lowEnd -> "0.65"
                else -> "0.75"
            }
            setProp("renderQuality", targetRenderQuality, onlyIfDefault = (userRatio == 100), defaultValue = "1.0")
            setProp("maxShadowRenderDistance", if (lowEnd) "48" else "64")
            setProp("allowUnknownShaders", "true")

            irisPropsFile.writeText(lines.joinToString("\n") + "\n")
            LoggerBridge.append("▷ [Shader Optimizer] Iris tuned (renderQuality=$targetRenderQuality, shadowDist=${if (lowEnd) 48 else 64})")
        } catch (e: Throwable) {
            LoggerBridge.append("▷ [Shader Optimizer Warning] ${e.message}")
        }

        try {
            val shaderCacheDir = File(cacheDir, "bell_shader_cache").apply { mkdirs() }
            Os.setenv("MESA_SHADER_CACHE_DIR", shaderCacheDir.absolutePath, true)
            Os.setenv("MESA_SHADER_CACHE_MAX_SIZE", if (lowEnd) "64M" else "128M", true)
            Os.setenv("LIBGL_NOERROR", "1", true)
            Os.setenv("LIBGL_FASTEX", "1", true)
        } catch (_: Throwable) {}

        return true
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
                appendLiveLog("[BELL Engine] Game process ended (code: $exitCode, signal: $isSignal)")
                handleGameExit(exitCode, isSignal)
            }
        }

        CallbackBridge.setGraphicOutputListener {
            runOnUiThread {
                isGameRenderingState.value = true
                appendLiveLog("🎨 [Render Engine] First graphical frame rendered on Surface!")
                val winW = CallbackBridge.windowWidth.takeIf { it > 0 } ?: getNativeLandscapeDimensions().first
                val winH = CallbackBridge.windowHeight.takeIf { it > 0 } ?: getNativeLandscapeDimensions().second
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
                        if (!shouldBypassSurfaceScaler() && SettingsManager.settings.value.resolutionRatio != 100) {
                            val (targetWidth, targetHeight) = getScaledDisplayDimensions()
                            surfaceHolder.setFixedSize(targetWidth, targetHeight)
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
        val useHardwareScaler = !shouldBypassSurfaceScaler() && SettingsManager.settings.value.resolutionRatio != 100

        val (targetWidth, targetHeight) = if (useHardwareScaler) {
            getScaledDisplayDimensions().also { (w, h) ->
                holder.setFixedSize(w, h)
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
            appendLiveLog("[Render Bridge] Native window bound (${targetWidth}x${targetHeight}).")
        } catch (e: Throwable) {
            appendLiveLog("[Render Bridge Error] ${e.message}")
        }

        if (!isGameStarted) {
            isGameStarted = true
            startGameJVM()
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        if (width < height && !isGameStarted) return

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
                val initW = CallbackBridge.windowWidth.takeIf { it > 0 } ?: getNativeLandscapeDimensions().first
                val initH = CallbackBridge.windowHeight.takeIf { it > 0 } ?: getNativeLandscapeDimensions().second

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

                    set("overrideWidth", "$initW")
                    set("overrideHeight", "$initH")
                    save()
                }
                LoggerBridge.append("▷ [MCOptions] Minecraft options verified (res=${initW}x${initH}, vulkan=$vulkanMode)")

                val isShaderMode = if (activeSettings.autoOptimizeMinecraft || lowEnd) {
                    optimizeShaderPipeline(gameDir, lowEnd)
                } else {
                    hasActiveShaderPack(gameDir)
                }
                if (isShaderMode) {
                    runOnUiThread { appendLiveLog("[BELL Shader Boost] Mode hemat GPU untuk Shader diaktifkan!") }
                }

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

                if (rendererId.startsWith("opengles2") || targetRenderer.id == "gl4es" || isMobileGlues) {
                    Os.setenv("LIBGL_NOERROR", "1", true)
                    Os.setenv("LIBGL_FASTEX", "1", true)
                }

                if (rendererId.startsWith("opengles2") || targetRenderer.id == "gl4es") {
                    Os.setenv("LIBGL_ES", "2", true)
                    Os.setenv("LIBGL_MIPMAP", "3", true)
                    Os.setenv("LIBGL_NOINTOVLHACK", "1", true)
                    Os.setenv("LIBGL_NORMALIZE", "1", true)
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

                if (rendererId.startsWith("gallium") || rendererId.contains("zink") || vulkanMode || isShaderMode) {
                    Os.setenv("MESA_GLSL_CACHE_DIR", cacheDir.absolutePath, true)
                    Os.setenv("MESA_GLSL_CACHE_MAX_SIZE", if (lowEnd) "64M" else "128M", true)
                }
                Os.setenv("JAVA_HOME", runtimeHome.absolutePath, true)
                Os.setenv("HOME", gameDir.absolutePath, true)
                Os.setenv("TMPDIR", cacheDir.absolutePath, true)
                Os.setenv("PATH", "${runtimeHome.absolutePath}/bin:" + (Os.getenv("PATH") ?: "/system/bin"), true)
                Os.setenv("LD_LIBRARY_PATH", gameLdLibraryPath, true)
                Os.setenv("AWTSTUB_WIDTH", "$initW", true)
                Os.setenv("AWTSTUB_HEIGHT", "$initH", true)
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
                jvmArgs.add("-Dglfwstub.windowWidth=$initW")
                jvmArgs.add("-Dglfwstub.windowHeight=$initH")
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

                val directMemMb = if (isShaderMode) {
                    if (lowEnd) 256 else 384
                } else {
                    192
                }
                jvmArgs.add("-XX:MaxDirectMemorySize=${directMemMb}M")

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
                        .filter { it.isNotEmpty() && !it.startsWith("-Xmx") && !it.startsWith("-Xms") && !it.startsWith("-XX:MaxDirectMemorySize") }
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
                onTouch = { isPhysicalMouseMode = false },
                onTap = { pos ->
                    val winW = CallbackBridge.windowWidth
                    val winH = CallbackBridge.windowHeight
                    val targetX = if (screenWidth > 0 && winW > 0) pos.x * (winW.toFloat() / screenWidth) else pos.x
                    val targetY = if (screenHeight > 0 && winH > 0) pos.y * (winH.toFloat() / screenHeight) else pos.y
                    CallbackBridge.putMouseEventWithCoords(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT, targetX, targetY)
                },
                onLongPress = {
                    isMousePressed = true
                    CallbackBridge.putMouseEvent(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT, true)
                },
                onLongPressEnd = {
                    isMousePressed = false
                    CallbackBridge.putMouseEvent(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT, false)
                },
                onCapturedMove = { delta ->
                    val sens = launcherSettings.captureSensitivity / 100f
                    CallbackBridge.sendCursorDelta(delta.x * sens, delta.y * sens)
                }
            )

            if (showLoadingOverlay) {
                GameLoadingOverlay(
                    visible = true,
                    instanceName = instanceName,
                    mcVersion = mcVersion,
                    latestLog = latestLogState.value,
                    onClose = { isManualLoadingDismissed = true },
                    onViewLog = {
                        fpsMode = FpsMode.SHOW_LOG
                        isConsoleVisible = true
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (isKeyboardRequested) {
                HidableInputLayout(onClose = { isKeyboardRequested = false })
            }

            val shouldShowPointer = if (mouseControlMode == MouseControlMode.CLICK && launcherSettings.hideMouseInClickMode) {
                false
            } else if (PhysicalMouseChecker.physicalMouseConnected && isPhysicalMouseMode && launcherSettings.physicalMouseMode) {
                false
            } else {
                gameCursorMode == CURSOR_ENABLED && cursorX > 0f && cursorY > 0f
            }

            if (shouldShowPointer) {
                VirtualCursorPointer(
                    x = cursorX,
                    y = cursorY,
                    isPressed = isMousePressed,
                    cursorSizeDp = launcherSettings.mouseSizeDp
                )
            }

            if (recordState.isRecording) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(BellGlassElevated)
                        .border(1.2.dp, BellDanger, RoundedCornerShape(20.dp))
                        .clickable { onRequestStopRecord() }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(9.dp).background(BellDanger, CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "REC ${recordState.formattedDuration}",
                            color = BellTextPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "TAP TO STOP", color = BellDanger, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                    }
                }
            }

            if (isConsoleVisible) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                        .width(if (isConsoleExpanded) 420.dp else 300.dp)
                        .background(BellGlassElevated, RoundedCornerShape(12.dp))
                        .border(1.dp, BellBorderActive, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier.clickable {
                                inGameSettingsInitialTab = 0
                                showInGameSettingsDialog = true
                            }
                        ) {
                            NuxBadge(
                                text = if (currentFps > 0) "$currentFps FPS" else "FPS: --",
                                backgroundColor = when {
                                    currentFps >= 50 -> Color(0xFF10B981)
                                    currentFps >= 25 -> Color(0xFFF59E0B)
                                    currentFps > 0 -> BellDanger
                                    else -> BellGlassInput
                                },
                                textColor = BellTextPrimary
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clickable { isConsoleExpanded = !isConsoleExpanded }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isConsoleExpanded) "▲ KECILKAN" else "▼ LOG LENGKAP",
                                    color = BellTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clickable {
                                        isConsoleVisible = false
                                        fpsMode = FpsMode.NORMAL
                                    }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(text = "✕", color = BellTextMuted, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (isConsoleExpanded) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxWidth().height(180.dp)
                        ) {
                            items(liveLogs) { log ->
                                Text(
                                    text = log,
                                    color = BellTextSecondary,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    lineHeight = 12.sp
                                )
                            }
                        }
                    } else {
                        liveLogs.takeLast(4).forEach { log ->
                            Text(
                                text = log,
                                color = BellTextSecondary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                maxLines = 1,
                                lineHeight = 12.sp
                            )
                        }
                    }
                }
            }

            val density = LocalDensity.current
            customButtons.forEach { btn ->
                val btnWidthPx = with(density) { btn.widthDp.dp.toPx() }
                val btnHeightPx = with(density) { btn.heightDp.dp.toPx() }
                val centerX = screenWidth * (btn.xPercent / 100f)
                val centerY = screenHeight * (btn.yPercent / 100f)
                val leftPx = (centerX - btnWidthPx / 2f).coerceIn(0f, (screenWidth - btnWidthPx).coerceAtLeast(0f))
                val topPx = (centerY - btnHeightPx / 2f).coerceIn(0f, (screenHeight - btnHeightPx).coerceAtLeast(0f))
                val leftDp = with(density) { leftPx.toDp() }
                val topDp = with(density) { topPx.toDp() }
                val buttonModifier = Modifier.offset(x = leftDp, y = topDp)

                if (btn.isSystem) {
                    when (btn.systemAction) {
                        "FPS" -> {
                            val shouldShowFps = when (fpsMode) {
                                FpsMode.NORMAL -> isControlVisible && !isConsoleVisible
                                FpsMode.PINNED -> !isConsoleVisible
                                FpsMode.SHOW_LOG -> false
                            }
                            if (shouldShowFps) {
                                val isPinned = fpsMode == FpsMode.PINNED
                                Box(
                                    modifier = buttonModifier
                                        .wrapContentWidth()
                                        .defaultMinSize(minWidth = btn.widthDp.dp, minHeight = btn.heightDp.dp)
                                        .alpha(btn.opacity)
                                        .clip(RoundedCornerShape(btn.cornerRadiusDp.dp))
                                        .background(BellGlassCard)
                                        .border(
                                            1.dp,
                                            if (isPinned) BellPillWhite else BellBorderActive,
                                            RoundedCornerShape(btn.cornerRadiusDp.dp)
                                        )
                                        .clickable {
                                            inGameSettingsInitialTab = 0
                                            showInGameSettingsDialog = true
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        if (isPinned) {
                                            Text(text = "📌", fontSize = 9.sp, modifier = Modifier.padding(end = 4.dp))
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(
                                                    when {
                                                        currentFps >= 50 -> BellPillWhite
                                                        currentFps >= 25 -> Color(0xFFF59E0B)
                                                        currentFps > 0 -> BellDanger
                                                        else -> BellTextMuted
                                                    },
                                                    CircleShape
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = if (currentFps > 0) "$currentFps FPS" else "FPS: --",
                                            color = BellTextPrimary,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 10.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                        "KEYBOARD" -> {
                            if (isControlVisible) {
                                val active = isKeyboardRequested
                                Box(
                                    modifier = buttonModifier
                                        .size(width = btn.widthDp.dp, height = btn.heightDp.dp)
                                        .alpha(btn.opacity)
                                        .clip(RoundedCornerShape(btn.cornerRadiusDp.dp))
                                        .background(if (active) BellPillWhite else BellGlassCard)
                                        .border(
                                            1.dp,
                                            if (active) BellPillWhite else BellBorderActive,
                                            RoundedCornerShape(btn.cornerRadiusDp.dp)
                                        )
                                        .clickable { isKeyboardRequested = !isKeyboardRequested },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "KEYBOARD",
                                        color = if (active) BellInkDark else BellTextPrimary,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                        "HIDE_GUI" -> {
                            // Saat isControlVisible == false, tombol sepenuhnya transparan secara visual
                            // namun area sentuhnya tetap aktif di posisinya untuk memunculkan GUI kembali.
                            Box(
                                modifier = buttonModifier
                                    .size(width = btn.widthDp.dp, height = btn.heightDp.dp)
                                    .alpha(if (isControlVisible) btn.opacity else 0f)
                                    .clip(RoundedCornerShape(btn.cornerRadiusDp.dp))
                                    .background(if (isControlVisible) BellGlassCard else Color.Transparent)
                                    .border(
                                        1.dp,
                                        if (isControlVisible) BellBorderActive else Color.Transparent,
                                        RoundedCornerShape(btn.cornerRadiusDp.dp)
                                    )
                                    .clickable { isControlVisible = !isControlVisible },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isControlVisible) {
                                    Text(
                                        text = "HIDE GUI",
                                        color = BellTextSecondary,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                        "CLOSE" -> {
                            if (isControlVisible) {
                                Box(
                                    modifier = buttonModifier
                                        .size(width = btn.widthDp.dp, height = btn.heightDp.dp)
                                        .alpha(btn.opacity)
                                        .clip(RoundedCornerShape(btn.cornerRadiusDp.dp))
                                        .background(BellDanger.copy(alpha = 0.28f))
                                        .border(1.dp, BellDanger.copy(alpha = 0.6f), RoundedCornerShape(btn.cornerRadiusDp.dp))
                                        .clickable { showExitConfirmDialog = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "✕", color = BellDanger, fontWeight = FontWeight.Black, fontSize = 13.sp)
                                }
                            }
                        }
                        "RECORD" -> {
                            if (isControlVisible) {
                                val isRec = recordState.isRecording
                                Box(
                                    modifier = buttonModifier
                                        .size(width = btn.widthDp.dp, height = btn.heightDp.dp)
                                        .alpha(btn.opacity)
                                        .clip(RoundedCornerShape(btn.cornerRadiusDp.dp))
                                        .background(if (isRec) BellDanger else Color(0x4DD97706))
                                        .border(
                                            1.dp,
                                            if (isRec) BellDanger else Color(0xFFF59E0B),
                                            RoundedCornerShape(btn.cornerRadiusDp.dp)
                                        )
                                        .clickable {
                                            inGameSettingsInitialTab = 3
                                            showInGameSettingsDialog = true
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(if (isRec) BellPillWhite else BellDanger, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = if (isRec) recordState.formattedDuration else "REC",
                                            color = BellTextPrimary,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 10.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (isControlVisible) {
                        if (btn.isJoystick) {
                            CustomVirtualJoystick(button = btn, modifier = buttonModifier)
                        } else {
                            CustomVirtualButton(button = btn, modifier = buttonModifier)
                        }
                    }
                }
            }

            if (showExitConfirmDialog) {
                Dialog(
                    onDismissRequest = { showExitConfirmDialog = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        NuxCard(
                            modifier = Modifier.width(360.dp).wrapContentHeight(),
                            backgroundColor = NuxColors.SurfaceWhite,
                            shadowOffset = 5.dp,
                            cornerRadius = NuxSizes.CornerRadiusLarge,
                            fillMaxHeight = false
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
                                Text(text = "EXIT MINECRAFT?", color = NuxColors.DarkGray, fontWeight = FontWeight.Black, fontSize = 15.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "The Minecraft JVM process will be stopped and you will return to the launcher.",
                                    color = NuxColors.GrayNeutral,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    NuxButton(
                                        onClick = { showExitConfirmDialog = false },
                                        backgroundColor = NuxColors.SurfaceWhite,
                                        contentColor = NuxColors.DarkGray,
                                        shadowOffset = 2.dp,
                                        cornerRadius = 8.dp,
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Text("CANCEL", fontWeight = FontWeight.Black, fontSize = 11.sp)
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    NuxButton(
                                        onClick = {
                                            showExitConfirmDialog = false
                                            onExit()
                                        },
                                        backgroundColor = NuxColors.Coral,
                                        contentColor = Color.White,
                                        shadowOffset = 2.dp,
                                        cornerRadius = 8.dp,
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Text("QUIT ✕", fontWeight = FontWeight.Black, fontSize = 11.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            InGameSettingsDialog(
                visible = showInGameSettingsDialog,
                onDismissRequest = { showInGameSettingsDialog = false },
                initialTab = inGameSettingsInitialTab,
                currentFps = currentFps,
                instanceName = instanceName,
                mcVersion = mcVersion,
                currentResolutionRatio = launcherSettings.resolutionRatio,
                onResolutionChange = { newRatio -> onResolutionChange(newRatio) },
                fpsMode = fpsMode,
                onFpsModeChange = { newMode -> fpsMode = newMode },
                onOpenConsoleLog = {
                    fpsMode = FpsMode.SHOW_LOG
                    isConsoleVisible = true
                },
                cursorSensitivity = launcherSettings.cursorSensitivity,
                onCursorSensitivityChange = { newSens ->
                    SettingsManager.updateSettings(context, launcherSettings.copy(cursorSensitivity = newSens))
                },
                captureSensitivity = launcherSettings.captureSensitivity,
                onCaptureSensitivityChange = { newSens ->
                    SettingsManager.updateSettings(context, launcherSettings.copy(captureSensitivity = newSens))
                },
                mouseControlMode = mouseControlMode,
                onMouseControlModeChange = { newMode ->
                    SettingsManager.updateSettings(
                        context,
                        launcherSettings.copy(mouseControlMode = if (newMode == MouseControlMode.CLICK) "CLICK" else "SLIDE")
                    )
                },
                isControlVisible = isControlVisible,
                onToggleControlVisibility = { isControlVisible = !isControlVisible },
                onRequestKeyboard = { isKeyboardRequested = true },
                onSendKeycode = { keycode ->
                    CallbackBridge.sendKeyPress(keycode, true)
                    CallbackBridge.sendKeyPress(keycode, false)
                },
                onForceExitRequest = { showExitConfirmDialog = true },
                onOpenCustomGui = {
                    showInGameSettingsDialog = false
                    isCustomGuiEditorActive = true
                },
                isRecording = recordState.isRecording,
                recordingDurationSeconds = recordState.durationSeconds,
                onRequestStartRecord = { config -> onRequestStartRecord(config) },
                onStopRecord = { onRequestStopRecord() },
                onOpenPremium = { showPremiumDialog = true }
            )

            if (showPremiumDialog) {
                NuxPremiumDialog(onDismissRequest = { showPremiumDialog = false })
            }
        }
    }
}

@Composable
fun VirtualCursorPointer(
    x: Float,
    y: Float,
    isPressed: Boolean = false,
    cursorSizeDp: Int = 24,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val xDp = with(density) { x.toDp() }
    val yDp = with(density) { y.toDp() }

    Box(
        modifier = modifier
            .offset(x = xDp, y = yDp)
            .size(cursorSizeDp.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val scale = (cursorSizeDp / 24f) * (if (isPressed) 0.88f else 1.0f)
            scale(scale, pivot = Offset.Zero) {
                val path = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(0f, 22f * density.density)
                    lineTo(5.5f * density.density, 16.5f * density.density)
                    lineTo(10.5f * density.density, 24.5f * density.density)
                    lineTo(13.5f * density.density, 23f * density.density)
                    lineTo(8.5f * density.density, 15f * density.density)
                    lineTo(15.5f * density.density, 15f * density.density)
                    close()
                }

                val shadowPath = Path().apply {
                    addPath(path, Offset(2f * density.density, 2f * density.density))
                }
                drawPath(path = shadowPath, color = Color.Black.copy(alpha = 0.45f), style = Fill)
                drawPath(path = path, color = Color.White, style = Fill)
                drawPath(
                    path = path,
                    color = Color.Black,
                    style = Stroke(width = 1.8f * density.density, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
    }
}

@Composable
fun CustomVirtualButton(
    button: CustomControlButton,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var turboJob by remember { mutableStateOf<Job?>(null) }

    DisposableEffect(button.id) {
        onDispose { turboJob?.cancel() }
    }

    if (button.isScroll) {
        val density = LocalDensity.current
        val thresholdPx = with(density) { 14.dp.toPx() }

        Box(
            modifier = modifier
                .size(width = button.widthDp.dp, height = button.heightDp.dp)
                .alpha(button.opacity)
                .clip(RoundedCornerShape(button.cornerRadiusDp.dp))
                .background(if (isPressed) BellPillWhite else BellGlassCard)
                .border(
                    1.dp,
                    if (isPressed) BellPillWhite else BellBorderActive,
                    RoundedCornerShape(button.cornerRadiusDp.dp)
                )
                .pointerInput(button.id) {
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            isPressed = true
                            val startY = down.position.y
                            var totalDragY = 0f
                            var hasDragged = false

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) {
                                    if (!hasDragged) {
                                        val h = size.height
                                        if (startY < h / 2f) {
                                            CallbackBridge.sendScroll(0.0, 1.0)
                                        } else {
                                            CallbackBridge.sendScroll(0.0, -1.0)
                                        }
                                    }
                                    break
                                }

                                val dy = change.position.y - change.previousPosition.y
                                totalDragY += dy
                                if (kotlin.math.abs(totalDragY) > 6f) hasDragged = true

                                if (totalDragY <= -thresholdPx) {
                                    CallbackBridge.sendScroll(0.0, 1.0)
                                    totalDragY = 0f
                                } else if (totalDragY >= thresholdPx) {
                                    CallbackBridge.sendScroll(0.0, -1.0)
                                    totalDragY = 0f
                                }
                                change.consume()
                            }
                            isPressed = false
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxHeight().padding(vertical = 4.dp)
            ) {
                Text(text = "▲", color = if (isPressed) BellInkDark else BellTextSecondary, fontSize = 8.sp, fontWeight = FontWeight.Black)
                Text(text = button.name, color = if (isPressed) BellInkDark else BellTextPrimary, fontWeight = FontWeight.Black, fontSize = 9.sp, maxLines = 1)
                Text(text = "▼", color = if (isPressed) BellInkDark else BellTextSecondary, fontSize = 8.sp, fontWeight = FontWeight.Black)
            }
        }
        return
    }

    fun sendPress(down: Boolean) {
        if (button.isMouseButton) {
            CallbackBridge.putMouseEvent(button.mouseButton, down)
        } else {
            CallbackBridge.sendKeyPress(button.keyCode, down)
        }
    }

    Box(
        modifier = modifier
            .size(width = button.widthDp.dp, height = button.heightDp.dp)
            .alpha(button.opacity)
            .clip(RoundedCornerShape(button.cornerRadiusDp.dp))
            .background(if (isPressed) BellPillWhite else BellGlassCard)
            .border(
                1.dp,
                if (isPressed) BellPillWhite else BellBorderActive,
                RoundedCornerShape(button.cornerRadiusDp.dp)
            )
            .pointerInput(button.id, button.isToggle, button.isMacro, button.macroType, button.macroCommand, button.macroComboKey, button.macroComboKeys, button.macroTurboIntervalMs) {
                detectTapGestures(
                    onPress = {
                        if (button.isMacro) {
                            when (button.macroType) {
                                "COMMAND" -> {
                                    val cmd = button.macroCommand.trim()
                                    if (cmd.isNotEmpty()) {
                                        isPressed = true
                                        coroutineScope.launch {
                                            try {
                                                if (cmd.startsWith("/")) {
                                                    CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_SLASH, true)
                                                    delay(20)
                                                    CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_SLASH, false)
                                                    delay(50)
                                                    for (ch in cmd.substring(1)) {
                                                        CallbackBridge.sendChar(ch, 0)
                                                        delay(10)
                                                    }
                                                } else {
                                                    CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_T, true)
                                                    delay(20)
                                                    CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_T, false)
                                                    delay(50)
                                                    for (ch in cmd) {
                                                        CallbackBridge.sendChar(ch, 0)
                                                        delay(10)
                                                    }
                                                }
                                                delay(40)
                                                CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_ENTER, true)
                                                delay(25)
                                                CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_ENTER, false)
                                            } finally {
                                                delay(50)
                                                isPressed = false
                                            }
                                        }
                                        tryAwaitRelease()
                                    }
                                }
                                "COMBO" -> {
                                    val comboKeys = if (button.macroComboKeys.isNotEmpty()) button.macroComboKeys
                                    else if (button.macroComboKey != 0) listOf(button.macroComboKey) else emptyList()

                                    if (button.isToggle) {
                                        isPressed = !isPressed
                                        if (isPressed) {
                                            for (k in comboKeys) { CallbackBridge.sendKeyPress(k, true); delay(12) }
                                            sendPress(true)
                                        } else {
                                            sendPress(false)
                                            for (k in comboKeys.reversed()) { CallbackBridge.sendKeyPress(k, false); delay(12) }
                                        }
                                    } else {
                                        isPressed = true
                                        for (k in comboKeys) { CallbackBridge.sendKeyPress(k, true); delay(12) }
                                        sendPress(true)
                                        tryAwaitRelease()
                                        sendPress(false)
                                        for (k in comboKeys.reversed()) { CallbackBridge.sendKeyPress(k, false); delay(12) }
                                        isPressed = false
                                    }
                                }
                                "TURBO" -> {
                                    val interval = button.macroTurboIntervalMs.coerceAtLeast(30L)
                                    val downTime = (interval / 2).coerceAtLeast(15L)
                                    val upTime = (interval - downTime).coerceAtLeast(15L)

                                    if (button.isToggle) {
                                        isPressed = !isPressed
                                        if (isPressed) {
                                            turboJob = coroutineScope.launch {
                                                while (isActive) { sendPress(true); delay(downTime); sendPress(false); delay(upTime) }
                                            }
                                        } else {
                                            turboJob?.cancel(); turboJob = null; sendPress(false)
                                        }
                                    } else {
                                        isPressed = true
                                        val job = coroutineScope.launch {
                                            while (isActive) { sendPress(true); delay(downTime); sendPress(false); delay(upTime) }
                                        }
                                        tryAwaitRelease()
                                        job.cancel(); sendPress(false); isPressed = false
                                    }
                                }
                                else -> {
                                    if (button.isToggle) { isPressed = !isPressed; sendPress(isPressed) }
                                    else { isPressed = true; sendPress(true); tryAwaitRelease(); isPressed = false; sendPress(false) }
                                }
                            }
                        } else {
                            if (button.isToggle) { isPressed = !isPressed; sendPress(isPressed) }
                            else { isPressed = true; sendPress(true); tryAwaitRelease(); isPressed = false; sendPress(false) }
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(
                text = button.name,
                color = if (isPressed) BellInkDark else BellTextPrimary,
                fontWeight = FontWeight.Black,
                fontSize = if (button.name.length > 5) 10.sp else 12.sp,
                maxLines = 1
            )
            if (button.isMacro) {
                Text(
                    text = when (button.macroType) {
                        "COMMAND" -> "⚡CMD"
                        "COMBO" -> "⚡CMB"
                        "TURBO" -> "⚡TRB"
                        else -> "⚡MAC"
                    },
                    color = if (isPressed) BellInkDark else BellTextSecondary,
                    fontWeight = FontWeight.Black,
                    fontSize = 7.sp
                )
            }
            if (button.isToggle && isPressed) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(modifier = Modifier.size(4.dp).background(BellInkDark, CircleShape))
            }
        }
    }
}

@Composable
fun CustomVirtualJoystick(
    button: CustomControlButton,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val sizePx = with(density) { button.widthDp.dp.toPx() }
    val radiusPx = sizePx / 2f
    val knobRadiusPx = radiusPx * 0.38f
    val maxDragDistance = radiusPx - knobRadiusPx

    var knobOffset by remember { mutableStateOf(Offset.Zero) }
    var isTouching by remember { mutableStateOf(false) }

    var isWDown by remember { mutableStateOf(false) }
    var isADown by remember { mutableStateOf(false) }
    var isSDown by remember { mutableStateOf(false) }
    var isDDown by remember { mutableStateOf(false) }

    fun updateKeys(needW: Boolean, needA: Boolean, needS: Boolean, needD: Boolean) {
        if (needW != isWDown) { isWDown = needW; CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_W, needW) }
        if (needA != isADown) { isADown = needA; CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_A, needA) }
        if (needS != isSDown) { isSDown = needS; CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_S, needS) }
        if (needD != isDDown) { isDDown = needD; CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_D, needD) }
    }

    fun releaseAll() {
        updateKeys(false, false, false, false)
        knobOffset = Offset.Zero
        isTouching = false
    }

    DisposableEffect(button.id) {
        onDispose { releaseAll() }
    }

    Box(
        modifier = modifier
            .size(button.widthDp.dp)
            .alpha(button.opacity)
            .pointerInput(button.id) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        isTouching = true
                        val center = Offset(size.width / 2f, size.height / 2f)

                        fun processPosition(pos: Offset) {
                            val rawDelta = pos - center
                            val dist = kotlin.math.hypot(rawDelta.x.toDouble(), rawDelta.y.toDouble()).toFloat()
                            val clampedDist = dist.coerceAtMost(maxDragDistance)
                            val angleRad = kotlin.math.atan2(rawDelta.y.toDouble(), rawDelta.x.toDouble())

                            knobOffset = if (dist > 0f) {
                                Offset(
                                    (kotlin.math.cos(angleRad) * clampedDist).toFloat(),
                                    (kotlin.math.sin(angleRad) * clampedDist).toFloat()
                                )
                            } else {
                                Offset.Zero
                            }

                            val deadzone = maxDragDistance * 0.20f
                            if (clampedDist < deadzone) {
                                updateKeys(false, false, false, false)
                            } else {
                                val deg = Math.toDegrees(angleRad)
                                updateKeys(
                                    needW = deg in -157.5..-22.5,
                                    needA = deg !in -112.5..112.5,
                                    needS = deg in 22.5..157.5,
                                    needD = deg in -67.5..67.5
                                )
                            }
                        }

                        processPosition(down.position)

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            processPosition(change.position)
                            change.consume()
                        }
                        releaseAll()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            val c = Offset(size.width / 2f, size.height / 2f)

            drawCircle(color = if (isTouching) BellGlassElevated else BellGlassCard, radius = r, center = c)
            drawCircle(
                color = if (isTouching) BellPillWhite else BellBorderActive,
                radius = r - 2.dp.toPx(),
                center = c,
                style = Stroke(width = 2.dp.toPx())
            )
            drawLine(color = BellBorder, start = Offset(c.x, c.y - r * 0.7f), end = Offset(c.x, c.y + r * 0.7f), strokeWidth = 1.dp.toPx())
            drawLine(color = BellBorder, start = Offset(c.x - r * 0.7f, c.y), end = Offset(c.x + r * 0.7f, c.y), strokeWidth = 1.dp.toPx())

            if (isWDown) drawCircle(BellPillWhite, radius = 3.dp.toPx(), center = Offset(c.x, c.y - r * 0.8f))
            if (isSDown) drawCircle(BellPillWhite, radius = 3.dp.toPx(), center = Offset(c.x, c.y + r * 0.8f))
            if (isADown) drawCircle(BellPillWhite, radius = 3.dp.toPx(), center = Offset(c.x - r * 0.8f, c.y))
            if (isDDown) drawCircle(BellPillWhite, radius = 3.dp.toPx(), center = Offset(c.x + r * 0.8f, c.y))

            val currentKnobCenter = c + knobOffset
            drawCircle(
                brush = Brush.radialGradient(
                    colors = if (isTouching) listOf(Color(0xFF27272F), BellInkDark) else listOf(BellGlassElevated, BellGlassInput),
                    center = currentKnobCenter,
                    radius = knobRadiusPx
                ),
                radius = knobRadiusPx,
                center = currentKnobCenter
            )
            drawCircle(color = BellPillWhite, radius = knobRadiusPx, center = currentKnobCenter, style = Stroke(width = 1.5.dp.toPx()))
            drawCircle(color = BellPillWhite, radius = 3.5.dp.toPx(), center = currentKnobCenter)
        }

        Text(text = "W", color = if (isWDown) BellTextPrimary else BellTextSecondary, fontSize = 8.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.TopCenter).padding(top = 4.dp))
        Text(text = "S", color = if (isSDown) BellTextPrimary else BellTextSecondary, fontSize = 8.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp))
        Text(text = "A", color = if (isADown) BellTextPrimary else BellTextSecondary, fontSize = 8.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.CenterStart).padding(start = 5.dp))
        Text(text = "D", color = if (isDDown) BellTextPrimary else BellTextSecondary, fontSize = 8.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 5.dp))
    }
}
