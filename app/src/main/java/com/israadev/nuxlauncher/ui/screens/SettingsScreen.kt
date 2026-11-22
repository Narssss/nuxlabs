package com.israadev.nuxlauncher.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.israadev.nuxlauncher.R
import com.israadev.nuxlauncher.core.account.AccountManager
import com.israadev.nuxlauncher.core.auth.AuthService
import com.israadev.nuxlauncher.core.crash.AICrashAnalyzer
import com.israadev.nuxlauncher.core.lang.NuxLanguageManager
import com.israadev.nuxlauncher.core.lang.tr
import com.israadev.nuxlauncher.core.renderer.NuxRendererInfo
import com.israadev.nuxlauncher.core.renderer.NuxRendererPluginManager
import com.israadev.nuxlauncher.core.renderer.NuxRendererRegistry
import com.israadev.nuxlauncher.core.settings.SettingsManager
import com.israadev.nuxlauncher.core.utils.NuxVersionUtils
import com.israadev.nuxlauncher.ui.components.*
import com.israadev.nuxlauncher.ui.dialogs.NuxAboutDialog
import com.israadev.nuxlauncher.ui.dialogs.NuxPremiumDialog
import com.israadev.nuxlauncher.ui.dialogs.NuxRendererV2ConfigDialog
import com.israadev.nuxlauncher.ui.theme.NuxSizes
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// Token Desain Bell Launcher (Monochrome Obsidian Glass + Crisp White)
private val BellGlassSidebar = Color(0xCC0D0D11)
private val BellGlassCard = Color(0xBF131318)
private val BellGlassElevated = Color(0xE61A1A22)
private val BellGlassInput = Color(0xCC0A0A0E)
private val BellBorder = Color(0x26FFFFFF)
private val BellBorderActive = Color(0x66FFFFFF)
private val BellPillWhite = Color(0xFFFFFFFF)
private val BellInkDark = Color(0xFF09090B)
private val BellTextPrimary = Color(0xFFFFFFFF)
private val BellTextSecondary = Color(0xFFB4B6BE)
private val BellTextMuted = Color(0xFF787B85)
private val BellDanger = Color(0xFFF87171)
private val BellAmber = Color(0xFFFBBF24)

private data class SettingsSectionItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

private val SETTINGS_SECTIONS = listOf(
    SettingsSectionItem("profile", "PROFILE", "Identity & Language", Icons.Outlined.AccountCircle),
    SettingsSectionItem("memory", "MEMORY & JVM", "RAM & Java Runtime", Icons.Outlined.Memory),
    SettingsSectionItem("graphics", "GRAPHICS", "Renderer & Vulkan", Icons.Outlined.Speed),
    SettingsSectionItem("controls", "CONTROLS", "Mouse & Virtual GUI", Icons.Outlined.Mouse),
    SettingsSectionItem("ai", "AI DIAGNOSTICS", "Crash Analyzer & Key", Icons.Outlined.AutoFixHigh)
)

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onOpenGuiEditor: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentSettings by SettingsManager.settings.collectAsState()
    val launcherUser by AccountManager.launcherUser.collectAsState()

    var activeSection by remember { mutableStateOf("profile") }
    val availableLanguages by NuxLanguageManager.availableLanguages.collectAsState()
    val activeLanguage by NuxLanguageManager.currentLanguage.collectAsState()
    var isLangMenuExpanded by remember { mutableStateOf(false) }

    // Profile state
    var editUsername by remember(launcherUser?.username) { mutableStateOf(launcherUser?.username ?: "") }
    var isSavingUsername by remember { mutableStateOf(false) }
    var usernameError by remember { mutableStateOf<String?>(null) }
    var isUploadingAvatar by remember { mutableStateOf(false) }
    var selectedPreviewUri by remember { mutableStateOf<Uri?>(null) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showPremiumDialog by remember { mutableStateOf(false) }
    var premiumInitialPrompt by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && launcherUser != null) {
            selectedPreviewUri = uri
            scope.launch {
                isUploadingAvatar = true
                val uploadRes = AuthService.uploadProfileImage(context, uri)
                uploadRes.onSuccess { cloudUrl ->
                    val updateRes = AuthService.updateProfile(
                        uid = launcherUser!!.uid,
                        photoURL = cloudUrl
                    )
                    updateRes.onSuccess {
                        AccountManager.updateProfile(context, newPhotoUrl = cloudUrl)
                        Toast.makeText(context, "Profile picture updated successfully!", Toast.LENGTH_SHORT).show()
                    }.onFailure { err ->
                        Toast.makeText(context, "Failed to update profile: ${err.message}", Toast.LENGTH_LONG).show()
                    }
                }.onFailure { err ->
                    Toast.makeText(context, "Failed to upload image: ${err.message}", Toast.LENGTH_LONG).show()
                }
                isUploadingAvatar = false
            }
        }
    }

    var aiAutoAnalyze by remember(currentSettings.aiAutoAnalyze) { mutableStateOf(currentSettings.aiAutoAnalyze) }
    var aiApiKey by remember(currentSettings.aiApiKey) { mutableStateOf(currentSettings.aiApiKey) }
    var aiModel by remember(currentSettings.aiModel) { mutableStateOf(currentSettings.aiModel) }
    var isTestingAiConnection by remember { mutableStateOf(false) }
    var aiConnectionTestResult by remember { mutableStateOf<String?>(null) }
    var isAiConnectionSuccess by remember { mutableStateOf(false) }
    var showApiKeyPlaintext by remember { mutableStateOf(false) }

    fun handleSaveUsername() {
        val cleanName = editUsername.trim()
        if (cleanName.isBlank()) {
            usernameError = "Username cannot be empty"
            return
        }
        if (cleanName.length < 3 || cleanName.length > 20) {
            usernameError = "Username must be 3 - 20 characters"
            return
        }
        if (!cleanName.matches(Regex("^[a-zA-Z0-9_]+$"))) {
            usernameError = "Letters, numbers, and underscores (_) only"
            return
        }
        val user = launcherUser ?: return
        val uid = user.uid
        if (cleanName == user.username) {
            usernameError = null
            Toast.makeText(context, "Username unchanged.", Toast.LENGTH_SHORT).show()
            return
        }

        usernameError = null
        isSavingUsername = true
        scope.launch {
            val res = AuthService.updateProfile(uid = uid, newUsername = cleanName)
            res.onSuccess {
                AccountManager.updateProfile(context, newUsername = cleanName)
                Toast.makeText(context, "Username successfully changed to $cleanName!", Toast.LENGTH_SHORT).show()
            }.onFailure { err ->
                usernameError = err.message ?: "Failed to update username"
                Toast.makeText(context, err.message ?: "Failed to update username", Toast.LENGTH_LONG).show()
            }
            isSavingUsername = false
        }
    }

    // Game settings local state
    var ramMb by remember(currentSettings.ramMb) { mutableIntStateOf(currentSettings.ramMb) }
    var initialHeapMb by remember(currentSettings.initialHeapMb) { mutableIntStateOf(currentSettings.initialHeapMb) }
    var customJvmArgs by remember(currentSettings.customJvmArgs) { mutableStateOf(currentSettings.customJvmArgs) }
    var defaultRuntime by remember(currentSettings.defaultJavaRuntime) { mutableStateOf(currentSettings.defaultJavaRuntime) }

    var mouseMode by remember(currentSettings.mouseControlMode) { mutableStateOf(currentSettings.mouseControlMode) }
    var cursorSensitivity by remember(currentSettings.cursorSensitivity) { mutableIntStateOf(currentSettings.cursorSensitivity) }
    var captureSensitivity by remember(currentSettings.captureSensitivity) { mutableIntStateOf(currentSettings.captureSensitivity) }
    var mouseSizeDp by remember(currentSettings.mouseSizeDp) { mutableIntStateOf(currentSettings.mouseSizeDp) }
    var physicalMouseMode by remember(currentSettings.physicalMouseMode) { mutableStateOf(currentSettings.physicalMouseMode) }

    var resolutionRatio by remember(currentSettings.resolutionRatio) { mutableIntStateOf(currentSettings.resolutionRatio) }
    var autoOptimizeMC by remember(currentSettings.autoOptimizeMinecraft) { mutableStateOf(currentSettings.autoOptimizeMinecraft) }
    var sustainedPerf by remember(currentSettings.sustainedPerformanceMode) { mutableStateOf(currentSettings.sustainedPerformanceMode) }
    var selectedRenderer by remember(currentSettings.selectedRenderer) { mutableStateOf(currentSettings.selectedRenderer) }
    var vulkanDriver by remember(currentSettings.vulkanDriver) { mutableStateOf(currentSettings.vulkanDriver) }
    var graphicsApi by remember(currentSettings.graphicsApi) { mutableStateOf(currentSettings.graphicsApi) }
    var zinkPreferSystemDriver by remember(currentSettings.zinkPreferSystemDriver) { mutableStateOf(currentSettings.zinkPreferSystemDriver) }
    var vsyncInZink by remember(currentSettings.vsyncInZink) { mutableStateOf(currentSettings.vsyncInZink) }
    var showRendererDialog by remember { mutableStateOf(false) }
    var showRendererConfigDialog by remember { mutableStateOf(false) }
    var selectedConfigRenderer by remember { mutableStateOf<NuxRendererInfo?>(null) }
    var showAdrenoWarningDialog by remember { mutableStateOf(false) }

    val totalRamMb = remember { SettingsManager.getTotalDeviceMemoryMb(context) }
    val maxAllocatableRam = remember(totalRamMb) { (totalRamMb * 0.85f).toInt().coerceAtLeast(1024) }
    val cpuCores = remember { SettingsManager.getCpuCoreCount() }

    fun commitSettings() {
        val updated = currentSettings.copy(
            ramMb = ramMb,
            initialHeapMb = initialHeapMb,
            customJvmArgs = customJvmArgs,
            defaultJavaRuntime = defaultRuntime,
            mouseControlMode = mouseMode,
            cursorSensitivity = cursorSensitivity,
            captureSensitivity = captureSensitivity,
            mouseSizeDp = mouseSizeDp,
            physicalMouseMode = physicalMouseMode,
            resolutionRatio = resolutionRatio,
            autoOptimizeMinecraft = autoOptimizeMC,
            sustainedPerformanceMode = sustainedPerf,
            selectedRenderer = selectedRenderer,
            vulkanDriver = vulkanDriver,
            graphicsApi = graphicsApi,
            zinkPreferSystemDriver = zinkPreferSystemDriver,
            vsyncInZink = vsyncInZink,
            heroAnimationEnabled = currentSettings.heroAnimationEnabled,
            heroAnimationVideoPath = currentSettings.heroAnimationVideoPath,
            heroAnimationRotation = currentSettings.heroAnimationRotation,
            aiAutoAnalyze = aiAutoAnalyze,
            aiApiKey = aiApiKey,
            aiModel = aiModel
        )
        SettingsManager.updateSettings(context, updated)
    }

    // =========================================================================
    // MASTER-DETAIL SPLIT LAYOUT (KIRI: KATEGORI PENGATURAN | KANAN: PANEL DETAIL)
    // =========================================================================
    Row(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // =====================================================================
        // PANEL KIRI: SETTINGS CATEGORY SIDEBAR (180.dp)
        // =====================================================================
        Column(
            modifier = Modifier
                .width(180.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(14.dp))
                .background(BellGlassSidebar)
                .border(1.dp, BellBorder, RoundedCornerShape(14.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "PREFERENCES",
                    color = BellTextMuted,
                    fontWeight = FontWeight.Black,
                    fontSize = 8.sp,
                    letterSpacing = 0.9.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )

                SETTINGS_SECTIONS.forEach { sec ->
                    val isSelected = activeSection == sec.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) BellPillWhite else Color.Transparent)
                            .clickable { activeSection = sec.id }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = sec.icon,
                            contentDescription = sec.title,
                            tint = if (isSelected) BellInkDark else BellTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = sec.title,
                                color = if (isSelected) BellInkDark else BellTextPrimary,
                                fontWeight = FontWeight.Black,
                                fontSize = 9.5.sp,
                                letterSpacing = 0.5.sp,
                                maxLines = 1
                            )
                            Text(
                                text = sec.subtitle,
                                color = if (isSelected) BellInkDark.copy(alpha = 0.7f) else BellTextMuted,
                                fontSize = 8.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Footer Sidebar: Hardware Telemetry & Reset
            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x14FFFFFF))
                        .border(1.dp, BellBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${totalRamMb / 1024}GB RAM",
                        color = BellTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.5.sp
                    )
                    Text(
                        text = "$cpuCores CORES",
                        color = BellTextMuted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.5.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(BellDanger.copy(alpha = 0.14f))
                        .border(1.dp, BellDanger.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .clickable {
                            SettingsManager.resetToDefaults(context)
                            val reset = SettingsManager.settings.value
                            ramMb = reset.ramMb
                            initialHeapMb = reset.initialHeapMb
                            customJvmArgs = reset.customJvmArgs
                            defaultRuntime = reset.defaultJavaRuntime
                            mouseMode = reset.mouseControlMode
                            cursorSensitivity = reset.cursorSensitivity
                            captureSensitivity = reset.captureSensitivity
                            mouseSizeDp = reset.mouseSizeDp
                            physicalMouseMode = reset.physicalMouseMode
                            resolutionRatio = reset.resolutionRatio
                            autoOptimizeMC = reset.autoOptimizeMinecraft
                            sustainedPerf = reset.sustainedPerformanceMode
                            aiAutoAnalyze = reset.aiAutoAnalyze
                            aiApiKey = reset.aiApiKey
                            aiModel = reset.aiModel
                            Toast.makeText(context, "Settings reset to defaults!", Toast.LENGTH_SHORT).show()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "RESET ALL",
                        color = BellDanger,
                        fontWeight = FontWeight.Black,
                        fontSize = 9.sp,
                        letterSpacing = 0.6.sp
                    )
                }
            }
        }

        // =====================================================================
        // PANEL KANAN: DETAIL VIEW
        // =====================================================================
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(14.dp))
                .background(BellGlassCard)
                .border(1.dp, BellBorder, RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            when (activeSection) {
                // -------------------------------------------------------------
                // SECTION 1: PROFILE & ACCOUNT
                // -------------------------------------------------------------
                "profile" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BellSectionHeader(
                            title = "PROFILE & ACCOUNT IDENTITY",
                            subtitle = "Manage your Bell Launcher profile, display name, language, and session"
                        )

                        // Kartu Avatar + Ganti Username dalam satu grid rapi
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Kolom Avatar
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(BellGlassInput)
                                    .border(1.dp, BellBorder, RoundedCornerShape(10.dp))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .border(1.dp, BellBorderActive, RoundedCornerShape(10.dp))
                                    ) {
                                        NuxNetworkImage(
                                            model = selectedPreviewUri ?: launcherUser?.photoURL?.takeIf { it.isNotBlank() },
                                            contentDescription = "Profile Avatar",
                                            modifier = Modifier.fillMaxSize(),
                                            fallbackInitials = launcherUser?.username ?: "BELL",
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        if (isUploadingAvatar) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(10.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(18.dp),
                                                    color = BellTextPrimary,
                                                    strokeWidth = 2.dp
                                                )
                                            }
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = launcherUser?.username ?: "Bell User",
                                            color = BellTextPrimary,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = com.israadev.nuxlauncher.core.utils.PrivacyMasker.maskEmail(launcherUser?.email),
                                            color = BellTextSecondary,
                                            fontSize = 9.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                BellWhiteButton(
                                    text = if (isUploadingAvatar) "UPLOADING..." else "CHANGE AVATAR",
                                    icon = Icons.Outlined.PhotoCamera,
                                    enabled = !isUploadingAvatar,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { photoPickerLauncher.launch("image/*") }
                                )
                            }

                            // Kolom Ubah Username
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(BellGlassInput)
                                    .border(1.dp, BellBorder, RoundedCornerShape(10.dp))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val titleText = tr("settings.card_change_username").let {
                                    if (it == "settings.card_change_username") "CHANGE USERNAME" else it.uppercase()
                                }
                                Text(
                                    text = titleText,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp,
                                    letterSpacing = 0.6.sp,
                                    color = BellTextPrimary
                                )
                                NuxTextField(
                                    value = editUsername,
                                    onValueChange = {
                                        editUsername = it
                                        usernameError = null
                                    },
                                    placeholder = "e.g. BellPlayer99"
                                )
                                if (usernameError != null) {
                                    Text(
                                        text = usernameError!!,
                                        color = BellDanger,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                val saveBtnText = tr("settings.btn_save_username").let {
                                    if (it == "settings.btn_save_username") "SAVE USERNAME" else it.uppercase()
                                }
                                BellWhiteButton(
                                    text = if (isSavingUsername) "SAVING..." else saveBtnText,
                                    icon = Icons.Outlined.Check,
                                    enabled = !isSavingUsername,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { handleSaveUsername() }
                                )
                            }
                        }

                        // Bahasa & Status Lisensi
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Pemilih Bahasa
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(BellGlassInput)
                                    .border(1.dp, BellBorder, RoundedCornerShape(10.dp))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "LAUNCHER LANGUAGE",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.6.sp,
                                    color = BellTextPrimary
                                )
                                Box {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(34.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(BellGlassCard)
                                            .border(1.dp, BellBorderActive, RoundedCornerShape(8.dp))
                                            .clickable { isLangMenuExpanded = true }
                                            .padding(horizontal = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(text = activeLanguage.flag, fontSize = 13.sp)
                                            Text(
                                                text = activeLanguage.name,
                                                color = BellTextPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.5.sp
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = BellTextSecondary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = isLangMenuExpanded,
                                        onDismissRequest = { isLangMenuExpanded = false },
                                        shape = RoundedCornerShape(12.dp),
                                        containerColor = BellGlassElevated,
                                        border = BorderStroke(1.dp, BellBorder)
                                    ) {
                                        availableLanguages.forEach { lang ->
                                            val isSelected = lang.code.equals(activeLanguage.code, ignoreCase = true)
                                            DropdownMenuItem(
                                                text = {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            Text(text = lang.flag, fontSize = 14.sp)
                                                            Text(
                                                                text = lang.name,
                                                                color = if (isSelected) BellTextPrimary else BellTextSecondary,
                                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                                fontSize = 11.sp
                                                            )
                                                        }
                                                        if (isSelected) {
                                                            Icon(
                                                                imageVector = Icons.Default.Check,
                                                                contentDescription = null,
                                                                tint = BellTextPrimary,
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                        }
                                                    }
                                                },
                                                onClick = {
                                                    NuxLanguageManager.setLanguage(context, lang.code)
                                                    isLangMenuExpanded = false
                                                    Toast.makeText(context, "${lang.flag} ${lang.name}", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Status Membership
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(BellGlassInput)
                                    .border(1.dp, BellBorder, RoundedCornerShape(10.dp))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val isVip = launcherUser?.isActivated == true
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "MEMBERSHIP",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.6.sp,
                                        color = BellTextPrimary
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (isVip) BellAmber.copy(alpha = 0.2f) else Color(0x1AFFFFFF))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (isVip) "PREMIUM" else "FREE TIER",
                                            color = if (isVip) BellAmber else BellTextSecondary,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 8.sp
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isVip) Color(0x1FFBBF24) else BellAmber)
                                        .clickable {
                                            premiumInitialPrompt = if (isVip) "Your Bell Premium status is currently active!" else null
                                            showPremiumDialog = true
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_vip_crown),
                                            contentDescription = null,
                                            tint = if (isVip) BellAmber else BellInkDark,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = if (isVip) "MANAGE PREMIUM" else "UPGRADE PREMIUM",
                                            color = if (isVip) BellAmber else BellInkDark,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 9.sp,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Footer Actions: About & Sign Out
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BellGlassInput)
                                    .border(1.dp, BellBorder, RoundedCornerShape(8.dp))
                                    .clickable { showAboutDialog = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Info,
                                        contentDescription = null,
                                        tint = BellTextPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "ABOUT & LICENSES",
                                        color = BellTextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BellDanger.copy(alpha = 0.14f))
                                    .border(1.dp, BellDanger.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                    .clickable { showLogoutDialog = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.Logout,
                                        contentDescription = null,
                                        tint = BellDanger,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "SIGN OUT",
                                        color = BellDanger,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 9.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // -------------------------------------------------------------
                // SECTION 2: MEMORY & JVM ARGUMENTS
                // -------------------------------------------------------------
                "memory" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BellSectionHeader(
                            title = "MEMORY & JAVA VIRTUAL MACHINE",
                            subtitle = "Configure maximum heap allocation and custom JVM launch flags"
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(BellGlassInput)
                                .border(1.dp, BellBorder, RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tr("settings.ram_max_label"),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BellTextPrimary
                                )
                                val isHighRam = ramMb > (totalRamMb * 0.75f)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(if (isHighRam) BellDanger.copy(alpha = 0.2f) else Color(0x26FFFFFF))
                                        .border(
                                            1.dp,
                                            if (isHighRam) BellDanger.copy(alpha = 0.5f) else BellBorderActive,
                                            RoundedCornerShape(5.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "$ramMb MB (${String.format("%.1f", ramMb / 1024f)} GB)",
                                        color = if (isHighRam) BellDanger else BellTextPrimary,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }

                            Slider(
                                value = ramMb.toFloat(),
                                onValueChange = {
                                    ramMb = ((it / 128).roundToInt() * 128).coerceIn(512, maxAllocatableRam)
                                    commitSettings()
                                },
                                valueRange = 512f..maxAllocatableRam.toFloat(),
                                steps = ((maxAllocatableRam - 512) / 128) - 1,
                                colors = SliderDefaults.colors(
                                    thumbColor = BellPillWhite,
                                    activeTrackColor = BellPillWhite,
                                    inactiveTrackColor = Color(0x26FFFFFF)
                                )
                            )

                            Text(
                                text = tr("settings.ram_quick_presets"),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp,
                                color = BellTextMuted
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(1024, 1536, 2048, 3072, 4096, 6144).forEach { preset ->
                                    if (preset <= maxAllocatableRam) {
                                        val isSelected = ramMb == preset
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(28.dp)
                                                .clip(RoundedCornerShape(7.dp))
                                                .background(if (isSelected) BellPillWhite else BellGlassCard)
                                                .border(
                                                    1.dp,
                                                    if (isSelected) BellPillWhite else BellBorder,
                                                    RoundedCornerShape(7.dp)
                                                )
                                                .clickable {
                                                    ramMb = preset
                                                    commitSettings()
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (preset >= 1024) "${preset / 1024}GB" else "${preset}MB",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (isSelected) BellInkDark else BellTextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(BellGlassInput)
                                .border(1.dp, BellBorder, RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = tr("settings.jvm_args_label"),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = BellTextPrimary
                            )
                            NuxTextField(
                                value = customJvmArgs,
                                onValueChange = {
                                    customJvmArgs = it
                                    commitSettings()
                                },
                                placeholder = tr("settings.jvm_args_placeholder")
                            )
                        }
                    }
                }

                // -------------------------------------------------------------
                // SECTION 3: GRAPHICS & RENDERER
                // -------------------------------------------------------------
                "graphics" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BellSectionHeader(
                            title = "GRAPHICS BACKEND & RENDERER",
                            subtitle = "Select OpenGL/Vulkan translator, render scale, and driver optimizations"
                        )

                        val activeRendererInfo = remember(selectedRenderer) {
                            NuxRendererRegistry.findRendererById(selectedRenderer)
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(BellGlassInput)
                                .border(1.dp, BellBorder, RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = activeRendererInfo.displayName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = BellTextPrimary
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(BellPillWhite)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = activeRendererInfo.badge,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Black,
                                            color = BellInkDark
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (activeRendererInfo.isConfigurable) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(RoundedCornerShape(7.dp))
                                                .background(BellGlassCard)
                                                .border(1.dp, BellBorderActive, RoundedCornerShape(7.dp))
                                                .clickable {
                                                    selectedConfigRenderer = activeRendererInfo
                                                    showRendererConfigDialog = true
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Settings,
                                                contentDescription = "Configure Renderer",
                                                tint = BellTextPrimary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .height(28.dp)
                                            .clip(RoundedCornerShape(7.dp))
                                            .background(BellPillWhite)
                                            .clickable {
                                                NuxRendererPluginManager.scanPlugins(context)
                                                showRendererDialog = true
                                            }
                                            .padding(horizontal = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "SWITCH",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = BellInkDark
                                        )
                                    }
                                }
                            }
                            Text(
                                text = activeRendererInfo.summary,
                                fontSize = 9.5.sp,
                                color = BellTextSecondary,
                                lineHeight = 13.sp
                            )
                        }

                        // Resolution Scale + Vulkan Driver
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(BellGlassInput)
                                    .border(1.dp, BellBorder, RoundedCornerShape(10.dp))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "RENDER SCALE",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = BellTextPrimary
                                    )
                                    Text(
                                        text = "$resolutionRatio%",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = BellTextPrimary
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf(50, 75, 100, 125).forEach { ratio ->
                                        val isSelected = resolutionRatio == ratio
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(26.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isSelected) BellPillWhite else BellGlassCard)
                                                .border(1.dp, if (isSelected) BellPillWhite else BellBorder, RoundedCornerShape(6.dp))
                                                .clickable {
                                                    resolutionRatio = ratio
                                                    commitSettings()
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "$ratio%",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) BellInkDark else BellTextSecondary
                                            )
                                        }
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(BellGlassInput)
                                    .border(1.dp, BellBorder, RoundedCornerShape(10.dp))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "VULKAN DRIVER",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = BellTextPrimary
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf("auto" to "AUTO", "system" to "SYSTEM", "turnip" to "TURNIP").forEach { (id, label) ->
                                        val isSelected = vulkanDriver == id
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(26.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isSelected) BellPillWhite else BellGlassCard)
                                                .border(1.dp, if (isSelected) BellPillWhite else BellBorder, RoundedCornerShape(6.dp))
                                                .clickable {
                                                    vulkanDriver = id
                                                    commitSettings()
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) BellInkDark else BellTextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Graphics API 26.2+
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(BellGlassInput)
                                .border(1.dp, BellBorder, RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "MODERN GRAPHICS API (26.2+)",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Black,
                                color = BellTextPrimary
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("DEFAULT" to "DEFAULT", "OPENGL" to "OPENGL", "VULKAN" to "VULKAN").forEach { (id, label) ->
                                    val isSelected = graphicsApi == id
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(28.dp)
                                            .clip(RoundedCornerShape(7.dp))
                                            .background(if (isSelected) BellPillWhite else BellGlassCard)
                                            .border(1.dp, if (isSelected) BellPillWhite else BellBorder, RoundedCornerShape(7.dp))
                                            .clickable {
                                                graphicsApi = id
                                                commitSettings()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) BellInkDark else BellTextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        // Toggles: Auto-Optimize, System Vulkan, VSync
                        BellToggleRow(
                            title = "Auto-Optimize options.txt",
                            subtitle = "Automatically optimize visual settings (Clouds, Shadows) for peak FPS",
                            checked = autoOptimizeMC,
                            onCheckedChange = {
                                autoOptimizeMC = it
                                commitSettings()
                            }
                        )

                        BellToggleRow(
                            title = tr("settings.vulkan_system_label"),
                            subtitle = "Force device system Vulkan driver instead of Turnip for Zink renderer",
                            checked = zinkPreferSystemDriver,
                            onCheckedChange = { checked ->
                                if (checked && NuxVersionUtils.isAdrenoGPU()) {
                                    showAdrenoWarningDialog = true
                                } else {
                                    zinkPreferSystemDriver = checked
                                    commitSettings()
                                }
                            }
                        )

                        BellToggleRow(
                            title = tr("settings.vsync_zink_label"),
                            subtitle = "Enable vertical synchronization (V-Sync) for Kopper Zink renderer",
                            checked = vsyncInZink,
                            onCheckedChange = {
                                vsyncInZink = it
                                commitSettings()
                            }
                        )
                    }
                }

                // -------------------------------------------------------------
                // SECTION 4: CONTROLS & VIRTUAL GUI
                // -------------------------------------------------------------
                "controls" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BellSectionHeader(
                            title = "TOUCH, MOUSE & VIRTUAL CONTROLS",
                            subtitle = "Customize on-screen buttons, cursor sensitivity, and input behavior"
                        )

                        // Virtual GUI Editor Card
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(BellGlassInput)
                                .border(1.dp, BellBorder, RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tr("settings.virtual_controls_title"),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp,
                                    color = BellTextPrimary
                                )
                                Text(
                                    text = tr("settings.virtual_controls_desc"),
                                    fontSize = 9.5.sp,
                                    color = BellTextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            BellWhiteButton(
                                text = "OPEN EDITOR",
                                icon = Icons.Outlined.Edit,
                                onClick = onOpenGuiEditor
                            )
                        }

                        // Mouse Mode Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val isSlide = mouseMode == "SLIDE"
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSlide) Color(0x26FFFFFF) else BellGlassInput)
                                    .border(1.dp, if (isSlide) BellPillWhite else BellBorder, RoundedCornerShape(10.dp))
                                    .clickable {
                                        mouseMode = "SLIDE"
                                        commitSettings()
                                    }
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Outlined.TouchApp,
                                            contentDescription = null,
                                            tint = BellTextPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = tr("settings.mouse_mode_slide_title"),
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Black,
                                            color = BellTextPrimary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = tr("settings.mouse_mode_slide_desc"),
                                        fontSize = 9.sp,
                                        color = BellTextSecondary,
                                        lineHeight = 12.sp
                                    )
                                }
                            }

                            val isClick = mouseMode == "CLICK"
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isClick) Color(0x26FFFFFF) else BellGlassInput)
                                    .border(1.dp, if (isClick) BellPillWhite else BellBorder, RoundedCornerShape(10.dp))
                                    .clickable {
                                        mouseMode = "CLICK"
                                        commitSettings()
                                    }
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Outlined.NearMe,
                                            contentDescription = null,
                                            tint = BellTextPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = tr("settings.mouse_mode_click_title"),
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Black,
                                            color = BellTextPrimary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = tr("settings.mouse_mode_click_desc"),
                                        fontSize = 9.sp,
                                        color = BellTextSecondary,
                                        lineHeight = 12.sp
                                    )
                                }
                            }
                        }

                        // Sensitivities & Mouse Size
                        BellSliderCard(
                            label = tr("settings.camera_sensitivity_label"),
                            valueText = "$captureSensitivity%",
                            value = captureSensitivity.toFloat(),
                            range = 50f..300f,
                            steps = 25,
                            onValueChange = {
                                captureSensitivity = it.roundToInt()
                                commitSettings()
                            }
                        )

                        BellSliderCard(
                            label = tr("settings.mouse_sensitivity_label"),
                            valueText = "$cursorSensitivity%",
                            value = cursorSensitivity.toFloat(),
                            range = 50f..300f,
                            steps = 25,
                            onValueChange = {
                                cursorSensitivity = it.roundToInt()
                                commitSettings()
                            }
                        )

                        BellSliderCard(
                            label = tr("settings.mouse_size_label"),
                            valueText = "${mouseSizeDp}dp",
                            value = mouseSizeDp.toFloat(),
                            range = 16f..48f,
                            steps = 16,
                            onValueChange = {
                                mouseSizeDp = it.roundToInt()
                                commitSettings()
                            }
                        )

                        BellToggleRow(
                            title = "Physical Mouse Mode (Auto-Hide)",
                            subtitle = "Automatically hide virtual controls when an external mouse is connected",
                            checked = physicalMouseMode,
                            onCheckedChange = {
                                physicalMouseMode = it
                                commitSettings()
                            }
                        )
                    }
                }

                // -------------------------------------------------------------
                // SECTION 5: AI CRASH DIAGNOSTICS
                // -------------------------------------------------------------
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BellSectionHeader(
                            title = "AI CRASH DIAGNOSTICS & API",
                            subtitle = "Automatic crash log inspection and OpenRouter API configuration"
                        )

                        BellToggleRow(
                            title = tr("settings.ai_auto_analyze_label"),
                            subtitle = if (aiAutoAnalyze) "Active: AI analyzes crash logs automatically when game terminates unexpectedly."
                            else "Disabled: AI analysis is triggered manually in crash dialogs to conserve quota.",
                            checked = aiAutoAnalyze,
                            onCheckedChange = {
                                aiAutoAnalyze = it
                                commitSettings()
                            }
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(BellGlassInput)
                                .border(1.dp, BellBorder, RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "OPENROUTER API KEY (OPTIONAL)",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.6.sp,
                                color = BellTextPrimary
                            )
                            NuxTextField(
                                value = aiApiKey,
                                onValueChange = {
                                    aiApiKey = it
                                    commitSettings()
                                },
                                placeholder = "Leave blank = Default launcher free quota",
                                visualTransformation = if (showApiKeyPlaintext) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                trailingContent = {
                                    Icon(
                                        imageVector = if (showApiKeyPlaintext) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = null,
                                        tint = BellTextSecondary,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { showApiKeyPlaintext = !showApiKeyPlaintext }
                                    )
                                }
                            )
                            Text(
                                text = "Supports OpenRouter 'sk-or-v1-...' format. Leave empty to use built-in free quota.",
                                fontSize = 8.5.sp,
                                color = BellTextMuted
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(BellGlassCard)
                                        .border(1.dp, BellBorder, RoundedCornerShape(8.dp))
                                        .clickable(enabled = !isTestingAiConnection) {
                                            isTestingAiConnection = true
                                            aiConnectionTestResult = null
                                            scope.launch {
                                                val res = AICrashAnalyzer.testConnection(aiApiKey, "")
                                                isTestingAiConnection = false
                                                res.onSuccess { msg ->
                                                    isAiConnectionSuccess = true
                                                    aiConnectionTestResult = msg
                                                }.onFailure { err ->
                                                    isAiConnectionSuccess = false
                                                    aiConnectionTestResult = err.message ?: "Connection failed"
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (isTestingAiConnection) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(12.dp),
                                                color = BellTextPrimary,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Outlined.NetworkCheck,
                                                contentDescription = null,
                                                tint = BellTextPrimary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        Text(
                                            text = if (isTestingAiConnection) "TESTING..." else "TEST CONNECTION",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            color = BellTextPrimary
                                        )
                                    }
                                }

                                BellWhiteButton(
                                    text = "SAVE KEY",
                                    icon = Icons.Outlined.Save,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        commitSettings()
                                        Toast.makeText(context, "AI Analytics settings saved!", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }

                            if (aiConnectionTestResult != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isAiConnectionSuccess) Color(0x26FFFFFF)
                                            else BellDanger.copy(alpha = 0.15f)
                                        )
                                        .border(
                                            1.dp,
                                            if (isAiConnectionSuccess) BellBorderActive
                                            else BellDanger.copy(alpha = 0.4f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isAiConnectionSuccess) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
                                        contentDescription = null,
                                        tint = if (isAiConnectionSuccess) BellTextPrimary else BellDanger,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = aiConnectionTestResult!!,
                                        fontSize = 9.sp,
                                        color = if (isAiConnectionSuccess) BellTextPrimary else BellDanger
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // =========================================================================
    // DIALOGS (MONOCHROME BELL THEME)
    // =========================================================================
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    text = "Sign Out of Account?",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = BellTextPrimary
                )
            },
            text = {
                Text(
                    text = "You will be signed out of ${launcherUser?.username ?: ""}. You can log back in at any time.",
                    color = BellTextSecondary,
                    fontSize = 11.sp
                )
            },
            confirmButton = {
                Box(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(BellDanger)
                        .clickable {
                            showLogoutDialog = false
                            AccountManager.logout(context)
                            onNavigateBack()
                            Toast.makeText(context, "Signed out successfully", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("YES, SIGN OUT", fontWeight = FontWeight.Black, fontSize = 10.sp, color = BellInkDark)
                }
            },
            dismissButton = {
                Box(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(BellGlassInput)
                        .clickable { showLogoutDialog = false }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("CANCEL", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = BellTextSecondary)
                }
            },
            containerColor = BellGlassElevated,
            shape = RoundedCornerShape(14.dp)
        )
    }

    if (showRendererDialog) {
        LaunchedEffect(Unit) {
            NuxRendererPluginManager.scanPlugins(context)
        }
        NuxDialog(
            onDismissRequest = { showRendererDialog = false },
            modifier = Modifier.fillMaxWidth(0.75f),
            fillMaxHeight = true
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SELECT GRAPHICS RENDERER",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = BellTextPrimary
                        )
                        Text(
                            text = "Match graphics backend with Minecraft version & device GPU",
                            fontSize = 9.5.sp,
                            color = BellTextSecondary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(BellGlassInput)
                            .clickable { showRendererDialog = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = BellTextPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(NuxRendererRegistry.availableRenderers) { rendererItem ->
                        val isSelected = selectedRenderer.equals(rendererItem.id, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0x26FFFFFF) else BellGlassInput)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) BellPillWhite else BellBorder,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    selectedRenderer = rendererItem.id
                                    commitSettings()
                                    showRendererDialog = false
                                    Toast.makeText(context, "Renderer changed to: ${rendererItem.displayName}", Toast.LENGTH_SHORT).show()
                                }
                                .padding(10.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) BellPillWhite else Color.Transparent)
                                                .border(1.5.dp, if (isSelected) BellPillWhite else BellTextMuted, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = rendererItem.displayName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp,
                                            color = BellTextPrimary
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (rendererItem.isConfigurable) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(BellGlassCard)
                                                    .border(1.dp, BellBorderActive, RoundedCornerShape(6.dp))
                                                    .clickable {
                                                        selectedConfigRenderer = rendererItem
                                                        showRendererDialog = false
                                                        showRendererConfigDialog = true
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Settings,
                                                    contentDescription = null,
                                                    tint = BellTextPrimary,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(5.dp))
                                                .background(if (isSelected) BellPillWhite else BellGlassCard)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = rendererItem.badge,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (isSelected) BellInkDark else BellTextSecondary
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = rendererItem.summary,
                                    fontSize = 9.5.sp,
                                    color = BellTextSecondary,
                                    lineHeight = 12.sp,
                                    modifier = Modifier.padding(start = 22.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Compatibility: ${rendererItem.compatibility}",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BellTextMuted,
                                    modifier = Modifier.padding(start = 22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdrenoWarningDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showAdrenoWarningDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                NuxCard(
                    modifier = Modifier
                        .width(420.dp)
                        .wrapContentHeight(),
                    backgroundColor = BellGlassElevated,
                    borderColor = BellBorder,
                    cornerRadius = NuxSizes.CornerRadiusLarge,
                    fillMaxHeight = false
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(R.drawable.ic_warning_triangle),
                                contentDescription = null,
                                tint = BellAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ADRENO DRIVER WARNING",
                                color = BellAmber,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }
                        Text(
                            text = "Launcher detected an Adreno GPU. Enabling this option may cause Zink renderer to malfunction or crash. Are you sure you want to proceed?",
                            color = BellTextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BellGlassInput)
                                    .clickable {
                                        zinkPreferSystemDriver = false
                                        commitSettings()
                                        showAdrenoWarningDialog = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("CANCEL", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = BellTextPrimary)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BellAmber)
                                    .clickable {
                                        zinkPreferSystemDriver = true
                                        commitSettings()
                                        showAdrenoWarningDialog = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("ENABLE ANYWAY", fontWeight = FontWeight.Black, fontSize = 10.sp, color = BellInkDark)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRendererConfigDialog && selectedConfigRenderer != null) {
        NuxRendererV2ConfigDialog(
            rendererInfo = selectedConfigRenderer,
            onDismiss = { showRendererConfigDialog = false }
        )
    }

    if (showAboutDialog) {
        NuxAboutDialog(onDismissRequest = { showAboutDialog = false })
    }

    if (showPremiumDialog) {
        NuxPremiumDialog(
            initialPrompt = premiumInitialPrompt,
            onDismissRequest = {
                showPremiumDialog = false
                premiumInitialPrompt = null
            }
        )
    }
}

@Composable
private fun BellSectionHeader(
    title: String,
    subtitle: String
) {
    Column(modifier = Modifier.padding(bottom = 2.dp)) {
        Text(
            text = title,
            color = BellTextPrimary,
            fontWeight = FontWeight.Black,
            fontSize = 13.sp,
            letterSpacing = 0.7.sp
        )
        Text(
            text = subtitle,
            color = BellTextSecondary,
            fontSize = 9.5.sp
        )
    }
}

@Composable
private fun BellWhiteButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (enabled) BellPillWhite else Color(0x33FFFFFF))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) BellInkDark else BellTextMuted,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = text,
                color = if (enabled) BellInkDark else BellTextMuted,
                fontWeight = FontWeight.Black,
                fontSize = 9.sp,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
private fun BellToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(BellGlassInput)
            .border(1.dp, BellBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = BellTextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 9.sp,
                color = BellTextSecondary,
                lineHeight = 12.sp
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = BellInkDark,
                checkedTrackColor = BellPillWhite,
                uncheckedThumbColor = BellTextSecondary,
                uncheckedTrackColor = Color(0x26FFFFFF)
            )
        )
    }
}

@Composable
private fun BellSliderCard(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(BellGlassInput)
            .border(1.dp, BellBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = BellTextPrimary
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0x26FFFFFF))
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text(
                    text = valueText,
                    color = BellTextPrimary,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = BellPillWhite,
                activeTrackColor = BellPillWhite,
                inactiveTrackColor = Color(0x26FFFFFF)
            )
        )
    }
}
