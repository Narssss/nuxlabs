package com.israadev.nuxlauncher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.israadev.nuxlauncher.core.auth.AuthUser
import com.israadev.nuxlauncher.core.lang.tr
import com.israadev.nuxlauncher.core.models.UserAccount
import com.israadev.nuxlauncher.core.social.NuxSocialManager
import com.israadev.nuxlauncher.ui.theme.NuxColors
import com.israadev.nuxlauncher.ui.theme.resp

data class NuxNavItem(
    val id: String,
    val icon: ImageVector,
    val labelKey: String
)

val NUX_NAV_ITEMS = listOf(
    NuxNavItem("home", Icons.Outlined.Home, "sidebar.home"),
    NuxNavItem("friends", Icons.Outlined.Group, "sidebar.friends"),
    NuxNavItem("accounts", Icons.Outlined.Person, "sidebar.accounts"),
    NuxNavItem("mods", Icons.Outlined.Extension, "sidebar.mods"),
    NuxNavItem("ai", Icons.Outlined.Psychology, "sidebar.ai"),
    NuxNavItem("settings", Icons.Outlined.Settings, "sidebar.settings")
)

/**
 * Bell Launcher Slate Navy Navigation Rail
 */
@Composable
fun NuxSidebar(
    activeTab: String,
    onTabSelected: (String) -> Unit,
    onOpenAbout: () -> Unit = {},
    currentAccount: UserAccount? = null,
    launcherUser: AuthUser? = null,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(Color(0xFF0E1525))
            .padding(vertical = (12.dp).resp(), horizontal = (6.dp).resp()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // TOP BRAND EMBLEM (Bell Launcher Cobalt Badge)
        val logoShape = RoundedCornerShape((10.dp).resp())
        Box(
            modifier = Modifier
                .size((38.dp).resp())
                .clip(logoShape)
                .background(
                    Brush.linearGradient(
                        listOf(NuxColors.ForestGreen, NuxColors.MintGreen)
                    ),
                    logoShape
                )
                .border(1.dp, Color(0x6693C5FD), logoShape)
                .clickable { onTabSelected("home") },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = "Bell Launcher",
                tint = Color.White,
                modifier = Modifier.size((21.dp).resp())
            )
        }

        Spacer(modifier = Modifier.height((8.dp).resp()))

        // CENTER: UNIFIED NAVIGATION TILES WITH LEFT INDICATOR BAR
        val friendsList by NuxSocialManager.friends.collectAsState()
        val totalSocialUnread = remember(friendsList) {
            friendsList.sumOf { it.unreadCount } + friendsList.count { it.isPendingReceived }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy((6.dp).resp()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            NUX_NAV_ITEMS.forEach { item ->
                val isSelected = activeTab == item.id
                val tileShape = RoundedCornerShape((10.dp).resp())

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(tileShape)
                        .background(
                            color = if (isSelected) NuxColors.ForestGreen.copy(alpha = 0.22f) else Color.Transparent,
                            shape = tileShape
                        )
                        .then(
                            if (isSelected) {
                                Modifier.border(1.dp, NuxColors.ForestGreen.copy(alpha = 0.55f), tileShape)
                            } else {
                                Modifier
                            }
                        )
                        .clickable { onTabSelected(item.id) }
                        .padding(vertical = (6.dp).resp()),
                    contentAlignment = Alignment.Center
                ) {
                    // Left Vertical Sky-Blue Active Accent Indicator
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .width((3.dp).resp())
                                .height((20.dp).resp())
                                .background(NuxColors.SkyBlue, CircleShape)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = tr(item.labelKey),
                                tint = if (isSelected) Color.White else NuxColors.GrayNeutral,
                                modifier = Modifier.size((18.dp).resp())
                            )

                            if (item.id == "friends" && totalSocialUnread > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = (3.dp).resp(), y = (-2).dp.resp())
                                        .size((7.dp).resp())
                                        .clip(CircleShape)
                                        .background(NuxColors.ErrorRed)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height((3.dp).resp()))

                        Text(
                            text = tr(item.labelKey),
                            color = if (isSelected) Color.White else NuxColors.GrayNeutral,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = (8.5.sp).resp(),
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height((8.dp).resp()))

        // BOTTOM: ABOUT / INFO BUTTON
        val infoShape = RoundedCornerShape((9.dp).resp())
        Box(
            modifier = Modifier
                .size((32.dp).resp())
                .clip(infoShape)
                .background(Color(0xFF162035), infoShape)
                .border(1.dp, NuxColors.CardBorder, infoShape)
                .clickable { onOpenAbout() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = "About & License",
                tint = NuxColors.SkyBlue,
                modifier = Modifier.size((16.dp).resp())
            )
        }
    }
}
