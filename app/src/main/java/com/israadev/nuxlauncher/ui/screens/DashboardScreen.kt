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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import kotlinx.coroutines.launch

private const val PREF_BG_MEDIA = "custom_dashboard_bg_prefs"
private const val KEY_BG_URI = "bg_media_uri"
private const val KEY_BG_TYPE = "bg_media_type" // "image" | "video" | ""

private val UiBgBase = Color(0xFF0A0A0C)
private val UiSurface = Color(0xFF141418)
private val UiSurfaceAlt = Color(0xFF1C1C22)
private val UiBorder = Color(0xFF2C2C34)
private val UiAccent = Color(0xFFE4E4E7)
private val UiMuted = Color(0xFFA1A1AA)
private val UiSuccess = Color(0xFFD4D4D8)

private data class NavTabItem(
    val id: String,
    val label: String,
    val icon: ImageVector
)

private val TOP_NAV_TABS = listOf(
    NavTabItem("home", "HOME", Icons.Outlined.Home),
    NavTabItem("mods", "MODS", Icons.Outlined.Extension),
    NavTabItem("accounts", "ACCOUNTS", Icons.Outlined.Person),
    NavTabItem("ai", "ASSISTANT", Icons.Outlined.Psychology),
    NavTabItem("settings", "SETTINGS", Icons.Outlined.Settings)
)

@Composable
private fun LocalDashboardBackground(
    mediaUriString: String?,
    mediaType: String
) {
    val context = LocalContext.current
    val parsedUri = remember(mediaUriString) {
        mediaUriString?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
    }

    if (parsedUri != null && mediaType == "video") {
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
                        start()
                    }
                    setOnErrorListener { _, _, _ -> true }
                }
            },
            update = { videoView ->
                if (videoView.tag != parsedUri.toString()) {
                    videoView.tag = parsedUri.toString()
                    videoView.setVideoURI(parsedUri)
                    videoView.setOnPreparedListener { mp ->
                        mp.isLooping = true
                        mp.setVolume(0f, 0f)
                        mp.start()
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
            alpha = 0.35f
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xF20A0A0C),
                        Color(0xCC0A0A0C),
                        Color(0x550A0A0C)
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

    val handleRequestCreateInstance = {
        if (instances.size >= 3 && launcherUser?.isActivated != true) {
            premiumInitialPrompt = "Free accounts are limited to a maximum of 3 instances. Upgrade to unlock unlimited instances."
            showPremiumDialog = true
        } else {
            showAddDialog = true
        }
    }

    var isDownloading by remember { mutableStateOf(false) }
    var downloadTargetName by remember { mutableStateOf("") }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadMessage by remember { mutableStateOf("") }

    val downloader = remember { MinecraftDownloader(context) }

    var isVersionDropdownExpanded by remember { mutableStateOf(false) }
    var isAccountDropdownExpanded by remember { mutableStateOf(false) }

    if (currentTab == "gui_editor") {
        CustomGuiEditorScreen(
            onNavigateBack = { currentTab = "settings" },
            modifier = Modifier.fillMaxSize()
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(UiBgBase)
        ) {
            // 1. TOP BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((48.dp).resp())
                    .background(UiSurface)
                    .padding(horizontal = (14.dp).resp()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape((8.dp).resp()))
                        .clickable {
                            if (currentTab == "home") showAboutDialog = true else currentTab = "home"
                        }
                        .padding(vertical = (4.dp).resp(), horizontal = (4.dp).resp()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy((8.dp).resp())
                ) {
                    Box(
                        modifier = Modifier
                            .size((28.dp).resp())
                            .background(UiSurfaceAlt, RoundedCornerShape((7.dp).resp()))
                            .border(1.dp, UiBorder, RoundedCornerShape((7.dp).resp())),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Terminal,
                            contentDescription = "Workspace",
                            tint = Color.White,
                            modifier = Modifier.size((15.dp).resp())
                        )
                    }
                    Text(
                        text = "WORKSPACE",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = (12.sp).resp(),
                        letterSpacing = (1.sp).resp()
                    )
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = (12.dp).resp())
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy((4.dp).resp())) {
                        TOP_NAV_TABS.forEach { tab ->
                            val isSelected = currentTab == tab.id
                            val tabShape = RoundedCornerShape((7.dp).resp())
                            Row(
                                modifier = Modifier
                                    .clip(tabShape)
                                    .background(
                                        if (isSelected) Color.White else Color.Transparent,
                                        tabShape
                                    )
                                    .clickable { currentTab = tab.id }
                                    .padding(horizontal = (10.dp).resp(), vertical = (6.dp).resp()),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy((5.dp).resp())
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label,
                                    tint = if (isSelected) UiBgBase else UiMuted,
                                    modifier = Modifier.size((14.dp).resp())
                                )
                                Text(
                                    text = tab.label,
                                    color = if (isSelected) UiBgBase else UiMuted,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = (10.sp).resp(),
                                    letterSpacing = (0.4.sp).resp(),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy((8.dp).resp())
                ) {
                    val accounts by AccountManager.accounts.collectAsState()
                    val activeUsername = currentAccount?.username ?: launcherUser?.username ?: tr("dashboard.select_account")
                    val initialLetter = activeUsername.firstOrNull()?.uppercase() ?: "U"

                    Box {
                        val accShape = RoundedCornerShape((8.dp).resp())
                        Row(
                            modifier = Modifier
                                .clip(accShape)
                                .background(UiSurfaceAlt, accShape)
                                .border(1.dp, UiBorder, accShape)
                                .clickable { isAccountDropdownExpanded = true }
                                .padding(horizontal = (8.dp).resp(), vertical = (4.dp).resp()),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy((6.dp).resp())
                        ) {
                            val photo = launcherUser?.photoURL ?: currentAccount?.photoUrl
                            if (!photo.isNullOrBlank()) {
                                NuxNetworkImage(
                                    model = photo,
                                    contentDescription = "Profile",
                                    fallbackInitials = activeUsername,
                                    modifier = Modifier.size((20.dp).resp()),
                                    shape = RoundedCornerShape((5.dp).resp())
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size((20.dp).resp())
                                        .background(Color.White, RoundedCornerShape((5.dp).resp())),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = initialLetter,
                                        color = UiBgBase,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = (10.sp).resp()
                                    )
                                }
                            }

                            Text(
                                text = activeUsername,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = (10.5.sp).resp(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = (95.dp).resp())
                            )

                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = tr("dashboard.switch_account"),
                                tint = UiMuted,
                                modifier = Modifier.size((14.dp).resp())
                            )
                        }

                        DropdownMenu(
                            expanded = isAccountDropdownExpanded,
                            onDismissRequest = { isAccountDropdownExpanded = false },
                            shape = RoundedCornerShape((10.dp).resp()),
                            containerColor = UiSurface,
                            border = BorderStroke(1.dp, UiBorder),
                            modifier = Modifier.widthIn(min = (200.dp).resp())
                        ) {
                            if (accounts.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text(tr("dashboard.no_accounts"), color = UiMuted, fontSize = (11.sp).resp()) },
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
                                                    color = if (isSelected) Color.White else UiAccent,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = (11.5.sp).resp()
                                                )
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Selected",
                                                        tint = UiSuccess,
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

                            HorizontalDivider(color = UiBorder, thickness = 1.dp)

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

                    Box(
                        modifier = Modifier
                            .size((28.dp).resp())
                            .clip(RoundedCornerShape((7.dp).resp()))
                            .background(UiSurfaceAlt)
                            .border(1.dp, UiBorder, RoundedCornerShape((7.dp).resp()))
                            .clickable { showAboutDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "About",
                            tint = UiAccent,
                            modifier = Modifier.size((15.dp).resp())
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(UiBorder)
            )

            // 2. MAIN WORKSPACE STAGE
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
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
                                mediaType = bgMediaType
                            )

                            // Kontrol ganti/reset latar belakang gambar atau video lokal di pojok kanan bawah
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding((16.dp).resp()),
                                horizontalArrangement = Arrangement.spacedBy((8.dp).resp()),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape((7.dp).resp()))
                                        .background(UiSurface.copy(alpha = 0.9f))
                                        .border(1.dp, UiBorder, RoundedCornerShape((7.dp).resp()))
                                        .clickable {
                                            mediaPickerLauncher.launch(arrayOf("image/*", "video/*"))
                                        }
                                        .padding(horizontal = (10.dp).resp(), vertical = (6.dp).resp())
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
                                            text = "BG MEDIA",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = (9.sp).resp(),
                                            letterSpacing = (0.5.sp).resp()
                                        )
                                    }
                                }

                                if (!bgUriString.isNullOrBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape((7.dp).resp()))
                                            .background(UiSurface.copy(alpha = 0.9f))
                                            .border(1.dp, UiBorder, RoundedCornerShape((7.dp).resp()))
                                            .clickable {
                                                bgPrefs.edit().clear().apply()
                                                bgUriString = null
                                                bgMediaType = ""
                                                Toast.makeText(context, "Latar belakang direset ke bawaan", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(horizontal = (8.dp).resp(), vertical = (6.dp).resp())
                                    ) {
                                        Text(
                                            text = "RESET",
                                            color = UiMuted,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = (9.sp).resp()
                                        )
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(start = (24.dp).resp(), end = (24.dp).resp())
                                    .widthIn(max = (370.dp).resp()),
                                verticalArrangement = Arrangement.spacedBy((12.dp).resp())
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy((6.dp).resp())
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size((6.dp).resp())
                                            .background(Color.White, CircleShape)
                                    )
                                    Text(
                                        text = "LOCAL RUNTIME  •  CUSTOM STAGE",
                                        color = UiAccent,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = (9.sp).resp(),
                                        letterSpacing = (0.8.sp).resp()
                                    )
                                }

                                Box {
                                    Column(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape((8.dp).resp()))
                                            .clickable { isVersionDropdownExpanded = true }
                                            .padding(vertical = (2.dp).resp())
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy((8.dp).resp())
                                        ) {
                                            Text(
                                                text = selectedInstance?.name ?: tr("dashboard.select_instance"),
                                                color = Color.White,
                                                fontWeight = FontWeight.Black,
                                                fontSize = (24.sp).resp(),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size((24.dp).resp())
                                                    .background(UiSurfaceAlt, RoundedCornerShape((6.dp).resp()))
                                                    .border(1.dp, UiBorder, RoundedCornerShape((6.dp).resp())),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardArrowDown,
                                                    contentDescription = tr("dashboard.select_version"),
                                                    tint = Color.White,
                                                    modifier = Modifier.size((16.dp).resp())
                                                )
                                            }
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = isVersionDropdownExpanded,
                                        onDismissRequest = { isVersionDropdownExpanded = false },
                                        shape = RoundedCornerShape((10.dp).resp()),
                                        containerColor = UiSurface,
                                        border = BorderStroke(1.dp, UiBorder),
                                        modifier = Modifier.widthIn(min = (250.dp).resp(), max = (330.dp).resp())
                                    ) {
                                        if (instances.isEmpty()) {
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = tr("dashboard.no_instances"),
                                                        color = UiMuted,
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
                                                                    color = if (isSelected) Color.White else UiAccent,
                                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                                    fontSize = (12.sp).resp(),
                                                                    maxLines = 1
                                                                )
                                                                Text(
                                                                    text = "${inst.loader.uppercase()} • ${inst.mcVersion}",
                                                                    color = UiMuted,
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
                                                                        tint = UiSuccess,
                                                                        modifier = Modifier.size((15.dp).resp())
                                                                    )
                                                                }
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size((26.dp).resp())
                                                                        .clip(RoundedCornerShape((6.dp).resp()))
                                                                        .background(UiSurfaceAlt)
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
                                                                        modifier = Modifier.size((14.dp).resp())
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

                                        HorizontalDivider(color = UiBorder, thickness = 1.dp)

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

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy((6.dp).resp()),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (selectedInstance != null) {
                                        val inst = selectedInstance!!
                                        Box(
                                            modifier = Modifier
                                                .background(Color.White, RoundedCornerShape((5.dp).resp()))
                                                .padding(horizontal = (8.dp).resp(), vertical = (3.dp).resp())
                                        ) {
                                            Text(
                                                text = inst.loader.uppercase(),
                                                color = UiBgBase,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = (9.5.sp).resp()
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .background(UiSurface, RoundedCornerShape((5.dp).resp()))
                                                .border(1.dp, UiBorder, RoundedCornerShape((5.dp).resp()))
                                                .padding(horizontal = (8.dp).resp(), vertical = (3.dp).resp())
                                        ) {
                                            Text(
                                                text = inst.mcVersion,
                                                color = Color.White,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = (9.5.sp).resp()
                                            )
                                        }

                                        val activeRuntime = if (inst.javaRuntime != "auto") inst.javaRuntime
                                            else JavaRuntimeManager.getRecommendedRuntime(inst.mcVersion)
                                        Box(
                                            modifier = Modifier
                                                .background(UiSurface, RoundedCornerShape((5.dp).resp()))
                                                .border(1.dp, UiBorder, RoundedCornerShape((5.dp).resp()))
                                                .padding(horizontal = (8.dp).resp(), vertical = (3.dp).resp())
                                        ) {
                                            Text(
                                                text = activeRuntime.replace("jre-", "JAVA "),
                                                color = UiMuted,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = (9.5.sp).resp()
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = tr("dashboard.no_version_selected"),
                                            color = UiMuted,
                                            fontSize = (11.sp).resp()
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height((4.dp).resp()))

                                val inst = selectedInstance
                                val isFullyDownloaded = inst != null && inst.isDownloaded && InstanceManager.isInstanceDownloaded(context, inst)
                                val playBtnShape = RoundedCornerShape((9.dp).resp())

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy((10.dp).resp()),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .height((46.dp).resp())
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
                                            tint = UiBgBase,
                                            modifier = Modifier.size((20.dp).resp())
                                        )
                                        Text(
                                            text = if (inst == null) tr("dashboard.play_new_instance") else if (isFullyDownloaded) tr("dashboard.play_button") else tr("dashboard.play_download"),
                                            color = UiBgBase,
                                            fontWeight = FontWeight.Black,
                                            fontSize = (14.sp).resp(),
                                            letterSpacing = (0.8.sp).resp()
                                        )
                                    }

                                    if (inst != null) {
                                        Box(
                                            modifier = Modifier
                                                .size((46.dp).resp())
                                                .clip(playBtnShape)
                                                .background(UiSurface, playBtnShape)
                                                .border(1.dp, UiBorder, playBtnShape)
                                                .clickable { instanceToEdit = inst },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Tune,
                                                contentDescription = "Configure Instance",
                                                tint = Color.White,
                                                modifier = Modifier.size((19.dp).resp())
                                            )
                                        }
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
