package com.israadev.nuxlauncher.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import com.israadev.nuxlauncher.core.lang.tr
import com.israadev.nuxlauncher.core.lang.trStr
import com.israadev.nuxlauncher.core.settings.SettingsManager
import com.israadev.nuxlauncher.ui.components.NuxMarkdownView
import com.israadev.nuxlauncher.ui.theme.NuxColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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

    val outerShape = RoundedCornerShape(12.dp)
    val cardShape = RoundedCornerShape(10.dp)
    val pillShape = RoundedCornerShape(8.dp)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(outerShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xE60F172A),
                            Color(0xF2090E1A)
                        )
                    )
                )
                .border(1.dp, NuxColors.CardBorder, outerShape)
                .padding(10.dp)
        ) {
            // ==========================================
            // 1. TOP HEADER BAR
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Tombol Back
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF131C31))
                            .border(1.dp, NuxColors.CardBorder, RoundedCornerShape(8.dp))
                            .clickable { onNavigateBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowBack,
                            contentDescription = tr("common.back"),
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Cobalt AI Icon
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF2563EB).copy(alpha = 0.20f))
                            .border(1.dp, Color(0xFF3B82F6).copy(alpha = 0.55f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Psychology,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = tr("ai.title"),
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val modelTag = when {
                                effectiveModel.contains("lightning", ignoreCase = true) -> "Nemotron Lightning"
                                effectiveModel.contains("120b", ignoreCase = true) -> "Nemotron 120B"
                                effectiveModel.contains("550b", ignoreCase = true) -> "Nemotron 550B"
                                effectiveModel.contains("gemini", ignoreCase = true) -> "Gemini Flash"
                                effectiveModel.contains("llama", ignoreCase = true) -> "Llama 3.3"
                                effectiveModel.contains("qwen", ignoreCase = true) -> "Qwen 3.8"
                                effectiveModel.contains("deepseek", ignoreCase = true) -> "DeepSeek R1"
                                effectiveModel.contains("mistral", ignoreCase = true) -> "Mistral"
                                else -> "Nemotron AI"
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF2563EB).copy(alpha = 0.22f))
                                    .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.45f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = modelTag,
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8)
                                )
                            }
                        }
                        Text(
                            text = tr("ai.subtitle"),
                            fontSize = 8.5.sp,
                            color = NuxColors.GrayNeutral
                        )
                    }
                }

                // Controls Kanan: Quota Pill & Clear Chat
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val isUnlimited = remainingQuota < 0
                    val quotaText = if (isUnlimited) tr("ai.quota_unlimited") else tr("ai.quota_badge", remainingQuota)
                    val quotaColor = when {
                        isUnlimited -> Color(0xFF38BDF8)
                        remainingQuota > 1 -> Color(0xFF38BDF8)
                        remainingQuota == 1 -> Color(0xFFFBBF24)
                        else -> Color(0xFFF43F5E)
                    }

                    Box(
                        modifier = Modifier
                            .clip(pillShape)
                            .background(quotaColor.copy(alpha = 0.14f))
                            .border(1.dp, quotaColor.copy(alpha = 0.40f), pillShape)
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = quotaText,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                            color = quotaColor
                        )
                    }

                    if (chatMessages.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .height(26.dp)
                                .clip(pillShape)
                                .background(Color(0xFF131C31))
                                .border(1.dp, NuxColors.CardBorder, pillShape)
                                .clickable {
                                    chatMessages.clear()
                                    streamState = AIStreamState.Idle
                                }
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteSweep,
                                    contentDescription = null,
                                    tint = Color(0xFFCBD5E1),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = tr("common.clear"),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFCBD5E1)
                                )
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 2. CONTEXT RIBBON & INSTANCE SELECTOR
            // ==========================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(pillShape)
                    .background(Color(0xFF0E1629))
                    .border(1.dp, NuxColors.CardBorder, pillShape)
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Layers,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = tr("ai.active_instance_label"),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = NuxColors.GrayNeutral
                        )
                        Text(
                            text = currentInstance?.name ?: "(None)",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (currentInstance != null) {
                            Text(
                                text = " • ${currentInstance.mcVersion} (${currentInstance.loader.uppercase()}) • RENDERER: ${settings.selectedRenderer.uppercase()}",
                                fontSize = 8.sp,
                                color = Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (instances.size > 1) {
                        Box {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF1E293B))
                                    .clickable { showInstanceSelectorMenu = true }
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(tr("ai.btn_switch"), fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                                Icon(Icons.Outlined.ArrowDropDown, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(12.dp))
                            }

                            DropdownMenu(
                                expanded = showInstanceSelectorMenu,
                                onDismissRequest = { showInstanceSelectorMenu = false }
                            ) {
                                instances.forEachIndexed { idx, inst ->
                                    DropdownMenuItem(
                                        text = { Text("${inst.name} (${inst.mcVersion})", fontSize = 10.sp) },
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
            }

            Spacer(modifier = Modifier.height(6.dp))

            // ==========================================
            // 3. CHAT HISTORY CONVERSATION
            // ==========================================
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(Color(0xFF090E1A))
                    .border(1.dp, NuxColors.CardBorder, cardShape)
                    .padding(8.dp)
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
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF2563EB).copy(alpha = 0.16f))
                                .border(1.dp, Color(0xFF3B82F6).copy(alpha = 0.50f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AutoFixHigh,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = tr("ai.welcome_title"),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = tr("ai.welcome_desc"),
                            fontSize = 9.5.sp,
                            color = NuxColors.GrayNeutral,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 13.sp,
                            modifier = Modifier.fillMaxWidth(0.85f)
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        items(chatMessages, key = { it.id }) { msg ->
                            val isUser = msg.role == "user"
                            val isStreamingThis = !isUser && streamState is AIStreamState.Streaming && msg.id == chatMessages.lastOrNull()?.id

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                            ) {
                                if (!isUser) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF2563EB).copy(alpha = 0.20f))
                                            .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.50f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("AI", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color(0xFF38BDF8))
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(if (isUser) 0.8f else 0.92f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isUser) Color(0xFF162238) else Color(0xFF0F172A)
                                        )
                                        .border(
                                            1.dp,
                                            if (isUser) Color(0x5538BDF8) else Color(0x402563EB),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .padding(8.dp)
                                ) {
                                    Column {
                                        if (!isUser) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = tr("ai.agent_name"),
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 7.5.sp,
                                                    color = Color(0xFF38BDF8),
                                                    letterSpacing = 0.5.sp
                                                )

                                                if (msg.content.isNotBlank() && !isStreamingThis) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.ContentCopy,
                                                        contentDescription = "Copy",
                                                        tint = Color(0xFF64748B),
                                                        modifier = Modifier
                                                            .size(13.dp)
                                                            .clickable {
                                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                                clipboard.setPrimaryClip(ClipData.newPlainText("AI Answer", msg.content))
                                                                Toast.makeText(context, trStr("common.copied"), Toast.LENGTH_SHORT).show()
                                                            }
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                        }

                                        if (isUser) {
                                            Text(
                                                text = msg.content,
                                                color = Color.White,
                                                fontSize = 9.5.sp,
                                                lineHeight = 13.5.sp
                                            )
                                        } else {
                                            if (msg.content.isBlank() && (streamState is AIStreamState.Connecting || streamState is AIStreamState.Idle)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(vertical = 4.dp)
                                                ) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(11.dp),
                                                        color = Color(0xFF38BDF8),
                                                        strokeWidth = 1.5.dp
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = tr("ai.thinking"),
                                                        fontSize = 8.5.sp,
                                                        color = NuxColors.GrayNeutral
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

            Spacer(modifier = Modifier.height(6.dp))

            // ==========================================
            // 4. INPUT FIELD BAR
            // ==========================================
            val isBusy = streamState is AIStreamState.Connecting || streamState is AIStreamState.Streaming

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .clip(pillShape)
                    .background(Color(0xFF0E1629), pillShape)
                    .border(1.dp, NuxColors.CardBorder, pillShape)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (currentInputText.isEmpty()) {
                        Text(
                            text = tr("ai.input_placeholder"),
                            fontSize = 8.5.sp,
                            color = Color(0xFF64748B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    BasicTextField(
                        value = currentInputText,
                        onValueChange = { currentInputText = it },
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(Color(0xFF38BDF8)),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Send Button (Cobalt Blue)
                Box(
                    modifier = Modifier
                        .height(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isBusy || currentInputText.isBlank()) {
                                Brush.horizontalGradient(listOf(Color(0xFF1E293B), Color(0xFF1E293B)))
                            } else {
                                Brush.horizontalGradient(listOf(Color(0xFF1D4ED8), Color(0xFF2563EB)))
                            }
                        )
                        .clickable(enabled = !isBusy && currentInputText.isNotBlank()) {
                            sendMessage(currentInputText)
                        }
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(11.dp),
                            color = Color(0xFF38BDF8),
                            strokeWidth = 1.5.dp
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = tr("ai.btn_send"),
                                fontWeight = FontWeight.Black,
                                fontSize = 8.sp,
                                color = if (currentInputText.isNotBlank()) Color.White else Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                imageVector = Icons.Outlined.Send,
                                contentDescription = tr("ai.btn_send"),
                                tint = if (currentInputText.isNotBlank()) Color.White else Color(0xFF64748B),
                                modifier = Modifier.size(9.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
