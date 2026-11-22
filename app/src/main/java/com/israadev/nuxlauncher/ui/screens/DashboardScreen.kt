package com.israadev.nuxlauncher.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import android.widget.VideoView
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.israadev.nuxlauncher.R
import com.israadev.nuxlauncher.core.account.AccountManager
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
import com.israadev.nuxlauncher.ui.components.NuxNetworkImage
import com.israadev.nuxlauncher.ui.dialogs.*
import com.israadev.nuxlauncher.ui.theme.resp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PREF_BG_MEDIA = "custom_dashboard_bg_prefs"
private const val KEY_BG_URI = "bg_media_uri"
private const val KEY_BG_TYPE = "bg_media_type" // "image" | "video" | ""

private val BellBgBase = Color(0xFF09090B)
private val BellRailSurface = Color(0xFF0E0E12) // Solid opaque agar tidak overdraw GPU di low-end
private val BellGlassCard = Color(0xD9121217)
private val BellGlassAlt = Color(0xF21A1A22)
private val BellBorder = Color(0x26FFFFFF)
private val BellBorderActive = Color(0x66FFFFFF)
private val BellAccent = Color(0xFFE4E4E7)
private val BellMuted = Color(0xFF8E8E9A)
private val BellSuccess = Color(0xFFD4D4D8)

private data class SideNavItem(
    val id: String,
    val label: String,
    val icon: ImageVector
)

private val SIDE_NAV_ITEMS = listOf(
    SideNavItem("home", "HOME", Icons.Outlined.Home),
    SideNavItem("mods", "MODS", Icons.Outlined.Extension),
    SideNavItem("friends", "FRIENDS", Icons.Outlined.Group),
    SideNavItem("accounts", "ACCOUNTS", Icons.Outlined.Person),
    SideNavItem("ai", "AI", Icons.Outlined.Psychology),
    SideNavItem("settings", "SETTINGS", Icons.Outlined.Settings)
)

@Composable
private fun LocalDashboardBackground(
    mediaUriString: String?,
    mediaType: String,
    isActive: Boolean
) {
    val parsedUri = remember(mediaUriString) {
        mediaUriString?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
    }

    if (parsedUri != null && mediaType == "video") {
        val lifecycleOwner = LocalLifecycleOwner.current
        var videoViewRef by remember { mutableStateOf<VideoView?>(null) }

        // Pause otomatis saat launcher di-minimize atau game sedang berjalan (hemat CPU/GPU & baterai)
        DisposableEffect(lifecycleOwner, isActive) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> videoViewRef?.pause()
                    Lifecycle.Event.ON_RESUME -> if (isActive) videoViewRef?.start()
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            if (!isActive) videoViewRef?.pause() else videoViewRef?.start()

            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                videoViewRef?.stopPlayback()
            }
        }

        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setVideoURI(parsedUri)
                    setOnPreparedListener { mp ->
                        mp.isLooping = true
                        mp.setVolume(0f, 0f)
                        if (isActive) start()
                    }
                    setOnErrorListener { _, _, _ -> true }
                    videoViewRef = this
                }
            },
            update = { videoView ->
                videoViewRef = videoView
                if (videoView.tag != parsedUri.toString()) {
                    videoView.tag = parsedUri.toString()
                    videoView.setVideoURI(parsedUri)
                    videoView.setOnPreparedListener { mp ->
                        mp.isLooping = true
                        mp.setVolume(0f, 0f)
                        if (isActive) mp.start()
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    } else if (parsedUri != null && mediaType == "image") {
        NuxNetworkImage(
            model = parsedUri.toString(),
            contentDescription = "Custom Background",
            modifier = Modifier.fillMaxSize()
        )
    } else {
        Image(
            painter = painterResource(id = R.drawable.mc_hero_bg),
            contentDescription = "Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            alpha = 0.42f
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0x9909090B),
                        Color(0x3309090B),
                        Color(0xE609090B)
                    )
                )
            )
    )
}

@Composable
fun DashboardScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val bgPrefs = remember { context.getSharedPreferences(PREF_BG_MEDIA, Context.MODE_PRIVATE) }

    var bgUriString by remember { mutableStateOf(bgPrefs.getString(KEY_BG_URI, null)) }
    var bgMediaType by remember { mutableStateOf(bgPrefs.getString(KEY_BG_TYPE, "") ?: "") }

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
            }
            val mime = context.contentResolver.getType(uri).orEmpty()
            val detectedType = if (mime.startsWith("video/")) "video" else "image"
            bgPrefs.edit()
                .putString(KEY_BG_URI, uri.toString())
                .putString(KEY_BG_TYPE, detectedType)
                .apply()
            bgUriString = uri.toString()
            bgMediaType = detectedType
            Toast.makeText(
                context,
                if (detectedType == "video") "Latar belakang video lokal diterapkan" else "Latar belakang gambar lokal diterapkan",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

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
    var pendingLaunchInstance by remember { mutableStateOf<com.israadev.nuxlauncher.core.models.Instance?>(null) }
    var unsupportedRendererInfo by remember { mutableStateOf<NuxRendererInfo?>(null) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showPremiumDialog by remember { mutableStateOf(false) }
    var premiumInitialPrompt by remember { mutableStateOf<String?>(null) }
    val pendingImport by NuxAddonImportManager.pendingImport.collectAsState()

    val handleRequestCreateInstance = remember(instances.size, launcherUser?.isActivated) {
        {
            if (instances.size >= 3 && launcherUser?.isActivated != true) {
                premiumInitialPrompt = "Free accounts are limited to a maximum of 3 instances. Upgrade to unlock unlimited instances."
                showPremiumDialog = true
            } else {
                showAddDialog = true
            }
        }
    }

    var isDownloading by remember { mutableStateOf(false) }
    var downloadTargetName by remember { mutableStateOf("") }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadMessage by remember { mutableStateOf("") }

    val downloader = remember { MinecraftDownloader(context) }

    var isVersionDropdownExpanded by remember { mutableStateOf(false) }
    var isAccountDropdownExpanded by remember { mutableStateOf(false) }

    // Pengecekan file instance dipindah ke IO thread agar UI thread tidak pernah nge-lag
    val isFullyDownloaded by produceState(initialValue = false, selectedInstance, selectedInstance?.isDownloaded) {
        val inst = selectedInstance
        value = if (inst != null && inst.isDownloaded) {
            withContext(Dispatchers.IO) {
                InstanceManager.isInstanceDownloaded(context, inst)
            }
        } else {
            false
        }
    }

    if (currentTab == "gui_editor") {
        CustomGuiEditorScreen(
            onNavigateBack = { currentTab = "settings" },
            modifier = Modifier.fillMaxSize()
        )
    } else {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(BellBgBase)
        ) {
            // =================================================================
            // 1. VERTICAL LEFT RAIL (Solid Surface -> Zero GPU Overdraw)
            // =================================================================
            Column(
                modifier = Modifier
                    .width((68.dp).resp())
                    .fillMaxHeight()
                    .background(BellRailSurface)
                    .padding(vertical = (10.dp).resp(), horizontal = (6.dp).resp()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size((38.dp).resp())
                        .clip(RoundedCornerShape((11.dp).resp()))
                        .background(Color.White)
                        .clickable {
                            if (currentTab == "home") showAboutDialog = true else currentTab = "home"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.NotificationsActive,
                        contentDescription = "Bell Launcher",
                        tint = BellBgBase,
                        modifier = Modifier.size((20.dp).resp())
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy((5.dp).resp())
                ) {
                    SIDE_NAV_ITEMS.forEach { tab ->
                        val isSelected = currentTab == tab.id
                        val itemShape = RoundedCornerShape((11.dp).resp())
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(itemShape)
                                .background(if (isSelected) Color.White else Color.Transparent)
                                .clickable { currentTab = tab.id }
                                .padding(vertical = (6.dp).resp()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy((2.dp).resp())
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                tint = if (isSelected) BellBgBase else BellMuted,
                                modifier = Modifier.size((16.dp).resp())
                            )
                            Text(
                                text = tab.label,
                                color = if (isSelected) BellBgBase else BellMuted,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                                fontSize = (7.5.sp).resp(),
                                letterSpacing = (0.4.sp).resp(),
                                maxLines = 1
                            )
                        }
                    }
                }

                // Account Avatar & Quick Switcher
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy((6.dp).resp())
                ) {
                    val activeUsername = currentAccount?.username ?: launcherUser?.username ?: "Guest"
                    val initialLetter = remember(activeUsername) {
                        activeUsername.firstOrNull()?.uppercase() ?: "G"
                    }

                    Box {
                        Box(
                            modifier = Modifier
                                .size((38.dp).resp())
                                .clip(CircleShape)
                                .background(BellGlassAlt)
                                .border(1.5.dp, BellBorderActive, CircleShape)
                                .clickable { isAccountDropdownExpanded = true },
                            contentAlignment = Alignment.Center
                        ) {
                            val photo = launcherUser?.photoURL ?: currentAccount?.photoUrl
                            if (!photo.isNullOrBlank()) {
                                NuxNetworkImage(
                                    model = photo,
                                    contentDescription = "Profile",
                                    fallbackInitials = activeUsername,
                                    modifier = Modifier.fillMaxSize(),
                                    shape = CircleShape
                                )
                            } else {
                                Text(
                                    text = initialLetter,
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = (13.sp).resp()
                                )
                            }
                        }

                        if (isAccountDropdownExpanded) {
                            val accounts by AccountManager.accounts.collectAsState()
                            DropdownMenu(
                                expanded = true,
                                onDismissRequest = { isAccountDropdownExpanded = false },
                                shape = RoundedCornerShape((12.dp).resp()),
                                containerColor = BellGlassAlt,
                                border = BorderStroke(1.dp, BellBorder),
                                modifier = Modifier.widthIn(min = (210.dp).resp())
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = "ACTIVE PROFILE",
                                                color = BellMuted,
                                                fontSize = (8.5.sp).resp(),
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = (0.6.sp).resp()
                                            )
                                            Text(
                                                text = activeUsername,
                                                color = Color.White,
                                                fontSize = (12.sp).resp(),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    },
                                    onClick = { }
                                )

                                HorizontalDivider(color = BellBorder, thickness = 1.dp)

                                if (accounts.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text(tr("dashboard.no_accounts"), color = BellMuted, fontSize = (11.sp).resp()) },
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
                                                        color = if (isSelected) Color.White else BellAccent,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        fontSize = (11.5.sp).resp()
                                                    )
                                                    if (isSelected) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = "Selected",
                                                            tint = BellSuccess,
                                                            modifier = Modifier.size((15.dp).resp())
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

                                HorizontalDivider(color = BellBorder, thickness = 1.dp)

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = tr("dashboard.manage_accounts"),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = (11.5.sp).resp()
                                        )
                                    },
                                    onClick = {
                                        isAccountDropdownExpanded = false
                                        currentTab = "accounts"
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(BellBorder)
            )

            // =================================================================
            // 2. MAIN WORKSPACE STAGE
            // =================================================================
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                when (currentTab) {
                    "accounts" -> AccountsScreen(
                        onNavigateBack = { currentTab = "home" },
                        modifier = Modifier.fillMaxSize()
                    )
                    "mods" -> ModsScreen(
                        onNavigateBack = { currentTab = "home" },
                        modifier = Modifier.fillMaxSize()
                    )
                    "ai" -> AIScreen(
                        onNavigateBack = { currentTab = "home" },
                        modifier = Modifier.fillMaxSize()
                    )
                    "settings" -> SettingsScreen(
                        onNavigateBack = { currentTab = "home" },
                        onOpenGuiEditor = { currentTab = "gui_editor" },
                        modifier = Modifier.fillMaxSize()
                    )
                    "friends" -> FriendsScreen(
                        onNavigateBack = { currentTab = "home" },
                        modifier = Modifier.fillMaxSize()
                    )
                    else -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            LocalDashboardBackground(
                                mediaUriString = bgUriString,
                                mediaType = bgMediaType,
                                isActive = currentTab == "home"
                            )

                            // TOP BAR FLOATING CHIPS
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.TopCenter)
                                    .padding(horizontal = (20.dp).resp(), vertical = (14.dp).resp()),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(BellGlassCard)
                                        .border(1.dp, BellBorder, CircleShape)
                                        .clickable { showAboutDialog = true }
                                        .padding(horizontal = (12.dp).resp(), vertical = (6.dp).resp()),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy((8.dp).resp())
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size((7.dp).resp())
                                            .background(Color.White, CircleShape)
                                    )
                                    Text(
                                        text = "BELL LAUNCHER",
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = (9.5.sp).resp(),
                                        letterSpacing = (1.sp).resp()
                                    )
                                    val activeUsername = currentAccount?.username ?: launcherUser?.username ?: "Guest"
                                    Text(
                                        text = "•  $activeUsername",
                                        color = BellMuted,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = (9.5.sp).resp()
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy((8.dp).resp()),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(BellGlassCard)
                                            .border(1.dp, BellBorder, CircleShape)
                                            .clickable {
                                                mediaPickerLauncher.launch(arrayOf("image/*", "video/*"))
                                            }
                                            .padding(horizontal = (12.dp).resp(), vertical = (6.dp).resp())
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy((5.dp).resp())
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.PermMedia,
                                                contentDescription = "Pilih Gambar atau Video",
                                                tint = Color.White,
                                                modifier = Modifier.size((13.dp).resp())
                                            )
                                            Text(
                                                text = "WALLPAPER",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = (9.sp).resp(),
                                                letterSpacing = (0.7.sp).resp()
                                            )
                                        }
                                    }

                                    if (!bgUriString.isNullOrBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(BellGlassCard)
                                                .border(1.dp, BellBorder, CircleShape)
                                                .clickable {
                                                    bgPrefs.edit().clear().apply()
                                                    bgUriString = null
                                                    bgMediaType = ""
                                                    Toast.makeText(context, "Latar belakang direset ke bawaan", Toast.LENGTH_SHORT).show()
                                                }
                                                .padding(horizontal = (10.dp).resp(), vertical = (6.dp).resp())
                                        ) {
                                            Text(
                                                text = "RESET",
                                                color = BellMuted,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = (9.sp).resp()
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size((28.dp).resp())
                                            .clip(CircleShape)
                                            .background(BellGlassCard)
                                            .border(1.dp, BellBorder, CircleShape)
                                            .clickable { showAboutDialog = true },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Info,
                                            contentDescription = "About",
                                            tint = Color.White,
                                            modifier = Modifier.size((14.dp).resp())
                                        )
                                    }
                                }
                            }

                            // KIRI BAWAH: QUICK ACTION SHORTCUTS
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(start = (20.dp).resp(), bottom = (18.dp).resp()),
                                horizontalArrangement = Arrangement.spacedBy((8.dp).resp()),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .height((38.dp).resp())
                                        .clip(RoundedCornerShape((10.dp).resp()))
                                        .background(BellGlassCard)
                                        .border(1.dp, BellBorder, RoundedCornerShape((10.dp).resp()))
                                        .clickable { handleRequestCreateInstance() }
                                        .padding(horizontal = (14.dp).resp()),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy((6.dp).resp())
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "New Instance",
                                            tint = Color.White,
                                            modifier = Modifier.size((15.dp).resp())
                                        )
                                        Text(
                                            text = "NEW INSTANCE",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = (9.5.sp).resp(),
                                            letterSpacing = (0.6.sp).resp()
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .height((38.dp).resp())
                                        .clip(RoundedCornerShape((10.dp).resp()))
                                        .background(BellGlassCard)
                                        .border(1.dp, BellBorder, RoundedCornerShape((10.dp).resp()))
                                        .clickable { currentTab = "mods" }
                                        .padding(horizontal = (14.dp).resp()),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy((6.dp).resp())
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Extension,
                                            contentDescription = "Browse Mods",
                                            tint = Color.White,
                                            modifier = Modifier.size((15.dp).resp())
                                        )
                                        Text(
                                            text = "MODS & PACKS",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = (9.5.sp).resp(),
                                            letterSpacing = (0.6.sp).resp()
                                        )
                                    }
                                }
                            }

                            // KANAN BAWAH: FLOATING LAUNCH DECK CARD
                            val inst = selectedInstance

                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(end = (20.dp).resp(), bottom = (18.dp).resp())
                                    .width((310.dp).resp())
                                    .clip(RoundedCornerShape((16.dp).resp()))
                                    .background(BellGlassCard)
                                    .border(1.dp, BellBorderActive, RoundedCornerShape((16.dp).resp()))
                                    .padding((14.dp).resp())
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy((10.dp).resp())
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            Row(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape((8.dp).resp()))
                                                    .clickable { isVersionDropdownExpanded = true }
                                                    .padding(vertical = (2.dp).resp()),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy((6.dp).resp())
                                            ) {
                                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                                    Text(
                                                        text = "SELECTED PROFILE",
                                                        color = BellMuted,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = (8.sp).resp(),
                                                        letterSpacing = (0.8.sp).resp()
                                                    )
                                                    Text(
                                                        text = inst?.name ?: tr("dashboard.select_instance"),
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Black,
                                                        fontSize = (16.sp).resp(),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Icon(
                                                    imageVector = Icons.Outlined.UnfoldMore,
                                                    contentDescription = tr("dashboard.select_version"),
                                                    tint = Color.White,
                                                    modifier = Modifier.size((18.dp).resp())
                                                )
                                            }

                                            if (isVersionDropdownExpanded) {
                                                DropdownMenu(
                                                    expanded = true,
                                                    onDismissRequest = { isVersionDropdownExpanded = false },
                                                    shape = RoundedCornerShape((12.dp).resp()),
                                                    containerColor = BellGlassAlt,
                                                    border = BorderStroke(1.dp, BellBorder),
                                                    modifier = Modifier.widthIn(min = (250.dp).resp(), max = (320.dp).resp())
                                                ) {
                                                    if (instances.isEmpty()) {
                                                        DropdownMenuItem(
                                                            text = {
                                                                Text(
                                                                    text = tr("dashboard.no_instances"),
                                                                    color = BellMuted,
                                                                    fontSize = (11.sp).resp()
                                                                )
                                                            },
                                                            onClick = { }
                                                        )
                                                    } else {
                                                        instances.forEach { itemInst ->
                                                            val isSelected = itemInst.id == selectedInstance?.id
                                                            DropdownMenuItem(
                                                                text = {
                                                                    Row(
                                                                        modifier = Modifier.fillMaxWidth(),
                                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                                        verticalAlignment = Alignment.CenterVertically
                                                                    ) {
                                                                        Column(modifier = Modifier.weight(1f)) {
                                                                            Text(
                                                                                text = itemInst.name,
                                                                                color = if (isSelected) Color.White else BellAccent,
                                                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                                                fontSize = (12.sp).resp(),
                                                                                maxLines = 1
                                                                            )
                                                                            Text(
                                                                                text = "${itemInst.loader.uppercase()} • ${itemInst.mcVersion}",
                                                                                color = BellMuted,
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
                                                                                    tint = BellSuccess,
                                                                                    modifier = Modifier.size((15.dp).resp())
                                                                                )
                                                                            }
                                                                            Box(
                                                                                modifier = Modifier
                                                                                    .size((26.dp).resp())
                                                                                    .clip(RoundedCornerShape((6.dp).resp()))
                                                                                    .background(BellRailSurface)
                                                                                    .clickable {
                                                                                        isVersionDropdownExpanded = false
                                                                                        instanceToEdit = itemInst
                                                                                    },
                                                                                contentAlignment = Alignment.Center
                                                                            ) {
                                                                                Icon(
                                                                                    imageVector = Icons.Outlined.Settings,
                                                                                    contentDescription = tr("dashboard.instance_settings"),
                                                                                    tint = Color.White,
                                                                                    modifier = Modifier.size((14.dp).resp())
                                                                                )
                                                                            }
                                                                        }
                                                                    }
                                                                },
                                                                onClick = {
                                                                    InstanceManager.selectInstance(itemInst)
                                                                    isVersionDropdownExpanded = false
                                                                }
                                                            )
                                                        }
                                                    }

                                                    HorizontalDivider(color = BellBorder, thickness = 1.dp)

                                                    DropdownMenuItem(
                                                        text = {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy((8.dp).resp())
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Add,
                                                                    contentDescription = tr("common.add"),
                                                                    tint = Color.White,
                                                                    modifier = Modifier.size((17.dp).resp())
                                                                )
                                                                Text(
                                                                    text = tr("dashboard.add_new_instance"),
                                                                    color = Color.White,
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = (12.sp).resp()
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
                                        }

                                        if (inst != null) {
                                            Box(
                                                modifier = Modifier
                                                    .size((34.dp).resp())
                                                    .clip(RoundedCornerShape((9.dp).resp()))
                                                    .background(Color(0x1AFFFFFF))
                                                    .border(1.dp, BellBorder, RoundedCornerShape((9.dp).resp()))
                                                    .clickable { instanceToEdit = inst },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Tune,
                                                    contentDescription = "Configure Instance",
                                                    tint = Color.White,
                                                    modifier = Modifier.size((16.dp).resp())
                                                )
                                            }
                                        }
                                    }

                                    // Chips Spesifikasi
                                    if (inst != null) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy((6.dp).resp()),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .background(Color(0x26FFFFFF), RoundedCornerShape((5.dp).resp()))
                                                    .padding(horizontal = (7.dp).resp(), vertical = (3.dp).resp())
                                            ) {
                                                Text(
                                                    text = inst.loader.uppercase(),
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = (8.5.sp).resp(),
                                                    letterSpacing = (0.5.sp).resp()
                                                )
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .background(Color(0x14FFFFFF), RoundedCornerShape((5.dp).resp()))
                                                    .border(1.dp, BellBorder, RoundedCornerShape((5.dp).resp()))
                                                    .padding(horizontal = (7.dp).resp(), vertical = (3.dp).resp())
                                            ) {
                                                Text(
                                                    text = inst.mcVersion,
                                                    color = BellAccent,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = (8.5.sp).resp()
                                                )
                                            }

                                            val activeRuntime = remember(inst.javaRuntime, inst.mcVersion) {
                                                if (inst.javaRuntime != "auto") inst.javaRuntime
                                                else JavaRuntimeManager.getRecommendedRuntime(inst.mcVersion)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .background(Color(0x14FFFFFF), RoundedCornerShape((5.dp).resp()))
                                                    .border(1.dp, BellBorder, RoundedCornerShape((5.dp).resp()))
                                                    .padding(horizontal = (7.dp).resp(), vertical = (3.dp).resp())
                                            ) {
                                                Text(
                                                    text = activeRuntime.replace("jre-", "JAVA "),
                                                    color = BellMuted,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = (8.5.sp).resp()
                                                )
                                            }
                                        }
                                    }

                                    // Tombol Utama PLAY / DOWNLOAD
                                    val playBtnShape = RoundedCornerShape((11.dp).resp())
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height((44.dp).resp())
                                            .clip(playBtnShape)
                                            .background(Color.White, playBtnShape)
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
                                                                trimLauncherMemoryBeforeGame()
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
                                                        trimLauncherMemoryBeforeGame()
                                                        Toast.makeText(context, trStr("dashboard.toast_launching", inst.name), Toast.LENGTH_SHORT).show()
                                                        GameLauncher.launch(context, inst, account)
                                                    }
                                                }
                                            },
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = if (inst == null) Icons.Default.Add else if (isFullyDownloaded) Icons.Default.PlayArrow else Icons.Default.Download,
                                            contentDescription = "Action",
                                            tint = BellBgBase,
                                            modifier = Modifier.size((19.dp).resp())
                                        )
                                        Spacer(modifier = Modifier.width((8.dp).resp()))
                                        Text(
                                            text = if (inst == null) tr("dashboard.play_new_instance") else if (isFullyDownloaded) tr("dashboard.play_button") else tr("dashboard.play_download"),
                                            color = BellBgBase,
                                            fontWeight = FontWeight.Black,
                                            fontSize = (13.sp).resp(),
                                            letterSpacing = (0.9.sp).resp()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

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

        if (isDownloading) {
            NuxDownloadProgressDialog(
                instanceName = downloadTargetName,
                progress = downloadProgress,
                message = downloadMessage
            )
        }

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
                        trimLauncherMemoryBeforeGame()
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

        activeCrash?.let { crash ->
            NuxCrashDialog(
                crashInfo = crash,
                onDismiss = { CrashManager.dismissCrash(context) }
            )
        }

        pendingImport?.let { importItem ->
            NuxAddonImportDialog(
                pendingImport = importItem,
                onDismiss = { NuxAddonImportManager.clearPendingImport() }
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
}

/**
 * Mengosongkan sisa memori UI launcher tepat sebelum proses JVM Minecraft dimulai
 * agar HP dengan RAM 3GB–4GB memiliki ruang maksimum dan tidak terkena LMK (Low Memory Killer).
 */
private fun trimLauncherMemoryBeforeGame() {
    try {
        System.gc()
        System.runFinalization()
    } catch (_: Throwable) {
    }
}
