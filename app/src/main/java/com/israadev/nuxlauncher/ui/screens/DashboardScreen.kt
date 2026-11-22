package com.israadev.nuxlauncher.ui.screens

import android.media.MediaMetadataRetriever
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.RotateRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.israadev.nuxlauncher.R
import com.israadev.nuxlauncher.core.account.AccountManager
import com.israadev.nuxlauncher.core.announcement.AnnouncementManager
import com.israadev.nuxlauncher.core.announcement.NuxAnnouncement
import com.israadev.nuxlauncher.core.crash.CrashManager
import com.israadev.nuxlauncher.core.download.MinecraftDownloader
import com.israadev.nuxlauncher.core.instance.InstanceManager
import com.israadev.nuxlauncher.core.lang.tr
import com.israadev.nuxlauncher.core.lang.trStr
import com.israadev.nuxlauncher.core.launch.GameLauncher
import com.israadev.nuxlauncher.core.mods.NuxAddonImportManager
import com.israadev.nuxlauncher.core.renderer.NuxRendererInfo
import com.israadev.nuxlauncher.core.renderer.NuxRendererRegistry
import com.israadev.nuxlauncher.core.runtime.JavaRuntimeManager
import com.israadev.nuxlauncher.core.settings.SettingsManager
import com.israadev.nuxlauncher.core.social.NuxSocialManager
import com.israadev.nuxlauncher.core.update.AndroidUpdateInfo
import com.israadev.nuxlauncher.core.update.UpdateManager
import com.israadev.nuxlauncher.ui.components.*
import com.israadev.nuxlauncher.ui.dialogs.NuxAboutDialog
import com.israadev.nuxlauncher.ui.dialogs.NuxAddonImportDialog
import com.israadev.nuxlauncher.ui.dialogs.NuxAnnouncementDialog
import com.israadev.nuxlauncher.ui.dialogs.NuxCrashDialog
import com.israadev.nuxlauncher.ui.dialogs.NuxDeleteInstanceDialog
import com.israadev.nuxlauncher.ui.dialogs.NuxDownloadProgressDialog
import com.israadev.nuxlauncher.ui.dialogs.NuxEditInstanceDialog
import com.israadev.nuxlauncher.ui.dialogs.NuxPremiumDialog
import com.israadev.nuxlauncher.ui.dialogs.NuxRendererWarningDialog
import com.israadev.nuxlauncher.ui.dialogs.NuxUpdateDialog
import com.israadev.nuxlauncher.ui.dialogs.NuxAddInstanceDialog
import com.israadev.nuxlauncher.ui.theme.NuxColors
import com.israadev.nuxlauncher.ui.theme.resp

@Composable
fun DashboardScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentTab by remember { mutableStateOf("home") }

    val activeCrash by CrashManager.activeCrash.collectAsState()

    val instances by InstanceManager.instances.collectAsState()
    val selectedInstance by InstanceManager.selectedInstance.collectAsState()
    val currentAccount by AccountManager.currentAccount.collectAsState()
    val launcherUser by AccountManager.launcherUser.collectAsState()
    val launcherSettings by SettingsManager.settings.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditInstanceDialog by remember { mutableStateOf(false) }
    var instanceToEdit by remember { mutableStateOf<com.israadev.nuxlauncher.core.models.Instance?>(null) }
    var instanceToDelete by remember { mutableStateOf<com.israadev.nuxlauncher.core.models.Instance?>(null) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateDialogInfo by remember { mutableStateOf<AndroidUpdateInfo?>(null) }
    var announcementDialogInfo by remember { mutableStateOf<NuxAnnouncement?>(null) }
    var pendingAnnouncement by remember { mutableStateOf<NuxAnnouncement?>(null) }
    var isUpdateCheckDone by remember { mutableStateOf(false) }
    var pendingLaunchInstance by remember { mutableStateOf<com.israadev.nuxlauncher.core.models.Instance?>(null) }
    var unsupportedRendererInfo by remember { mutableStateOf<NuxRendererInfo?>(null) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showPremiumDialog by remember { mutableStateOf(false) }
    var premiumInitialPrompt by remember { mutableStateOf<String?>(null) }
    val pendingImport by NuxAddonImportManager.pendingImport.collectAsState()

    val handleRequestCreateInstance = {
        if (instances.size >= 3 && launcherUser?.isActivated != true) {
            premiumInitialPrompt = "Free accounts are limited to a maximum of 3 instances. Upgrade to Bell Premium to create unlimited instances!"
            showPremiumDialog = true
        } else {
            showAddDialog = true
        }
    }

    // Auto check update and announcement on launch
    LaunchedEffect(Unit) {
        launch {
            try {
                NuxSocialManager.startSync()
            } catch (_: Exception) {}
        }

        launch {
            try {
                val ann = AnnouncementManager.fetchAnnouncement()
                if (ann != null && !AnnouncementManager.isDismissed(context, ann.id)) {
                    pendingAnnouncement = ann
                }
            } catch (e: Exception) {
                // Ignore announcement fetch failures
            }
        }

        try {
            val result = UpdateManager.checkForUpdate(context)
            result.onSuccess { info ->
                if (info.isUpdateAvailable) {
                    updateDialogInfo = info
                }
            }
        } catch (e: Exception) {
            // Ignore update check failures
        } finally {
            isUpdateCheckDone = true
        }
    }

    LaunchedEffect(isUpdateCheckDone, updateDialogInfo, pendingAnnouncement) {
        if (isUpdateCheckDone && updateDialogInfo == null && pendingAnnouncement != null) {
            announcementDialogInfo = pendingAnnouncement
            pendingAnnouncement = null
        }
    }

    // Download progress state
    var isDownloading by remember { mutableStateOf(false) }
    var downloadTargetName by remember { mutableStateOf("") }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadMessage by remember { mutableStateOf("") }

    val downloader = remember { MinecraftDownloader(context) }

    var isVersionDropdownExpanded by remember { mutableStateOf(false) }
    var isAccountDropdownExpanded by remember { mutableStateOf(false) }

    // Instant Background Video Animation Picker
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(context, uri)
                    val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    val durationMs = durationStr?.toLongOrNull() ?: 0L
                    retriever.release()

                    if (durationMs > 20_500L) {
                        val seconds = (durationMs / 1000f).roundToInt()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Video duration is too long (${seconds}s)! Maximum is 20 seconds.", Toast.LENGTH_LONG).show()
                        }
                        return@launch
                    }

                    val destFile = File(context.filesDir, "hero_banner_${System.currentTimeMillis()}.mp4")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }

                    context.filesDir.listFiles()?.forEach { file ->
                        if (file.name.startsWith("hero_banner") && file.absolutePath != destFile.absolutePath) {
                            file.delete()
                        }
                    }

                    val updated = launcherSettings.copy(
                        heroAnimationEnabled = true,
                        heroAnimationVideoPath = destFile.absolutePath
                    )
                    SettingsManager.updateSettings(context, updated)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Background animation updated successfully!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to process video: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    if (currentTab == "gui_editor") {
        CustomGuiEditorScreen(
            onNavigateBack = { currentTab = "settings" },
            modifier = Modifier.fillMaxSize()
        )
    } else {
        // Bell Launcher Clean Full-Bleed Slate Navy Shell (No thick green outer frame)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NuxColors.Background)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. LEFT NAVIGATION RAIL
                NuxSidebar(
                    activeTab = currentTab,
                    onTabSelected = { tabId ->
                        if (tabId == "home" || tabId == "accounts" || tabId == "settings" || tabId == "mods" || tabId == "ai" || tabId == "friends") {
                            currentTab = tabId
                        } else {
                            Toast.makeText(context, "${tabId.replaceFirstChar { it.uppercase() }} feature coming soon to mobile!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onOpenAbout = { showAboutDialog = true },
                    currentAccount = currentAccount,
                    launcherUser = launcherUser,
                    modifier = Modifier
                        .width((64.dp).resp())
                        .fillMaxHeight()
                )

                // Hairline Slate Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(NuxColors.CardBorder)
                )

                // 2. MAIN WORKSPACE STAGE
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color(0xFF090E1A))
                ) {
                    val hasValidHeroVideo = launcherSettings.heroAnimationEnabled &&
                            launcherSettings.heroAnimationVideoPath.isNotBlank() &&
                            File(launcherSettings.heroAnimationVideoPath).exists()
                    val isHomeTab = currentTab == "home"

                    // Shared Background Layer
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(if (!isHomeTab) Modifier.blur((18.dp).resp()) else Modifier)
                    ) {
                        if (hasValidHeroVideo) {
                            key(launcherSettings.heroAnimationVideoPath) {
                                HeroBannerVideoPlayer(
                                    videoPath = launcherSettings.heroAnimationVideoPath,
                                    rotationDegrees = launcherSettings.heroAnimationRotation,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.mc_hero_bg),
                                contentDescription = "Minecraft Scenery",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                                alpha = if (isHomeTab) 0.50f else 0.22f
                            )
                        }
                    }

                    // Slate Navy Overlay for Secondary Tabs
                    if (!isHomeTab) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xE00B1120))
                        )
                    }

                    if (currentTab == "accounts") {
                        AccountsScreen(
                            onNavigateBack = { currentTab = "home" },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (currentTab == "mods") {
                        ModsScreen(
                            onNavigateBack = { currentTab = "home" },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (currentTab == "ai") {
                        AIScreen(
                            onNavigateBack = { currentTab = "home" },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (currentTab == "settings") {
                        SettingsScreen(
                            onNavigateBack = { currentTab = "home" },
                            onOpenGuiEditor = { currentTab = "gui_editor" },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (currentTab == "friends") {
                        FriendsScreen(
                            onNavigateBack = { currentTab = "home" },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // --- BELL LAUNCHER MODERN SPLIT-DECK HOME VIEW ---
                        Box(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Subtle Top & Bottom Navy Vignette for Text Legibility
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height((90.dp).resp())
                                    .align(Alignment.TopCenter)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color(0xB3090E1A), Color.Transparent)
                                        )
                                    )
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height((150.dp).resp())
                                    .align(Alignment.BottomCenter)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, Color(0xE6090E1A))
                                        )
                                    )
                            )

                            // TOP-LEFT COMMAND HEADER (Brand Badge + Account Switcher + Wallpaper Tools)
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(start = (18.dp).resp(), top = (14.dp).resp(), end = (18.dp).resp()),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy((10.dp).resp())
                            ) {
                                // Bell Launcher Brand Chip
                                val brandShape = RoundedCornerShape((10.dp).resp())
                                Row(
                                    modifier = Modifier
                                        .clip(brandShape)
                                        .background(Color(0xE60F172A), brandShape)
                                        .border(1.dp, NuxColors.CardBorder, brandShape)
                                        .clickable { showAboutDialog = true }
                                        .padding(horizontal = (11.dp).resp(), vertical = (7.dp).resp()),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy((7.dp).resp())
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size((20.dp).resp())
                                            .background(NuxColors.ForestGreen, RoundedCornerShape((6.dp).resp())),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Notifications,
                                            contentDescription = "Bell Launcher",
                                            tint = Color.White,
                                            modifier = Modifier.size((13.dp).resp())
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "BELL LAUNCHER",
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = (10.5.sp).resp(),
                                            letterSpacing = (0.8.sp).resp()
                                        )
                                        Text(
                                            text = tr("dashboard.unofficial_notice"),
                                            color = NuxColors.SkyBlue,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = (7.5.sp).resp()
                                        )
                                    }
                                }

                                // Compact Account Switcher Card
                                val accounts by AccountManager.accounts.collectAsState()
                                val activeUsername = currentAccount?.username ?: launcherUser?.username ?: tr("dashboard.select_account")
                                val initialLetter = activeUsername.firstOrNull()?.uppercase() ?: "B"

                                Box {
                                    val accCardShape = RoundedCornerShape((10.dp).resp())
                                    Row(
                                        modifier = Modifier
                                            .clip(accCardShape)
                                            .background(Color(0xE60F172A), accCardShape)
                                            .border(1.dp, NuxColors.CardBorder, accCardShape)
                                            .clickable { isAccountDropdownExpanded = true }
                                            .padding(horizontal = (10.dp).resp(), vertical = (7.dp).resp()),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy((8.dp).resp())
                                    ) {
                                        val photo = launcherUser?.photoURL ?: currentAccount?.photoUrl
                                        if (!photo.isNullOrBlank()) {
                                            NuxNetworkImage(
                                                model = photo,
                                                contentDescription = "Profile",
                                                fallbackInitials = activeUsername,
                                                modifier = Modifier.size((22.dp).resp()),
                                                shape = RoundedCornerShape((6.dp).resp())
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size((22.dp).resp())
                                                    .background(NuxColors.SageGreen, RoundedCornerShape((6.dp).resp())),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = initialLetter,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = (11.sp).resp()
                                                )
                                            }
                                        }

                                        Text(
                                            text = activeUsername,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = (11.sp).resp(),
                                            maxLines = 1
                                        )

                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = tr("dashboard.switch_account"),
                                            tint = NuxColors.GrayNeutral,
                                            modifier = Modifier.size((15.dp).resp())
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = isAccountDropdownExpanded,
                                        onDismissRequest = { isAccountDropdownExpanded = false },
                                        shape = RoundedCornerShape((12.dp).resp()),
                                        containerColor = Color(0xF50F172A),
                                        shadowElevation = (16.dp).resp(),
                                        border = BorderStroke(1.dp, NuxColors.CardBorder),
                                        modifier = Modifier.widthIn(min = (210.dp).resp())
                                    ) {
                                        if (accounts.isEmpty()) {
                                            DropdownMenuItem(
                                                text = { Text(tr("dashboard.no_accounts"), color = NuxColors.GrayNeutral, fontSize = (11.sp).resp()) },
                                                onClick = { }
                                            )
                                        } else {
                                            accounts.forEach { acc ->
                                                val isSelected = acc.id == currentAccount?.id
                                                DropdownMenuItem(
                                                    text = {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = acc.username,
                                                                color = if (isSelected) NuxColors.SkyBlue else Color.White,
                                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                                fontSize = (12.sp).resp()
                                                            )
                                                            if (isSelected) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Check,
                                                                    contentDescription = "Selected",
                                                                    tint = NuxColors.MintGreen,
                                                                    modifier = Modifier.size((16.dp).resp())
                                                                )
                                                            }
                                                        }
                                                    },
                                                    onClick = {
                                                        AccountManager.selectAccount(acc)
                                                        isAccountDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }

                                        HorizontalDivider(color = NuxColors.LightGray, thickness = 1.dp)

                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = tr("dashboard.manage_accounts"),
                                                    color = NuxColors.SkyBlue,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = (12.sp).resp()
                                                )
                                            },
                                            onClick = {
                                                isAccountDropdownExpanded = false
                                                currentTab = "accounts"
                                            }
                                        )
                                    }
                                }

                                // Rotate Wallpaper Video Button
                                val toolBtnShape = RoundedCornerShape((10.dp).resp())
                                Box(
                                    modifier = Modifier
                                        .size((34.dp).resp())
                                        .clip(toolBtnShape)
                                        .background(Color(0xE60F172A), toolBtnShape)
                                        .border(1.dp, NuxColors.CardBorder, toolBtnShape)
                                        .clickable {
                                            val nextRotation = (launcherSettings.heroAnimationRotation + 90) % 360
                                            SettingsManager.updateSettings(
                                                context,
                                                launcherSettings.copy(heroAnimationRotation = nextRotation)
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.RotateRight,
                                        contentDescription = tr("dashboard.rotate_video"),
                                        tint = Color.White,
                                        modifier = Modifier.size((16.dp).resp())
                                    )
                                }

                                // Change Wallpaper Video Button
                                Box(
                                    modifier = Modifier
                                        .size((34.dp).resp())
                                        .clip(toolBtnShape)
                                        .background(Color(0xE60F172A), toolBtnShape)
                                        .border(1.dp, NuxColors.CardBorder, toolBtnShape)
                                        .clickable {
                                            videoPickerLauncher.launch("video/*")
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Image,
                                        contentDescription = tr("dashboard.change_video"),
                                        tint = Color.White,
                                        modifier = Modifier.size((16.dp).resp())
                                    )
                                }
                            }

                            // BOTTOM SPLIT-DECK LAUNCH AREA (Left Instance Card + Right Standalone Cobalt Play Button)
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .padding(horizontal = (18.dp).resp(), vertical = (16.dp).resp()),
                                horizontalArrangement = Arrangement.spacedBy((12.dp).resp()),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                // LEFT DECK: Instance Selector Card (Replaces NUX Capsule & Numbered Box)
                                val instanceCardShape = RoundedCornerShape((12.dp).resp())
                                Box(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(instanceCardShape)
                                            .background(Color(0xEB0F172A), instanceCardShape)
                                            .border(1.dp, NuxColors.CardBorder, instanceCardShape)
                                            .clickable { isVersionDropdownExpanded = true }
                                            .padding(horizontal = (14.dp).resp(), vertical = (11.dp).resp()),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Vertical Cobalt Accent Bar
                                        Box(
                                            modifier = Modifier
                                                .width((4.dp).resp())
                                                .height((36.dp).resp())
                                                .background(NuxColors.ForestGreen, CircleShape)
                                        )

                                        Spacer(modifier = Modifier.width((12.dp).resp()))

                                        Column(
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy((6.dp).resp())
                                            ) {
                                                Text(
                                                    text = selectedInstance?.name ?: tr("dashboard.select_instance"),
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = (15.sp).resp(),
                                                    maxLines = 1
                                                )
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardArrowDown,
                                                    contentDescription = tr("dashboard.select_version"),
                                                    tint = NuxColors.SkyBlue,
                                                    modifier = Modifier.size((18.dp).resp())
                                                )
                                            }

                                            Spacer(modifier = Modifier.height((5.dp).resp()))

                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy((6.dp).resp()),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (selectedInstance != null) {
                                                    val inst = selectedInstance!!
                                                    // Loader Badge (Cobalt Blue with Crisp White Text)
                                                    Box(
                                                        modifier = Modifier
                                                            .background(NuxColors.ForestGreen, RoundedCornerShape((5.dp).resp()))
                                                            .padding(horizontal = (7.dp).resp(), vertical = (2.dp).resp())
                                                    ) {
                                                        Text(
                                                            text = inst.loader.replaceFirstChar { it.uppercase() },
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = (9.5.sp).resp()
                                                        )
                                                    }

                                                    // Version Badge
                                                    Box(
                                                        modifier = Modifier
                                                            .background(Color(0xFF1E293B), RoundedCornerShape((5.dp).resp()))
                                                            .border(1.dp, NuxColors.CardBorder, RoundedCornerShape((5.dp).resp()))
                                                            .padding(horizontal = (7.dp).resp(), vertical = (2.dp).resp())
                                                    ) {
                                                        Text(
                                                            text = inst.mcVersion,
                                                            color = Color.White,
                                                            fontWeight = FontWeight.SemiBold,
                                                            fontSize = (9.5.sp).resp()
                                                        )
                                                    }

                                                    // Java Runtime Badge
                                                    val activeRuntime = if (inst.javaRuntime != "auto") inst.javaRuntime
                                                        else JavaRuntimeManager.getRecommendedRuntime(inst.mcVersion)
                                                    val jreLabel = activeRuntime.replace("jre-", "Java ")
                                                    Box(
                                                        modifier = Modifier
                                                            .background(Color(0xFF1E293B), RoundedCornerShape((5.dp).resp()))
                                                            .border(1.dp, NuxColors.CardBorder, RoundedCornerShape((5.dp).resp()))
                                                            .padding(horizontal = (7.dp).resp(), vertical = (2.dp).resp())
                                                    ) {
                                                        Text(
                                                            text = jreLabel,
                                                            color = NuxColors.GrayNeutral,
                                                            fontWeight = FontWeight.SemiBold,
                                                            fontSize = (9.5.sp).resp()
                                                        )
                                                    }
                                                } else {
                                                    Box(
                                                        modifier = Modifier
                                                            .background(Color(0xFF1E293B), RoundedCornerShape((5.dp).resp()))
                                                            .padding(horizontal = (7.dp).resp(), vertical = (2.dp).resp())
                                                    ) {
                                                        Text(
                                                            text = tr("dashboard.no_version_selected"),
                                                            color = NuxColors.GrayNeutral,
                                                            fontSize = (9.5.sp).resp()
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Dropdown Menu for Installed Instances
                                    DropdownMenu(
                                        expanded = isVersionDropdownExpanded,
                                        onDismissRequest = { isVersionDropdownExpanded = false },
                                        shape = RoundedCornerShape((12.dp).resp()),
                                        containerColor = Color(0xF50F172A),
                                        shadowElevation = (16.dp).resp(),
                                        border = BorderStroke(1.dp, NuxColors.CardBorder),
                                        modifier = Modifier.widthIn(min = (260.dp).resp(), max = (340.dp).resp())
                                    ) {
                                        if (instances.isEmpty()) {
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = tr("dashboard.no_instances"),
                                                        color = NuxColors.GrayNeutral,
                                                        fontSize = (11.sp).resp()
                                                    )
                                                },
                                                onClick = { }
                                            )
                                        } else {
                                            instances.forEach { inst ->
                                                val isSelected = inst.id == selectedInstance?.id
                                                DropdownMenuItem(
                                                    text = {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text(
                                                                    text = inst.name,
                                                                    color = if (isSelected) NuxColors.SkyBlue else Color.White,
                                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                                    fontSize = (12.sp).resp(),
                                                                    maxLines = 1
                                                                )
                                                                Text(
                                                                    text = "${inst.loader.uppercase()} • ${inst.mcVersion}",
                                                                    color = NuxColors.GrayNeutral,
                                                                    fontSize = (9.5.sp).resp()
                                                                )
                                                            }
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy((6.dp).resp())
                                                            ) {
                                                                if (isSelected) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.Check,
                                                                        contentDescription = tr("common.selected"),
                                                                        tint = NuxColors.MintGreen,
                                                                        modifier = Modifier.size((16.dp).resp())
                                                                    )
                                                                }
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size((28.dp).resp())
                                                                        .clip(RoundedCornerShape((8.dp).resp()))
                                                                        .background(Color(0xFF1E293B), RoundedCornerShape((8.dp).resp()))
                                                                        .clickable {
                                                                            isVersionDropdownExpanded = false
                                                                            instanceToEdit = inst
                                                                        },
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Outlined.Settings,
                                                                        contentDescription = tr("dashboard.instance_settings"),
                                                                        tint = Color.White,
                                                                        modifier = Modifier.size((15.dp).resp())
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    },
                                                    onClick = {
                                                        InstanceManager.selectInstance(inst)
                                                        isVersionDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }

                                        HorizontalDivider(color = NuxColors.LightGray, thickness = 1.dp)

                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy((8.dp).resp())
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Add,
                                                        contentDescription = tr("common.add"),
                                                        tint = NuxColors.SkyBlue,
                                                        modifier = Modifier.size((18.dp).resp())
                                                    )
                                                    Text(
                                                        text = tr("dashboard.add_new_instance"),
                                                        color = NuxColors.SkyBlue,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = (12.5.sp).resp()
                                                    )
                                                }
                                            },
                                            onClick = {
                                                isVersionDropdownExpanded = false
                                                handleRequestCreateInstance()
                                            }
                                        )
                                    }
                                }

                                // RIGHT DECK: Standalone Royal Cobalt PLAY Action Card
                                val inst = selectedInstance
                                val isFullyDownloaded = inst != null && inst.isDownloaded && InstanceManager.isInstanceDownloaded(context, inst)
                                val playBtnShape = RoundedCornerShape((12.dp).resp())

                                Row(
                                    modifier = Modifier
                                        .height((62.dp).resp())
                                        .clip(playBtnShape)
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(NuxColors.ForestGreen, NuxColors.MintGreen)
                                            ),
                                            playBtnShape
                                        )
                                        .border(1.dp, Color(0x6693C5FD), playBtnShape)
                                        .clickable {
                                            if (inst == null) {
                                                handleRequestCreateInstance()
                                                return@clickable
                                            }

                                            val account = currentAccount
                                            if (account == null) {
                                                Toast.makeText(context, trStr("dashboard.toast_select_account"), Toast.LENGTH_SHORT).show()
                                                currentTab = "accounts"
                                                return@clickable
                                            }

                                            if (!isFullyDownloaded) {
                                                val targetRuntime = JavaRuntimeManager.getRecommendedRuntime(inst.mcVersion)
                                                isDownloading = true
                                                downloadTargetName = inst.name
                                                downloadProgress = 0f
                                                downloadMessage = trStr("dashboard.download_preparing_openjdk", JavaRuntimeManager.getRuntimeDisplayName(targetRuntime))

                                                scope.launch {
                                                    JavaRuntimeManager.extractRuntime(context, targetRuntime) { p, msg ->
                                                        downloadProgress = p
                                                        downloadMessage = msg
                                                    }

                                                    val res = downloader.downloadInstance(inst) { p, msg ->
                                                        downloadProgress = p
                                                        downloadMessage = msg
                                                    }
                                                    isDownloading = false
                                                    if (res.isSuccess) {
                                                        Toast.makeText(context, trStr("dashboard.toast_install_complete"), Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        Toast.makeText(context, trStr("dashboard.toast_download_failed", res.exceptionOrNull()?.localizedMessage ?: ""), Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            } else {
                                                val targetRuntime = selectedInstance?.let { JavaRuntimeManager.getRecommendedRuntime(it.mcVersion) } ?: "jre-21"
                                                if (!JavaRuntimeManager.isRuntimeInstalled(context, targetRuntime)) {
                                                    isDownloading = true
                                                    downloadTargetName = inst.name
                                                    downloadProgress = 0f
                                                    downloadMessage = trStr("dashboard.download_preparing_openjdk", JavaRuntimeManager.getRuntimeDisplayName(targetRuntime))
                                                    scope.launch {
                                                        val extRes = JavaRuntimeManager.extractRuntime(context, targetRuntime) { p, msg ->
                                                            downloadProgress = p
                                                            downloadMessage = msg
                                                        }
                                                        isDownloading = false
                                                        if (extRes.isSuccess) {
                                                            Toast.makeText(context, trStr("dashboard.toast_launching", inst.name), Toast.LENGTH_SHORT).show()
                                                            GameLauncher.launch(context, inst, account)
                                                        } else {
                                                            Toast.makeText(context, trStr("dashboard.toast_openjdk_failed", extRes.exceptionOrNull()?.message ?: ""), Toast.LENGTH_LONG).show()
                                                        }
                                                    }
                                                    return@clickable
                                                }

                                                val currentRendererInfo = NuxRendererRegistry.findRendererById(launcherSettings.selectedRenderer)
                                                val isSupported = NuxRendererRegistry.isSupportedForVersion(currentRendererInfo, inst.mcVersion)
                                                if (!isSupported) {
                                                    unsupportedRendererInfo = currentRendererInfo
                                                    pendingLaunchInstance = inst
                                                } else {
                                                    Toast.makeText(context, trStr("dashboard.toast_launching", inst.name), Toast.LENGTH_SHORT).show()
                                                    GameLauncher.launch(context, inst, account)
                                                }
                                            }
                                        }
                                        .padding(horizontal = (28.dp).resp()),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy((8.dp).resp())
                                ) {
                                    Icon(
                                        imageVector = if (inst == null) Icons.Default.Add else if (isFullyDownloaded) Icons.Default.PlayArrow else Icons.Default.Download,
                                        contentDescription = "Action",
                                        tint = Color.White,
                                        modifier = Modifier.size((22.dp).resp())
                                    )
                                    Text(
                                        text = if (inst == null) tr("dashboard.play_new_instance") else if (isFullyDownloaded) tr("dashboard.play_button") else tr("dashboard.play_download"),
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = (15.sp).resp(),
                                        letterSpacing = (0.8.sp).resp()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Floating Voice Bar
            if (currentTab != "friends") {
                FloatingVoiceBar(
                    onOpenVoiceRoom = { currentTab = "friends" },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = (14.dp).resp(), end = (18.dp).resp())
                )
            }
        }

        // Add Instance Dialog
        if (showAddDialog) {
            NuxAddInstanceDialog(
                onDismiss = { showAddDialog = false },
                onInstanceCreated = { newInst ->
                    showAddDialog = false
                    InstanceManager.createInstance(context, newInst)
                    Toast.makeText(context, trStr("dialogs.add_instance.toast_created", newInst.name), Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Edit Instance Dialog
        val targetEditInstance = instanceToEdit ?: if (showEditInstanceDialog) selectedInstance else null
        targetEditInstance?.let { inst ->
            NuxEditInstanceDialog(
                instance = inst,
                onDismiss = {
                    instanceToEdit = null
                    showEditInstanceDialog = false
                },
                onInstanceUpdated = { updated ->
                    instanceToEdit = null
                    showEditInstanceDialog = false
                    InstanceManager.updateInstance(context, updated)
                },
                onInstanceDeleted = { toDelete ->
                    instanceToEdit = null
                    showEditInstanceDialog = false
                    InstanceManager.deleteInstance(context, toDelete.id)
                    Toast.makeText(context, trStr("dialogs.delete_instance.toast_deleted", toDelete.name), Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Delete Instance Confirmation Dialog
        instanceToDelete?.let { inst ->
            NuxDeleteInstanceDialog(
                instance = inst,
                onConfirm = {
                    instanceToDelete = null
                    InstanceManager.deleteInstance(context, inst.id)
                    Toast.makeText(context, trStr("dialogs.delete_instance.toast_deleted", inst.name), Toast.LENGTH_SHORT).show()
                },
                onDismiss = { instanceToDelete = null }
            )
        }

        // Download Progress Dialog
        if (isDownloading) {
            NuxDownloadProgressDialog(
                instanceName = downloadTargetName,
                progress = downloadProgress,
                message = downloadMessage
            )
        }

        // Launcher Update Dialog
        updateDialogInfo?.let { info ->
            NuxUpdateDialog(
                updateInfo = info,
                onDismiss = {
                    updateDialogInfo = null
                    pendingAnnouncement?.let { ann ->
                        announcementDialogInfo = ann
                        pendingAnnouncement = null
                    }
                }
            )
        }

        // Launcher Announcement Dialog
        if (updateDialogInfo == null) {
            announcementDialogInfo?.let { ann ->
                NuxAnnouncementDialog(
                    announcement = ann,
                    onDismiss = {
                        announcementDialogInfo = null
                    }
                )
            }
        }

        // Unsupported Renderer Warning Dialog
        val warnRenderer = unsupportedRendererInfo
        val launchInst = pendingLaunchInstance
        if (warnRenderer != null && launchInst != null) {
            NuxRendererWarningDialog(
                renderer = warnRenderer,
                mcVersion = launchInst.mcVersion,
                onConfirm = {
                    val toLaunch = pendingLaunchInstance
                    val account = currentAccount
                    unsupportedRendererInfo = null
                    pendingLaunchInstance = null
                    if (toLaunch != null && account != null) {
                        Toast.makeText(context, trStr("dashboard.toast_launching", toLaunch.name), Toast.LENGTH_SHORT).show()
                        GameLauncher.launch(context, toLaunch, account)
                    }
                },
                onDismiss = {
                    unsupportedRendererInfo = null
                    pendingLaunchInstance = null
                }
            )
        }

        // Game Crash Popup Dialog
        activeCrash?.let { crash ->
            NuxCrashDialog(
                crashInfo = crash,
                onDismiss = {
                    CrashManager.dismissCrash(context)
                }
            )
        }

        // External Addon Import Dialog
        pendingImport?.let { importItem ->
            NuxAddonImportDialog(
                pendingImport = importItem,
                onDismiss = {
                    NuxAddonImportManager.clearPendingImport()
                }
            )
        }

        // About & Open Source Licenses Dialog
        if (showAboutDialog) {
            NuxAboutDialog(
                onDismissRequest = { showAboutDialog = false }
            )
        }

        // Bell Premium & Showcase Dialog
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
}

/**
 * Quick Action Card in Clean Slate Navy Style
 */
@Composable
fun QuickActionCard(
    title: String,
    description: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false
) {
    val cardShape = RoundedCornerShape((10.dp).resp())
    val bgColor = if (isDestructive) Color(0xFF1F1219) else Color(0xFF131C31)
    val borderColor = if (isDestructive) Color(0xFFEF4444).copy(alpha = 0.30f) else NuxColors.CardBorder

    Box(
        modifier = modifier
            .clip(cardShape)
            .background(bgColor, cardShape)
            .border(1.dp, borderColor, cardShape)
            .clickable { onClick() }
            .padding((8.dp).resp())
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                val iconShape = RoundedCornerShape((6.dp).resp())
                Box(
                    modifier = Modifier
                        .size((24.dp).resp())
                        .clip(iconShape)
                        .background(accentColor.copy(alpha = 0.14f), iconShape)
                        .border(1.dp, accentColor.copy(alpha = 0.30f), iconShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size((14.dp).resp())
                    )
                }

                Spacer(modifier = Modifier.height((4.dp).resp()))

                Text(
                    text = title,
                    color = if (isDestructive) Color(0xFFFECDD3) else Color.White,
                    fontSize = (9.5.sp).resp(),
                    fontWeight = FontWeight.Black,
                    lineHeight = (11.5.sp).resp(),
                    letterSpacing = (0.3.sp).resp()
                )

                Spacer(modifier = Modifier.height((2.dp).resp()))

                Box(
                    modifier = Modifier
                        .size(width = (16.dp).resp(), height = (1.5.dp).resp())
                        .background(accentColor.copy(alpha = 0.55f), CircleShape)
                )
            }

            Text(
                text = description,
                color = NuxColors.GrayNeutral,
                fontSize = (8.sp).resp(),
                lineHeight = (9.5.sp).resp(),
                maxLines = 2
            )
        }
    }
}
