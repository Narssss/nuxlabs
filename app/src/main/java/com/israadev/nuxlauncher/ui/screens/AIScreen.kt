package com.israadev.nuxlauncher.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.israadev.nuxlauncher.core.ai.AIChatMessage
import com.israadev.nuxlauncher.core.ai.NuxAIEngine
import com.israadev.nuxlauncher.core.crash.AICrashAnalyzer
import com.israadev.nuxlauncher.core.crash.AICrashQuotaManager
import com.israadev.nuxlauncher.core.crash.AIStreamState
import com.israadev.nuxlauncher.core.instance.InstanceManager
import com.israadev.nuxlauncher.core.lang.NuxLanguageManager
import com.israadev.nuxlauncher.core.lang.trStr
import com.israadev.nuxlauncher.core.settings.SettingsManager
import com.israadev.nuxlauncher.ui.components.NuxMarkdownView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

private fun safeAiTr(key: String, fallback: String): String {
    val translated = trStr(key)
    return if (translated.isBlank() || translated == key) fallback else translated
}

private data class AiQuickPrompt(
    val title: String,
    val prompt: String,
    val icon: ImageVector
)

private val QUICK_PROMPTS = listOf(
    AiQuickPrompt(
        title = "Analyze Last Crash",
        prompt = "Analyze my current instance setup and recent logs to find why Minecraft might crash or fail to launch.",
        icon = Icons.Outlined.BugReport
    ),
    AiQuickPrompt(
        title = "Boost FPS & Renderer",
        prompt = "Recommend the best renderer, JVM flags, and performance mods for my current Minecraft version and device.",
        icon = Icons.Outlined.Speed
    ),
    AiQuickPrompt(
        title = "Check Mod Conflicts",
        prompt = "Check my installed mods list for incompatible loaders, duplicate libraries, or version mismatches.",
        icon = Icons.Outlined.Extension
    )
)

@Composable
fun AIScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val langVersion by NuxLanguageManager.languageVersion.collectAsState()

    val instances by InstanceManager.instances.collectAsState()
    val settings by SettingsManager.settings.collectAsState()

    // Instance aktif yang dipilih untuk konteks AI
    var selectedInstanceIndex by remember { mutableIntStateOf(0) }
    val currentInstance = remember(instances, selectedInstanceIndex) {
        if (instances.isNotEmpty() && selectedInstanceIndex in instances.indices) {
            instances[selectedInstanceIndex]
        } else instances.firstOrNull()
    }
    var showInstanceSelectorMenu by remember { mutableStateOf(false) }

    // Quota State
    var remainingQuota by remember {
        mutableIntStateOf(AICrashQuotaManager.getRemainingQuota(context, settings))
    }

    LaunchedEffect(Unit) {
        val synced = AICrashQuotaManager.syncQuotaFromDatabase(context, settings)
        remainingQuota = synced
    }

    // Chat History State
    val chatMessages = remember { mutableStateListOf<AIChatMessage>() }
    var currentInputText by remember { mutableStateOf("") }
    var streamState by remember { mutableStateOf<AIStreamState>(AIStreamState.Idle) }

    // Auto-scroll list state
    val listState = rememberLazyListState()

    LaunchedEffect(chatMessages.size, streamState) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    // Cursor Blink for real-time typewriter effect
    var isCursorBlinkVisible by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(450)
            isCursorBlinkVisible = !isCursorBlinkVisible
        }
    }

    val effectiveModel = remember(settings.aiModel) {
        AICrashAnalyzer.getEffectiveModel(settings)
    }

    val modelTag = remember(effectiveModel) {
        when {
            effectiveModel.contains("lightning", ignoreCase = true) -> "NEMOTRON LIGHTNING"
            effectiveModel.contains("120b", ignoreCase = true) -> "NEMOTRON 120B"
            effectiveModel.contains("550b", ignoreCase = true) -> "NEMOTRON 550B"
            effectiveModel.contains("gemini", ignoreCase = true) -> "GEMINI FLASH"
            effectiveModel.contains("llama", ignoreCase = true) -> "LLAMA 3.3"
            effectiveModel.contains("qwen", ignoreCase = true) -> "QWEN 3.8"
            effectiveModel.contains("deepseek", ignoreCase = true) -> "DEEPSEEK R1"
            effectiveModel.contains("mistral", ignoreCase = true) -> "MISTRAL"
            else -> "BELL AI CORE"
        }
    }

    fun sendMessage(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isBlank() || streamState is AIStreamState.Connecting || streamState is AIStreamState.Streaming) {
            return
        }

        if (!AICrashQuotaManager.hasQuota(context, settings)) {
            Toast.makeText(context, "Today's free Bell AI quota has been reached (resets at 00:00).", Toast.LENGTH_LONG).show()
            streamState = AIStreamState.QuotaExceeded
            return
        }

        val userMsg = AIChatMessage(role = "user", content = trimmed)
        chatMessages.add(userMsg)
        currentInputText = ""

        val aiMsgId = java.util.UUID.randomUUID().toString()
        val aiMsg = AIChatMessage(id = aiMsgId, role = "assistant", content = "")
        chatMessages.add(aiMsg)

        val instanceContext = NuxAIEngine.collectInstanceContext(context, currentInstance, settings)

        coroutineScope.launch {
            NuxAIEngine.streamChat(
                context = context,
                chatHistory = chatMessages.filter { it.id != aiMsgId },
                instanceContext = instanceContext,
                settings = settings
            ).collect { state ->
                streamState = state
                when (state) {
                    is AIStreamState.Streaming -> {
                        val index = chatMessages.indexOfFirst { it.id == aiMsgId }
                        if (index != -1) {
                            chatMessages[index] = chatMessages[index].copy(content = state.fullText)
                        }
                    }
                    is AIStreamState.Completed -> {
                        val index = chatMessages.indexOfFirst { it.id == aiMsgId }
                        if (index != -1) {
                            chatMessages[index] = chatMessages[index].copy(content = state.fullText)
                        }
                        remainingQuota = AICrashQuotaManager.getRemainingQuota(context, settings)
                    }
                    is AIStreamState.Error -> {
                        val index = chatMessages.indexOfFirst { it.id == aiMsgId }
                        if (index != -1) {
                            chatMessages[index] = chatMessages[index].copy(
                                content = "⚠️ **Error Occurred:** ${state.errorMessage}\n\nPlease try again or check your internet connection."
                            )
                        }
                    }
                    is AIStreamState.QuotaExceeded -> {
                        val index = chatMessages.indexOfFirst { it.id == aiMsgId }
                        if (index != -1) {
                            chatMessages[index] = chatMessages[index].copy(
                                content = "⏱️ **Daily Bell AI Quota Reached (5/5)**\n\nDaily free usage limit has been reached. Quota will reset tomorrow. You can also configure a custom API key in Settings for unlimited access."
                            )
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    val isBusy = streamState is AIStreamState.Connecting || streamState is AIStreamState.Streaming

    // =========================================================================
    // MASTER-DETAIL SPLIT LAYOUT (KIRI: KONTEKS & QUICK PROMPTS | KANAN: CHAT)
    // =========================================================================
    Row(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // =====================================================================
        // PANEL KIRI: AI TELEMETRY, INSTANCE CONTEXT & QUICK PROMPTS (205.dp)
        // =====================================================================
        Column(
            modifier = Modifier
                .width(205.dp)
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header Identitas AI
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(BellPillWhite),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoFixHigh,
                            contentDescription = null,
                            tint = BellInkDark,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "BELL AI ASSISTANT",
                            color = BellTextPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp,
                            letterSpacing = 0.6.sp
                        )
                        Text(
                            text = modelTag,
                            color = BellTextMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Kartu Target Instance Context
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(BellGlassInput)
                        .border(1.dp, BellBorder, RoundedCornerShape(10.dp))
                        .padding(9.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TARGET INSTANCE",
                            color = BellTextMuted,
                            fontWeight = FontWeight.Black,
                            fontSize = 8.sp,
                            letterSpacing = 0.6.sp
                        )

                        if (instances.size > 1) {
                            Box {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(BellPillWhite)
                                        .clickable { showInstanceSelectorMenu = true }
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "SWITCH",
                                        fontSize = 7.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = BellInkDark
                                    )
                                }

                                DropdownMenu(
                                    expanded = showInstanceSelectorMenu,
                                    onDismissRequest = { showInstanceSelectorMenu = false },
                                    shape = RoundedCornerShape(12.dp),
                                    containerColor = BellGlassElevated,
                                    border = BorderStroke(1.dp, BellBorder)
                                ) {
                                    instances.forEachIndexed { idx, inst ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "${inst.name} (${inst.mcVersion})",
                                                    fontSize = 10.sp,
                                                    color = if (idx == selectedInstanceIndex) BellTextPrimary else BellTextSecondary,
                                                    fontWeight = if (idx == selectedInstanceIndex) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            onClick = {
                                                selectedInstanceIndex = idx
                                                showInstanceSelectorMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Text(
                        text = currentInstance?.name ?: "No Instance Selected",
                        color = BellTextPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (currentInstance != null) {
                        Text(
                            text = "${currentInstance.mcVersion} • ${currentInstance.loader.uppercase()} • ${settings.selectedRenderer.uppercase()}",
                            color = BellTextSecondary,
                            fontSize = 8.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Quick Prompts Section
                Text(
                    text = "QUICK DIAGNOSTICS",
                    color = BellTextMuted,
                    fontWeight = FontWeight.Black,
                    fontSize = 8.sp,
                    letterSpacing = 0.7.sp,
                    modifier = Modifier.padding(start = 2.dp, top = 2.dp)
                )

                QUICK_PROMPTS.forEach { qp ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(9.dp))
                            .background(BellGlassInput)
                            .border(1.dp, BellBorder, RoundedCornerShape(9.dp))
                            .clickable(enabled = !isBusy) { sendMessage(qp.prompt) }
                            .padding(horizontal = 9.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Icon(
                            imageVector = qp.icon,
                            contentDescription = null,
                            tint = BellTextPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = qp.title,
                            color = BellTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Footer Sidebar: Daily Quota & Clear Chat
            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val isUnlimited = remainingQuota < 0
                val quotaText = if (isUnlimited) "UNLIMITED API KEY" else "DAILY QUOTA: $remainingQuota / 5"
                val quotaColor = when {
                    isUnlimited -> BellTextPrimary
                    remainingQuota > 1 -> BellTextPrimary
                    remainingQuota == 1 -> BellAmber
                    else -> BellDanger
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x14FFFFFF))
                        .border(1.dp, BellBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = quotaText,
                        color = quotaColor,
                        fontWeight = FontWeight.Black,
                        fontSize = 8.5.sp,
                        letterSpacing = 0.5.sp
                    )
                }

                if (chatMessages.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(BellDanger.copy(alpha = 0.14f))
                            .border(1.dp, BellDanger.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .clickable {
                                chatMessages.clear()
                                streamState = AIStreamState.Idle
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteSweep,
                                contentDescription = null,
                                tint = BellDanger,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "CLEAR SESSION",
                                color = BellDanger,
                                fontWeight = FontWeight.Black,
                                fontSize = 8.5.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }

        // =====================================================================
        // PANEL KANAN: CHAT FEED & INPUT BAR
        // =====================================================================
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(14.dp))
                .background(BellGlassCard)
                .border(1.dp, BellBorder, RoundedCornerShape(14.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Area Percakapan
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(BellGlassInput)
                    .border(1.dp, BellBorder, RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                if (chatMessages.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x1AFFFFFF))
                                .border(1.dp, BellBorderActive, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Psychology,
                                contentDescription = null,
                                tint = BellTextPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = safeAiTr("ai.welcome_title", "HOW CAN BELL AI HELP?").uppercase(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.6.sp,
                            color = BellTextPrimary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = safeAiTr(
                                "ai.welcome_desc",
                                "Ask about crash logs, mod compatibility, renderer optimization, or Java arguments for your active instance."
                            ),
                            fontSize = 9.5.sp,
                            color = BellTextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 14.sp,
                            modifier = Modifier.fillMaxWidth(0.82f)
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(chatMessages, key = { it.id }) { msg ->
                            val isUser = msg.role == "user"
                            val isStreamingThis = !isUser &&
                                    streamState is AIStreamState.Streaming &&
                                    msg.id == chatMessages.lastOrNull()?.id

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                            ) {
                                if (!isUser) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(BellPillWhite),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "AI",
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Black,
                                            color = BellInkDark
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(7.dp))
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(if (isUser) 0.78f else 0.91f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isUser) Color(0x26FFFFFF) else BellGlassCard
                                        )
                                        .border(
                                            1.dp,
                                            if (isUser) BellBorderActive else BellBorder,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .padding(9.dp)
                                ) {
                                    Column {
                                        if (!isUser) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "BELL AI DIAGNOSTICS",
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 8.sp,
                                                    color = BellTextSecondary,
                                                    letterSpacing = 0.6.sp
                                                )

                                                if (msg.content.isNotBlank() && !isStreamingThis) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.ContentCopy,
                                                        contentDescription = "Copy",
                                                        tint = BellTextSecondary,
                                                        modifier = Modifier
                                                            .size(13.dp)
                                                            .clickable {
                                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                                clipboard.setPrimaryClip(ClipData.newPlainText("AI Answer", msg.content))
                                                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                                            }
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                        }

                                        if (isUser) {
                                            Text(
                                                text = msg.content,
                                                color = BellTextPrimary,
                                                fontSize = 10.sp,
                                                lineHeight = 14.sp
                                            )
                                        } else {
                                            if (msg.content.isBlank() && (streamState is AIStreamState.Connecting || streamState is AIStreamState.Idle)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(vertical = 4.dp)
                                                ) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(11.dp),
                                                        color = BellTextPrimary,
                                                        strokeWidth = 1.5.dp
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = safeAiTr("ai.thinking", "Analyzing instance & thinking..."),
                                                        fontSize = 9.sp,
                                                        color = BellTextSecondary
                                                    )
                                                }
                                            } else {
                                                NuxMarkdownView(
                                                    markdownText = msg.content,
                                                    isStreaming = isStreamingThis,
                                                    showCursor = isCursorBlinkVisible && isStreamingThis
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

            // =================================================================
            // INPUT BAR BAWAH (MONOCHROME GLASS + CRISP WHITE SEND BUTTON)
            // =================================================================
            val canSend = !isBusy && currentInputText.isNotBlank()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(BellGlassInput)
                    .border(1.dp, BellBorderActive, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (currentInputText.isEmpty()) {
                        Text(
                            text = safeAiTr("ai.input_placeholder", "Ask Bell AI about crashes, mods, or FPS optimization..."),
                            fontSize = 9.5.sp,
                            color = BellTextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    BasicTextField(
                        value = currentInputText,
                        onValueChange = { currentInputText = it },
                        textStyle = TextStyle(
                            color = BellTextPrimary,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(BellPillWhite),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Tombol Kirim Putih Solid
                Box(
                    modifier = Modifier
                        .height(26.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(if (canSend) BellPillWhite else Color(0x1FFFFFFF))
                        .clickable(enabled = canSend) {
                            sendMessage(currentInputText)
                        }
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(11.dp),
                            color = BellTextPrimary,
                            strokeWidth = 1.5.dp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "SEND",
                                fontWeight = FontWeight.Black,
                                fontSize = 8.5.sp,
                                letterSpacing = 0.5.sp,
                                color = if (canSend) BellInkDark else BellTextMuted
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Send,
                                contentDescription = "Send",
                                tint = if (canSend) BellInkDark else BellTextMuted,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
