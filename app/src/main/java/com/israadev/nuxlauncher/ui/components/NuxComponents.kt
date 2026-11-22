package com.israadev.nuxlauncher.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.israadev.nuxlauncher.ui.theme.NuxSizes
import com.israadev.nuxlauncher.ui.theme.LocalNuxScale
import com.israadev.nuxlauncher.ui.theme.resp

/**
 * Midnight Navy Card matching Bell Launcher Dashboard
 */
@Composable
fun NuxCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xCC111827),
    borderColor: Color = Color(0xFF1F2B42),
    shadowColor: Color = Color.Transparent,
    shadowOffset: Dp = 0.dp,
    cornerRadius: Dp = (14.dp).resp(),
    borderWidth: Dp = 1.dp,
    fillMaxHeight: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val cardModifier = if (fillMaxHeight) modifier.fillMaxSize() else modifier.fillMaxWidth()
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = cardModifier
            .clip(shape)
            .background(backgroundColor, shape)
            .border(borderWidth, borderColor, shape)
    ) {
        content()
    }
}

/**
 * Royal Blue & Midnight Navy Interactive Button matching Bell Launcher Dashboard
 */
@Composable
fun NuxButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFF2563EB),
    contentColor: Color = Color.White,
    borderColor: Color = Color(0xFF3B82F6),
    enabled: Boolean = true,
    shadowOffset: Dp = 0.dp,
    cornerRadius: Dp = (12.dp).resp(),
    contentPadding: PaddingValues = PaddingValues(horizontal = (14.dp).resp(), vertical = (6.dp).resp()),
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.96f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "btnScale"
    )

    val shape = RoundedCornerShape(cornerRadius)
    val effectiveBg = if (enabled) backgroundColor else Color(0xFF161F30)
    val effectiveBorder = if (enabled) borderColor else Color(0xFF23304A)
    val effectiveContentColor = if (enabled) contentColor else Color(0xFF64748B)

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(effectiveBg, shape)
            .border(
                width = NuxSizes.BorderWidth,
                color = effectiveBorder,
                shape = shape
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .defaultMinSize(minHeight = (36.dp).resp())
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides effectiveContentColor
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                content()
            }
        }
    }
}

/**
 * Midnight Navy / Royal Blue Badge Indicator
 */
@Composable
fun NuxBadge(
    text: String,
    backgroundColor: Color = Color(0xFF162033),
    textColor: Color = Color(0xFF93C5FD),
    borderColor: Color? = null,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape((7.dp).resp())
    val effectiveBorderColor = borderColor ?: Color(0xFF253554)

    Box(
        modifier = modifier
            .background(backgroundColor, shape)
            .border(1.dp, effectiveBorderColor, shape)
            .padding(horizontal = (8.dp).resp(), vertical = (3.dp).resp())
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = (10.sp).resp(),
            fontWeight = FontWeight.Bold,
            letterSpacing = (0.5.sp).resp(),
            maxLines = 1,
            softWrap = false
        )
    }
}

/**
 * Midnight Navy Text Input Field
 */
@Composable
fun NuxTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle? = null,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val shape = RoundedCornerShape((10.dp).resp())
    val resolvedTextStyle = textStyle ?: TextStyle(
        color = Color.White,
        fontSize = (13.sp).resp(),
        fontWeight = FontWeight.Medium
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0E1420), shape)
            .border(1.dp, Color(0xFF222E46), shape)
            .padding(horizontal = (12.dp).resp(), vertical = (8.dp).resp()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    color = Color(0xFF64748B),
                    fontSize = (13.sp).resp(),
                    fontWeight = FontWeight.Normal
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = resolvedTextStyle,
                cursorBrush = SolidColor(Color(0xFF3B82F6)),
                singleLine = true,
                visualTransformation = visualTransformation,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (trailingContent != null) {
            Spacer(modifier = Modifier.width((8.dp).resp()))
            trailingContent()
        }
    }
}

/**
 * Midnight Navy Glass Dialog Container
 */
@Composable
fun NuxDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    fillMaxHeight: Boolean = false,
    content: @Composable () -> Unit
) {
    val isTablet = LocalNuxScale.current.isTablet
    val defaultWidthFraction = if (isTablet) 0.82f else 0.96f
    val dialogShape = RoundedCornerShape((18.dp).resp())

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF050811).copy(alpha = 0.78f))
                .padding(horizontal = (12.dp).resp(), vertical = (8.dp).resp()),
            contentAlignment = Alignment.Center
        ) {
            val dialogModifier = if (fillMaxHeight) {
                modifier.fillMaxWidth(defaultWidthFraction).fillMaxHeight(0.96f)
            } else {
                modifier.fillMaxWidth(defaultWidthFraction).wrapContentHeight()
            }
            Box(
                modifier = dialogModifier
                    .clip(dialogShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF131B2E),
                                Color(0xFF0B101D)
                            )
                        ),
                        dialogShape
                    )
                    .border(
                        1.dp,
                        Color(0xFF253554),
                        dialogShape
                    )
            ) {
                content()
            }
        }
    }
}
