package com.israadev.nuxlauncher.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.israadev.nuxlauncher.core.lang.tr
import com.israadev.nuxlauncher.core.lang.trStr
import com.israadev.nuxlauncher.core.lang.NuxLanguageManager
import com.israadev.nuxlauncher.core.instance.InstanceManager
import com.israadev.nuxlauncher.core.models.*
import com.israadev.nuxlauncher.core.mods.NuxModManager
import com.israadev.nuxlauncher.core.mods.WorldInfo
import com.israadev.nuxlauncher.ui.components.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DecimalFormat

// Palet netral monokrom yang diselaraskan dengan tampilan Dashboard
private val UiSurfaceCard = Color(0xFF141418)
private val UiSurfaceElevated = Color(0xFF1A1A20)
private val UiSurfaceInput = Color(0xFF111115)
private val UiBorderSubtle = Color(0xFF282830)
private val UiBorderActive = Color(0xFF52525B)
private val UiPrimaryBtnBg = Color(0xFFF4F4F5)
private val UiPrimaryBtnFg = Color(0xFF0A0A0C)
private val UiTextPrimary = Color(0xFFFFFFFF)
private val UiTextSecondary = Color(0xFFA1A1AA)
private val UiTextMuted = Color(0xFF71717A)

private fun getMimeTypesForType(type: String): Array<String> {
    return when (type) {
        "mods" -> arrayOf(
            "application/java-archive",
            "application/x-java-archive",
            "application/x-jar",
            "application/jar",
            "application/octet-stream"
        )
        "modpacks" -> arrayOf(
            "application/zip",
            "application/x-zip-compressed",
            "application/octet-stream"
        )
        else -> arrayOf(
            "application/zip",
            "application/x-zip-compressed"
        )
    }
}

private fun isValidExtensionForType(itemType: String, filename: String): Boolean {
    val lower = filename.lowercase()
    return when (itemType) {
        "mods" -> lower.endsWith(".jar")
        "modpacks" -> lower.endsWith(".mrpack") || lower.endsWith(".zip")
        "shaderpacks", "resourcepacks", "datapacks" -> lower.endsWith(".zip")
        else -> false
    }
}

private fun getAllowedExtensionLabel(itemType: String): String {
    return when (itemType) {
        "mods" -> ".jar"
        "modpacks" -> ".mrpack, .zip"
        "shaderpacks" -> ".zip (Shaderpack)"
        "resourcepacks" -> ".zip (Resource Pack)"
        "datapacks" -> ".zip (Datapack)"
        else -> "supported formats"
    }
}

private fun getFileNameFromUri(context: android.content.Context, uri: Uri): String {
    var name: String? = null
    if (uri.scheme == "content") {
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    name = cursor.getString(nameIndex)
                }
            }
        } catch (_: Exception) {}
    }
    if (name.isNullOrBlank()) {
        name = uri.lastPathSegment?.substringAfterLast('/')
    }
    return name ?: "unknown_file"
}

private fun isModInstalled(inst: InstalledModItem, hit: ModrinthSearchHit): Boolean {
    if (inst.id.isNotBlank() && inst.id.equals(hit.projectId, ignoreCase = true)) {
        return true
    }
    if (inst.name.equals(hit.title, ignoreCase = true)) {
        return true
    }
    val slug = hit.slug?.trim()?.lowercase() ?: ""
    if (slug.isNotBlank()) {
        val clean = inst.filename.removeSuffix(".disabled").removeSuffix(".jar").lowercase()
        val namePrefix = clean.takeWhile { !it.isDigit() }.trimEnd('-', '_', '+')
        val base = namePrefix
            .removeSuffix("-fabric").removeSuffix("-forge").removeSuffix("-neoforge").removeSuffix("-quilt")
            .removeSuffix("_fabric").removeSuffix("_forge").removeSuffix("_neoforge").removeSuffix("_quilt")
            .trimEnd('-', '_')
        if (base.equals(slug, ignoreCase = true)) {
            return true
        }
    }
    return false
}

data class ModCategoryItem(val id: String, val label: String)

private val CATEGORIES_MAP = mapOf(
    "mods" to listOf(
        ModCategoryItem("All", "All"),
        ModCategoryItem("optimization", "Optimization"),
        ModCategoryItem("technology", "Technology"),
        ModCategoryItem("magic", "Magic"),
        ModCategoryItem("adventure", "Adventure"),
        ModCategoryItem("utility", "Utility"),
        ModCategoryItem("decoration", "Decoration")
    ),
    "modpacks" to listOf(
        ModCategoryItem("All", "All"),
        ModCategoryItem("adventure", "Adventure"),
        ModCategoryItem("optimization", "Optimization"),
        ModCategoryItem("technology", "Technology"),
        ModCategoryItem("magic", "Magic"),
        ModCategoryItem("quests", "Quests")
    ),
    "resourcepacks" to listOf(
        ModCategoryItem("All", "All"),
        ModCategoryItem("16x", "16x"),
        ModCategoryItem("32x", "32x"),
        ModCategoryItem("simplistic", "Simplistic"),
        ModCategoryItem("traditional", "Traditional"),
        ModCategoryItem("medieval", "Medieval")
    ),
    "shaderpacks" to listOf(
        ModCategoryItem("All", "All"),
        ModCategoryItem("realistic", "Realistic"),
        ModCategoryItem("stylized", "Stylized"),
        ModCategoryItem("fantasy", "Fantasy")
    ),
    "datapacks" to listOf(
        ModCategoryItem("All", "All"),
        ModCategoryItem("adventure", "Adventure"),
        ModCategoryItem("utility", "Utility"),
        ModCategoryItem("magic", "Magic"),
        ModCategoryItem("worldgen", "Worldgen")
    )
)

@Composable
fun ModsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val langVersion by NuxLanguageManager.languageVersion.collectAsState()

    val instances by InstanceManager.instances.collectAsState()
    val selectedInstance by InstanceManager.selectedInstance.collectAsState()

    // Mode: "installed" | "browse"
    var mainMode by remember { mutableStateOf("browse") }

    // Item Type: "mods" | "modpacks" | "resourcepacks" | "shaderpacks" | "datapacks"
    var activeType by remember { mutableStateOf("mods") }

    // World state for Datapacks
    var worlds by remember { mutableStateOf<List<WorldInfo>>(emptyList()) }
    var selectedWorld by remember { mutableStateOf("") }
    var showWorldMenu by remember { mutableStateOf(false) }

    // Installed list state
    var installedItems by remember { mutableStateOf<List<InstalledModItem>>(emptyList()) }
    var isInstalledLoading by remember { mutableStateOf(false) }
    var installedSearch by remember { mutableStateOf("") }
    var itemToDelete by remember { mutableStateOf<InstalledModItem?>(null) }

    // Browse Modrinth state
    var browseQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var searchHits by remember { mutableStateOf<List<ModrinthSearchHit>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var searchErrorMessage by remember { mutableStateOf<String?>(null) }
    var filterByGameVersion by remember { mutableStateOf(true) }
    var currentPage by remember { mutableStateOf(1) }
    var totalHits by remember { mutableStateOf(0) }
    var downloadingProjectIds by remember { mutableStateOf(emptyMap<String, String>()) }

    // Detail Modal state
    var detailModalHit by remember { mutableStateOf<ModrinthSearchHit?>(null) }
    var detailProject by remember { mutableStateOf<ModrinthProject?>(null) }
    var compatibleVersions by remember { mutableStateOf<List<ModrinthVersion>>(emptyList()) }
    var selectedVersion by remember { mutableStateOf<ModrinthVersion?>(null) }
    var isDetailLoading by remember { mutableStateOf(false) }
    var isDetailInstalling by remember { mutableStateOf(false) }
    var detailProgressText by remember { mutableStateOf("") }
    var detailTab by remember { mutableStateOf("overview") }
    var previewGalleryUrl by remember { mutableStateOf<String?>(null) }

    // Instance switch dropdown
    var showInstanceMenu by remember { mutableStateOf(false) }

    fun reloadWorlds() {
        val inst = selectedInstance ?: return
        val list = NuxModManager.getInstanceWorlds(context, inst)
        worlds = list
        if (list.isNotEmpty()) {
            if (selectedWorld.isBlank() || list.none { it.folderName == selectedWorld }) {
                selectedWorld = list.first().folderName
            }
        } else {
            selectedWorld = ""
        }
    }

    fun reloadInstalled() {
        val inst = selectedInstance ?: return
        isInstalledLoading = true
        scope.launch {
            val targetWorld = if (activeType == "datapacks") selectedWorld else null
            installedItems = NuxModManager.getInstalledItems(context, inst, activeType, targetWorld)
            isInstalledLoading = false
        }
    }

    fun performSearch(page: Int = 1) {
        val inst = selectedInstance ?: return
        isSearching = true
        searchErrorMessage = null
        scope.launch {
            val cat = if (selectedCategory == "All") null else selectedCategory
            val res = NuxModManager.searchProjects(
                query = browseQuery,
                itemType = activeType,
                instance = inst,
                category = cat,
                filterByGameVersion = filterByGameVersion,
                page = page
            )
            if (res.isSuccess) {
                val data = res.getOrNull()
                val rawHits = data?.hits ?: emptyList()
                searchHits = rawHits.distinctBy { it.projectId }
                totalHits = data?.totalHits ?: 0
                currentPage = page
                searchErrorMessage = null
            } else {
                searchHits = emptyList()
                totalHits = 0
                searchErrorMessage = res.exceptionOrNull()?.message ?: "Failed to connect to Modrinth"
            }
            isSearching = false
        }
    }

    LaunchedEffect(selectedInstance, activeType, mainMode, selectedWorld, filterByGameVersion) {
        if (selectedInstance != null) {
            reloadWorlds()
            reloadInstalled()
            if (mainMode == "browse") {
                performSearch(1)
            }
        }
    }

    var searchJob by remember { mutableStateOf<Job?>(null) }
    fun onQueryChanged(newQuery: String) {
        browseQuery = newQuery
        searchJob?.cancel()
        searchJob = scope.launch {
            delay(300)
            currentPage = 1
            performSearch(1)
        }
    }

    var isImportingModpack by remember { mutableStateOf(false) }
    var modpackImportStatus by remember { mutableStateOf("") }
    var modpackImportCurrent by remember { mutableStateOf(0) }
    var modpackImportTotal by remember { mutableStateOf(0) }
    var modpackImportName by remember { mutableStateOf("") }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri>? ->
        if (!uris.isNullOrEmpty() && selectedInstance != null) {
            val inst = selectedInstance!!
            scope.launch {
                val targetWorld = if (activeType == "datapacks") selectedWorld else null

                val validUris = mutableListOf<Pair<Uri, String>>()
                val rejectedNames = mutableListOf<String>()

                for (uri in uris) {
                    val fileName = getFileNameFromUri(context, uri)
                    if (isValidExtensionForType(activeType, fileName)) {
                        validUris.add(Pair(uri, fileName))
                    } else {
                        rejectedNames.add(fileName)
                    }
                }

                if (rejectedNames.isNotEmpty()) {
                    val hint = getAllowedExtensionLabel(activeType)
                    val msg = if (rejectedNames.size == 1) {
                        "File \"${rejectedNames.first()}\" ignored! Supported formats for $activeType are only $hint"
                    } else {
                        "${rejectedNames.size} file(s) ignored due to unsupported format ($hint)!"
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }

                if (validUris.isEmpty()) return@launch

                if (activeType == "modpacks") {
                    isImportingModpack = true
                    var successCount = 0
                    var lastError: String? = null

                    for ((uri, fileName) in validUris) {
                        modpackImportName = fileName
                        modpackImportStatus = "Reading modpack archive..."
                        modpackImportCurrent = 0
                        modpackImportTotal = 0

                        val importRes = NuxModManager.importLocalFile(
                            context = context,
                            instance = inst,
                            itemType = activeType,
                            uri = uri,
                            worldName = targetWorld,
                            onProgress = { statusText, cur, tot ->
                                modpackImportStatus = statusText
                                modpackImportCurrent = cur
                                modpackImportTotal = tot
                            }
                        )

                        if (importRes.isSuccess) {
                            successCount++
                        } else {
                            lastError = importRes.exceptionOrNull()?.message
                        }
                    }

                    isImportingModpack = false
                    reloadInstalled()

                    if (successCount > 0) {
                        Toast.makeText(
                            context,
                            "Successfully imported and installed $successCount modpack(s)!",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else if (lastError != null) {
                        Toast.makeText(
                            context,
                            "Failed to import modpack: $lastError",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    var successCount = 0
                    var lastError: String? = null

                    for ((uri, _) in validUris) {
                        val importRes = NuxModManager.importLocalFile(
                            context = context,
                            instance = inst,
                            itemType = activeType,
                            uri = uri,
                            worldName = targetWorld
                        )
                        if (importRes.isSuccess) {
                            successCount++
                        } else {
                            lastError = importRes.exceptionOrNull()?.message
                        }
                    }

                    reloadInstalled()

                    if (successCount > 0) {
                        Toast.makeText(
                            context,
                            "Successfully imported $successCount $activeType file(s)!",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else if (lastError != null) {
                        Toast.makeText(
                            context,
                            "Failed to import file(s): $lastError",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)
    ) {
        if (selectedInstance == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                NuxCard(
                    modifier = Modifier.width(320.dp),
                    backgroundColor = UiSurfaceCard,
                    borderColor = UiBorderSubtle,
                    cornerRadius = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ExtensionOff,
                            contentDescription = null,
                            tint = UiTextSecondary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No Instances Found",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = UiTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Select or create an instance on Dashboard to start installing mods.",
                            color = UiTextSecondary,
                            fontSize = 11.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        NuxButton(
                            onClick = { onNavigateBack() },
                            backgroundColor = UiPrimaryBtnBg,
                            contentColor = UiPrimaryBtnFg,
                            cornerRadius = 6.dp
                        ) {
                            Text(tr("mods.btn_back_dashboard"), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }
                    }
                }
            }
            return
        }

        val currentInst = selectedInstance!!

        // =========================================================================
        // ROW 1: TOP NAVIGATION BAR
        // =========================================================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Back Button
                Box(
                    modifier = Modifier
                        .height(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(UiSurfaceCard, RoundedCornerShape(6.dp))
                        .border(1.dp, UiBorderSubtle, RoundedCornerShape(6.dp))
                        .clickable { onNavigateBack() }
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = tr("common.back"),
                            tint = UiTextPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "DASHBOARD",
                            color = UiTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // Segmented Mode Tabs: [ Terpasang (X) | Jelajah Modrinth ]
                Row(
                    modifier = Modifier
                        .height(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(UiSurfaceCard, RoundedCornerShape(6.dp))
                        .border(1.dp, UiBorderSubtle, RoundedCornerShape(6.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isInstalled = mainMode == "installed"
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isInstalled) UiPrimaryBtnBg else Color.Transparent)
                            .clickable { mainMode = "installed" }
                            .padding(horizontal = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tr("mods.installed_count", installedItems.size),
                            color = if (isInstalled) UiPrimaryBtnFg else UiTextSecondary,
                            fontWeight = if (isInstalled) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 9.5.sp
                        )
                    }

                    val isBrowse = mainMode == "browse"
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isBrowse) UiPrimaryBtnBg else Color.Transparent)
                            .clickable { mainMode = "browse" }
                            .padding(horizontal = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tr("mods.browse_modrinth"),
                            color = if (isBrowse) UiPrimaryBtnFg else UiTextSecondary,
                            fontWeight = if (isBrowse) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 9.5.sp
                        )
                    }
                }
            }

            // Instance Selector Dropdown Pill
            Box {
                Row(
                    modifier = Modifier
                        .height(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(UiSurfaceCard, RoundedCornerShape(6.dp))
                        .border(1.dp, UiBorderSubtle, RoundedCornerShape(6.dp))
                        .clickable { if (instances.size > 1) showInstanceMenu = true }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Inventory2,
                        contentDescription = null,
                        tint = UiTextSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "${currentInst.name} (${currentInst.mcVersion} · ${currentInst.loader.uppercase()})",
                        color = UiTextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 9.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (instances.size > 1) {
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.Outlined.ArrowDropDown,
                            contentDescription = null,
                            tint = UiTextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = showInstanceMenu,
                    onDismissRequest = { showInstanceMenu = false }
                ) {
                    instances.forEach { inst ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "${inst.name} (${inst.mcVersion} · ${inst.loader.uppercase()})",
                                    fontWeight = if (inst.id == selectedInstance?.id) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.5.sp
                                )
                            },
                            onClick = {
                                InstanceManager.selectInstance(inst)
                                showInstanceMenu = false
                            }
                        )
                    }
                }
            }
        }

        // =========================================================================
        // ROW 2: ADDON TYPES + SEARCH & ACTIONS TOOLBAR
        // =========================================================================
        val addonTypes = listOf(
            "mods" to "Mods",
            "modpacks" to "Modpacks",
            "resourcepacks" to "Resource Packs",
            "shaderpacks" to "Shaders",
            "datapacks" to "Datapacks"
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LazyRow(
                modifier = Modifier.weight(1f, fill = false),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(addonTypes) { (typeId, typeLabel) ->
                    val isSelected = activeType == typeId
                    Box(
                        modifier = Modifier
                            .height(25.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                if (isSelected) UiSurfaceElevated else UiSurfaceCard.copy(alpha = 0.65f),
                                RoundedCornerShape(5.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) UiBorderActive else UiBorderSubtle,
                                shape = RoundedCornerShape(5.dp)
                            )
                            .clickable {
                                activeType = typeId
                                selectedCategory = "All"
                            }
                            .padding(horizontal = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = typeLabel,
                            color = if (isSelected) UiTextPrimary else UiTextSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 9.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                if (mainMode == "browse") {
                    CompactSearchBar(
                        value = browseQuery,
                        onValueChange = { onQueryChanged(it) },
                        placeholder = tr("mods.search_placeholder"),
                        modifier = Modifier.width(160.dp)
                    )

                    if (activeType == "datapacks") {
                        Box {
                            Row(
                                modifier = Modifier
                                    .height(25.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(UiSurfaceCard, RoundedCornerShape(5.dp))
                                    .border(1.dp, UiBorderSubtle, RoundedCornerShape(5.dp))
                                    .clickable { if (worlds.isNotEmpty()) showWorldMenu = true }
                                    .padding(horizontal = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Public,
                                    contentDescription = null,
                                    tint = UiTextSecondary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (worlds.isNotEmpty()) "World: $selectedWorld" else "No Worlds",
                                    color = UiTextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 9.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (worlds.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Icon(
                                        imageVector = Icons.Outlined.ArrowDropDown,
                                        contentDescription = null,
                                        tint = UiTextSecondary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showWorldMenu,
                                onDismissRequest = { showWorldMenu = false }
                            ) {
                                worlds.forEach { w ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = w.displayName,
                                                fontWeight = if (w.folderName == selectedWorld) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 11.sp
                                            )
                                        },
                                        onClick = {
                                            selectedWorld = w.folderName
                                            showWorldMenu = false
                                            reloadInstalled()
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Version Filter Toggle Pill
                    Box(
                        modifier = Modifier
                            .height(25.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                if (filterByGameVersion) UiSurfaceElevated else UiSurfaceCard,
                                RoundedCornerShape(5.dp)
                            )
                            .border(
                                1.dp,
                                if (filterByGameVersion) UiBorderActive else UiBorderSubtle,
                                RoundedCornerShape(5.dp)
                            )
                            .clickable {
                                filterByGameVersion = !filterByGameVersion
                                currentPage = 1
                                performSearch(1)
                            }
                            .padding(horizontal = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (filterByGameVersion) Icons.Outlined.Check else Icons.Outlined.Close,
                                contentDescription = null,
                                tint = if (filterByGameVersion) UiTextPrimary else UiTextMuted,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (filterByGameVersion) currentInst.mcVersion else tr("mods.all_versions"),
                                color = if (filterByGameVersion) UiTextPrimary else UiTextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )
                        }
                    }
                } else {
                    CompactSearchBar(
                        value = installedSearch,
                        onValueChange = { installedSearch = it },
                        placeholder = tr("mods.search_installed_placeholder", installedItems.size),
                        modifier = Modifier.width(160.dp)
                    )

                    if (activeType == "datapacks") {
                        Box {
                            Row(
                                modifier = Modifier
                                    .height(25.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(UiSurfaceCard, RoundedCornerShape(5.dp))
                                    .border(1.dp, UiBorderSubtle, RoundedCornerShape(5.dp))
                                    .clickable { if (worlds.isNotEmpty()) showWorldMenu = true }
                                    .padding(horizontal = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Public,
                                    contentDescription = null,
                                    tint = UiTextSecondary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (worlds.isNotEmpty()) "World: $selectedWorld" else "No Worlds",
                                    color = UiTextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 9.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (worlds.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Icon(
                                        imageVector = Icons.Outlined.ArrowDropDown,
                                        contentDescription = null,
                                        tint = UiTextSecondary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showWorldMenu,
                                onDismissRequest = { showWorldMenu = false }
                            ) {
                                worlds.forEach { w ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = w.displayName,
                                                fontWeight = if (w.folderName == selectedWorld) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 11.sp
                                            )
                                        },
                                        onClick = {
                                            selectedWorld = w.folderName
                                            showWorldMenu = false
                                            reloadInstalled()
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Import Button
                    Box(
                        modifier = Modifier
                            .height(25.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(UiPrimaryBtnBg, RoundedCornerShape(5.dp))
                            .border(1.dp, UiPrimaryBtnBg, RoundedCornerShape(5.dp))
                            .clickable {
                                if (selectedInstance == null) {
                                    Toast.makeText(context, trStr("dashboard.select_instance"), Toast.LENGTH_SHORT).show()
                                    return@clickable
                                }
                                val mimeTypes = getMimeTypesForType(activeType)
                                filePicker.launch(mimeTypes)
                            }
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.FileUpload,
                                contentDescription = "Import",
                                tint = UiPrimaryBtnFg,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = tr("mods.import_btn"),
                                color = UiPrimaryBtnFg,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // ROW 3: CATEGORY SUB-FILTERS
        // =========================================================================
        if (mainMode == "browse") {
            val categories = CATEGORIES_MAP[activeType] ?: listOf(ModCategoryItem("All", "All"))
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat.id
                    Box(
                        modifier = Modifier
                            .height(22.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (isSelected) UiPrimaryBtnBg else UiSurfaceCard.copy(alpha = 0.6f),
                                RoundedCornerShape(4.dp)
                            )
                            .border(
                                1.dp,
                                if (isSelected) UiPrimaryBtnBg else UiBorderSubtle,
                                RoundedCornerShape(4.dp)
                            )
                            .clickable {
                                selectedCategory = cat.id
                                currentPage = 1
                                performSearch(1)
                            }
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cat.label,
                            color = if (isSelected) UiPrimaryBtnFg else UiTextSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 8.5.sp
                        )
                    }
                }
            }
        }

        // =========================================================================
        // INSTALLED OR BROWSE LIST
        // =========================================================================
        if (mainMode == "installed") {
            val filteredInstalled = remember(installedItems, installedSearch) {
                if (installedSearch.isBlank()) installedItems
                else installedItems.filter {
                    it.name.contains(installedSearch, ignoreCase = true) ||
                            it.filename.contains(installedSearch, ignoreCase = true)
                }
            }

            if (isInstalledLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = UiTextPrimary, modifier = Modifier.size(24.dp))
                }
            } else if (filteredInstalled.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.FolderOpen,
                            contentDescription = null,
                            tint = UiTextMuted,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (activeType == "datapacks" && worlds.isEmpty())
                                "No worlds found in this instance. Launch the game to create a world first."
                            else if (installedSearch.isBlank())
                                "No ${activeType} installed in this instance"
                            else "No matching items found",
                            color = UiTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        NuxButton(
                            onClick = { mainMode = "browse" },
                            backgroundColor = UiPrimaryBtnBg,
                            contentColor = UiPrimaryBtnFg,
                            cornerRadius = 6.dp
                        ) {
                            Text(tr("mods.browse_modrinth"), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    itemsIndexed(filteredInstalled, key = { index, item -> "${item.id}_${item.filename}_$index" }) { _, item ->
                        NuxCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = if (item.isEnabled) UiSurfaceCard else UiSurfaceCard.copy(alpha = 0.6f),
                            borderColor = UiBorderSubtle,
                            shadowOffset = 0.dp,
                            cornerRadius = 8.dp,
                            borderWidth = 1.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(UiSurfaceElevated, RoundedCornerShape(6.dp))
                                            .border(1.dp, UiBorderSubtle, RoundedCornerShape(6.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = item.name.take(2).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            color = if (item.isEnabled) UiTextPrimary else UiTextMuted,
                                            fontSize = 11.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = item.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = if (item.isEnabled) UiTextPrimary else UiTextMuted,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (!item.isEnabled) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .background(UiSurfaceElevated, RoundedCornerShape(3.dp))
                                                        .border(1.dp, UiBorderSubtle, RoundedCornerShape(3.dp))
                                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                                ) {
                                                    Text(
                                                        text = tr("common.disabled"),
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = UiTextSecondary
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = "${item.filename} · ${formatBytes(item.sizeBytes)}",
                                            fontSize = 9.5.sp,
                                            color = UiTextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Switch(
                                        checked = item.isEnabled,
                                        onCheckedChange = {
                                            scope.launch {
                                                val targetWorld = if (activeType == "datapacks") selectedWorld else null
                                                NuxModManager.toggleItemEnabled(context, currentInst, activeType, item, targetWorld)
                                                reloadInstalled()
                                            }
                                        },
                                        modifier = Modifier.scale(0.75f),
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = UiPrimaryBtnFg,
                                            checkedTrackColor = UiPrimaryBtnBg,
                                            uncheckedThumbColor = UiTextSecondary,
                                            uncheckedTrackColor = UiSurfaceElevated
                                        )
                                    )

                                    Spacer(modifier = Modifier.width(4.dp))

                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(UiSurfaceElevated, RoundedCornerShape(5.dp))
                                            .border(1.dp, UiBorderSubtle, RoundedCornerShape(5.dp))
                                            .clickable { itemToDelete = item },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Delete,
                                            contentDescription = tr("common.delete"),
                                            tint = Color(0xFFF87171),
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // ==========================================
            // BROWSE MODRINTH
            // ==========================================
            if (isSearching) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = UiTextPrimary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(tr("common.loading"), color = UiTextSecondary, fontSize = 11.sp)
                    }
                }
            } else if (searchErrorMessage != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CloudOff,
                            contentDescription = null,
                            tint = Color(0xFFF87171),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Failed to Load from Modrinth",
                            color = UiTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = searchErrorMessage ?: "Network error occurred",
                            color = UiTextSecondary,
                            fontSize = 10.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        NuxButton(
                            onClick = { performSearch(currentPage) },
                            backgroundColor = UiPrimaryBtnBg,
                            contentColor = UiPrimaryBtnFg,
                            cornerRadius = 6.dp
                        ) {
                            Text(tr("common.retry"), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }
                    }
                }
            } else if (searchHits.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.SearchOff,
                            contentDescription = null,
                            tint = UiTextMuted,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (filterByGameVersion) {
                                "No results found for Minecraft ${currentInst.mcVersion} (${currentInst.loader.uppercase()})"
                            } else {
                                "No results found for this search"
                            },
                            color = UiTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try modifying keywords or disable version filter",
                            color = UiTextSecondary,
                            fontSize = 10.sp
                        )
                        if (filterByGameVersion) {
                            Spacer(modifier = Modifier.height(10.dp))
                            NuxButton(
                                onClick = {
                                    filterByGameVersion = false
                                    currentPage = 1
                                    performSearch(1)
                                },
                                backgroundColor = UiPrimaryBtnBg,
                                contentColor = UiPrimaryBtnFg,
                                cornerRadius = 6.dp
                            ) {
                                Text(tr("mods.btn_search_all_versions"), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    itemsIndexed(searchHits, key = { index, hit -> "${hit.projectId}_$index" }) { _, hit ->
                        val isInstalled = installedItems.any { isModInstalled(it, hit) }
                        val isDownloading = downloadingProjectIds.containsKey(hit.projectId)
                        val downloadStatus: String = downloadingProjectIds[hit.projectId].orEmpty()

                        NuxCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    detailModalHit = hit
                                    detailProject = null
                                    detailTab = "overview"
                                    compatibleVersions = emptyList()
                                    selectedVersion = null
                                    isDetailLoading = true
                                    scope.launch {
                                        val pRes = NuxModManager.getProjectDetails(hit.projectId)
                                        val vRes = NuxModManager.getCompatibleVersions(hit.projectId, currentInst, activeType)
                                        detailProject = pRes.getOrNull()
                                        val vers = vRes.getOrNull() ?: emptyList()
                                        compatibleVersions = vers
                                        selectedVersion = vers.firstOrNull()
                                        isDetailLoading = false
                                    }
                                },
                            backgroundColor = UiSurfaceCard,
                            borderColor = UiBorderSubtle,
                            shadowOffset = 0.dp,
                            cornerRadius = 8.dp,
                            borderWidth = 1.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                NuxNetworkImage(
                                    model = hit.iconUrl,
                                    contentDescription = hit.title,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .border(1.dp, UiBorderSubtle, RoundedCornerShape(6.dp)),
                                    fallbackInitials = hit.title.take(2).uppercase(),
                                    shape = RoundedCornerShape(6.dp)
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = hit.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = UiTextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (!hit.author.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = "by ${hit.author}",
                                                fontSize = 9.5.sp,
                                                color = UiTextSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        if (hit.projectType == "modpack") {
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Box(
                                                modifier = Modifier
                                                    .background(UiSurfaceElevated, RoundedCornerShape(3.dp))
                                                    .border(1.dp, UiBorderSubtle, RoundedCornerShape(3.dp))
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                Text(tr("mods.pack_badge"), fontSize = 8.sp, fontWeight = FontWeight.Bold, color = UiTextSecondary)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(1.dp))

                                    Text(
                                        text = hit.description ?: "No description available",
                                        fontSize = 10.sp,
                                        color = UiTextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(3.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .background(UiSurfaceElevated, RoundedCornerShape(3.dp))
                                                .border(1.dp, UiBorderSubtle, RoundedCornerShape(3.dp))
                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = formatCompactNumber(hit.downloads),
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = UiTextSecondary
                                            )
                                        }

                                        hit.categories?.take(2)?.forEach { cat ->
                                            Box(
                                                modifier = Modifier
                                                    .background(UiSurfaceElevated.copy(alpha = 0.7f), RoundedCornerShape(3.dp))
                                                    .border(1.dp, UiBorderSubtle, RoundedCornerShape(3.dp))
                                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = cat,
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.Normal,
                                                    color = UiTextMuted
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Action Button
                                if (isDownloading) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(
                                            color = UiTextPrimary,
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = downloadStatus.ifBlank { "Download..." },
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = UiTextSecondary
                                        )
                                    }
                                } else if (isInstalled) {
                                    Box(
                                        modifier = Modifier
                                            .height(26.dp)
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(UiSurfaceElevated, RoundedCornerShape(5.dp))
                                            .border(1.dp, UiBorderSubtle, RoundedCornerShape(5.dp))
                                            .padding(horizontal = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Outlined.Check,
                                                contentDescription = null,
                                                tint = UiTextPrimary,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = tr("mods.installed_badge"),
                                                color = UiTextPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }
                                } else {
                                    val buttonLabel = if (hit.projectType == "modpack") "+ INSTALL PACK" else "+ INSTALL"
                                    Box(
                                        modifier = Modifier
                                            .height(26.dp)
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(UiPrimaryBtnBg, RoundedCornerShape(5.dp))
                                            .border(1.dp, UiPrimaryBtnBg, RoundedCornerShape(5.dp))
                                            .clickable {
                                                scope.launch {
                                                    downloadingProjectIds = downloadingProjectIds + (hit.projectId to "Finding version...")
                                                    val vRes = NuxModManager.getCompatibleVersions(hit.projectId, currentInst, activeType)
                                                    val compList = vRes.getOrNull()
                                                    if (compList.isNullOrEmpty()) {
                                                        Toast.makeText(context, "No compatible version for ${currentInst.mcVersion}!", Toast.LENGTH_SHORT).show()
                                                        downloadingProjectIds = downloadingProjectIds - hit.projectId
                                                        return@launch
                                                    }

                                                    val targetVer = compList.first()
                                                    downloadingProjectIds = downloadingProjectIds + (hit.projectId to "Downloading...")

                                                    val targetWorld = if (activeType == "datapacks") selectedWorld else null
                                                    val downRes = NuxModManager.downloadVersionAndDependencies(
                                                        context = context,
                                                        instance = currentInst,
                                                        version = targetVer,
                                                        itemType = activeType,
                                                        worldName = targetWorld,
                                                        onProgress = { status ->
                                                            downloadingProjectIds = downloadingProjectIds + (hit.projectId to status)
                                                        }
                                                    )

                                                    downloadingProjectIds = downloadingProjectIds - hit.projectId

                                                    if (downRes.isSuccess) {
                                                        val downloaded = downRes.getOrNull() ?: emptyList()
                                                        val depCount = (downloaded.size - 1).coerceAtLeast(0)
                                                        val msg = if (activeType == "modpacks") {
                                                            "Modpack ${hit.title} installed successfully!"
                                                        } else if (depCount > 0) {
                                                            "Installed ${hit.title} and $depCount dependencies!"
                                                        } else {
                                                            "Successfully installed ${hit.title}!"
                                                        }
                                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                        reloadInstalled()
                                                    } else {
                                                        Toast.makeText(context, "Download failed: ${downRes.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            }
                                            .padding(horizontal = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = buttonLabel,
                                            color = UiPrimaryBtnFg,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Pagination
                    item {
                        val totalPages = ((totalHits + 17) / 18).coerceAtLeast(1)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(if (currentPage > 1) UiSurfaceCard else UiSurfaceCard.copy(alpha = 0.4f), RoundedCornerShape(5.dp))
                                    .border(1.dp, UiBorderSubtle, RoundedCornerShape(5.dp))
                                    .clickable(enabled = currentPage > 1) { performSearch(currentPage - 1) }
                                    .padding(horizontal = 9.dp, vertical = 4.dp)
                            ) {
                                Text(tr("common.previous"), fontWeight = FontWeight.Bold, fontSize = 9.5.sp, color = if (currentPage > 1) UiTextPrimary else UiTextMuted)
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Text(
                                text = "$currentPage / $totalPages",
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp,
                                color = UiTextSecondary
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(if (currentPage < totalPages) UiSurfaceCard else UiSurfaceCard.copy(alpha = 0.4f), RoundedCornerShape(5.dp))
                                    .border(1.dp, UiBorderSubtle, RoundedCornerShape(5.dp))
                                    .clickable(enabled = currentPage < totalPages) { performSearch(currentPage + 1) }
                                    .padding(horizontal = 9.dp, vertical = 4.dp)
                            ) {
                                Text(tr("common.next"), fontWeight = FontWeight.Bold, fontSize = 9.5.sp, color = if (currentPage < totalPages) UiTextPrimary else UiTextMuted)
                            }
                        }
                    }
                }
            }
        }
    }

    // --- 4. DELETE CONFIRMATION DIALOG ---
    itemToDelete?.let { item ->
        NuxDialog(onDismissRequest = { itemToDelete = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Outlined.WarningAmber,
                    contentDescription = null,
                    tint = Color(0xFFF87171),
                    modifier = Modifier.size(34.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (activeType == "modpacks") "Delete Modpack?" else "Delete ${item.category}?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = UiTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (activeType == "modpacks") {
                        "Are you sure you want to delete modpack \"${item.name}\"? All mods installed by this pack will also be removed."
                    } else {
                        "Are you sure you want to delete \"${item.name}\" from this instance?"
                    },
                    fontSize = 11.sp,
                    color = UiTextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NuxButton(
                        onClick = { itemToDelete = null },
                        backgroundColor = UiSurfaceElevated,
                        contentColor = UiTextPrimary,
                        borderColor = UiBorderSubtle,
                        cornerRadius = 6.dp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(tr("common.cancel"), fontWeight = FontWeight.Bold, fontSize = 10.5.sp)
                    }
                    NuxButton(
                        onClick = {
                            if (selectedInstance != null) {
                                scope.launch {
                                    val targetWorld = if (activeType == "datapacks") selectedWorld else null
                                    NuxModManager.deleteItem(context, selectedInstance!!, activeType, item, targetWorld)
                                    itemToDelete = null
                                    reloadInstalled()
                                    val toastMsg = if (activeType == "modpacks") {
                                        "Modpack and installed mods deleted!"
                                    } else {
                                        "${item.name} deleted successfully!"
                                    }
                                    Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        backgroundColor = Color(0xFFDC2626),
                        borderColor = Color(0xFFEF4444),
                        contentColor = UiTextPrimary,
                        cornerRadius = 6.dp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(tr("common.delete"), fontWeight = FontWeight.Bold, fontSize = 10.5.sp)
                    }
                }
            }
        }
    }

    // --- 4.5 MODPACK IMPORT PROGRESS DIALOG ---
    if (isImportingModpack) {
        NuxDialog(onDismissRequest = {}) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(UiSurfaceElevated)
                        .border(1.dp, UiBorderSubtle, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = UiTextPrimary,
                        strokeWidth = 2.5.dp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Importing & Installing Modpack",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = UiTextPrimary
                )

                if (modpackImportName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = modpackImportName,
                        fontSize = 11.sp,
                        color = UiTextSecondary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (modpackImportStatus.isNotBlank()) modpackImportStatus else "Parsing mod list and downloading files...",
                    fontSize = 11.sp,
                    color = UiTextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (modpackImportTotal > 0) {
                    val progressRatio = (modpackImportCurrent.toFloat() / modpackImportTotal.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progressRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = UiPrimaryBtnBg,
                        trackColor = UiSurfaceElevated
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$modpackImportCurrent / $modpackImportTotal mod files",
                        fontSize = 10.sp,
                        color = UiTextSecondary
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = UiPrimaryBtnBg,
                        trackColor = UiSurfaceElevated
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Launcher is reading mod list and downloading modpack files. Please wait.",
                    fontSize = 9.5.sp,
                    color = UiTextMuted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }

    // --- 5. MODRINTH PROJECT DETAIL MODAL ---
    detailModalHit?.let { hit ->
        val isHitInstalled = installedItems.any { isModInstalled(it, hit) }

        NuxDialog(
            onDismissRequest = {
                if (!isDetailInstalling) detailModalHit = null
            },
            modifier = Modifier.fillMaxWidth(0.92f),
            fillMaxHeight = true
        ) {
            val modalOuterShape = RoundedCornerShape(16.dp)
            val modalInnerShape = RoundedCornerShape(10.dp)
            val subCardShape = RoundedCornerShape(8.dp)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(modalOuterShape)
                    .background(UiSurfaceCard)
                    .border(1.dp, UiBorderSubtle, modalOuterShape)
                    .padding(14.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(subCardShape)
                                    .background(UiSurfaceElevated)
                                    .border(1.dp, UiBorderSubtle, subCardShape),
                                contentAlignment = Alignment.Center
                            ) {
                                NuxNetworkImage(
                                    model = hit.iconUrl,
                                    contentDescription = hit.title,
                                    modifier = Modifier.fillMaxSize(),
                                    fallbackInitials = hit.title.take(2).uppercase(),
                                    shape = subCardShape
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = hit.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = UiTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(UiSurfaceElevated)
                                            .border(1.dp, UiBorderSubtle, RoundedCornerShape(5.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = hit.projectType.uppercase(),
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = UiTextSecondary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "by ${hit.author ?: "Community"}",
                                        fontSize = 10.sp,
                                        color = UiTextSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "·", color = UiTextMuted, fontSize = 10.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = formatCompactNumber(hit.downloads),
                                        fontSize = 10.sp,
                                        color = UiTextSecondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(UiSurfaceElevated)
                                .border(1.dp, UiBorderSubtle, CircleShape)
                                .clickable(enabled = !isDetailInstalling) { detailModalHit = null },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Close",
                                tint = UiTextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Segmented Tab Switcher
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(UiSurfaceInput)
                            .border(1.dp, UiBorderSubtle, RoundedCornerShape(8.dp))
                            .padding(3.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val isTabOverview = detailTab == "overview"
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isTabOverview) UiPrimaryBtnBg else Color.Transparent)
                                    .clickable { detailTab = "overview" },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Description,
                                        contentDescription = null,
                                        tint = if (isTabOverview) UiPrimaryBtnFg else UiTextSecondary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "RINGKASAN & GALERI",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isTabOverview) UiPrimaryBtnFg else UiTextSecondary
                                    )
                                }
                            }

                            val isTabVersions = detailTab == "versions"
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isTabVersions) UiPrimaryBtnBg else Color.Transparent)
                                    .clickable { detailTab = "versions" },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Layers,
                                        contentDescription = null,
                                        tint = if (isTabVersions) UiPrimaryBtnFg else UiTextSecondary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "VERSI KOMPATIBEL (${compatibleVersions.size})",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isTabVersions) UiPrimaryBtnFg else UiTextSecondary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(modalInnerShape)
                            .background(UiSurfaceInput)
                            .border(1.dp, UiBorderSubtle, modalInnerShape)
                            .padding(10.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            if (isDetailLoading) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = UiTextPrimary, modifier = Modifier.size(26.dp))
                                }
                            } else if (detailTab == "overview") {
                                val galleryList = detailProject?.gallery ?: emptyList()
                                if (galleryList.isNotEmpty()) {
                                    Text(
                                        text = "SCREENSHOTS",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        color = UiTextMuted,
                                        letterSpacing = 0.8.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(galleryList) { gal ->
                                            Box(
                                                modifier = Modifier
                                                    .width(170.dp)
                                                    .height(96.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(UiSurfaceElevated)
                                                    .border(1.dp, UiBorderSubtle, RoundedCornerShape(8.dp))
                                                    .clickable { previewGalleryUrl = gal.url }
                                            ) {
                                                NuxNetworkImage(
                                                    model = gal.url,
                                                    contentDescription = gal.title ?: "Screenshot",
                                                    modifier = Modifier.fillMaxSize(),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                }

                                Text(
                                    text = "DESKRIPSI LENGKAP",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    color = UiTextMuted,
                                    letterSpacing = 0.8.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = detailProject?.description ?: hit.description ?: "No description summary available.",
                                    fontSize = 11.5.sp,
                                    color = UiTextPrimary,
                                    lineHeight = 17.sp
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                val allLoaders = (detailProject?.loaders ?: hit.categories ?: emptyList()).distinct()
                                if (allLoaders.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        allLoaders.take(5).forEach { tag ->
                                            Box(
                                                modifier = Modifier
                                                    .background(UiSurfaceElevated, RoundedCornerShape(6.dp))
                                                    .border(1.dp, UiBorderSubtle, RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = tag.uppercase(),
                                                    fontSize = 8.5.sp,
                                                    color = UiTextSecondary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                if (compatibleVersions.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(UiSurfaceElevated, RoundedCornerShape(8.dp))
                                            .border(1.dp, UiBorderSubtle, RoundedCornerShape(8.dp))
                                            .padding(12.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = "No Supported Version",
                                                color = Color(0xFFF87171),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = "This mod has no release matching Minecraft ${selectedInstance?.mcVersion ?: ""} (${(selectedInstance?.loader ?: "Vanilla").uppercase()}).",
                                                color = UiTextSecondary,
                                                fontSize = 10.sp,
                                                lineHeight = 14.sp
                                            )
                                        }
                                    }
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                        compatibleVersions.forEach { ver ->
                                            val isPicked = selectedVersion?.id == ver.id
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        if (isPicked) UiSurfaceElevated else UiSurfaceCard,
                                                        RoundedCornerShape(8.dp)
                                                    )
                                                    .border(
                                                        width = 1.dp,
                                                        color = if (isPicked) UiPrimaryBtnBg else UiBorderSubtle,
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable { selectedVersion = ver }
                                                    .padding(horizontal = 10.dp, vertical = 7.dp)
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
                                                        Box(
                                                            modifier = Modifier
                                                                .size(14.dp)
                                                                .clip(CircleShape)
                                                                .background(if (isPicked) UiPrimaryBtnBg else Color.Transparent)
                                                                .border(1.5.dp, if (isPicked) UiPrimaryBtnBg else UiTextMuted, CircleShape)
                                                        )
                                                        Spacer(modifier = Modifier.width(9.dp))
                                                        Column {
                                                            Text(
                                                                text = ver.name.ifBlank { ver.versionNumber },
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 11.5.sp,
                                                                color = UiTextPrimary,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                            Text(
                                                                text = "Versi: ${ver.versionNumber} · MC: ${ver.gameVersions.joinToString(", ")}",
                                                                fontSize = 9.sp,
                                                                color = UiTextSecondary
                                                            )
                                                        }
                                                    }

                                                    Box(
                                                        modifier = Modifier
                                                            .background(UiSurfaceInput, RoundedCornerShape(4.dp))
                                                            .border(1.dp, UiBorderSubtle, RoundedCornerShape(4.dp))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = ver.versionType.uppercase(),
                                                            fontSize = 8.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = UiTextSecondary
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(UiSurfaceElevated, RoundedCornerShape(8.dp))
                                    .border(1.dp, UiBorderSubtle, RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.AutoFixHigh,
                                        contentDescription = null,
                                        tint = UiTextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (hit.projectType == "modpack")
                                            "Modpack will be extracted directly into the instance (overrides, configuration, and modpack manifest)."
                                        else
                                            "Auto-Download Dependencies active: required dependencies will be installed automatically.",
                                        fontSize = 9.sp,
                                        color = UiTextSecondary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (isDetailInstalling) {
                        val installingLabel: String = detailProgressText.ifBlank { "Downloading and installing..." }
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = UiTextPrimary, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = installingLabel,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp,
                                color = UiTextPrimary
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            NuxButton(
                                onClick = { detailModalHit = null },
                                backgroundColor = UiSurfaceElevated,
                                contentColor = UiTextSecondary,
                                borderColor = UiBorderSubtle,
                                cornerRadius = 8.dp,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(tr("common.close"), fontWeight = FontWeight.Bold, fontSize = 10.5.sp)
                            }

                            if (isHitInstalled) {
                                Box(
                                    modifier = Modifier
                                        .weight(2f)
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(UiSurfaceElevated)
                                        .border(1.dp, UiBorderSubtle, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Outlined.Check,
                                            contentDescription = null,
                                            tint = UiTextPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = tr("mods.already_installed"),
                                            color = UiTextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            } else {
                                val canInstall = compatibleVersions.isNotEmpty()
                                Box(
                                    modifier = Modifier
                                        .weight(2f)
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (canInstall) UiPrimaryBtnBg else UiSurfaceElevated)
                                        .border(
                                            1.dp,
                                            if (canInstall) UiPrimaryBtnBg else UiBorderSubtle,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable(enabled = canInstall) {
                                            val ver = selectedVersion ?: compatibleVersions.firstOrNull()
                                            val inst = selectedInstance
                                            if (ver != null && inst != null) {
                                                scope.launch {
                                                    isDetailInstalling = true
                                                    detailProgressText = "Starting download..."

                                                    val targetWorld = if (activeType == "datapacks") selectedWorld else null
                                                    val res = NuxModManager.downloadVersionAndDependencies(
                                                        context = context,
                                                        instance = inst,
                                                        version = ver,
                                                        itemType = activeType,
                                                        worldName = targetWorld,
                                                        onProgress = { status ->
                                                            detailProgressText = status
                                                        }
                                                    )

                                                    isDetailInstalling = false

                                                    if (res.isSuccess) {
                                                        val files = res.getOrNull() ?: emptyList()
                                                        val depCount = (files.size - 1).coerceAtLeast(0)
                                                        val msg = if (activeType == "modpacks") {
                                                            "Modpack ${hit.title} installed successfully!"
                                                        } else if (depCount > 0) {
                                                            "Installed ${hit.title} and $depCount dependencies!"
                                                        } else {
                                                            "Successfully installed ${hit.title}!"
                                                        }
                                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                        reloadInstalled()
                                                    } else {
                                                        Toast.makeText(context, "Failed to download: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Outlined.Download,
                                            contentDescription = null,
                                            tint = if (canInstall) UiPrimaryBtnFg else UiTextMuted,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (hit.projectType == "modpack") tr("mods.install_modpack") else tr("mods.install_to_instance"),
                                            color = if (canInstall) UiPrimaryBtnFg else UiTextMuted,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
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

    // --- 5.5 MODAL PREVIEW GALERI GAMBAR FULLSCREEN ---
    previewGalleryUrl?.let { url ->
        NuxDialog(onDismissRequest = { previewGalleryUrl = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Screenshots",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = UiTextPrimary
                    )
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(UiSurfaceElevated, CircleShape)
                            .border(1.dp, UiBorderSubtle, CircleShape)
                            .clickable { previewGalleryUrl = null },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close", tint = UiTextPrimary, modifier = Modifier.size(13.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                NuxNetworkImage(
                    model = url,
                    contentDescription = "Image Preview",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, UiBorderSubtle, RoundedCornerShape(8.dp)),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }
    }
}

@Composable
private fun CompactSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(25.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(UiSurfaceInput, RoundedCornerShape(5.dp))
            .border(1.dp, UiBorderSubtle, RoundedCornerShape(5.dp))
            .padding(horizontal = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            tint = UiTextMuted,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    color = UiTextMuted,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    color = UiTextPrimary,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Normal
                ),
                cursorBrush = SolidColor(UiTextPrimary),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (value.isNotEmpty()) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = tr("common.delete"),
                tint = UiTextSecondary,
                modifier = Modifier
                    .size(12.dp)
                    .clickable { onValueChange("") }
            )
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, 3)
    return DecimalFormat("#,##0.#").format(bytes / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
}

private fun formatCompactNumber(number: Long): String {
    return when {
        number >= 1_000_000 -> DecimalFormat("#,##0.#").format(number / 1_000_000.0) + "M"
        number >= 1_000 -> DecimalFormat("#,##0.#").format(number / 1_000.0) + "K"
        else -> number.toString()
    }
}
