package com.israadev.nuxlauncher.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.israadev.nuxlauncher.R
import com.israadev.nuxlauncher.core.account.AccountManager
import com.israadev.nuxlauncher.core.auth.AuthService
import com.israadev.nuxlauncher.core.auth.AuthUser
import com.israadev.nuxlauncher.core.auth.OtpCooldownManager
import com.israadev.nuxlauncher.core.lang.trStr
import com.israadev.nuxlauncher.core.network.NuxConfig
import com.israadev.nuxlauncher.ui.theme.resp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// Token Desain Bell Launcher (Monochrome Obsidian Glass + Crisp White)
private val BellBgCanvas = Color(0xFF07070A)
private val BellGlassSidebar = Color(0xCC0D0D11)
private val BellGlassCard = Color(0xBF131318)
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

private fun safeAuthTr(key: String, fallback: String): String {
    val translated = trStr(key)
    return if (translated.isBlank() || translated == key) fallback else translated
}

enum class AuthMode {
    LOGIN,
    REGISTER,
    VERIFY_REGISTER_OTP,
    ACTIVATE_KEY,
    FORGOT_PASSWORD_EMAIL,
    FORGOT_PASSWORD_RESET
}

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var mode by remember { mutableStateOf(AuthMode.LOGIN) }

    // Form states
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var licenseKey by remember { mutableStateOf("") }

    // Activation pending user
    var pendingUser by remember { mutableStateOf<AuthUser?>(null) }

    // UI feedback
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    // Monotonic Anti-Bypass Countdown timer for OTP
    var countdown by remember { mutableIntStateOf(OtpCooldownManager.getRemainingSeconds()) }

    LaunchedEffect(Unit) {
        OtpCooldownManager.init(context)
        while (isActive) {
            countdown = OtpCooldownManager.getRemainingSeconds()
            delay(500)
        }
    }

    // Two-Column Landscape Console Layout
    Row(
        modifier = modifier
            .fillMaxSize()
            .background(BellBgCanvas)
            .padding((10.dp).resp()),
        horizontalArrangement = Arrangement.spacedBy((10.dp).resp())
    ) {
        // =====================================================================
        // LEFT PANE: BRANDING & GAMING SHOWCASE (38% Width)
        // =====================================================================
        val leftPanelShape = RoundedCornerShape((16.dp).resp())
        Box(
            modifier = Modifier
                .weight(0.38f)
                .fillMaxHeight()
                .clip(leftPanelShape)
                .background(BellGlassSidebar)
                .border(1.dp, BellBorder, leftPanelShape)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Background Wallpaper with dark obsidian gradient overlay
                Image(
                    painter = painterResource(id = R.drawable.mc_hero_bg),
                    contentDescription = "Background",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = 0.25f
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x6607070A),
                                    Color(0xCC0A0A0E),
                                    Color(0xF50A0A0E)
                                )
                            )
                        )
                )

                // Left Content Column
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Bar: App Badge & Online Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(BellPillWhite)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "BELL CLIENT",
                                color = BellInkDark,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.9.sp
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(BellPillWhite, CircleShape)
                            )
                            Text(
                                text = "ONLINE GATEWAY",
                                color = BellTextSecondary,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    // Middle: Title & Gaming Highlights
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = "MINECRAFT JAVA EDITION",
                            color = BellTextSecondary,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp
                        )

                        Text(
                            text = "BELL LAUNCHER",
                            color = BellTextPrimary,
                            fontSize = 23.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.3).sp,
                            lineHeight = 27.sp
                        )

                        Text(
                            text = "Play Minecraft Java Edition on Android with high-FPS Vulkan renderers, custom touch controls, and modpack support.",
                            color = BellTextSecondary,
                            fontSize = 10.5.sp,
                            lineHeight = 14.5.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }

                    // Bottom: Feature Badges
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            GamingFeaturePill(text = "FABRIC / FORGE", icon = Icons.Default.Extension, modifier = Modifier.weight(1f))
                            GamingFeaturePill(text = "VULKAN / ZINK", icon = Icons.Default.Speed, modifier = Modifier.weight(1f))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            GamingFeaturePill(text = "CUSTOM GUI", icon = Icons.Default.TouchApp, modifier = Modifier.weight(1f))
                            GamingFeaturePill(text = "AI DIAGNOSTICS", icon = Icons.Default.AutoFixHigh, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // =====================================================================
        // RIGHT PANE: COMPACT INTERACTIVE AUTH CONSOLE (62% Width)
        // =====================================================================
        val rightPanelShape = RoundedCornerShape((16.dp).resp())
        Box(
            modifier = Modifier
                .weight(0.62f)
                .fillMaxHeight()
                .clip(rightPanelShape)
                .background(BellGlassCard)
                .border(1.dp, BellBorder, rightPanelShape)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = (16.dp).resp(), vertical = (12.dp).resp()),
                verticalArrangement = Arrangement.spacedBy((9.dp).resp())
            ) {
                // Top Segmented Switcher (Only in Login & Register)
                if (mode == AuthMode.LOGIN || mode == AuthMode.REGISTER) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((36.dp).resp())
                            .clip(RoundedCornerShape((9.dp).resp()))
                            .background(BellGlassInput)
                            .border(1.dp, BellBorder, RoundedCornerShape((9.dp).resp()))
                            .padding((3.dp).resp())
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val isLogin = mode == AuthMode.LOGIN
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(if (isLogin) BellPillWhite else Color.Transparent)
                                    .clickable {
                                        errorMessage = ""
                                        successMessage = ""
                                        mode = AuthMode.LOGIN
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = safeAuthTr("auth.sign_in", "SIGN IN").uppercase(),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.6.sp,
                                    color = if (isLogin) BellInkDark else BellTextSecondary
                                )
                            }

                            val isRegister = mode == AuthMode.REGISTER
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(if (isRegister) BellPillWhite else Color.Transparent)
                                    .clickable {
                                        errorMessage = ""
                                        successMessage = ""
                                        mode = AuthMode.REGISTER
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = safeAuthTr("auth.sign_up", "CREATE ACCOUNT").uppercase(),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.6.sp,
                                    color = if (isRegister) BellInkDark else BellTextSecondary
                                )
                            }
                        }
                    }
                }

                // Error Banner
                if (errorMessage.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(BellDanger.copy(alpha = 0.14f))
                            .border(1.dp, BellDanger.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = BellDanger, modifier = Modifier.size(15.dp))
                            Text(text = errorMessage, color = BellDanger, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Success Banner
                if (successMessage.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x26FFFFFF))
                            .border(1.dp, BellBorderActive, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = BellTextPrimary, modifier = Modifier.size(15.dp))
                            Text(text = successMessage, color = BellTextPrimary, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Important Reminder to check SPAM folder when entering OTP
                if (mode == AuthMode.VERIFY_REGISTER_OTP || mode == AuthMode.FORGOT_PASSWORD_RESET) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(BellAmber.copy(alpha = 0.14f))
                            .border(1.dp, BellAmber.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = BellAmber, modifier = Modifier.size(15.dp))
                            Text(
                                text = "Code didn't arrive in Inbox? Check your SPAM / JUNK email folder before requesting again.",
                                color = BellAmber,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // SUB-SCREENS
                when (mode) {
                    // ==========================================
                    // 1. LOGIN
                    // ==========================================
                    AuthMode.LOGIN -> {
                        AuthInputField(
                            value = email,
                            onValueChange = { email = it; errorMessage = "" },
                            label = safeAuthTr("auth.email_label", "EMAIL ADDRESS"),
                            placeholder = safeAuthTr("auth.email_placeholder", "you@example.com"),
                            leadingIcon = Icons.Default.Email
                        )

                        AuthInputField(
                            value = password,
                            onValueChange = { password = it; errorMessage = "" },
                            label = safeAuthTr("auth.password_label", "PASSWORD"),
                            placeholder = safeAuthTr("auth.password_placeholder", "Enter your password"),
                            leadingIcon = Icons.Default.Lock,
                            isPassword = true,
                            isPasswordVisible = isPasswordVisible,
                            onPasswordToggle = { isPasswordVisible = !isPasswordVisible }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = safeAuthTr("auth.forgot_password", "Forgot Password?"),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = BellTextSecondary,
                                modifier = Modifier.clickable {
                                    errorMessage = ""
                                    successMessage = ""
                                    mode = AuthMode.FORGOT_PASSWORD_EMAIL
                                }
                            )
                        }

                        BellAuthPrimaryButton(
                            text = if (isLoading) safeAuthTr("common.loading", "SIGNING IN...").uppercase() else safeAuthTr("auth.btn_sign_in", "SIGN IN TO LAUNCHER").uppercase(),
                            icon = Icons.Default.SportsEsports,
                            isLoading = isLoading,
                            onClick = {
                                if (email.isBlank() || password.isBlank()) {
                                    errorMessage = "Email and password are required."
                                    return@BellAuthPrimaryButton
                                }
                                isLoading = true
                                errorMessage = ""
                                coroutineScope.launch {
                                    val res = AuthService.login(email, password)
                                    isLoading = false
                                    res.fold(
                                        onSuccess = { loginResult ->
                                            AccountManager.saveAuthUser(context, loginResult.user)
                                            if (AccountManager.accounts.value.isEmpty()) {
                                                AccountManager.addAccount(
                                                    context,
                                                    AccountManager.createGuestAccount(loginResult.user.username)
                                                )
                                            }
                                            onAuthSuccess()
                                        },
                                        onFailure = { err ->
                                            errorMessage = err.message ?: "Sign in failed. Please check your credentials."
                                        }
                                    )
                                }
                            }
                        )
                    }

                    // ==========================================
                    // 2. REGISTER
                    // ==========================================
                    AuthMode.REGISTER -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                AuthInputField(
                                    value = username,
                                    onValueChange = { username = it; errorMessage = "" },
                                    label = safeAuthTr("auth.username_label", "USERNAME"),
                                    placeholder = safeAuthTr("auth.username_placeholder", "BellPlayer"),
                                    leadingIcon = Icons.Default.Person
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                AuthInputField(
                                    value = email,
                                    onValueChange = { email = it; errorMessage = "" },
                                    label = safeAuthTr("auth.email_label", "EMAIL ADDRESS"),
                                    placeholder = safeAuthTr("auth.email_placeholder", "you@example.com"),
                                    leadingIcon = Icons.Default.Email
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                AuthInputField(
                                    value = password,
                                    onValueChange = { password = it; errorMessage = "" },
                                    label = safeAuthTr("auth.password_label", "PASSWORD"),
                                    placeholder = safeAuthTr("auth.password_placeholder", "Min. 6 characters"),
                                    leadingIcon = Icons.Default.Lock,
                                    isPassword = true,
                                    isPasswordVisible = isPasswordVisible,
                                    onPasswordToggle = { isPasswordVisible = !isPasswordVisible }
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                AuthInputField(
                                    value = confirmPassword,
                                    onValueChange = { confirmPassword = it; errorMessage = "" },
                                    label = safeAuthTr("auth.confirm_password_label", "CONFIRM PASSWORD"),
                                    placeholder = safeAuthTr("auth.confirm_password_placeholder", "Repeat password"),
                                    leadingIcon = Icons.Default.Lock,
                                    isPassword = true,
                                    isPasswordVisible = isPasswordVisible,
                                    onPasswordToggle = { isPasswordVisible = !isPasswordVisible }
                                )
                            }
                        }

                        BellAuthPrimaryButton(
                            text = if (isLoading) safeAuthTr("common.loading", "CREATING ACCOUNT...").uppercase() else safeAuthTr("auth.btn_create_account", "CREATE BELL ACCOUNT").uppercase(),
                            icon = Icons.Default.PersonAdd,
                            isLoading = isLoading,
                            onClick = {
                                val cleanUser = username.trim()
                                val cleanEmail = email.trim()
                                if (cleanUser.isEmpty()) {
                                    errorMessage = "Username is required."
                                    return@BellAuthPrimaryButton
                                }
                                if (!cleanEmail.contains("@")) {
                                    errorMessage = "Invalid email format."
                                    return@BellAuthPrimaryButton
                                }
                                if (password.length < 6) {
                                    errorMessage = "Password must be at least 6 characters."
                                    return@BellAuthPrimaryButton
                                }
                                if (password != confirmPassword) {
                                    errorMessage = "Password confirmation does not match."
                                    return@BellAuthPrimaryButton
                                }

                                isLoading = true
                                errorMessage = ""
                                coroutineScope.launch {
                                    val res = AuthService.register(cleanEmail, cleanUser, password)
                                    isLoading = false
                                    res.fold(
                                        onSuccess = { regResult ->
                                            AccountManager.saveAuthUser(context, regResult.user)
                                            if (AccountManager.accounts.value.isEmpty()) {
                                                AccountManager.addAccount(
                                                    context,
                                                    AccountManager.createGuestAccount(regResult.user.username)
                                                )
                                            }
                                            onAuthSuccess()
                                        },
                                        onFailure = { err ->
                                            errorMessage = err.message ?: "Failed to register account."
                                        }
                                    )
                                }
                            }
                        )
                    }

                    // ==========================================
                    // 3. VERIFY REGISTER OTP (DEPRECATED - BYPASS)
                    // ==========================================
                    AuthMode.VERIFY_REGISTER_OTP -> {
                        LaunchedEffect(Unit) {
                            mode = AuthMode.REGISTER
                        }
                    }

                    // ==========================================
                    // 4. MANDATORY LICENSE ACTIVATION GATE
                    // ==========================================
                    AuthMode.ACTIVATE_KEY -> {
                        val user = pendingUser ?: AccountManager.launcherUser.value

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(BellGlassInput)
                                .border(1.dp, BellBorderActive, RoundedCornerShape(10.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "ACCOUNT: ${user?.username ?: "User"}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = BellTextPrimary
                                )
                                Text(
                                    text = com.israadev.nuxlauncher.core.utils.PrivacyMasker.maskEmail(user?.email ?: email),
                                    fontSize = 10.sp,
                                    color = BellTextSecondary
                                )
                            }

                            Text(
                                text = "Switch Account",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = BellTextPrimary,
                                modifier = Modifier.clickable {
                                    AccountManager.logout(context)
                                    pendingUser = null
                                    mode = AuthMode.LOGIN
                                }
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = safeAuthTr("auth.activate_title", "ACTIVATE LICENSE KEY").uppercase(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = BellTextPrimary
                            )
                            Text(
                                text = "Format: XXXX-XXXX-XXXX-XXXX (16 characters)",
                                fontSize = 10.sp,
                                color = BellTextSecondary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(BellGlassInput)
                                .border(1.dp, BellBorderActive, RoundedCornerShape(10.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            if (licenseKey.isEmpty()) {
                                Text(
                                    text = "PASTE YOUR LICENSE KEY HERE",
                                    color = BellTextMuted,
                                    fontSize = 11.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            BasicTextField(
                                value = licenseKey,
                                onValueChange = { input ->
                                    val cleaned = input.filter { it.isLetterOrDigit() || it == '-' }.uppercase()
                                    licenseKey = cleaned
                                    errorMessage = ""
                                },
                                textStyle = TextStyle(
                                    color = BellTextPrimary,
                                    fontSize = 13.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                ),
                                cursorBrush = SolidColor(BellPillWhite),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(0.42f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(BellGlassInput)
                                    .border(1.dp, BellBorder, RoundedCornerShape(9.dp))
                                    .clickable {
                                        try {
                                            val buyUrl = NuxConfig.BUY_KEY_URL
                                            if (buyUrl.isNotBlank()) {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(buyUrl))
                                                context.startActivity(intent)
                                            }
                                        } catch (_: Exception) {}
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = BellTextPrimary, modifier = Modifier.size(13.dp))
                                    Text("BUY KEY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BellTextPrimary)
                                }
                            }

                            Box(modifier = Modifier.weight(0.58f)) {
                                BellAuthPrimaryButton(
                                    text = if (isLoading) "ACTIVATING..." else safeAuthTr("auth.btn_activate", "ACTIVATE LICENSE").uppercase(),
                                    icon = Icons.Default.Key,
                                    isLoading = isLoading,
                                    onClick = {
                                        val uid = user?.uid ?: ""
                                        val rawKey = licenseKey.trim().uppercase()
                                        val key = if (!rawKey.contains("-") && rawKey.length == 16) {
                                            "${rawKey.substring(0, 4)}-${rawKey.substring(4, 8)}-${rawKey.substring(8, 12)}-${rawKey.substring(12, 16)}"
                                        } else {
                                            rawKey
                                        }
                                        if (uid.isEmpty()) {
                                            errorMessage = "Invalid session. Please sign in again."
                                            mode = AuthMode.LOGIN
                                            return@BellAuthPrimaryButton
                                        }
                                        if (key.length < 8) {
                                            errorMessage = "Invalid license key format."
                                            return@BellAuthPrimaryButton
                                        }

                                        val targetUser = user ?: AuthUser(uid = uid, email = email, username = username)

                                        isLoading = true
                                        errorMessage = ""
                                        coroutineScope.launch {
                                            val token = user?.idToken?.takeIf { it.isNotBlank() }
                                                ?: AccountManager.getActiveUser()?.idToken?.takeIf { it.isNotBlank() }
                                            val res = AuthService.activateLicense(uid, key, token)
                                            isLoading = false
                                            res.fold(
                                                onSuccess = { actResult ->
                                                    if (!actResult.success || !actResult.isActivated) {
                                                        errorMessage = if (actResult.message.isNotBlank()) {
                                                            actResult.message
                                                        } else {
                                                            "License key is invalid or already registered to another account."
                                                        }
                                                        return@fold
                                                    }
                                                    val sessionPair = AuthService.requestSessionToken(uid)
                                                    val activatedUser = targetUser.copy(
                                                        isActivated = true,
                                                        tier = actResult.tier,
                                                        idToken = sessionPair?.first ?: targetUser.idToken,
                                                        refreshToken = sessionPair?.second ?: targetUser.refreshToken
                                                    )
                                                    AccountManager.saveAuthUser(context, activatedUser)
                                                    onAuthSuccess()
                                                },
                                                onFailure = { err ->
                                                    errorMessage = err.message ?: "Activation failed. Please check your license key."
                                                }
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // ==========================================
                    // 5. FORGOT PASSWORD (EMAIL RESET VIA FIREBASE)
                    // ==========================================
                    AuthMode.FORGOT_PASSWORD_EMAIL -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.clickable { mode = AuthMode.LOGIN }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = BellTextSecondary, modifier = Modifier.size(15.dp))
                            Text(safeAuthTr("auth.btn_back_to_login", "Back to Sign In"), fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = BellTextSecondary)
                        }

                        Text(
                            text = "RESET ACCOUNT PASSWORD",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.6.sp,
                            color = BellTextPrimary
                        )

                        Text(
                            text = "Enter your registered email address. An official password reset link will be sent directly to your inbox.",
                            fontSize = 10.5.sp,
                            color = BellTextSecondary,
                            lineHeight = 15.sp
                        )

                        AuthInputField(
                            value = email,
                            onValueChange = { email = it; errorMessage = "" },
                            label = "REGISTERED EMAIL ADDRESS",
                            placeholder = "you@example.com",
                            leadingIcon = Icons.Default.Email
                        )

                        BellAuthPrimaryButton(
                            text = if (isLoading) "SENDING LINK..." else "SEND PASSWORD RESET LINK",
                            icon = Icons.Default.Send,
                            isLoading = isLoading,
                            onClick = {
                                val cleanEmail = email.trim()
                                if (!cleanEmail.contains("@")) {
                                    errorMessage = "Invalid email format."
                                    return@BellAuthPrimaryButton
                                }

                                isLoading = true
                                errorMessage = ""
                                coroutineScope.launch {
                                    val res = AuthService.sendPasswordResetEmail(cleanEmail)
                                    isLoading = false
                                    res.fold(
                                        onSuccess = { msg ->
                                            successMessage = msg
                                            mode = AuthMode.LOGIN
                                        },
                                        onFailure = { err ->
                                            errorMessage = err.message ?: "Failed to send password reset link."
                                        }
                                    )
                                }
                            }
                        )
                    }

                    // ==========================================
                    // 6. FORGOT PASSWORD (DEPRECATED - BYPASS)
                    // ==========================================
                    AuthMode.FORGOT_PASSWORD_RESET -> {
                        LaunchedEffect(Unit) {
                            mode = AuthMode.LOGIN
                        }
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------------------------
// HELPER COMPOSABLES (BELL MONOCHROME THEME)
// --------------------------------------------------------------------------

@Composable
private fun BellAuthPrimaryButton(
    text: String,
    icon: ImageVector,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(if (!isLoading) BellPillWhite else Color(0x33FFFFFF))
            .clickable(enabled = !isLoading) { onClick() }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = BellInkDark,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = BellInkDark,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = text,
                fontWeight = FontWeight.Black,
                fontSize = 11.sp,
                letterSpacing = 0.6.sp,
                color = BellInkDark
            )
        }
    }
}

@Composable
private fun GamingFeaturePill(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(BellGlassInput)
            .border(1.dp, BellBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = BellTextPrimary,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = text,
                color = BellTextPrimary,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AuthInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: ImageVector,
    isPassword: Boolean = false,
    isPasswordVisible: Boolean = false,
    onPasswordToggle: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            color = BellTextSecondary,
            letterSpacing = 0.7.sp
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(9.dp))
                .background(BellGlassInput)
                .border(1.dp, BellBorder, RoundedCornerShape(9.dp))
                .padding(horizontal = 10.dp, vertical = 8.5.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = BellTextSecondary,
                    modifier = Modifier.size(15.dp)
                )

                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = BellTextMuted,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        visualTransformation = if (isPassword && !isPasswordVisible) PasswordVisualTransformation() else VisualTransformation.None,
                        textStyle = TextStyle(
                            color = BellTextPrimary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        cursorBrush = SolidColor(BellPillWhite),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (isPassword && onPasswordToggle != null) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle password",
                        tint = BellTextSecondary,
                        modifier = Modifier
                            .size(15.dp)
                            .clickable { onPasswordToggle() }
                    )
                }
            }
        }
    }
}

@Composable
private fun OtpBoxes(
    otp: String,
    onOtpChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(BellGlassInput)
            .border(1.dp, BellBorder, RoundedCornerShape(10.dp))
            .padding(vertical = 10.dp, horizontal = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until 6) {
                val digit = otp.getOrNull(i)?.toString() ?: ""
                val isFilled = digit.isNotEmpty()

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isFilled) Color(0x26FFFFFF) else BellGlassCard)
                        .border(
                            1.dp,
                            if (isFilled) BellPillWhite else BellBorder,
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = digit,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = BellTextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Invisible input layer capturing keyboard
        BasicTextField(
            value = otp,
            onValueChange = onOtpChange,
            textStyle = TextStyle(color = Color.Transparent),
            cursorBrush = SolidColor(Color.Transparent),
            singleLine = true,
            modifier = Modifier.matchParentSize()
        )
    }
}
