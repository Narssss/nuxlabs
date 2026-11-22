package com.israadev.nuxlauncher.ui.screens

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.israadev.nuxlauncher.core.account.AccountManager
import com.israadev.nuxlauncher.core.account.elyby.ElyByAuthService
import com.israadev.nuxlauncher.core.account.elyby.ElyByLoginState
import com.israadev.nuxlauncher.core.account.microsoft.MicrosoftAuthService
import com.israadev.nuxlauncher.core.account.microsoft.MicrosoftDeviceCode
import com.israadev.nuxlauncher.core.lang.tr
import com.israadev.nuxlauncher.core.lang.trStr
import com.israadev.nuxlauncher.core.models.UserAccount
import com.israadev.nuxlauncher.core.skin.SkinUtils
import com.israadev.nuxlauncher.ui.components.*
import com.israadev.nuxlauncher.ui.dialogs.NuxMicrosoftAuthDialog
import com.israadev.nuxlauncher.ui.dialogs.NuxPremiumDialog
import com.israadev.nuxlauncher.ui.dialogs.NuxWardrobeDialog
import kotlinx.coroutines.launch
import java.util.UUID

// Token Desain Bell Launcher (Monochrome Obsidian Glass + Crisp White Pills)
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

private fun safeTr(key: String, fallback: String): String {
    val translated = trStr(key)
    return if (translated.isBlank() || translated == key) fallback else translated
}

@Composable
fun AccountsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val accounts by AccountManager.accounts.collectAsState()
    val currentAccount by AccountManager.currentAccount.collectAsState()
    val launcherUser by AccountManager.launcherUser.collectAsState()

    var showAddAccountDialog by remember { mutableStateOf(false) }
    var accountToDelete by remember { mutableStateOf<UserAccount?>(null) }
    var accountForWardrobe by remember { mutableStateOf<UserAccount?>(null) }
    var showPremiumDialog by remember { mutableStateOf(false) }
    var premiumInitialPrompt by remember { mutableStateOf<String?>(null) }

    // 3D Player Model Preview
    val leftPlayerSkin = remember { PlayerSkin(context) }
    var leftPageFinished by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            leftPlayerSkin.destroy()
        }
    }

    val activeType = currentAccount?.safeAccountType ?: "offline"
    val activeTypeLabel = when (activeType) {
        "microsoft" -> "MICROSOFT"
        "elyby" -> "ELY.BY"
        else -> "OFFLINE"
    }

    // =========================================================================
    // MAIN 2-COLUMN STUDIO & ROSTER LAYOUT (BELL MONOCHROME GLASS)
    // =========================================================================
    Row(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ---------------------------------------------------------------------
        // KOLOM KIRI: 3D ACTIVE CHARACTER STUDIO SHOWCASE
        // ---------------------------------------------------------------------
        Box(
            modifier = Modifier
                .weight(0.95f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(14.dp))
                .background(BellGlassSidebar)
                .border(1.dp, BellBorder, RoundedCornerShape(14.dp))
                .padding(12.dp)
        ) {
            if (currentAccount != null) {
                val acc = currentAccount!!
                val isSkinChangeAllowed = acc.safeAccountType == "microsoft" || acc.safeAccountType == "offline"

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header status di dalam kartu 3D
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(BellPillWhite, CircleShape)
                            )
                            Text(
                                text = "ACTIVE PROFILE",
                                color = BellTextPrimary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.7.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(BellPillWhite)
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = activeTypeLabel,
                                color = BellInkDark,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    // 3D Interactive Player Showcase
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { ctx ->
                                leftPlayerSkin.loadWebView(ctx) {
                                    leftPageFinished = true
                                    leftPlayerSkin.loadAccount(acc)
                                    leftPlayerSkin.startAnim("NewIdle", 0.7f)
                                    leftPlayerSkin.setAutoRotate(true, 0.6f)
                                    leftPlayerSkin.setCamera(20, 8, 44)
                                }
                            },
                            update = {
                                if (leftPageFinished) {
                                    leftPlayerSkin.loadAccount(acc)
                                }
                            }
                        )
                    }

                    LaunchedEffect(acc.id, acc.customSkinPath, acc.customCapePath, acc.skinModel, leftPageFinished) {
                        if (leftPageFinished) {
                            val updated = SkinUtils.ensureSkinAndCapeCached(context, acc)
                            leftPlayerSkin.loadAccount(updated)
                        }
                    }

                    // Info Username & Tombol Wardrobe
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = acc.username,
                                color = BellTextPrimary,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = when (acc.safeAccountType) {
                                    "microsoft" -> "Official Microsoft Account"
                                    "elyby" -> "Ely.by Skin & Auth Account"
                                    else -> "Local Offline Profile"
                                },
                                color = BellTextSecondary,
                                fontSize = 9.5.sp
                            )
                        }

                        // Tombol Wardrobe Langsung di Bawah Model 3D
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSkinChangeAllowed) BellGlassCard else Color(0x12FFFFFF))
                                .border(
                                    1.dp,
                                    if (isSkinChangeAllowed) BellBorderActive else BellBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    if (isSkinChangeAllowed) {
                                        accountForWardrobe = acc
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Skins & Capes for Ely.by accounts are managed directly at https://account.ely.by",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Checkroom,
                                    contentDescription = null,
                                    tint = if (isSkinChangeAllowed) BellTextPrimary else BellTextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "CUSTOMIZE SKIN & CAPE",
                                    color = if (isSkinChangeAllowed) BellTextPrimary else BellTextMuted,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }
            } else {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PersonOff,
                        contentDescription = null,
                        tint = BellTextMuted,
                        modifier = Modifier.size(38.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "NO PROFILE SELECTED",
                        color = BellTextPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        letterSpacing = 0.6.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Add or select an account on the right to preview your character.",
                        color = BellTextSecondary,
                        fontSize = 9.5.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        // ---------------------------------------------------------------------
        // KOLOM KANAN: ACCOUNT ROSTER & ADD ACTION
        // ---------------------------------------------------------------------
        Box(
            modifier = Modifier
                .weight(1.35f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(14.dp))
                .background(BellGlassCard)
                .border(1.dp, BellBorder, RoundedCornerShape(14.dp))
                .padding(12.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Roster + Tombol Putih Solid + ADD ACCOUNT
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SAVED ACCOUNTS (${accounts.size})",
                            color = BellTextPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            letterSpacing = 0.7.sp
                        )
                        Text(
                            text = "Tap any profile below to switch active session",
                            color = BellTextSecondary,
                            fontSize = 9.5.sp
                        )
                    }

                    // Tombol Putih Solid gaya UNDUH / PLAY di Dashboard
                    Box(
                        modifier = Modifier
                            .height(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(BellPillWhite)
                            .clickable {
                                if (accounts.size >= 2 && launcherUser?.isActivated != true) {
                                    premiumInitialPrompt = "Free users are limited to 2 saved accounts. Upgrade to Bell Premium to save and switch unlimited accounts!"
                                    showPremiumDialog = true
                                } else {
                                    showAddAccountDialog = true
                                }
                            }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PersonAdd,
                                contentDescription = null,
                                tint = BellInkDark,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "ADD ACCOUNT",
                                color = BellInkDark,
                                fontWeight = FontWeight.Black,
                                fontSize = 9.sp,
                                letterSpacing = 0.6.sp
                            )
                        }
                    }
                }

                // Accounts List
                if (accounts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No accounts added yet",
                                color = BellTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        items(accounts, key = { "${it.safeAccountType}_${it.id}_${it.username}" }) { acc ->
                            val isCurrent = acc.id == currentAccount?.id ||
                                    (acc.username == currentAccount?.username && acc.safeAccountType == currentAccount?.safeAccountType)

                            val badgeTxt = when (acc.safeAccountType) {
                                "microsoft" -> "MICROSOFT"
                                "elyby" -> "ELY.BY"
                                else -> "OFFLINE"
                            }

                            val itemShape = RoundedCornerShape(10.dp)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(itemShape)
                                    .background(if (isCurrent) Color(0x26FFFFFF) else BellGlassInput)
                                    .border(
                                        width = 1.dp,
                                        color = if (isCurrent) BellPillWhite else BellBorder,
                                        shape = itemShape
                                    )
                                    .clickable {
                                        AccountManager.selectAccount(acc)
                                        coroutineScope.launch {
                                            val updated = SkinUtils.ensureSkinAndCapeCached(context, acc)
                                            leftPlayerSkin.loadAccount(updated)
                                        }
                                    }
                                    .padding(horizontal = 11.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Avatar Head
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0x1AFFFFFF))
                                            .border(1.dp, BellBorder, RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        NuxAccountAvatar(
                                            account = acc,
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(RoundedCornerShape(6.dp)),
                                            fallbackInitials = acc.username.take(2).uppercase()
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = acc.username,
                                                color = BellTextPrimary,
                                                fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Bold,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(if (isCurrent) BellPillWhite else Color(0x1FFFFFFF))
                                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = badgeTxt,
                                                    color = if (isCurrent) BellInkDark else BellTextSecondary,
                                                    fontSize = 7.5.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        val shortUuid = if (acc.uuid.length > 10) "${acc.uuid.take(6)}...${acc.uuid.takeLast(4)}" else acc.uuid
                                        Text(
                                            text = shortUuid,
                                            color = BellTextMuted,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                // Actions
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (isCurrent) {
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .background(BellPillWhite, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = BellInkDark,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .height(26.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(BellGlassCard)
                                                .border(1.dp, BellBorder, RoundedCornerShape(6.dp))
                                                .clickable {
                                                    AccountManager.selectAccount(acc)
                                                    coroutineScope.launch {
                                                        val updated = SkinUtils.ensureSkinAndCapeCached(context, acc)
                                                        leftPlayerSkin.loadAccount(updated)
                                                    }
                                                }
                                                .padding(horizontal = 9.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "USE",
                                                color = BellTextPrimary,
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }

                                    // Wardrobe icon
                                    val isSkinChangeAllowed = acc.safeAccountType == "microsoft" || acc.safeAccountType == "offline"
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(BellGlassCard)
                                            .border(1.dp, BellBorder, RoundedCornerShape(6.dp))
                                            .clickable {
                                                if (isSkinChangeAllowed) {
                                                    accountForWardrobe = acc
                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        "Skins & Capes for Ely.by accounts are managed directly at https://account.ely.by",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Checkroom,
                                            contentDescription = "Wardrobe",
                                            tint = if (isSkinChangeAllowed) BellTextSecondary else BellTextMuted,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }

                                    // Delete icon
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(BellGlassCard)
                                            .border(1.dp, BellBorder, RoundedCornerShape(6.dp))
                                            .clickable { accountToDelete = acc },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.DeleteOutline,
                                            contentDescription = "Delete",
                                            tint = BellDanger,
                                            modifier = Modifier.size(14.dp)
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

    // =========================================================================
    // MODAL DIALOG: WARDROBE / CUSTOM SKIN & CAPE
    // =========================================================================
    if (accountForWardrobe != null) {
        NuxWardrobeDialog(
            account = accountForWardrobe!!,
            onDismissRequest = { accountForWardrobe = null },
            onSaved = { updated ->
                accountForWardrobe = null
                leftPlayerSkin.loadAccount(updated)
            }
        )
    }

    // =========================================================================
    // MODAL DIALOG: TAMBAH AKUN (BELL MONOCHROME THEME)
    // =========================================================================
    if (showAddAccountDialog) {
        AddAccountModal(
            onDismiss = { showAddAccountDialog = false },
            onAccountAdded = { newAcc ->
                AccountManager.addAccount(context, newAcc)
                Toast.makeText(context, "Account ${newAcc.username} added successfully!", Toast.LENGTH_SHORT).show()
                showAddAccountDialog = false
            }
        )
    }

    // =========================================================================
    // CONFIRM DELETE DIALOG
    // =========================================================================
    if (accountToDelete != null) {
        val target = accountToDelete!!
        NuxDialog(
            onDismissRequest = { accountToDelete = null },
            modifier = Modifier
                .fillMaxWidth(0.55f)
                .wrapContentHeight()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(BellGlassElevated)
                    .border(1.dp, BellBorder, RoundedCornerShape(16.dp))
                    .padding(18.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(BellDanger.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = BellDanger,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = safeTr("accounts.delete_confirm_title", "DELETE ACCOUNT?").uppercase(),
                        color = BellTextPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Remove \"${target.username}\" from your saved profiles?",
                        color = BellTextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BellGlassInput)
                                .border(1.dp, BellBorder, RoundedCornerShape(8.dp))
                                .clickable { accountToDelete = null },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("CANCEL", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = BellTextPrimary)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BellDanger)
                                .clickable {
                                    AccountManager.removeAccount(context, target)
                                    Toast.makeText(context, "Account deleted", Toast.LENGTH_SHORT).show()
                                    accountToDelete = null
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("DELETE", fontWeight = FontWeight.Black, fontSize = 10.sp, color = BellInkDark)
                        }
                    }
                }
            }
        }
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

/**
 * High-End Landscape Modal for Adding Accounts (Bell Monochrome Theme)
 */
@Composable
private fun AddAccountModal(
    onDismiss: () -> Unit,
    onAccountAdded: (UserAccount) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf("offline") } // "offline", "microsoft", "elyby"

    // Offline State
    var offlineName by remember { mutableStateOf("") }

    // Microsoft State
    var msDeviceCode by remember { mutableStateOf<MicrosoftDeviceCode?>(null) }
    var msErrorMsg by remember { mutableStateOf<String?>(null) }
    var isMsLoading by remember { mutableStateOf(false) }

    // Ely.by State
    val elyLoginState by ElyByAuthService.loginState.collectAsState()

    LaunchedEffect(elyLoginState) {
        val state = elyLoginState
        if (state is ElyByLoginState.Success) {
            Toast.makeText(context, "Account ${state.account.username} added successfully!", Toast.LENGTH_SHORT).show()
            ElyByAuthService.resetState()
            onDismiss()
        }
    }

    NuxDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth(0.74f)
            .wrapContentHeight(),
        fillMaxHeight = false
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(BellGlassElevated)
                .border(1.dp, BellBorder, RoundedCornerShape(16.dp))
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Modal Header (Tanpa raw translation key)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = safeTr("accounts.dialog_add_title", "ADD MINECRAFT ACCOUNT").uppercase(),
                            color = BellTextPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Select your preferred Minecraft profile provider",
                            color = BellTextSecondary,
                            fontSize = 10.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(BellGlassInput, CircleShape)
                            .border(1.dp, BellBorder, CircleShape)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Close",
                            tint = BellTextPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3 Provider Selector Cards (Monochrome White Active State)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProviderCard(
                        title = safeTr("accounts.tab_offline", "OFFLINE").uppercase(),
                        badge = "FREE",
                        description = "Play offline without login",
                        isSelected = selectedTab == "offline",
                        icon = Icons.Outlined.Person,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = "offline" }
                    )

                    ProviderCard(
                        title = safeTr("accounts.tab_microsoft", "MICROSOFT").uppercase(),
                        badge = "OFFICIAL",
                        description = "Official Mojang account",
                        isSelected = selectedTab == "microsoft",
                        icon = Icons.Outlined.Language,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = "microsoft" }
                    )

                    ProviderCard(
                        title = safeTr("accounts.tab_elyby", "ELY.BY").uppercase(),
                        badge = "BROWSER",
                        description = "Online skins & capes",
                        isSelected = selectedTab == "elyby",
                        icon = Icons.Outlined.Public,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = "elyby" }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Interactive Action Area
                when (selectedTab) {
                    // =========================================
                    // TAB 1: OFFLINE
                    // =========================================
                    "offline" -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(BellGlassInput, RoundedCornerShape(10.dp))
                                    .border(1.dp, BellBorderActive, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (offlineName.trim().isNotBlank()) {
                                    NuxNetworkImage(
                                        model = "https://mc-heads.net/avatar/${offlineName.trim()}/80",
                                        contentDescription = offlineName,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(7.dp)),
                                        fallbackInitials = offlineName.trim().take(2).uppercase(),
                                        shape = RoundedCornerShape(7.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.Person,
                                        contentDescription = null,
                                        tint = BellTextPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Box(modifier = Modifier.weight(1f)) {
                                NuxTextField(
                                    value = offlineName,
                                    onValueChange = { offlineName = it },
                                    placeholder = safeTr("accounts.offline_username_placeholder", "Enter offline username...")
                                )
                            }

                            val canSave = offlineName.trim().isNotBlank()
                            Box(
                                modifier = Modifier
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(if (canSave) BellPillWhite else Color(0x26FFFFFF))
                                    .clickable(enabled = canSave) {
                                        val cleanName = offlineName.trim()
                                        val offlineUuid = UUID.nameUUIDFromBytes("OfflinePlayer:$cleanName".toByteArray(Charsets.UTF_8))
                                            .toString()
                                            .replace("-", "")
                                        val newAcc = UserAccount(
                                            id = "offline_${System.currentTimeMillis()}",
                                            username = cleanName,
                                            uuid = offlineUuid,
                                            accessToken = "0",
                                            isOffline = true,
                                            accountType = "offline"
                                        )
                                        onAccountAdded(newAcc)
                                    }
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = null,
                                        tint = if (canSave) BellInkDark else BellTextMuted,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = safeTr("accounts.btn_save_account", "SAVE ACCOUNT").uppercase(),
                                        color = if (canSave) BellInkDark else BellTextMuted,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }
                    }

                    // =========================================
                    // TAB 2: MICROSOFT
                    // =========================================
                    "microsoft" -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Microsoft / Xbox Live Authorization",
                                    color = BellTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Secure web authorization window opens directly inside the launcher.",
                                    color = BellTextSecondary,
                                    fontSize = 10.sp
                                )
                                if (msErrorMsg != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = msErrorMsg!!,
                                        color = BellDanger,
                                        fontSize = 10.sp,
                                        lineHeight = 13.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Box(
                                modifier = Modifier
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(if (!isMsLoading) BellPillWhite else Color(0x33FFFFFF))
                                    .clickable(enabled = !isMsLoading) {
                                        isMsLoading = true
                                        msErrorMsg = null
                                        scope.launch {
                                            val res = MicrosoftAuthService.startDeviceCodeFlow()
                                            isMsLoading = false
                                            res.fold(
                                                onSuccess = { dc -> msDeviceCode = dc },
                                                onFailure = { err ->
                                                    msErrorMsg = err.message ?: "Failed to start Microsoft authorization."
                                                }
                                            )
                                        }
                                    }
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (isMsLoading) {
                                        CircularProgressIndicator(modifier = Modifier.size(12.dp), color = BellInkDark, strokeWidth = 2.dp)
                                        Text("CONNECTING...", fontWeight = FontWeight.Bold, fontSize = 9.5.sp, color = BellInkDark)
                                    } else {
                                        Icon(imageVector = Icons.Outlined.Language, contentDescription = null, tint = BellInkDark, modifier = Modifier.size(15.dp))
                                        Text("SIGN IN MICROSOFT", fontWeight = FontWeight.Black, fontSize = 9.5.sp, color = BellInkDark)
                                    }
                                }
                            }
                        }
                    }

                    // =========================================
                    // TAB 3: ELY.BY
                    // =========================================
                    "elyby" -> {
                        when (val state = elyLoginState) {
                            is ElyByLoginState.WaitingForApproval -> {
                                val dc = state.deviceCode
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(12.dp),
                                                color = BellTextPrimary,
                                                strokeWidth = 1.5.dp
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = state.statusMsg.ifBlank { "Waiting for authorization in Ely.by browser..." },
                                                color = BellTextPrimary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "Code: ${dc.userCode}",
                                                color = BellTextPrimary,
                                                fontSize = 13.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Black
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "(Copy)",
                                                color = BellTextSecondary,
                                                fontSize = 10.sp,
                                                modifier = Modifier.clickable {
                                                    clipboardManager.setText(AnnotatedString(dc.userCode))
                                                    Toast.makeText(context, "Code copied", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .height(34.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(BellPillWhite)
                                                .clickable {
                                                    ElyByAuthService.openBrowser(context, dc.verificationUrlWithCode)
                                                }
                                                .padding(horizontal = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("REOPEN", fontWeight = FontWeight.Black, fontSize = 9.5.sp, color = BellInkDark)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .height(34.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(BellGlassInput)
                                                .border(1.dp, BellBorder, RoundedCornerShape(8.dp))
                                                .clickable { ElyByAuthService.cancelLogin() }
                                                .padding(horizontal = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("CANCEL", fontWeight = FontWeight.Bold, fontSize = 9.5.sp, color = BellTextPrimary)
                                        }
                                    }
                                }
                            }
                            else -> {
                                val isStarting = state is ElyByLoginState.Starting
                                val errMsg = (state as? ElyByLoginState.Error)?.message

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Ely.by Account Authorization",
                                            color = BellTextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Browser will open automatically to authorize your Ely.by account.",
                                            color = BellTextSecondary,
                                            fontSize = 10.sp
                                        )
                                        if (errMsg != null) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(text = errMsg, color = BellDanger, fontSize = 10.sp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Box(
                                        modifier = Modifier
                                            .height(38.dp)
                                            .clip(RoundedCornerShape(9.dp))
                                            .background(if (!isStarting) BellPillWhite else Color(0x33FFFFFF))
                                            .clickable(enabled = !isStarting) {
                                                ElyByAuthService.startLogin(context)
                                            }
                                            .padding(horizontal = 14.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            if (isStarting) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(12.dp),
                                                    color = BellInkDark,
                                                    strokeWidth = 2.dp
                                                )
                                                Text("CONNECTING...", fontWeight = FontWeight.Bold, fontSize = 9.5.sp, color = BellInkDark)
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Outlined.OpenInBrowser,
                                                    contentDescription = null,
                                                    tint = BellInkDark,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Text("OPEN ELY.BY BROWSER", fontWeight = FontWeight.Black, fontSize = 9.5.sp, color = BellInkDark)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (msDeviceCode != null) {
        NuxMicrosoftAuthDialog(
            deviceCode = msDeviceCode!!,
            onDismiss = { msDeviceCode = null },
            onSuccess = { acc ->
                msDeviceCode = null
                onAccountAdded(acc)
            },
            onError = { err ->
                msDeviceCode = null
                msErrorMsg = err
            }
        )
    }
}

/**
 * Monochrome Provider Selector Card (Bell Launcher Style)
 */
@Composable
private fun ProviderCard(
    title: String,
    badge: String,
    description: String,
    isSelected: Boolean,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(11.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (isSelected) BellPillWhite else BellGlassInput)
            .border(
                width = 1.dp,
                color = if (isSelected) BellPillWhite else BellBorder,
                shape = shape
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 9.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) BellInkDark else BellTextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = title,
                        color = if (isSelected) BellInkDark else BellTextPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(if (isSelected) Color(0x1F09090B) else Color(0x1AFFFFFF))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badge,
                        color = if (isSelected) BellInkDark else BellTextSecondary,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = description,
                color = if (isSelected) BellInkDark.copy(alpha = 0.72f) else BellTextMuted,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun NuxAccountAvatar(
    account: UserAccount,
    modifier: Modifier = Modifier,
    fallbackInitials: String = account.username.take(2).uppercase()
) {
    val context = LocalContext.current
    val headBitmapState = produceState<Bitmap?>(
        initialValue = null,
        account.id,
        account.customSkinPath,
        account.safeAccountType
    ) {
        value = SkinUtils.getAccountHeadBitmap(context, account, targetSize = 64)
    }

    val headBitmap = headBitmapState.value

    if (headBitmap != null) {
        Image(
            bitmap = headBitmap.asImageBitmap(),
            contentDescription = account.username,
            modifier = modifier,
            filterQuality = FilterQuality.None
        )
    } else {
        Box(
            modifier = modifier.background(BellGlassCard),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = fallbackInitials,
                color = BellTextPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
