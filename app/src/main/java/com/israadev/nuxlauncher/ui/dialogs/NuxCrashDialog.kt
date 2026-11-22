package com.israadev.nuxlauncher.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.israadev.nuxlauncher.ui.components.NuxDialog
import com.israadev.nuxlauncher.ui.theme.NuxColors
import com.israadev.nuxlauncher.ui.theme.resp

data class CrashInfo(
    val exitCode: Int = -1,
    val instanceName: String = "Default",
    val mcVersion: String = "",
    val summary: String = "",
    val rawLog: String = ""
)

/**
 * Bell Launcher Crash Diagnostic Dialog (Slate Navy & Cobalt Blue)
 */
@Composable
fun NuxCrashDialog(
    crashInfo: CrashInfo,
    onDismiss: () -> Unit,
    onAnalyzeWithAi: (() -> Unit)? = null,
    onUploadLog: (() -> Unit)? = null,
    onShareLog: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var aiAnalysisText by remember { mutableStateOf<String?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }

    val outerCardShape = RoundedCornerShape((12.dp).resp())
    val panelShape = RoundedCornerShape((10.dp).resp())
    val btnShape = RoundedCornerShape((8.dp).resp())

    NuxDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.94f),
        fillMaxHeight = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B1120), outerCardShape)
                .border(1.dp, NuxColors.CardBorder, outerCardShape)
                .padding((14.dp).resp())
        ) {
            // 1. HEADER BAR
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "DIAGNOSTIK INSIDEN  •  TERMINASI JVM",
                        color = NuxColors.SkyBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = (8.5.sp).resp(),
                        letterSpacing = (0.7.sp).resp()
                    )
                    Spacer(modifier = Modifier.height((3.dp).resp()))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy((8.dp).resp())
                    ) {
                        Box(
                            modifier = Modifier
                                .size((8.dp).resp())
                                .background(NuxColors.ErrorRed, CircleShape)
                        )
                        Text(
                            text = "MINECRAFT MENGALAMI CRASH",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = (13.sp).resp()
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape((5.dp).resp()))
                                .background(NuxColors.ErrorRed.copy(alpha = 0.16f))
                                .border(1.dp, NuxColors.ErrorRed.copy(alpha = 0.45f), RoundedCornerShape((5.dp).resp()))
                                .padding(horizontal = (6.dp).resp(), vertical = (2.dp).resp())
                        ) {
                            Text(
                                text = "Signal ${crashInfo.exitCode}",
                                color = Color(0xFFFCA5A5),
                                fontWeight = FontWeight.Bold,
                                fontSize = (9.sp).resp()
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size((28.dp).resp())
                        .clip(btnShape)
                        .background(Color(0xFF131C31), btnShape)
                        .border(1.dp, NuxColors.CardBorder, btnShape)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = NuxColors.GrayNeutral,
                        modifier = Modifier.size((15.dp).resp())
                    )
                }
            }

            Spacer(modifier = Modifier.height((10.dp).resp()))

            // 2. SPLIT BODY: LEFT SESSION INFO + RIGHT AI CRASH ANALYST
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy((10.dp).resp())
            ) {
                // LEFT PANEL: SESSION INFO & ACTIONS
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(panelShape)
                        .background(Color(0xFF0E1629), panelShape)
                        .border(1.dp, NuxColors.CardBorder, panelShape)
                        .padding((10.dp).resp()),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy((6.dp).resp())
                    ) {
                        Text(
                            text = "INFO SESI PERMAINAN",
                            color = NuxColors.GrayNeutral,
                            fontWeight = FontWeight.Bold,
                            fontSize = (8.sp).resp(),
                            letterSpacing = (0.6.sp).resp()
                        )
                        Text(
                            text = "${crashInfo.instanceName} (MC ${crashInfo.mcVersion})",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = (11.5.sp).resp()
                        )
                        Text(
                            text = crashInfo.summary.ifBlank {
                                "JVM dihentikan karena sinyal fatal ${crashInfo.exitCode}."
                            },
                            color = Color(0xFFE2E8F0),
                            fontSize = (9.5.sp).resp(),
                            lineHeight = (13.sp).resp()
                        )
                    }

                    Spacer(modifier = Modifier.height((8.dp).resp()))

                    // Primary Upload Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((34.dp).resp())
                            .clip(btnShape)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(NuxColors.SageGreen, NuxColors.ForestGreen)
                                ),
                                btnShape
                            )
                            .clickable {
                                if (onUploadLog != null) {
                                    onUploadLog()
                                } else {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Crash Log", crashInfo.rawLog))
                                    Toast.makeText(context, "Crash log copied to clipboard", Toast.LENGTH_SHORT).show()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "UPLOAD LOG LINK (MCLO.GS)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = (9.sp).resp(),
                            letterSpacing = (0.4.sp).resp()
                        )
                    }

                    Spacer(modifier = Modifier.height((6.dp).resp()))

                    // Secondary Share & Copy Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy((6.dp).resp())
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height((30.dp).resp())
                                .clip(btnShape)
                                .background(Color(0xFF131C31), btnShape)
                                .border(1.dp, NuxColors.CardBorder, btnShape)
                                .clickable { onShareLog?.invoke() },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy((4.dp).resp())
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Share,
                                    contentDescription = null,
                                    tint = NuxColors.SkyBlue,
                                    modifier = Modifier.size((12.dp).resp())
                                )
                                Text(
                                    text = "SHARE",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = (8.5.sp).resp()
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height((30.dp).resp())
                                .clip(btnShape)
                                .background(Color(0xFF131C31), btnShape)
                                .border(1.dp, NuxColors.CardBorder, btnShape)
                                .clickable {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Crash Log", crashInfo.rawLog))
                                    Toast.makeText(context, "Log copied", Toast.LENGTH_SHORT).show()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy((4.dp).resp())
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = null,
                                    tint = NuxColors.SkyBlue,
                                    modifier = Modifier.size((12.dp).resp())
                                )
                                Text(
                                    text = "COPY",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = (8.5.sp).resp()
                                )
                            }
                        }
                    }
                }

                // RIGHT PANEL: BELL AI CRASH ANALYST
                Column(
                    modifier = Modifier
                        .weight(1.15f)
                        .fillMaxHeight()
                        .clip(panelShape)
                        .background(Color(0xFF0E1629), panelShape)
                        .border(1.dp, NuxColors.CardBorder, panelShape)
                        .padding((10.dp).resp()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Mini Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "BELL AI CRASH ANALYST",
                            color = NuxColors.SkyBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = (8.sp).resp(),
                            letterSpacing = (0.5.sp).resp()
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape((4.dp).resp()))
                                .background(Color(0xFF131C31))
                                .border(1.dp, NuxColors.CardBorder, RoundedCornerShape((4.dp).resp()))
                                .padding(horizontal = (6.dp).resp(), vertical = (2.dp).resp())
                        ) {
                            Text(
                                text = if (isAnalyzing) "ANALYZING" else "STANDBY",
                                color = NuxColors.GrayNeutral,
                                fontWeight = FontWeight.Bold,
                                fontSize = (7.5.sp).resp()
                            )
                        }
                    }

                    // Center Diagnosis Content
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = (8.dp).resp()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size((38.dp).resp())
                                .clip(RoundedCornerShape((10.dp).resp()))
                                .background(NuxColors.ForestGreen.copy(alpha = 0.18f))
                                .border(1.dp, NuxColors.SkyBlue.copy(alpha = 0.45f), RoundedCornerShape((10.dp).resp())),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = NuxColors.SkyBlue,
                                modifier = Modifier.size((20.dp).resp())
                            )
                        }

                        Spacer(modifier = Modifier.height((8.dp).resp()))

                        Text(
                            text = "AUTOMATED CRASH DIAGNOSIS",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = (10.sp).resp(),
                            letterSpacing = (0.4.sp).resp()
                        )

                        Spacer(modifier = Modifier.height((4.dp).resp()))

                        Text(
                            text = aiAnalysisText
                                ?: "AI will analyze game error snippets to identify root causes and immediate solutions.",
                            color = NuxColors.GrayNeutral,
                            fontSize = (8.5.sp).resp(),
                            lineHeight = (12.sp).resp(),
                            textAlign = TextAlign.Center
                        )
                    }

                    // ANALYZE WITH AI Button
                    Box(
                        modifier = Modifier
                            .clip(btnShape)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(NuxColors.ForestGreen, NuxColors.MintGreen)
                                ),
                                btnShape
                            )
                            .clickable {
                                isAnalyzing = true
                                aiAnalysisText = "Checking log for exit code ${crashInfo.exitCode}..."
                                onAnalyzeWithAi?.invoke()
                                isAnalyzing = false
                            }
                            .padding(horizontal = (14.dp).resp(), vertical = (7.dp).resp()),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy((5.dp).resp())
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size((13.dp).resp())
                            )
                            Text(
                                text = "ANALYZE WITH AI",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = (9.sp).resp(),
                                letterSpacing = (0.4.sp).resp()
                            )
                        }
                    }
                }
            }
        }
    }
}
