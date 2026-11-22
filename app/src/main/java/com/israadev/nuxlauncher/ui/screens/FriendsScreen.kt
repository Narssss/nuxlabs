package com.israadev.nuxlauncher.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.israadev.nuxlauncher.core.account.AccountManager
import com.israadev.nuxlauncher.core.lang.NuxLanguageManager
import com.israadev.nuxlauncher.core.lang.tr
import com.israadev.nuxlauncher.core.social.*
import com.israadev.nuxlauncher.ui.components.NuxNetworkImage
import com.israadev.nuxlauncher.ui.components.NuxUserAvatar
import com.israadev.nuxlauncher.ui.components.NuxUserBadge
import com.israadev.nuxlauncher.ui.dialogs.NuxPremiumDialog
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Token Desain Bell Launcher (Monochrome Obsidian Glass + Crisp White)
private val BellBgCanvas = Color(0xFF07070A)
private val BellGlassSidebar = Color(0xD90D0D11)
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
private val BellSuccess = Color(0xFF10B981)
private val BellInGame = Color(0xFF06B6D4)
private val BellDanger = Color(0xFFF87171)

@Composable
fun FriendsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val langVersion by NuxLanguageManager.languageVersion.collectAsState()
    val scope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        NuxSocialManager.startSync()
        onDispose {}
    }

    val friends by NuxSocialManager.friends.collectAsState()
    val activeFriend by NuxSocialManager.activeChatFriend.collectAsState()
    val chatMessages by NuxSocialManager.chatMessages.collectAsState()
    val isFriendTyping by NuxSocialManager.isFriendTyping.collectAsState()
    val voiceRooms by NuxSocialManager.voiceRooms.collectAsState()
    val activeVoiceRoom by NuxSocialManager.activeVoiceRoom.collectAsState()
    val voiceMessages by NuxSocialManager.voiceMessages.collectAsState()
    val searchResults by NuxSocialManager.searchResults.collectAsState()
    val isSearching by NuxSocialManager.isSearching.collectAsState()
    val onlineRecommendations by NuxSocialManager.onlineRecommendations.collectAsState()
    val isLoadingRecommendations by NuxSocialManager.isLoadingRecommendations.collectAsState()
    val launcherUser by AccountManager.launcherUser.collectAsState()

    var selectedTab by remember { mutableStateOf("friends") } // "room_chat", "friends", "rooms", "requests"
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(activeVoiceRoom?.id) {
        if (activeVoiceRoom != null) {
            NuxSocialManager.closeChat()
            selectedTab = "room_chat"
        } else if (selectedTab == "room_chat") {
            selectedTab = "friends"
        }
    }

    var showAddFriendDialog by remember { mutableStateOf(false) }
    var showCreateRoomDialog by remember { mutableStateOf(false) }
    var showPremiumDialog by remember { mutableStateOf(false) }
    var premiumInitialPrompt by remember { mutableStateOf<String?>(null) }
    var roomToJoinWithPassword by remember { mutableStateOf<NuxVoiceRoom?>(null) }
    var replyingToMessage by remember { mutableStateOf<NuxChatMessage?>(null) }

    val openAddFriendDialogFresh: () -> Unit = {
        scope.launch {
            NuxSocialManager.fetchOnlineRecommendations(50)
        }
        showAddFriendDialog = true
    }

    val acceptedFriends = remember(friends) { friends.filter { it.isAccepted } }
    val pendingReceived = remember(friends) { friends.filter { it.isPendingReceived } }
    val pendingSent = remember(friends) { friends.filter { it.isPendingSent } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BellBgCanvas)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        // =====================================================================
        // 1. TOP HEADER BAR (Bell Launcher Obsidian Glass)
        // =====================================================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .height(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(BellGlassSidebar)
                        .border(1.dp, BellBorder, RoundedCornerShape(8.dp))
                        .clickable { onNavigateBack() }
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = tr("common.back"),
                            tint = BellTextPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "DASHBOARD",
                            color = BellTextPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 9.5.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "SOCIAL HUB",
                    color = BellTextPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    letterSpacing = 0.8.sp
                )
            }

            // Top Action Buttons: Find Friends & Create Room
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .height(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(BellGlassSidebar)
                        .border(1.dp, BellBorder, RoundedCornerShape(8.dp))
                        .clickable { openAddFriendDialogFresh() }
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, tint = BellTextPrimary, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("+ FIND FRIENDS", color = BellTextPrimary, fontWeight = FontWeight.Bold, fontSize = 9.5.sp, letterSpacing = 0.4.sp)
                    }
                }

                Box(
                    modifier = Modifier
                        .height(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(BellPillWhite)
                        .clickable {
                            if (launcherUser?.isActivated != true) {
                                premiumInitialPrompt = "Real-time voice rooms (LiveKit WebRTC) are exclusive to Bell Premium. Upgrade your account to create and join voice rooms!"
                                showPremiumDialog = true
                            } else {
                                showCreateRoomDialog = true
                            }
                        }
                        .padding(horizontal = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Mic, contentDescription = null, tint = BellInkDark, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("+ CREATE ROOM", color = BellInkDark, fontWeight = FontWeight.Black, fontSize = 9.5.sp, letterSpacing = 0.4.sp)
                    }
                }
            }
        }

        // =====================================================================
        // 2. MAIN 2-COLUMN STAGE
        // =====================================================================
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // -------------------------------------------------------------
            // A. LEFT PANEL: DIRECTORY CARD
            // -------------------------------------------------------------
            Column(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(BellGlassSidebar)
                    .border(1.dp, BellBorder, RoundedCornerShape(12.dp))
                    .padding(10.dp)
            ) {
                if (activeVoiceRoom != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        PillTab(
                            title = "CHAT",
                            count = voiceMessages.size,
                            isSelected = selectedTab == "room_chat",
                            badgeColor = BellSuccess,
                            onClick = { selectedTab = "room_chat" },
                            modifier = Modifier.weight(1.1f)
                        )
                        PillTab(
                            title = "FRIENDS",
                            count = acceptedFriends.size,
                            isSelected = selectedTab == "friends",
                            onClick = { selectedTab = "friends" },
                            modifier = Modifier.weight(1f)
                        )
                        PillTab(
                            title = "VOICE",
                            count = voiceRooms.size,
                            isSelected = selectedTab == "rooms",
                            onClick = { selectedTab = "rooms" },
                            modifier = Modifier.weight(1f)
                        )
                        PillTab(
                            title = "REQ",
                            count = pendingReceived.size,
                            isSelected = selectedTab == "requests",
                            badgeColor = if (pendingReceived.isNotEmpty()) BellDanger else null,
                            onClick = { selectedTab = "requests" },
                            modifier = Modifier.weight(0.9f)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        PillTab(
                            title = "FRIENDS",
                            count = acceptedFriends.size,
                            isSelected = selectedTab == "friends",
                            onClick = { selectedTab = "friends" },
                            modifier = Modifier.weight(1f)
                        )
                        PillTab(
                            title = "VOICE",
                            count = voiceRooms.size,
                            isSelected = selectedTab == "rooms",
                            onClick = { selectedTab = "rooms" },
                            modifier = Modifier.weight(1f)
                        )
                        PillTab(
                            title = "REQUESTS",
                            count = pendingReceived.size,
                            isSelected = selectedTab == "requests",
                            badgeColor = if (pendingReceived.isNotEmpty()) BellDanger else null,
                            onClick = { selectedTab = "requests" },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (selectedTab == "room_chat" && activeVoiceRoom != null) {
                    VoiceRoomChatPanel(
                        room = activeVoiceRoom!!,
                        messages = voiceMessages,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    BellCompactSearchBar(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            if (selectedTab == "friends" && it.isNotBlank()) {
                                scope.launch { NuxSocialManager.searchUser(it) }
                            }
                        },
                        placeholder = if (selectedTab == "rooms") "Search voice rooms..." else "Search friends..."
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(modifier = Modifier.weight(1f)) {
                        when (selectedTab) {
                            "friends" -> {
                                val filtered = remember(acceptedFriends, searchQuery) {
                                    if (searchQuery.isBlank()) acceptedFriends
                                    else acceptedFriends.filter { it.username.contains(searchQuery, ignoreCase = true) }
                                }

                                if (filtered.isEmpty()) {
                                    EmptyIndicator(
                                        icon = Icons.Outlined.Group,
                                        title = if (searchQuery.isNotBlank()) "No results found" else "No Friends Yet",
                                        subtitle = if (searchQuery.isNotBlank()) "No friends match \"$searchQuery\""
                                        else "Use \"+ FIND FRIENDS\" above to add friends!"
                                    )
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        itemsIndexed(filtered, key = { index, friend -> "${friend.uid}_$index" }) { _, friend ->
                                            FriendItem(
                                                friend = friend,
                                                isSelected = activeFriend?.uid == friend.uid,
                                                onClick = { NuxSocialManager.openChat(friend) }
                                            )
                                        }
                                    }
                                }
                            }

                            "rooms" -> {
                                val filtered = remember(voiceRooms, searchQuery) {
                                    if (searchQuery.isBlank()) voiceRooms
                                    else voiceRooms.filter { it.name.contains(searchQuery, ignoreCase = true) }
                                }

                                if (filtered.isEmpty()) {
                                    EmptyIndicator(
                                        icon = Icons.Outlined.RecordVoiceOver,
                                        title = "No Voice Rooms",
                                        subtitle = "Click \"+ CREATE ROOM\" at top right to start a voice room!"
                                    )
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        itemsIndexed(filtered, key = { index, room -> "${room.id}_$index" }) { _, room ->
                                            VoiceRoomItem(
                                                room = room,
                                                isJoined = activeVoiceRoom?.id == room.id,
                                                onJoin = {
                                                    if (launcherUser?.isActivated != true) {
                                                        premiumInitialPrompt = "Multiplayer Voice Rooms (LiveKit WebRTC) are exclusive to Bell Premium. Upgrade your account to voice chat with friends!"
                                                        showPremiumDialog = true
                                                        return@VoiceRoomItem
                                                    }
                                                    if (room.isLocked) {
                                                        roomToJoinWithPassword = room
                                                    } else {
                                                        scope.launch {
                                                            val res = NuxSocialManager.joinVoiceRoom(room)
                                                            if (res.isFailure) {
                                                                Toast.makeText(context, res.exceptionOrNull()?.message ?: "Failed to join", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            "requests" -> {
                                if (pendingReceived.isEmpty() && pendingSent.isEmpty()) {
                                    EmptyIndicator(
                                        icon = Icons.Outlined.MarkEmailRead,
                                        title = "No Requests",
                                        subtitle = "Incoming friend requests and pending invites will appear here."
                                    )
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (pendingReceived.isNotEmpty()) {
                                            item {
                                                Text(
                                                    text = "INCOMING REQUESTS (${pendingReceived.size})",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black,
                                                    letterSpacing = 0.6.sp,
                                                    color = BellTextSecondary,
                                                    modifier = Modifier.padding(vertical = 2.dp, horizontal = 4.dp)
                                                )
                                            }
                                            itemsIndexed(pendingReceived, key = { index, req -> "${req.uid}_$index" }) { _, req ->
                                                RequestReceivedItem(
                                                    friend = req,
                                                    onAccept = { scope.launch { NuxSocialManager.acceptFriendRequest(req.uid) } },
                                                    onReject = { scope.launch { NuxSocialManager.removeFriend(req.uid) } }
                                                )
                                            }
                                        }

                                        if (pendingSent.isNotEmpty()) {
                                            item {
                                                Text(
                                                    text = "PENDING INVITES (${pendingSent.size})",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black,
                                                    letterSpacing = 0.6.sp,
                                                    color = BellTextMuted,
                                                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp, start = 4.dp)
                                                )
                                            }
                                            itemsIndexed(pendingSent, key = { index, req -> "${req.uid}_$index" }) { _, req ->
                                                RequestSentItem(
                                                    friend = req,
                                                    onCancel = { scope.launch { NuxSocialManager.removeFriend(req.uid) } }
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

            // -------------------------------------------------------------
            // B. RIGHT PANEL: INTERACTIVE STAGE (Voice Room / Chat / Empty)
            // -------------------------------------------------------------
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(BellGlassCard)
                    .border(1.dp, BellBorder, RoundedCornerShape(12.dp))
            ) {
                when {
                    activeVoiceRoom != null -> {
                        VoiceRoomView(
                            room = activeVoiceRoom!!,
                            onLeave = { NuxSocialManager.leaveVoiceRoom() }
                        )
                    }

                    activeFriend != null -> {
                        DirectChatView(
                            friend = activeFriend!!,
                            messages = chatMessages,
                            isFriendTyping = isFriendTyping,
                            replyingTo = replyingToMessage,
                            onCancelReply = { replyingToMessage = null },
                            onReplyMessage = { msg -> replyingToMessage = msg },
                            onCloseChat = { NuxSocialManager.closeChat() },
                            onStartVoiceWithFriend = {
                                scope.launch {
                                    val res = NuxSocialManager.createVoiceRoom(
                                        name = "Room: ${activeFriend!!.username}",
                                        password = "",
                                        maxUsers = 5
                                    )
                                    if (res.isSuccess) {
                                        selectedTab = "rooms"
                                    }
                                }
                            }
                        )
                    }

                    else -> {
                        EmptySocialHubView(
                            friendsCount = acceptedFriends.size,
                            roomsCount = voiceRooms.size,
                            onOpenSearch = { openAddFriendDialogFresh() },
                            onOpenCreateRoom = { showCreateRoomDialog = true }
                        )
                    }
                }
            }
        }
    }

    // =====================================================================
    // DIALOGS
    // =====================================================================
    if (showAddFriendDialog) {
        AddFriendDialog(
            searchResults = searchResults,
            isSearching = isSearching,
            recommendations = onlineRecommendations,
            isLoadingRecommendations = isLoadingRecommendations,
            friends = friends,
            onSearch = { q -> scope.launch { NuxSocialManager.searchUser(q) } },
            onRefreshRecommendations = {
                scope.launch {
                    NuxSocialManager.fetchOnlineRecommendations(50)
                }
            },
            onSendRequest = { targetUid ->
                scope.launch {
                    val res = NuxSocialManager.sendFriendRequest(targetUid)
                    if (res.isSuccess) {
                        Toast.makeText(context, "Friend request sent!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, res.exceptionOrNull()?.message ?: "Failed to send", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onAcceptRequest = { targetUid ->
                scope.launch {
                    val res = NuxSocialManager.acceptFriendRequest(targetUid)
                    if (res.isSuccess) {
                        Toast.makeText(context, "Friend request accepted!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, res.exceptionOrNull()?.message ?: "Failed to accept", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onBlockUser = { targetUid ->
                scope.launch {
                    val res = NuxSocialManager.blockUser(targetUid)
                    if (res.isSuccess) {
                        Toast.makeText(context, "User blocked successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, res.exceptionOrNull()?.message ?: "Failed to block", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onUnblockUser = { targetUid ->
                scope.launch {
                    val res = NuxSocialManager.unblockUser(targetUid)
                    if (res.isSuccess) {
                        Toast.makeText(context, "User unblocked", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, res.exceptionOrNull()?.message ?: "Failed to unblock", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onOpenChat = { friend ->
                NuxSocialManager.openChat(friend)
            },
            onDismiss = { showAddFriendDialog = false }
        )
    }

    if (showCreateRoomDialog) {
        CreateVoiceRoomDialog(
            onCreate = { name, pass, max ->
                scope.launch {
                    val res = NuxSocialManager.createVoiceRoom(name, pass, max)
                    if (res.isSuccess) {
                        selectedTab = "rooms"
                        showCreateRoomDialog = false
                    } else {
                        Toast.makeText(context, res.exceptionOrNull()?.message ?: "Failed to create room", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDismiss = { showCreateRoomDialog = false }
        )
    }

    if (roomToJoinWithPassword != null) {
        JoinPasswordDialog(
            room = roomToJoinWithPassword!!,
            onJoin = { pass ->
                scope.launch {
                    val res = NuxSocialManager.joinVoiceRoom(roomToJoinWithPassword!!, pass)
                    if (res.isSuccess) {
                        roomToJoinWithPassword = null
                    } else {
                        Toast.makeText(context, res.exceptionOrNull()?.message ?: "Incorrect password", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDismiss = { roomToJoinWithPassword = null }
        )
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

// =====================================================================
// COMPONENT: PILL TAB
// =====================================================================
@Composable
private fun PillTab(
    title: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeColor: Color? = null
) {
    val bg = if (isSelected) BellPillWhite else BellGlassInput
    val textColor = if (isSelected) BellInkDark else BellTextSecondary
    val border = if (isSelected) BellPillWhite else BellBorder

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 6.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontSize = 9.sp,
                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                color = textColor,
                letterSpacing = 0.4.sp
            )
            if (count > 0) {
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(badgeColor ?: if (isSelected) BellInkDark else Color(0x26FFFFFF))
                        .padding(horizontal = 4.5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "$count",
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Black,
                        color = if (badgeColor == BellDanger) BellInkDark else BellTextPrimary
                    )
                }
            }
        }
    }
}

// =====================================================================
// COMPONENT: COMPACT SEARCH BAR
// =====================================================================
@Composable
private fun BellCompactSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(BellGlassInput)
            .border(1.dp, if (value.isNotEmpty()) BellBorderActive else BellBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = BellTextSecondary,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    fontSize = 10.5.sp,
                    color = BellTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = BellTextPrimary
                ),
                cursorBrush = SolidColor(BellPillWhite),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (value.isNotEmpty()) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Clear",
                tint = BellTextSecondary,
                modifier = Modifier
                    .size(14.dp)
                    .clickable { onValueChange("") }
            )
        }
    }
}

// =====================================================================
// COMPONENT: FRIEND ITEM IN LIST
// =====================================================================
@Composable
private fun FriendItem(
    friend: NuxFriend,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) Color(0x1FFFFFFF) else BellGlassInput
    val border = if (isSelected) BellPillWhite else BellBorder

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .background(bg)
            .border(if (isSelected) 1.5.dp else 1.dp, border, RoundedCornerShape(9.dp))
            .clickable { onClick() }
            .padding(7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val dotColor = when {
            friend.isInGame -> BellInGame
            friend.isOnline -> BellSuccess
            else -> BellTextMuted
        }
        NuxUserAvatar(
            photoUrl = friend.photoUrl,
            username = friend.username,
            avatarSize = 32.dp,
            platform = friend.platform,
            isAndroid = friend.isAndroid,
            statusDotColor = dotColor
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = friend.username,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = BellTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(3.dp))
                NuxUserBadge(
                    isVerified = friend.isVerified,
                    isPremium = friend.isPremium,
                    size = 11.dp
                )
            }

            val statusText = when {
                friend.isInGame -> if (!friend.inGameInstance.isNullOrBlank()) "In-Game (${friend.inGameInstance})" else "In-Game"
                friend.isOnline -> "Online"
                friend.lastOnline != null -> {
                    val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(friend.lastOnline))
                    "Last seen $timeStr"
                }
                else -> "Offline"
            }

            val statusColor = when {
                friend.isInGame -> BellInGame
                friend.isOnline -> BellSuccess
                else -> BellTextMuted
            }

            Text(
                text = statusText,
                fontSize = 9.5.sp,
                color = statusColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (friend.unreadCount > 0) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(BellPillWhite)
                    .padding(horizontal = 6.dp, vertical = 1.5.dp)
            ) {
                Text(
                    text = "${friend.unreadCount}",
                    color = BellInkDark,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

// =====================================================================
// COMPONENT: VOICE ROOM ITEM IN LIST
// =====================================================================
@Composable
private fun VoiceRoomItem(
    room: NuxVoiceRoom,
    isJoined: Boolean,
    onJoin: () -> Unit
) {
    val bg = if (isJoined) Color(0x1FFFFFFF) else BellGlassInput
    val border = if (isJoined) BellPillWhite else BellBorder

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(9.dp))
            .padding(7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isJoined) BellPillWhite else Color(0x1AFFFFFF))
                .border(1.dp, if (isJoined) BellPillWhite else BellBorder, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.RecordVoiceOver,
                contentDescription = null,
                tint = if (isJoined) BellInkDark else BellTextPrimary,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = room.name,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = BellTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (room.isLocked) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Private",
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(10.dp)
                    )
                }
            }

            Text(
                text = "${room.participantCount}/${room.maxUsers} Members",
                fontSize = 9.5.sp,
                color = BellTextSecondary
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(7.dp))
                .background(if (isJoined) BellSuccess.copy(alpha = 0.2f) else BellPillWhite)
                .border(1.dp, if (isJoined) BellSuccess else BellPillWhite, RoundedCornerShape(7.dp))
                .clickable { onJoin() }
                .padding(horizontal = 9.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isJoined) "ACTIVE" else "JOIN",
                color = if (isJoined) BellSuccess else BellInkDark,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.4.sp
            )
        }
    }
}

// =====================================================================
// COMPONENT: REQUEST ITEMS
// =====================================================================
@Composable
private fun RequestReceivedItem(
    friend: NuxFriend,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .background(BellGlassInput)
            .border(1.dp, BellBorder, RoundedCornerShape(9.dp))
            .padding(7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NuxUserAvatar(
            photoUrl = friend.photoUrl,
            username = friend.username,
            avatarSize = 30.dp,
            platform = friend.platform,
            isAndroid = friend.isAndroid
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = friend.username, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = BellTextPrimary)
            Text(text = "Friend Request", fontSize = 9.sp, color = BellTextSecondary)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(7.dp))
                .background(BellPillWhite)
                .clickable { onAccept() }
                .padding(5.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = "Accept", tint = BellInkDark, modifier = Modifier.size(12.dp))
        }
        Spacer(modifier = Modifier.width(5.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(7.dp))
                .background(BellDanger.copy(alpha = 0.16f))
                .border(1.dp, BellDanger.copy(alpha = 0.4f), RoundedCornerShape(7.dp))
                .clickable { onReject() }
                .padding(5.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Decline", tint = BellDanger, modifier = Modifier.size(12.dp))
        }
    }
}

@Composable
private fun RequestSentItem(
    friend: NuxFriend,
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .background(BellGlassInput)
            .border(1.dp, BellBorder, RoundedCornerShape(9.dp))
            .padding(7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NuxUserAvatar(
            photoUrl = friend.photoUrl,
            username = friend.username,
            avatarSize = 30.dp,
            platform = friend.platform,
            isAndroid = friend.isAndroid
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = friend.username, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = BellTextPrimary)
            Text(text = "Pending approval...", fontSize = 9.sp, color = BellTextMuted)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(7.dp))
                .background(BellGlassCard)
                .border(1.dp, BellBorder, RoundedCornerShape(7.dp))
                .clickable { onCancel() }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text("Cancel", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = BellTextSecondary)
        }
    }
}

// =====================================================================
// STAGE 1: DIRECT CHAT VIEW
// =====================================================================
@Composable
private fun DirectChatView(
    friend: NuxFriend,
    messages: List<NuxChatMessage>,
    isFriendTyping: Boolean,
    replyingTo: NuxChatMessage?,
    onCancelReply: () -> Unit,
    onReplyMessage: (NuxChatMessage) -> Unit,
    onCloseChat: () -> Unit,
    onStartVoiceWithFriend: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var inputMessage by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isSending by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> selectedImageUri = uri }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Chat Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BellGlassSidebar)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NuxUserAvatar(
                    photoUrl = friend.photoUrl,
                    username = friend.username,
                    avatarSize = 28.dp,
                    platform = friend.platform,
                    isAndroid = friend.isAndroid
                )
                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = friend.username,
                            color = BellTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        NuxUserBadge(isVerified = friend.isVerified, isPremium = friend.isPremium, size = 11.dp)
                    }

                    val headerStatusText = when {
                        friend.isInGame -> if (!friend.inGameInstance.isNullOrBlank()) "In-Game (${friend.inGameInstance})" else "In-Game"
                        friend.isOnline -> "Online"
                        else -> "Offline"
                    }
                    val headerStatusColor = when {
                        friend.isInGame -> BellInGame
                        friend.isOnline -> BellSuccess
                        else -> BellTextMuted
                    }

                    Text(
                        text = headerStatusText,
                        color = headerStatusColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .height(26.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(BellPillWhite)
                        .clickable { onStartVoiceWithFriend() }
                        .padding(horizontal = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Mic, contentDescription = null, tint = BellInkDark, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Voice", color = BellInkDark, fontSize = 9.5.sp, fontWeight = FontWeight.Black)
                    }
                }

                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(BellGlassInput)
                        .border(1.dp, BellBorder, RoundedCornerShape(7.dp))
                        .clickable { onCloseChat() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("✕", color = BellTextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        HorizontalDivider(color = BellBorder, thickness = 1.dp)

        // Messages Feed
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(BellBgCanvas.copy(alpha = 0.65f))
        ) {
            if (messages.isEmpty()) {
                EmptyIndicator(
                    icon = Icons.Outlined.Forum,
                    title = "No Messages Yet",
                    subtitle = "Say hello to ${friend.username}!"
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val myUid = AccountManager.launcherUser.value?.uid ?: ""
                    itemsIndexed(messages, key = { index, msg -> "${msg.id}_$index" }) { _, msg ->
                        ChatBubble(
                            message = msg,
                            isMine = msg.senderId == myUid,
                            onReply = { onReplyMessage(msg) }
                        )
                    }
                }
            }

            if (isFriendTyping) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(BellGlassElevated)
                        .border(1.dp, BellBorder, RoundedCornerShape(7.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${friend.username} is typing...",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = BellTextSecondary
                    )
                }
            }
        }

        if (replyingTo != null) {
            HorizontalDivider(color = BellBorder, thickness = 1.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BellGlassSidebar)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Reply, contentDescription = null, tint = BellTextPrimary, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(text = "Replying to: ${replyingTo.text.take(60)}", fontSize = 9.5.sp, color = BellTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel",
                    tint = BellTextMuted,
                    modifier = Modifier
                        .size(14.dp)
                        .clickable { onCancelReply() }
                )
            }
        }

        if (selectedImageUri != null) {
            HorizontalDivider(color = BellBorder, thickness = 1.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x1AFFFFFF))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Photo ready to send", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = BellTextPrimary)
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete",
                    tint = BellTextSecondary,
                    modifier = Modifier
                        .size(14.dp)
                        .clickable { selectedImageUri = null }
                )
            }
        }

        HorizontalDivider(color = BellBorder, thickness = 1.dp)

        // Bottom Input Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BellGlassSidebar)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(BellGlassInput)
                    .border(1.dp, BellBorder, RoundedCornerShape(8.dp))
                    .clickable { imagePickerLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Image, contentDescription = "Photo", tint = BellTextSecondary, modifier = Modifier.size(15.dp))
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(BellGlassInput)
                    .border(1.dp, BellBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (inputMessage.isEmpty()) {
                        Text("Type a message...", fontSize = 11.sp, color = BellTextMuted)
                    }
                    BasicTextField(
                        value = inputMessage,
                        onValueChange = {
                            inputMessage = it
                            NuxSocialManager.setMyTyping(it.isNotBlank())
                        },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = BellTextPrimary
                        ),
                        cursorBrush = SolidColor(BellPillWhite),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            val canSend = inputMessage.isNotBlank() || selectedImageUri != null
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (canSend) BellPillWhite else BellGlassInput)
                    .border(1.dp, if (canSend) BellPillWhite else BellBorder, RoundedCornerShape(8.dp))
                    .clickable(enabled = !isSending && canSend) {
                        val textToSend = inputMessage.trim()
                        val uriToSend = selectedImageUri
                        val replyObj = replyingTo?.let {
                            NuxChatReply(
                                id = it.id,
                                senderId = it.senderId,
                                senderName = it.senderName,
                                text = it.text
                            )
                        }

                        isSending = true
                        scope.launch {
                            var imgBytes: ByteArray? = null
                            if (uriToSend != null) {
                                try {
                                    imgBytes = context.contentResolver.openInputStream(uriToSend)?.use { it.readBytes() }
                                } catch (_: Exception) {}
                            }

                            val res = NuxSocialManager.sendChatMessage(
                                text = textToSend,
                                imageBytes = imgBytes,
                                replyTo = replyObj
                            )

                            isSending = false
                            if (res.isSuccess) {
                                inputMessage = ""
                                selectedImageUri = null
                                onCancelReply()
                            } else {
                                Toast.makeText(context, res.exceptionOrNull()?.message ?: "Failed to send message", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isSending) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = BellInkDark, strokeWidth = 1.8.dp)
                } else {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (canSend) BellInkDark else BellTextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

// =====================================================================
// COMPONENT: CHAT BUBBLE
// =====================================================================
@Composable
private fun ChatBubble(
    message: NuxChatMessage,
    isMine: Boolean,
    onReply: () -> Unit
) {
    val align = if (isMine) Alignment.End else Alignment.Start
    val bg = if (isMine) BellPillWhite else BellGlassElevated
    val border = if (isMine) BellPillWhite else BellBorder
    val contentColor = if (isMine) BellInkDark else BellTextPrimary
    val timeColor = if (isMine) BellInkDark.copy(alpha = 0.65f) else BellTextMuted
    val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalAlignment = align
    ) {
        Box(
            modifier = Modifier
                .widthIn(min = 60.dp, max = 250.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 10.dp,
                        topEnd = 10.dp,
                        bottomStart = if (isMine) 10.dp else 3.dp,
                        bottomEnd = if (isMine) 3.dp else 10.dp
                    )
                )
                .background(bg)
                .border(
                    width = 1.dp,
                    color = border,
                    shape = RoundedCornerShape(
                        topStart = 10.dp,
                        topEnd = 10.dp,
                        bottomStart = if (isMine) 10.dp else 3.dp,
                        bottomEnd = if (isMine) 3.dp else 10.dp
                    )
                )
                .clickable { onReply() }
                .padding(horizontal = 9.dp, vertical = 6.dp)
        ) {
            Column {
                if (message.replyTo != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isMine) Color(0x14000000) else BellGlassInput)
                            .padding(horizontal = 7.dp, vertical = 4.dp)
                    ) {
                        Column {
                            Text(
                                text = message.replyTo.senderName.ifBlank { "User" },
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isMine) BellInkDark else BellTextSecondary
                            )
                            Text(
                                text = message.replyTo.text,
                                fontSize = 8.5.sp,
                                color = contentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (!message.imageUrl.isNullOrEmpty()) {
                    NuxNetworkImage(
                        model = message.imageUrl,
                        contentDescription = "Foto",
                        shape = RoundedCornerShape(7.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 140.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (message.text.isNotEmpty()) {
                        Text(
                            text = message.text,
                            fontSize = 11.5.sp,
                            fontWeight = if (isMine) FontWeight.SemiBold else FontWeight.Normal,
                            color = contentColor,
                            lineHeight = 15.sp,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                    Text(
                        text = timeStr,
                        fontSize = 7.5.sp,
                        color = timeColor,
                        modifier = Modifier.padding(bottom = 1.dp)
                    )
                }
            }
        }
    }
}

// =====================================================================
// COMPONENT: VOICE ROOM CHAT PANEL
// =====================================================================
@Composable
private fun VoiceRoomChatPanel(
    room: NuxVoiceRoom,
    messages: List<NuxVoiceMessage>,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var textInput by remember { mutableStateOf("") }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(BellGlassInput)
                .border(1.dp, BellBorder, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(BellSuccess)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "CHAT: ${room.name}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = BellTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(BellSuccess.copy(alpha = 0.16f))
                    .border(1.dp, BellSuccess.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            ) {
                Text("LIVE", fontSize = 7.5.sp, fontWeight = FontWeight.Black, color = BellSuccess)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(BellBgCanvas.copy(alpha = 0.65f))
                .border(1.dp, BellBorder, RoundedCornerShape(8.dp))
                .padding(6.dp)
        ) {
            if (messages.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No messages in room yet.\nType a message below!",
                        fontSize = 10.sp,
                        color = BellTextMuted,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                val myUid = AccountManager.launcherUser.value?.uid ?: ""
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    itemsIndexed(messages, key = { index, msg -> "${msg.id}_$index" }) { _, msg ->
                        val isMine = msg.senderId == myUid
                        val senderDisplayName = if (msg.senderName.length > 8) "${msg.senderName.take(8)}..." else msg.senderName
                        val timeStr = if (msg.timestamp > 0) {
                            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(msg.timestamp))
                        } else ""

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
                        ) {
                            if (!isMine) {
                                Text(
                                    text = senderDisplayName,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BellTextSecondary,
                                    modifier = Modifier.padding(start = 4.dp, bottom = 1.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = 8.dp,
                                            topEnd = 8.dp,
                                            bottomStart = if (isMine) 8.dp else 2.dp,
                                            bottomEnd = if (isMine) 2.dp else 8.dp
                                        )
                                    )
                                    .background(if (isMine) BellPillWhite else BellGlassElevated)
                                    .border(1.dp, if (isMine) BellPillWhite else BellBorder, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = msg.text,
                                    fontSize = 10.sp,
                                    fontWeight = if (isMine) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isMine) BellInkDark else BellTextPrimary,
                                    lineHeight = 13.sp
                                )
                            }
                            if (timeStr.isNotEmpty()) {
                                Text(
                                    text = timeStr,
                                    fontSize = 7.5.sp,
                                    color = BellTextMuted,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(BellGlassInput)
                .border(1.dp, BellBorder, RoundedCornerShape(8.dp))
                .padding(horizontal = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (textInput.isEmpty()) {
                    Text("Send message in room...", fontSize = 10.sp, color = BellTextMuted)
                }
                BasicTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, color = BellTextPrimary),
                    cursorBrush = SolidColor(BellPillWhite),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "Send",
                tint = if (textInput.isNotBlank()) BellPillWhite else BellTextMuted,
                modifier = Modifier
                    .size(15.dp)
                    .clickable {
                        if (textInput.isNotBlank()) {
                            val txt = textInput.trim()
                            textInput = ""
                            scope.launch { NuxSocialManager.sendVoiceRoomMessage(txt) }
                        }
                    }
            )
        }
    }
}

// =====================================================================
// STAGE 2: VOICE ROOM VIEW
// =====================================================================
@Composable
private fun VoiceRoomView(
    room: NuxVoiceRoom,
    onLeave: () -> Unit
) {
    val isMuted by NuxVoiceManager.isMuted.collectAsState()
    val activeSpeakers by NuxVoiceManager.activeSpeakers.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BellGlassSidebar)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x1AFFFFFF))
                        .border(1.dp, BellBorder, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        tint = BellTextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = room.name,
                            color = BellTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(BellSuccess.copy(alpha = 0.16f))
                                .border(1.dp, BellSuccess.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text("LIVEKIT SFU", fontSize = 7.5.sp, fontWeight = FontWeight.Black, color = BellSuccess)
                        }
                    }
                    Text(
                        text = "${room.participantCount}/${room.maxUsers} Active Members",
                        color = BellTextSecondary,
                        fontSize = 9.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .height(26.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(BellDanger.copy(alpha = 0.16f))
                    .border(1.dp, BellDanger.copy(alpha = 0.45f), RoundedCornerShape(7.dp))
                    .clickable { onLeave() }
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CallEnd, contentDescription = null, tint = BellDanger, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("LEAVE", color = BellDanger, fontSize = 9.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        HorizontalDivider(color = BellBorder, thickness = 1.dp)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            val participants = room.participants.values.toList()
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 78.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(participants, key = { it.uid }) { p ->
                    val isSpeaking = activeSpeakers.contains(p.uid)
                    ParticipantBadge(
                        participant = p,
                        isSpeaking = isSpeaking
                    )
                }
            }
        }

        HorizontalDivider(color = BellBorder, thickness = 1.dp)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(BellGlassSidebar)
                .padding(vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .height(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isMuted) BellDanger.copy(alpha = 0.16f) else BellPillWhite)
                    .border(1.dp, if (isMuted) BellDanger.copy(alpha = 0.5f) else BellPillWhite, RoundedCornerShape(8.dp))
                    .clickable { NuxVoiceManager.toggleMute() }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = null,
                        tint = if (isMuted) BellDanger else BellInkDark,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isMuted) "MIC MUTED" else "MIC ACTIVE (SPEAKING)",
                        color = if (isMuted) BellDanger else BellInkDark,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.4.sp
                    )
                }
            }
        }
    }
}

// =====================================================================
// COMPONENT: PARTICIPANT BADGE IN VOICE ROOM
// =====================================================================
@Composable
private fun ParticipantBadge(
    participant: NuxParticipant,
    isSpeaking: Boolean
) {
    val displayName = if (participant.username.length > 4) {
        "${participant.username.take(4)}..."
    } else {
        participant.username
    }

    Column(
        modifier = Modifier
            .width(76.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSpeaking) Color(0x26FFFFFF) else BellGlassInput)
            .border(
                width = if (isSpeaking) 1.5.dp else 1.dp,
                color = if (isSpeaking) BellPillWhite else BellBorder,
                shape = RoundedCornerShape(10.dp)
            )
            .padding(vertical = 7.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .border(1.5.dp, if (isSpeaking) BellPillWhite else BellBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            NuxUserAvatar(
                photoUrl = participant.photoURL,
                username = participant.username,
                avatarSize = 34.dp,
                shape = CircleShape,
                platform = participant.platform,
                isAndroid = participant.isAndroid
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = displayName,
                color = BellTextPrimary,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.width(2.dp))
            NuxUserBadge(isVerified = participant.isVerified, isPremium = participant.isPremium, size = 9.dp)
        }

        Spacer(modifier = Modifier.height(2.dp))

        if (isSpeaking) {
            Text(
                text = "SPEAKING",
                color = BellSuccess,
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Black
            )
        } else {
            Text(
                text = participant.platform.take(3).uppercase(),
                color = BellTextMuted,
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// =====================================================================
// STAGE 3: EMPTY SOCIAL HUB VIEW
// =====================================================================
@Composable
private fun EmptySocialHubView(
    friendsCount: Int,
    roomsCount: Int,
    onOpenSearch: () -> Unit,
    onOpenCreateRoom: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x1AFFFFFF))
                .border(1.dp, BellBorder, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Group,
                contentDescription = null,
                tint = BellTextPrimary,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "COMMUNICATION & VOICE HUB",
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Black,
            color = BellTextPrimary,
            letterSpacing = 0.6.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Connect with friends across PC Windows & Android.\nDirect chat and low-latency voice via LiveKit WebRTC.",
            fontSize = 10.sp,
            color = BellTextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(BellGlassInput)
                    .border(1.dp, BellBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 11.dp, vertical = 6.dp)
            ) {
                Text("$friendsCount Friends Added", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BellTextPrimary)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(BellGlassInput)
                    .border(1.dp, BellBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 11.dp, vertical = 6.dp)
            ) {
                Text("$roomsCount Active Voice Rooms", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BellTextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .height(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(BellGlassInput)
                    .border(1.dp, BellBorder, RoundedCornerShape(8.dp))
                    .clickable { onOpenSearch() }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = BellTextPrimary, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("FIND NEW FRIENDS", color = BellTextPrimary, fontWeight = FontWeight.Bold, fontSize = 9.5.sp)
                }
            }

            Box(
                modifier = Modifier
                    .height(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(BellPillWhite)
                    .clickable { onOpenCreateRoom() }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = BellInkDark, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("CREATE VOICE ROOM", color = BellInkDark, fontWeight = FontWeight.Black, fontSize = 9.5.sp)
                }
            }
        }
    }
}

// =====================================================================
// COMPONENT: EMPTY STATE HELPER
// =====================================================================
@Composable
private fun EmptyIndicator(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = BellTextMuted, modifier = Modifier.size(26.dp))
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BellTextPrimary)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = subtitle, fontSize = 9.5.sp, color = BellTextMuted, textAlign = TextAlign.Center, lineHeight = 13.sp)
    }
}

// =====================================================================
// DIALOG 1: ADD / SEARCH FRIENDS
// =====================================================================
@Composable
private fun AddFriendDialog(
    searchResults: List<NuxUserProfile>,
    isSearching: Boolean,
    recommendations: List<NuxUserProfile>,
    isLoadingRecommendations: Boolean,
    friends: List<NuxFriend>,
    onSearch: (String) -> Unit,
    onRefreshRecommendations: () -> Unit,
    onSendRequest: (String) -> Unit,
    onAcceptRequest: (String) -> Unit,
    onBlockUser: (String) -> Unit,
    onUnblockUser: (String) -> Unit,
    onOpenChat: (NuxFriend) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var sentRequestUids by remember { mutableStateOf(setOf<String>()) }

    val isShowingRecommendations = query.isBlank()
    val activeList = if (isShowingRecommendations) recommendations else searchResults

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .width(440.dp)
                .wrapContentHeight()
                .clip(RoundedCornerShape(14.dp))
                .background(BellGlassElevated)
                .border(1.dp, BellBorder, RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "SEARCH & ADD FRIENDS", fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 0.6.sp, color = BellTextPrimary)
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(BellGlassInput)
                        .border(1.dp, BellBorder, CircleShape)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("✕", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BellTextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            BellCompactSearchBar(
                value = query,
                onValueChange = {
                    query = it
                    onSearch(it)
                },
                placeholder = "Type username to search..."
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 195.dp)
            ) {
                if (!isShowingRecommendations && isSearching) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = BellPillWhite, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Searching \"$query\"...", fontSize = 10.sp, color = BellTextSecondary)
                    }
                } else if (isShowingRecommendations && isLoadingRecommendations && recommendations.isEmpty()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = BellPillWhite, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Loading online recommendations...", fontSize = 10.sp, color = BellTextSecondary)
                    }
                } else if (activeList.isEmpty()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isShowingRecommendations) "No online recommendations available right now." else "No users found.",
                            fontSize = 10.5.sp,
                            color = BellTextMuted,
                            textAlign = TextAlign.Center
                        )
                        if (isShowingRecommendations) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(BellGlassInput)
                                    .border(1.dp, BellBorder, RoundedCornerShape(7.dp))
                                    .clickable { onRefreshRecommendations() }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Refresh", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = BellTextPrimary)
                            }
                        }
                    }
                } else {
                    val currentMyUid = AccountManager.launcherUser.value?.uid ?: ""
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (isShowingRecommendations) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 5.dp, start = 2.dp, end = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(BellSuccess)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "ONLINE RECOMMENDATIONS (${recommendations.size})",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.5.sp,
                                        color = BellTextSecondary
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable { onRefreshRecommendations() }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Shuffle",
                                        tint = BellTextPrimary,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Shuffle",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BellTextPrimary
                                    )
                                }
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            itemsIndexed(activeList, key = { index, user -> "${user.uid}_$index" }) { _, user ->
                                val isMe = user.uid == currentMyUid
                                val friendEntry = friends.find { it.uid == user.uid }
                                val isPendingSent = (friendEntry?.isPendingSent == true || sentRequestUids.contains(user.uid)) && friendEntry?.isAccepted != true
                                val isAccepted = friendEntry?.isAccepted == true
                                val isPendingReceived = friendEntry?.isPendingReceived == true
                                val isBlocked = friendEntry?.isBlocked == true

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(BellGlassInput)
                                        .border(1.dp, BellBorder, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val dotColor = when {
                                        user.status == "in_game" -> BellInGame
                                        user.status == "online" -> BellSuccess
                                        else -> BellTextMuted
                                    }

                                    NuxUserAvatar(
                                        photoUrl = user.photoURL,
                                        username = user.username,
                                        avatarSize = 26.dp,
                                        platform = user.platform,
                                        isAndroid = user.isAndroid,
                                        statusDotColor = dotColor
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(user.username, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BellTextPrimary)
                                            Spacer(modifier = Modifier.width(3.dp))
                                            NuxUserBadge(isVerified = user.isVerified, isPremium = user.isPremium, size = 10.dp)
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            val statusText = when {
                                                user.status == "in_game" -> "IN-GAME"
                                                user.status == "online" -> "ONLINE"
                                                else -> "OFFLINE"
                                            }
                                            val statusTextColor = when {
                                                user.status == "in_game" -> BellInGame
                                                user.status == "online" -> BellSuccess
                                                else -> BellTextMuted
                                            }
                                            Text(statusText, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = statusTextColor)
                                            Text(" • ${user.platform.uppercase()}", fontSize = 8.sp, color = BellTextMuted)
                                        }
                                    }

                                    if (isMe) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(BellGlassCard)
                                                .border(1.dp, BellBorder, RoundedCornerShape(6.dp))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text("You", color = BellTextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    } else if (isAccepted) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(BellSuccess.copy(alpha = 0.16f))
                                                    .border(1.dp, BellSuccess.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                                    .clickable {
                                                        if (friendEntry != null) {
                                                            onOpenChat(friendEntry)
                                                            onDismiss()
                                                        }
                                                    }
                                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                                            ) {
                                                Text("✓ Friends", color = BellSuccess, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(BellDanger.copy(alpha = 0.16f))
                                                    .border(1.dp, BellDanger.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                                    .clickable { onBlockUser(user.uid) }
                                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                                            ) {
                                                Text("Block", color = BellDanger, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    } else if (isPendingSent) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(BellGlassCard)
                                                .border(1.dp, BellBorder, RoundedCornerShape(6.dp))
                                                .padding(horizontal = 7.dp, vertical = 3.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Schedule, contentDescription = null, tint = BellTextSecondary, modifier = Modifier.size(10.dp))
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text("Pending", color = BellTextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    } else if (isPendingReceived) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(BellPillWhite)
                                                .clickable { onAcceptRequest(user.uid) }
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text("Accept", color = BellInkDark, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                        }
                                    } else if (isBlocked) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(BellDanger.copy(alpha = 0.16f))
                                                .border(1.dp, BellDanger.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                                .clickable { onUnblockUser(user.uid) }
                                                .padding(horizontal = 7.dp, vertical = 3.dp)
                                        ) {
                                            Text("Unblock", color = BellDanger, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(BellPillWhite)
                                                .clickable {
                                                    sentRequestUids = sentRequestUids + user.uid
                                                    onSendRequest(user.uid)
                                                }
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text("+ Add", color = BellInkDark, fontSize = 9.sp, fontWeight = FontWeight.Black)
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

// =====================================================================
// DIALOG 2: CREATE VOICE ROOM
// =====================================================================
@Composable
private fun CreateVoiceRoomDialog(
    onCreate: (name: String, pass: String, max: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var roomName by remember { mutableStateOf("") }
    var roomPassword by remember { mutableStateOf("") }
    var maxUsers by remember { mutableIntStateOf(5) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .width(450.dp)
                .wrapContentHeight()
                .clip(RoundedCornerShape(14.dp))
                .background(BellGlassElevated)
                .border(1.dp, BellBorder, RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "CREATE NEW VOICE ROOM", fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 0.6.sp, color = BellTextPrimary)
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(BellGlassInput)
                        .border(1.dp, BellBorder, CircleShape)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("✕", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BellTextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1.1f)) {
                    Text("ROOM NAME", fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp, color = BellTextMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(BellGlassInput)
                            .border(1.dp, BellBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            if (roomName.isEmpty()) {
                                Text("e.g. Survival Session", fontSize = 10.5.sp, color = BellTextMuted)
                            }
                            BasicTextField(
                                value = roomName,
                                onValueChange = { roomName = it },
                                singleLine = true,
                                textStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BellTextPrimary),
                                cursorBrush = SolidColor(BellPillWhite),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("PASSWORD (OPTIONAL)", fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp, color = BellTextMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(BellGlassInput)
                            .border(1.dp, BellBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            if (roomPassword.isEmpty()) {
                                Text("Leave blank if public", fontSize = 10.5.sp, color = BellTextMuted)
                            }
                            BasicTextField(
                                value = roomPassword,
                                onValueChange = { roomPassword = it },
                                singleLine = true,
                                textStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BellTextPrimary),
                                cursorBrush = SolidColor(BellPillWhite),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(0.9f),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("CAPACITY", fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp, color = BellTextMuted)
                            Text("$maxUsers Players", fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = BellTextPrimary)
                        }
                        Slider(
                            value = maxUsers.toFloat(),
                            onValueChange = { maxUsers = it.toInt() },
                            valueRange = 2f..10f,
                            steps = 7,
                            colors = SliderDefaults.colors(
                                thumbColor = BellPillWhite,
                                activeTrackColor = BellPillWhite,
                                inactiveTrackColor = Color(0x26FFFFFF)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BellGlassInput)
                                .border(1.dp, BellBorder, RoundedCornerShape(8.dp))
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("CANCEL", color = BellTextPrimary, fontWeight = FontWeight.Bold, fontSize = 9.5.sp)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1.3f)
                                .height(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BellPillWhite)
                                .clickable {
                                    if (roomName.isNotBlank()) {
                                        onCreate(roomName.trim(), roomPassword.trim(), maxUsers)
                                    } else {
                                        onCreate("Voice Room", roomPassword.trim(), maxUsers)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("CREATE ROOM", color = BellInkDark, fontWeight = FontWeight.Black, fontSize = 9.5.sp)
                        }
                    }
                }
            }
        }
    }
}

// =====================================================================
// DIALOG 3: JOIN PASSWORD ROOM
// =====================================================================
@Composable
private fun JoinPasswordDialog(
    room: NuxVoiceRoom,
    onJoin: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var passInput by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .width(340.dp)
                .wrapContentHeight()
                .clip(RoundedCornerShape(14.dp))
                .background(BellGlassElevated)
                .border(1.dp, BellBorder, RoundedCornerShape(14.dp))
                .padding(16.dp)
        ) {
            Text(text = "PASSWORD PROTECTED ROOM", fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 0.6.sp, color = BellTextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Enter password to join \"${room.name}\".", fontSize = 10.sp, color = BellTextSecondary)

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(BellGlassInput)
                    .border(1.dp, BellBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (passInput.isEmpty()) {
                        Text("Password...", fontSize = 10.5.sp, color = BellTextMuted)
                    }
                    BasicTextField(
                        value = passInput,
                        onValueChange = { passInput = it },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BellTextPrimary),
                        cursorBrush = SolidColor(BellPillWhite),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(BellGlassInput)
                        .border(1.dp, BellBorder, RoundedCornerShape(8.dp))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("CANCEL", color = BellTextPrimary, fontWeight = FontWeight.Bold, fontSize = 9.5.sp)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(BellPillWhite)
                        .clickable { onJoin(passInput) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("JOIN", color = BellInkDark, fontWeight = FontWeight.Black, fontSize = 9.5.sp)
                }
            }
        }
    }
}
