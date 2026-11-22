package com.israadev.nuxlauncher.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.israadev.nuxlauncher.R
import com.israadev.nuxlauncher.core.controls.ControlLayoutManager
import com.israadev.nuxlauncher.core.controls.KeycodeCatalog
import com.israadev.nuxlauncher.core.controls.models.CustomControlButton
import com.israadev.nuxlauncher.core.lang.NuxLanguageManager
import com.movtery.inputmap.keycodes.LwjglGlfwKeycode
import java.util.UUID
import kotlin.math.roundToInt

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
private val BellDanger = Color(0xFFF87171)

@Composable
fun CustomGuiEditorScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    isIngame: Boolean = false
) {
    val context = LocalContext.current
    val langVersion by NuxLanguageManager.languageVersion.collectAsState()
    val savedButtons by ControlLayoutManager.buttons.collectAsState()

    var buttonsList by remember { mutableStateOf(savedButtons.ifEmpty { ControlLayoutManager.getDefaultButtons() }) }
    var selectedButtonId by remember { mutableStateOf<String?>(null) }
    var showKeyPickerForButtonId by remember { mutableStateOf<String?>(null) }
    var keyPickerTargetMode by remember { mutableStateOf("MAIN") } // "MAIN" or "COMBO"
    var keyPickerSearchQuery by remember { mutableStateOf("") }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var manualFlipSide by remember { mutableStateOf<Boolean?>(null) }

    // Mode sembunyi visual: GUI menghilang secara visual, namun tetap bisa disentuh di posisinya
    var hideVisualHud by remember { mutableStateOf(false) }
    val hiddenButtonIds = remember { mutableStateListOf<String>() }

    LaunchedEffect(Unit) {
        ControlLayoutManager.init(context)
        if (ControlLayoutManager.buttons.value.isNotEmpty()) {
            buttonsList = ControlLayoutManager.buttons.value
        }
    }

    LaunchedEffect(savedButtons) {
        if (savedButtons.isNotEmpty() && buttonsList.isEmpty()) {
            buttonsList = savedButtons
        }
    }

    val selectedButton = remember(buttonsList, selectedButtonId) {
        buttonsList.firstOrNull { it.id == selectedButtonId }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(if (isIngame) Color.Black.copy(alpha = 0.35f) else BellBgCanvas)
    ) {
        val screenWidthPx = constraints.maxWidth.toFloat()
        val screenHeightPx = constraints.maxHeight.toFloat()
        val density = LocalDensity.current

        // 1. Blueprint Dot Grid Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 32.dp.toPx()
            val dotColor = if (isIngame) Color.White.copy(alpha = 0.14f) else Color(0x26FFFFFF)
            val guideLineColor = if (isIngame) Color.White.copy(alpha = 0.10f) else Color(0x1AFFFFFF)
            var x = step
            while (x < size.width) {
                var y = step
                while (y < size.height) {
                    drawCircle(dotColor, radius = 1.4.dp.toPx(), center = Offset(x, y))
                    y += step
                }
                x += step
            }

            drawLine(
                color = guideLineColor,
                start = Offset(size.width / 2f, 0f),
                end = Offset(size.width / 2f, size.height),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = guideLineColor,
                start = Offset(0f, size.height / 2f),
                end = Offset(size.width, size.height / 2f),
                strokeWidth = 1.dp.toPx()
            )
        }

        // 2. Interactive Draggable Buttons on Screen
        buttonsList.forEach { btn ->
            val isSelected = btn.id == selectedButtonId
            val isHideGuiButton = btn.isSystem && btn.systemAction == "HIDE_GUI"
            val isStealthHidden =
                (!isSelected && (hideVisualHud || hiddenButtonIds.contains(btn.id))) ||
                    (isHideGuiButton && hideVisualHud)

            val btnWidthPx = with(density) { btn.widthDp.dp.toPx() }
            val btnHeightPx = with(density) { btn.heightDp.dp.toPx() }

            val centerX = screenWidthPx * (btn.xPercent / 100f)
            val centerY = screenHeightPx * (btn.yPercent / 100f)

            val leftPx = (centerX - btnWidthPx / 2f).coerceIn(0f, (screenWidthPx - btnWidthPx).coerceAtLeast(0f))
            val topPx = (centerY - btnHeightPx / 2f).coerceIn(0f, (screenHeightPx - btnHeightPx).coerceAtLeast(0f))

            val leftDp = with(density) { leftPx.toDp() }
            val topDp = with(density) { topPx.toDp() }

            val btnBg = when {
                isStealthHidden -> Color.Transparent
                isSelected -> BellPillWhite
                btn.isSystem -> when (btn.systemAction) {
                    "CLOSE" -> BellDanger.copy(alpha = 0.28f)
                    "RECORD" -> Color(0x4DD97706)
                    else -> BellGlassCard
                }
                else -> BellGlassCard
            }

            val btnBorderColor = when {
                isStealthHidden -> Color.Transparent
                isSelected -> BellPillWhite
                btn.isSystem -> when (btn.systemAction) {
                    "CLOSE" -> BellDanger.copy(alpha = 0.6f)
                    else -> BellBorderActive
                }
                else -> BellBorderActive
            }

            val textColor = when {
                isStealthHidden -> Color.Transparent
                isSelected -> BellInkDark
                else -> BellTextPrimary
            }

            val accentTextColor = when {
                isStealthHidden -> Color.Transparent
                isSelected -> BellInkDark
                else -> BellTextSecondary
            }

            Box(
                modifier = Modifier
                    .offset(x = leftDp, y = topDp)
                    .size(width = btn.widthDp.dp, height = btn.heightDp.dp)
                    .alpha(if (isStealthHidden) 0f else btn.opacity)
                    .clip(RoundedCornerShape(btn.cornerRadiusDp.dp))
                    .background(btnBg)
                    .border(
                        if (isSelected) 2.dp else 1.dp,
                        btnBorderColor,
                        RoundedCornerShape(btn.cornerRadiusDp.dp)
                    )
                    .pointerInput(btn.id, isStealthHidden) {
                        detectTapGestures {
                            if (isHideGuiButton) {
                                hideVisualHud = !hideVisualHud
                                return@detectTapGestures
                            }
                            manualFlipSide = null
                            selectedButtonId = if (selectedButtonId == btn.id) null else btn.id
                        }
                    }
                    .pointerInput(btn.id, screenWidthPx, screenHeightPx) {
                        detectDragGestures(
                            onDragStart = {
                                manualFlipSide = null
                                selectedButtonId = btn.id
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val currentBtn = buttonsList.firstOrNull { it.id == btn.id } ?: return@detectDragGestures
                                val curCenterX = screenWidthPx * (currentBtn.xPercent / 100f)
                                val curCenterY = screenHeightPx * (currentBtn.yPercent / 100f)

                                val newCenterX = (curCenterX + dragAmount.x).coerceIn(btnWidthPx / 2f, screenWidthPx - btnWidthPx / 2f)
                                val newCenterY = (curCenterY + dragAmount.y).coerceIn(btnHeightPx / 2f, screenHeightPx - btnHeightPx / 2f)

                                val newXPercent = (newCenterX / screenWidthPx) * 100f
                                val newYPercent = (newCenterY / screenHeightPx) * 100f

                                buttonsList = buttonsList.map {
                                    if (it.id == btn.id) it.copy(xPercent = newXPercent, yPercent = newYPercent) else it
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (btn.isJoystick) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val r = size.minDimension / 2f
                            val c = Offset(size.width / 2f, size.height / 2f)

                            drawCircle(
                                color = if (isStealthHidden) Color.Transparent else if (isSelected) BellPillWhite else BellBorderActive,
                                radius = r - 2.dp.toPx(),
                                center = c,
                                style = Stroke(width = 2.dp.toPx())
                            )
                            drawLine(
                                color = if (isStealthHidden) Color.Transparent else BellBorder,
                                start = Offset(c.x, c.y - r * 0.7f),
                                end = Offset(c.x, c.y + r * 0.7f),
                                strokeWidth = 1.dp.toPx()
                            )
                            drawLine(
                                color = if (isStealthHidden) Color.Transparent else BellBorder,
                                start = Offset(c.x - r * 0.7f, c.y),
                                end = Offset(c.x + r * 0.7f, c.y),
                                strokeWidth = 1.dp.toPx()
                            )

                            val knobRadius = r * 0.38f
                            drawCircle(
                                color = if (isStealthHidden) Color.Transparent else if (isSelected) BellInkDark else BellGlassInput,
                                radius = knobRadius,
                                center = c
                            )
                            drawCircle(
                                color = if (isStealthHidden) Color.Transparent else BellPillWhite,
                                radius = knobRadius,
                                center = c,
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                            drawCircle(
                                color = if (isStealthHidden) Color.Transparent else BellPillWhite,
                                radius = 3.5.dp.toPx(),
                                center = c
                            )
                        }
                        Text("W", color = accentTextColor, fontSize = 8.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.TopCenter).padding(top = 4.dp))
                        Text("S", color = accentTextColor, fontSize = 8.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp))
                        Text("A", color = accentTextColor, fontSize = 8.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.CenterStart).padding(start = 5.dp))
                        Text("D", color = accentTextColor, fontSize = 8.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 5.dp))
                    }
                } else if (btn.isScroll) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(vertical = 4.dp)
                    ) {
                        Text("▲", color = accentTextColor, fontSize = 8.sp, fontWeight = FontWeight.Black)
                        Text(btn.name, color = textColor, fontWeight = FontWeight.Black, fontSize = 9.sp, maxLines = 1)
                        Text("▼", color = accentTextColor, fontSize = 8.sp, fontWeight = FontWeight.Black)
                    }
                } else if (btn.isSystem) {
                    when (btn.systemAction) {
                        "FPS" -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(if (isStealthHidden) Color.Transparent else BellPillWhite, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("FPS: --", color = textColor, fontWeight = FontWeight.Black, fontSize = 10.sp, maxLines = 1)
                            }
                        }
                        "KEYBOARD" -> {
                            Text("KEYBOARD", color = textColor, fontWeight = FontWeight.Black, fontSize = 10.sp, maxLines = 1)
                        }
                        "HIDE_GUI" -> {
                            if (!hideVisualHud) {
                                Text("HIDE GUI", color = accentTextColor, fontWeight = FontWeight.Black, fontSize = 10.sp, maxLines = 1)
                            }
                        }
                        "CLOSE" -> {
                            Text(
                                text = "✕",
                                color = if (isStealthHidden) Color.Transparent else BellDanger,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }
                        else -> {
                            Text(btn.name, color = textColor, fontWeight = FontWeight.Black, fontSize = 11.sp, maxLines = 1)
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = btn.name,
                            color = textColor,
                            fontWeight = FontWeight.Black,
                            fontSize = if (btn.name.length > 5) 10.sp else 12.sp,
                            maxLines = 1
                        )
                        if (btn.isToggle) {
                            Text("TOGGLE", color = accentTextColor, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                        }
                        if (btn.isMacro) {
                            Text(
                                text = when (btn.macroType) {
                                    "COMMAND" -> "⚡CMD"
                                    "COMBO" -> "⚡CMB"
                                    "TURBO" -> "⚡TRB"
                                    else -> "⚡MAC"
                                },
                                color = if (isStealthHidden) Color.Transparent else if (isSelected) BellInkDark else BellTextSecondary,
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }

        // 3. TOP ACTION BAR (BELL GLASS STYLE)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(if (isIngame) BellPillWhite else BellGlassSidebar)
                .border(1.dp, if (isIngame) BellPillWhite else BellBorder, RoundedCornerShape(9.dp))
                .clickable {
                    if (isIngame) {
                        ControlLayoutManager.saveButtons(context, buttonsList)
                        Toast.makeText(context, "✓ In-game button layout saved!", Toast.LENGTH_SHORT).show()
                    }
                    onNavigateBack()
                }
                .padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            val topBtnContentColor = if (isIngame) BellInkDark else BellTextPrimary
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isIngame) Icons.Outlined.Check else Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = if (isIngame) "Done" else "Back",
                    tint = topBtnContentColor,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = if (isIngame) "DONE" else "BACK",
                    color = topBtnContentColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }

        // Top-Center: Compact Floating Action Pill (+ BUTTON, + JOYSTICK, RESET, SAVE)
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 14.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(BellGlassSidebar)
                .border(1.dp, BellBorder, RoundedCornerShape(11.dp))
                .padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(7.dp))
                    .background(BellGlassInput)
                    .border(1.dp, BellBorder, RoundedCornerShape(7.dp))
                    .clickable {
                        val newBtn = CustomControlButton(
                            id = "btn_" + UUID.randomUUID().toString().take(6),
                            name = "NEW",
                            keyCode = LwjglGlfwKeycode.GLFW_KEY_SPACE,
                            xPercent = 50f,
                            yPercent = 50f,
                            widthDp = 52,
                            heightDp = 48
                        )
                        buttonsList = buttonsList + newBtn
                        selectedButtonId = newBtn.id
                        manualFlipSide = null
                        Toast.makeText(context, "New button added to center screen!", Toast.LENGTH_SHORT).show()
                    }
                    .padding(horizontal = 11.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("+ BUTTON", fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 0.4.sp, color = BellTextPrimary)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(7.dp))
                    .background(BellGlassInput)
                    .border(1.dp, BellBorder, RoundedCornerShape(7.dp))
                    .clickable {
                        val newJoy = CustomControlButton(
                            id = "joy_" + UUID.randomUUID().toString().take(6),
                            name = "JOYSTICK",
                            isJoystick = true,
                            xPercent = 20f,
                            yPercent = 70f,
                            widthDp = 130,
                            heightDp = 130,
                            cornerRadiusDp = 65,
                            opacity = 0.85f
                        )
                        buttonsList = buttonsList + newJoy
                        selectedButtonId = newJoy.id
                        manualFlipSide = null
                        Toast.makeText(context, "WASD Joystick added!", Toast.LENGTH_SHORT).show()
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_joystick),
                        contentDescription = null,
                        tint = BellTextSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ JOYSTICK", fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 0.4.sp, color = BellTextSecondary)
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(7.dp))
                    .background(BellDanger.copy(alpha = 0.16f))
                    .border(1.dp, BellDanger.copy(alpha = 0.4f), RoundedCornerShape(7.dp))
                    .clickable { showResetConfirmDialog = true }
                    .padding(horizontal = 11.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("RESET", color = BellDanger, fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 0.4.sp)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(7.dp))
                    .background(BellPillWhite)
                    .clickable {
                        ControlLayoutManager.saveButtons(context, buttonsList)
                        Toast.makeText(context, "✓ GUI layout saved successfully!", Toast.LENGTH_SHORT).show()
                    }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("SAVE", color = BellInkDark, fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 0.5.sp)
            }
        }

        // 4. SMART AUTO-DOCKING GLASS INSPECTOR PANEL
        if (selectedButton != null) {
            val shouldDockLeft = manualFlipSide ?: (selectedButton.xPercent > 48f)
            val panelAlignment = if (shouldDockLeft) Alignment.CenterStart else Alignment.CenterEnd
            val panelPadding = if (shouldDockLeft) PaddingValues(start = 14.dp) else PaddingValues(end = 14.dp)
            val isBtnHidden = hiddenButtonIds.contains(selectedButton.id)

            Box(
                modifier = Modifier
                    .align(panelAlignment)
                    .padding(panelPadding)
            ) {
                Box(
                    modifier = Modifier
                        .width(280.dp)
                        .fillMaxHeight(0.84f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(BellGlassElevated)
                        .border(1.dp, BellBorder, RoundedCornerShape(14.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "BUTTON SETTINGS",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    letterSpacing = 0.6.sp,
                                    color = BellTextPrimary
                                )
                                Text(
                                    text = "Position: ${selectedButton.xPercent.roundToInt()}% × ${selectedButton.yPercent.roundToInt()}%",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BellTextMuted
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(BellGlassInput)
                                        .border(1.dp, BellBorder, CircleShape)
                                        .clickable { manualFlipSide = !shouldDockLeft },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("⇄", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BellTextPrimary)
                                }

                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(BellDanger.copy(alpha = 0.16f))
                                        .border(1.dp, BellDanger.copy(alpha = 0.4f), CircleShape)
                                        .clickable { selectedButtonId = null },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("✕", fontSize = 11.sp, fontWeight = FontWeight.Black, color = BellDanger)
                                }
                            }
                        }

                        HorizontalDivider(color = BellBorder, thickness = 1.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Invisible Touch", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BellTextPrimary)
                                Text("Hide visual, keep tap area active", fontSize = 8.5.sp, color = BellTextMuted)
                            }
                            Switch(
                                checked = isBtnHidden,
                                onCheckedChange = { shouldHide ->
                                    if (shouldHide) {
                                        if (!hiddenButtonIds.contains(selectedButton.id)) hiddenButtonIds.add(selectedButton.id)
                                    } else {
                                        hiddenButtonIds.remove(selectedButton.id)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = BellInkDark,
                                    checkedTrackColor = BellPillWhite,
                                    uncheckedThumbColor = BellTextSecondary,
                                    uncheckedTrackColor = Color(0x26FFFFFF)
                                )
                            )
                        }

                        Text("BUTTON LABEL TEXT", fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.6.sp, color = BellTextMuted)
                        if (selectedButton.isSystem) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BellGlassInput)
                                    .border(1.dp, BellBorder, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 9.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(selectedButton.name, color = BellTextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_lock),
                                            contentDescription = null,
                                            tint = BellTextMuted,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Fixed", color = BellTextMuted, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BellGlassInput)
                                    .border(1.dp, BellBorder, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 9.dp)
                            ) {
                                if (selectedButton.name.isEmpty()) {
                                    Text("Button label...", color = BellTextMuted, fontSize = 12.sp)
                                }
                                BasicTextField(
                                    value = selectedButton.name,
                                    onValueChange = { newName ->
                                        buttonsList = buttonsList.map { if (it.id == selectedButton.id) it.copy(name = newName) else it }
                                    },
                                    textStyle = TextStyle(color = BellTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                                    cursorBrush = SolidColor(BellPillWhite),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        if (selectedButton.isJoystick) {
                            Text("CONTROL TYPE", fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.6.sp, color = BellTextMuted)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BellGlassInput)
                                    .border(1.dp, BellBorderActive, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_joystick),
                                            contentDescription = null,
                                            tint = BellTextPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text("Virtual Analog Joystick (WASD)", color = BellTextPrimary, fontWeight = FontWeight.Black, fontSize = 10.5.sp)
                                    }
                                    Text("Smooth 8-way analog movement replacing W, A, S, D keys.", color = BellTextMuted, fontSize = 8.5.sp)
                                }
                            }

                            BellEditorSlider(
                                label = "Joystick Diameter",
                                valueText = "${selectedButton.widthDp} dp",
                                value = selectedButton.widthDp.toFloat(),
                                range = 80f..220f,
                                onValueChange = { newD ->
                                    val d = newD.roundToInt()
                                    buttonsList = buttonsList.map {
                                        if (it.id == selectedButton.id) it.copy(widthDp = d, heightDp = d, cornerRadiusDp = d / 2) else it
                                    }
                                }
                            )

                            BellEditorSlider(
                                label = "Opacity",
                                valueText = "${(selectedButton.opacity * 100).roundToInt()}%",
                                value = selectedButton.opacity,
                                range = 0.15f..1.0f,
                                onValueChange = { newOp ->
                                    buttonsList = buttonsList.map {
                                        if (it.id == selectedButton.id) it.copy(opacity = (newOp * 100).roundToInt() / 100f) else it
                                    }
                                }
                            )
                        } else {
                            Text("KEY / INPUT ACTION", fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.6.sp, color = BellTextMuted)
                            if (selectedButton.isSystem) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(BellGlassInput)
                                        .border(1.dp, BellBorder, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 9.dp)
                                ) {
                                    val actionDesc = when (selectedButton.systemAction) {
                                        "FPS" -> "In-Game FPS Indicator"
                                        "KEYBOARD" -> "Toggle On-Screen Keyboard"
                                        "HIDE_GUI" -> "Hide GUI (Invisible Toggle)"
                                        "CLOSE" -> "Quit Game"
                                        "RECORD" -> "Screen Recorder"
                                        else -> "System Action"
                                    }
                                    Text(actionDesc, color = BellTextPrimary, fontWeight = FontWeight.Bold, fontSize = 10.5.sp)
                                }
                            } else {
                                val mappedKeyName = remember(selectedButton) {
                                    if (selectedButton.isScroll) {
                                        "Scroll Wheel (Slide Up/Down)"
                                    } else if (selectedButton.isMouseButton) {
                                        when (selectedButton.mouseButton) {
                                            LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT -> "Left Mouse (Attack)"
                                            LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_RIGHT -> "Right Mouse (Use)"
                                            LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_MIDDLE -> "Middle Mouse (Pick)"
                                            else -> "Mouse Btn ${selectedButton.mouseButton}"
                                        }
                                    } else {
                                        KeycodeCatalog.ALL_KEYS.firstOrNull { it.keyCode == selectedButton.keyCode }?.displayName
                                            ?: "Key Code ${selectedButton.keyCode}"
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(BellGlassInput)
                                        .border(1.dp, BellBorderActive, RoundedCornerShape(8.dp))
                                        .clickable {
                                            keyPickerTargetMode = "MAIN"
                                            keyPickerSearchQuery = ""
                                            showKeyPickerForButtonId = selectedButton.id
                                        }
                                        .padding(horizontal = 12.dp, vertical = 9.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = mappedKeyName,
                                            color = BellTextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.5.sp,
                                            maxLines = 1,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("CHANGE ›", color = BellTextSecondary, fontWeight = FontWeight.Black, fontSize = 9.sp)
                                    }
                                }
                            }

                            BellEditorSlider(
                                label = "Width",
                                valueText = "${selectedButton.widthDp} dp",
                                value = selectedButton.widthDp.toFloat(),
                                range = 24f..160f,
                                onValueChange = { newW ->
                                    buttonsList = buttonsList.map { if (it.id == selectedButton.id) it.copy(widthDp = newW.roundToInt()) else it }
                                }
                            )

                            BellEditorSlider(
                                label = "Height",
                                valueText = "${selectedButton.heightDp} dp",
                                value = selectedButton.heightDp.toFloat(),
                                range = 24f..160f,
                                onValueChange = { newH ->
                                    buttonsList = buttonsList.map { if (it.id == selectedButton.id) it.copy(heightDp = newH.roundToInt()) else it }
                                }
                            )

                            BellEditorSlider(
                                label = "Opacity",
                                valueText = "${(selectedButton.opacity * 100).roundToInt()}%",
                                value = selectedButton.opacity,
                                range = 0.15f..1.0f,
                                onValueChange = { newOp ->
                                    buttonsList = buttonsList.map {
                                        if (it.id == selectedButton.id) it.copy(opacity = (newOp * 100).roundToInt() / 100f) else it
                                    }
                                }
                            )

                            BellEditorSlider(
                                label = "Corner Radius",
                                valueText = "${selectedButton.cornerRadiusDp} dp",
                                value = selectedButton.cornerRadiusDp.toFloat(),
                                range = 0f..28f,
                                onValueChange = { newR ->
                                    buttonsList = buttonsList.map { if (it.id == selectedButton.id) it.copy(cornerRadiusDp = newR.roundToInt()) else it }
                                }
                            )
                        }

                        if (!selectedButton.isSystem && !selectedButton.isJoystick) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Toggle Mode", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BellTextPrimary)
                                    Text("Tap once to toggle/lock", fontSize = 8.5.sp, color = BellTextMuted)
                                }
                                Switch(
                                    checked = selectedButton.isToggle,
                                    onCheckedChange = { isToggled ->
                                        buttonsList = buttonsList.map { if (it.id == selectedButton.id) it.copy(isToggle = isToggled) else it }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = BellInkDark,
                                        checkedTrackColor = BellPillWhite,
                                        uncheckedThumbColor = BellTextSecondary,
                                        uncheckedTrackColor = Color(0x26FFFFFF)
                                    )
                                )
                            }
                        }

                        if (!selectedButton.isSystem && !selectedButton.isScroll && !selectedButton.isJoystick) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(BellGlassInput)
                                    .border(1.dp, if (selectedButton.isMacro) BellPillWhite else BellBorder, RoundedCornerShape(10.dp))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Outlined.Bolt,
                                                contentDescription = null,
                                                tint = BellTextPrimary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("MACRO FEATURES", fontSize = 10.sp, fontWeight = FontWeight.Black, color = BellTextPrimary)
                                            if (selectedButton.isMacro) {
                                                Spacer(modifier = Modifier.width(5.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(BellPillWhite)
                                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                                ) {
                                                    Text("ACTIVE", fontSize = 7.5.sp, fontWeight = FontWeight.Black, color = BellInkDark)
                                                }
                                            }
                                        }
                                        Text("Enable macro automation for this button", fontSize = 8.sp, color = BellTextMuted)
                                    }
                                    Switch(
                                        checked = selectedButton.isMacro,
                                        onCheckedChange = { isMacroEnabled ->
                                            buttonsList = buttonsList.map {
                                                if (it.id == selectedButton.id) it.copy(isMacro = isMacroEnabled) else it
                                            }
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = BellInkDark,
                                            checkedTrackColor = BellPillWhite,
                                            uncheckedThumbColor = BellTextSecondary,
                                            uncheckedTrackColor = Color(0x26FFFFFF)
                                        )
                                    )
                                }

                                if (selectedButton.isMacro) {
                                    HorizontalDivider(color = BellBorder, thickness = 1.dp)

                                    Text("SELECT MACRO MODE", fontSize = 8.5.sp, fontWeight = FontWeight.Black, color = BellTextMuted)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        val types = listOf(
                                            "COMMAND" to "CMD",
                                            "COMBO" to "COMBO",
                                            "TURBO" to "TURBO"
                                        )
                                        types.forEach { (typeKey, typeLabel) ->
                                            val isChosen = selectedButton.macroType == typeKey
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (isChosen) BellPillWhite else BellGlassCard)
                                                    .border(1.dp, if (isChosen) BellPillWhite else BellBorder, RoundedCornerShape(6.dp))
                                                    .clickable {
                                                        buttonsList = buttonsList.map {
                                                            if (it.id == selectedButton.id) it.copy(macroType = typeKey) else it
                                                        }
                                                    }
                                                    .padding(vertical = 6.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = typeLabel,
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = if (isChosen) BellInkDark else BellTextSecondary,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }

                                    if (selectedButton.macroType == "COMMAND") {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text("Auto-typed text / command:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = BellTextPrimary)
                                            BasicTextField(
                                                value = selectedButton.macroCommand,
                                                onValueChange = { newCmd ->
                                                    buttonsList = buttonsList.map {
                                                        if (it.id == selectedButton.id) it.copy(macroCommand = newCmd) else it
                                                    }
                                                },
                                                textStyle = TextStyle(color = BellTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                                                cursorBrush = SolidColor(BellPillWhite),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(BellGlassCard)
                                                    .border(1.dp, BellBorder, RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                                decorationBox = { innerTextField ->
                                                    if (selectedButton.macroCommand.isEmpty()) {
                                                        Text("e.g. /gamemode creative", color = BellTextMuted, fontSize = 10.sp)
                                                    }
                                                    innerTextField()
                                                }
                                            )

                                            Text("Quick Templates:", fontSize = 8.sp, color = BellTextMuted)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                listOf("/gamemode c", "/gamemode s", "/spawn", "/home").forEach { sample ->
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(BellGlassCard)
                                                            .border(0.5.dp, BellBorder, RoundedCornerShape(4.dp))
                                                            .clickable {
                                                                buttonsList = buttonsList.map {
                                                                    if (it.id == selectedButton.id) it.copy(macroCommand = sample) else it
                                                                }
                                                            }
                                                            .padding(horizontal = 6.dp, vertical = 3.dp)
                                                    ) {
                                                        Text(sample, fontSize = 8.sp, color = BellTextSecondary, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (selectedButton.macroType == "COMBO") {
                                        val comboKeysList = remember(selectedButton.macroComboKeys, selectedButton.macroComboKey) {
                                            if (selectedButton.macroComboKeys.isNotEmpty()) {
                                                selectedButton.macroComboKeys
                                            } else if (selectedButton.macroComboKey != 0) {
                                                listOf(selectedButton.macroComboKey)
                                            } else {
                                                emptyList()
                                            }
                                        }

                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Combo Key Sequence:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = BellTextPrimary)
                                                if (comboKeysList.isNotEmpty()) {
                                                    Text(
                                                        text = "Clear All",
                                                        fontSize = 8.sp,
                                                        color = BellDanger,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.clickable {
                                                            buttonsList = buttonsList.map {
                                                                if (it.id == selectedButton.id) it.copy(macroComboKeys = emptyList(), macroComboKey = 0) else it
                                                            }
                                                        }
                                                    )
                                                }
                                            }

                                            if (comboKeysList.isEmpty()) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(BellGlassCard)
                                                        .border(1.dp, BellBorder, RoundedCornerShape(6.dp))
                                                        .padding(vertical = 10.dp, horizontal = 12.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("No combo keys added yet.", color = BellTextMuted, fontSize = 9.sp)
                                                }
                                            } else {
                                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    comboKeysList.forEachIndexed { index, keyCode ->
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(BellGlassCard)
                                                                .border(1.dp, BellBorder, RoundedCornerShape(6.dp))
                                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .clip(RoundedCornerShape(4.dp))
                                                                        .background(BellPillWhite)
                                                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                                                ) {
                                                                    Text("#${index + 1}", fontSize = 8.sp, fontWeight = FontWeight.Black, color = BellInkDark)
                                                                }
                                                                Spacer(modifier = Modifier.width(6.dp))
                                                                Text(
                                                                    text = KeycodeCatalog.getKeyName(keyCode),
                                                                    color = BellTextPrimary,
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 10.sp,
                                                                    maxLines = 1
                                                                )
                                                            }
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(20.dp)
                                                                    .clip(CircleShape)
                                                                    .background(BellDanger.copy(alpha = 0.16f))
                                                                    .clickable {
                                                                        val updated = comboKeysList.toMutableList().apply { removeAt(index) }
                                                                        buttonsList = buttonsList.map {
                                                                            if (it.id == selectedButton.id) {
                                                                                it.copy(macroComboKeys = updated, macroComboKey = updated.firstOrNull() ?: 0)
                                                                            } else it
                                                                        }
                                                                    },
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Text("✕", fontSize = 9.sp, color = BellDanger, fontWeight = FontWeight.Black)
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(BellPillWhite)
                                                    .clickable {
                                                        keyPickerTargetMode = "COMBO"
                                                        keyPickerSearchQuery = ""
                                                        showKeyPickerForButtonId = selectedButton.id
                                                    }
                                                    .padding(vertical = 7.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("+ ADD KEY TO COMBO", fontSize = 9.sp, color = BellInkDark, fontWeight = FontWeight.Black)
                                            }

                                            Text("Quick Add:", fontSize = 8.sp, color = BellTextMuted)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                listOf(
                                                    "F3" to LwjglGlfwKeycode.GLFW_KEY_F3,
                                                    "B" to LwjglGlfwKeycode.GLFW_KEY_B,
                                                    "SHIFT" to LwjglGlfwKeycode.GLFW_KEY_LEFT_SHIFT,
                                                    "CTRL" to LwjglGlfwKeycode.GLFW_KEY_LEFT_CONTROL,
                                                    "ALT" to LwjglGlfwKeycode.GLFW_KEY_LEFT_ALT,
                                                    "Q" to LwjglGlfwKeycode.GLFW_KEY_Q
                                                ).forEach { (label, code) ->
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(BellGlassCard)
                                                            .border(0.5.dp, BellBorder, RoundedCornerShape(4.dp))
                                                            .clickable {
                                                                val updated = comboKeysList + code
                                                                buttonsList = buttonsList.map {
                                                                    if (it.id == selectedButton.id) {
                                                                        it.copy(macroComboKeys = updated, macroComboKey = updated.firstOrNull() ?: 0)
                                                                    } else it
                                                                }
                                                            }
                                                            .padding(horizontal = 6.dp, vertical = 3.dp)
                                                    ) {
                                                        Text("+ $label", fontSize = 8.sp, color = BellTextSecondary, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (selectedButton.macroType == "TURBO") {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            val cps = (1000f / selectedButton.macroTurboIntervalMs.coerceAtLeast(1L)).roundToInt()
                                            BellEditorSlider(
                                                label = "Auto-Click Interval",
                                                valueText = "${selectedButton.macroTurboIntervalMs} ms (~$cps CPS)",
                                                value = selectedButton.macroTurboIntervalMs.toFloat(),
                                                range = 40f..500f,
                                                onValueChange = { newInterval ->
                                                    buttonsList = buttonsList.map {
                                                        if (it.id == selectedButton.id) it.copy(macroTurboIntervalMs = newInterval.toLong()) else it
                                                    }
                                                }
                                            )
                                            Text("Triggers repeated rapid presses while held.", fontSize = 8.sp, color = BellTextMuted)
                                        }
                                    }
                                }
                            }
                        }

                        if (!selectedButton.isSystem) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BellDanger.copy(alpha = 0.16f))
                                    .border(1.dp, BellDanger.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                                    .clickable {
                                        hiddenButtonIds.remove(selectedButton.id)
                                        buttonsList = buttonsList.filter { it.id != selectedButton.id }
                                        selectedButtonId = null
                                        Toast.makeText(context, "Button removed", Toast.LENGTH_SHORT).show()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("DELETE BUTTON", color = BellDanger, fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 0.5.sp)
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BellGlassInput)
                                    .border(1.dp, BellBorder, RoundedCornerShape(8.dp))
                                    .padding(vertical = 9.dp, horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "System buttons cannot be deleted",
                                    color = BellTextMuted,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.5.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. In-Game Mode Floating Status Badge
        if (isIngame) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(BellGlassSidebar)
                    .border(1.dp, BellBorder, RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(BellPillWhite, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = "IN-GAME MODE · Position buttons over game HUD then tap DONE",
                        color = BellTextSecondary,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Keycode Picker Dialog (Bell Monochrome Glass)
    if (showKeyPickerForButtonId != null) {
        val targetId = showKeyPickerForButtonId!!
        val isComboMode = keyPickerTargetMode == "COMBO"

        Dialog(onDismissRequest = {
            showKeyPickerForButtonId = null
            keyPickerSearchQuery = ""
        }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .fillMaxHeight(0.86f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(BellGlassElevated)
                    .border(1.dp, BellBorder, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isComboMode) "SELECT COMBO KEY" else "SELECT KEY INPUT",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                letterSpacing = 0.6.sp,
                                color = BellTextPrimary
                            )
                            Text(
                                text = if (isComboMode) "Executes sequentially with the primary key" else "Full keyboard: A-Z, F1-F12, numbers, modifiers & symbols",
                                fontSize = 9.5.sp,
                                color = BellTextSecondary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(BellGlassInput)
                                .border(1.dp, BellBorder, CircleShape)
                                .clickable {
                                    showKeyPickerForButtonId = null
                                    keyPickerSearchQuery = ""
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✕", fontSize = 11.sp, fontWeight = FontWeight.Black, color = BellTextPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(BellGlassInput)
                            .border(1.dp, BellBorderActive, RoundedCornerShape(9.dp))
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = BellTextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                        BasicTextField(
                            value = keyPickerSearchQuery,
                            onValueChange = { keyPickerSearchQuery = it },
                            singleLine = true,
                            textStyle = TextStyle(color = BellTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                            cursorBrush = SolidColor(BellPillWhite),
                            modifier = Modifier.weight(1f),
                            decorationBox = { innerTextField ->
                                if (keyPickerSearchQuery.isEmpty()) {
                                    Text("Search key (W, Alt, F3, Tab, Shift...)", color = BellTextMuted, fontSize = 10.sp, maxLines = 1)
                                }
                                innerTextField()
                            }
                        )
                        if (keyPickerSearchQuery.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(BellGlassCard)
                                    .clickable { keyPickerSearchQuery = "" },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("✕", fontSize = 9.sp, color = BellTextPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val availableKeys = remember(isComboMode) {
                        if (isComboMode) {
                            KeycodeCatalog.ALL_KEYS.filter { !it.isScroll && !it.isMouseButton }
                        } else {
                            KeycodeCatalog.ALL_KEYS
                        }
                    }

                    val filteredKeys = remember(availableKeys, keyPickerSearchQuery) {
                        if (keyPickerSearchQuery.isBlank()) {
                            availableKeys
                        } else {
                            val q = keyPickerSearchQuery.trim().lowercase()
                            availableKeys.filter {
                                it.displayName.lowercase().contains(q) || it.category.lowercase().contains(q)
                            }
                        }
                    }

                    if (filteredKeys.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No keys match \"$keyPickerSearchQuery\"",
                                color = BellTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val grouped = filteredKeys.groupBy { it.category }
                            grouped.forEach { (category, keys) ->
                                item {
                                    Text(
                                        text = category.uppercase(),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 9.5.sp,
                                        letterSpacing = 0.7.sp,
                                        color = BellTextMuted,
                                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                                    )
                                }
                                items(keys) { keyOpt ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(BellGlassInput)
                                            .border(1.dp, BellBorder, RoundedCornerShape(8.dp))
                                            .clickable {
                                                buttonsList = buttonsList.map {
                                                    if (it.id == targetId) {
                                                        if (isComboMode) {
                                                            val existing = it.macroComboKeys.ifEmpty {
                                                                if (it.macroComboKey != 0) listOf(it.macroComboKey) else emptyList()
                                                            }
                                                            val updated = existing + keyOpt.keyCode
                                                            it.copy(macroComboKeys = updated, macroComboKey = updated.firstOrNull() ?: 0)
                                                        } else {
                                                            it.copy(
                                                                keyCode = keyOpt.keyCode,
                                                                isMouseButton = keyOpt.isMouseButton,
                                                                mouseButton = keyOpt.mouseButton,
                                                                isScroll = keyOpt.isScroll,
                                                                name = if (keyOpt.isScroll) "SCROLL"
                                                                else if (it.name == "NEW" || it.name == "BTN" || it.name == "SCROLL") {
                                                                    keyOpt.displayName.substringBefore(" ").take(6)
                                                                } else it.name
                                                            )
                                                        }
                                                    } else it
                                                }
                                                showKeyPickerForButtonId = null
                                                keyPickerSearchQuery = ""
                                            }
                                            .padding(horizontal = 12.dp, vertical = 9.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = keyOpt.displayName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.5.sp,
                                                color = BellTextPrimary
                                            )
                                            Text(
                                                text = keyOpt.category,
                                                fontWeight = FontWeight.Normal,
                                                fontSize = 9.sp,
                                                color = BellTextMuted
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
    }

    // Reset Confirmation Dialog (Bell Monochrome Glass)
    if (showResetConfirmDialog) {
        Dialog(onDismissRequest = { showResetConfirmDialog = false }) {
            Box(
                modifier = Modifier
                    .width(320.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(BellGlassElevated)
                    .border(1.dp, BellBorder, RoundedCornerShape(16.dp))
                    .padding(18.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "RESET GUI LAYOUT?",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        letterSpacing = 0.6.sp,
                        color = BellTextPrimary
                    )
                    Text(
                        text = "All button positions and custom mappings will be restored to default layout.",
                        fontSize = 11.sp,
                        color = BellTextSecondary
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BellGlassInput)
                                .border(1.dp, BellBorder, RoundedCornerShape(8.dp))
                                .clickable { showResetConfirmDialog = false },
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
                                    hideVisualHud = false
                                    hiddenButtonIds.clear()
                                    buttonsList = ControlLayoutManager.getDefaultButtons()
                                    selectedButtonId = null
                                    showResetConfirmDialog = false
                                    Toast.makeText(context, "Layout reset to default", Toast.LENGTH_SHORT).show()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("RESET", fontWeight = FontWeight.Black, fontSize = 10.sp, color = BellInkDark)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BellEditorSlider(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BellTextMuted)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0x26FFFFFF))
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            ) {
                Text(valueText, fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = BellTextPrimary)
            }
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = BellPillWhite,
                activeTrackColor = BellPillWhite,
                inactiveTrackColor = Color(0x26FFFFFF)
            )
        )
    }
}
