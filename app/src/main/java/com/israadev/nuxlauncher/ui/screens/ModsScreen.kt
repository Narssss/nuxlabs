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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import com.israadev.nuxlauncher.core.instance.InstanceManager
import com.israadev.nuxlauncher.core.lang.NuxLanguageManager
import com.israadev.nuxlauncher.core.lang.tr
import com.israadev.nuxlauncher.core.lang.trStr
import com.israadev.nuxlauncher.core.models.*
import com.israadev.nuxlauncher.core.mods.NuxModManager
import com.israadev.nuxlauncher.core.mods.WorldInfo
import com.israadev.nuxlauncher.ui.components.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DecimalFormat

// Token desain Bell Launcher (Deep obsidian glass + crisp white pills)
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
        ModCategoryItem("All", "ALL"),
        ModCategoryItem("optimization", "OPTIMIZATION"),
        ModCategoryItem("technology", "TECHNOLOGY"),
        ModCategoryItem("magic", "MAGIC"),
        ModCategoryItem("adventure", "ADVENTURE"),
        ModCategoryItem("utility", "UTILITY"),
        ModCategoryItem("decoration", "DECORATION")
    ),
    "modpacks" to listOf(
        ModCategoryItem("All", "ALL"),
        ModCategoryItem("adventure", "ADVENTURE"),
        ModCategoryItem("optimization", "OPTIMIZATION"),
        ModCategoryItem("technology", "TECHNOLOGY"),
        ModCategoryItem("magic", "MAGIC"),
        ModCategoryItem("quests", "QUESTS")
    ),
    "resourcepacks" to listOf(
        ModCategoryItem("All", "ALL"),
        ModCategoryItem("16x", "16X"),
        ModCategoryItem("32x", "32X"),
        ModCategoryItem("simplistic", "SIMPLISTIC"),
        ModCategoryItem("traditional", "TRADITIONAL"),
        ModCategoryItem("medieval", "MEDIEVAL")
    ),
    "shaderpacks" to listOf(
        ModCategoryItem("All", "ALL"),
        ModCategoryItem("realistic", "REALISTIC"),
        ModCategoryItem("stylized", "STYLIZED"),
        ModCategoryItem("fantasy", "FANTASY")
    ),
    "datapacks" to listOf(
        ModCategoryItem("All", "ALL"),
        ModCategoryItem("adventure", "ADVENTURE"),
        ModCategoryItem("utility", "UTILITY"),
        ModCategoryItem("magic", "MAGIC"),
        ModCategoryItem("worldgen", "WORLDGEN")
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

    if (selectedInstance == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(340.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(BellGlassElevated)
                    .border(1.dp, BellBorder, RoundedCornerShape(16.dp))
                    .padding(22.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.ExtensionOff,
                        contentDescription = null,
                        tint = BellTextSecondary,
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "NO INSTANCES FOUND",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        letterSpacing = 0.8.sp,
                        color = BellTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Select or create an instance on Dashboard to start installing mods.",
                        color = BellTextSecondary,
                        fontSize = 11.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    BellPrimaryButton(
                        text = tr("mods.btn_back_dashboard").uppercase(),
                        icon = Icons.AutoMirrored.Outlined.ArrowBack,
                        onClick = { onNavigateBack() }
                    )
                }
            }
        }
        return
    }

    val currentInst = selectedInstance!!

    // =========================================================================
    // ARSITEKTUR BARU: SIDEBAR FILTER KIRI + BENTO GRID 2 KOLOM KANAN
    // =========================================================================
    Row(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // =====================================================================
        // KOLOM KIRI: FILTER & INSTANCE DOCK (170.dp)
        // =====================================================================
        Column(
            modifier = Modifier
                .width(172.dp)
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Pemilih Instance Aktif
                Box {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x14FFFFFF))
                            .border(1.dp, BellBorder, RoundedCornerShape(10.dp))
                            .clickable { if (instances.size > 1) showInstanceMenu = true }
                            .padding(9.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TARGET PROFILE",
                                color = BellTextMuted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 7.5.sp,
                                letterSpacing = 0.7.sp
                            )
                            if (instances.size > 1) {
                                Icon(
                                    imageVector = Icons.Outlined.UnfoldMore,
                                    contentDescription = null,
                                    tint = BellTextSecondary,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = currentInst.name,
                            color = BellTextPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(BellPillWhite)
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = currentInst.loader.uppercase(),
                                    color = BellInkDark,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 7.5.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0x1FFFFFFF))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = currentInst.mcVersion,
                                    color = BellTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 7.5.sp
                                )
                            }
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
                                        fontSize = 11.sp
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

                // Pilihan Tipe Konten (Vertikal)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "CONTENT TYPE",
                        color = BellTextMuted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 7.5.sp,
                        letterSpacing = 0.7.sp,
                        modifier = Modifier.padding(start = 2.dp)
                    )

                    val addonTypes = listOf(
                        Triple("mods", "MODS", Icons.Outlined.Extension),
                        Triple("modpacks", "MODPACKS", Icons.Outlined.Inventory2),
                        Triple("resourcepacks", "TEXTURES", Icons.Outlined.Palette),
                        Triple("shaderpacks", "SHADERS", Icons.Outlined.AutoAwesome),
                        Triple("datapacks", "DATAPACKS", Icons.Outlined.DataObject)
                    )

                    addonTypes.forEach { (typeId, typeLabel, typeIcon) ->
                        val isSelected = activeType == typeId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) BellPillWhite else Color.Transparent)
                                .clickable {
                                    activeType = typeId
                                    selectedCategory = "All"
                                }
                                .padding(horizontal = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Icon(
                                imageVector = typeIcon,
                                contentDescription = null,
                                tint = if (isSelected) BellInkDark else BellTextSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = typeLabel,
                                color = if (isSelected) BellInkDark else BellTextSecondary,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                                fontSize = 9.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                // Kategori (Hanya saat Browse)
                if (mainMode == "browse") {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = "CATEGORY",
                            color = BellTextMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 7.5.sp,
                            letterSpacing = 0.7.sp,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                        val categories = CATEGORIES_MAP[activeType] ?: listOf(ModCategoryItem("All", "ALL"))
                        categories.forEach { cat ->
                            val isSelected = selectedCategory == cat.id
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(25.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) Color(0x26FFFFFF) else Color.Transparent)
                                    .clickable {
                                        selectedCategory = cat.id
                                        currentPage = 1
                                        performSearch(1)
                                    }
                                    .padding(horizontal = 9.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = cat.label,
                                    color = if (isSelected) BellTextPrimary else BellTextMuted,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 8.5.sp,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }
            }

            // Footer Sidebar: Filter Versi / Tombol Import
            Column(
                modifier = Modifier.padding(top = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (activeType == "datapacks") {
                    Box {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x14FFFFFF))
                                .border(1.dp, BellBorder, RoundedCornerShape(8.dp))
                                .clickable { if (worlds.isNotEmpty()) showWorldMenu = true }
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Public,
                                    contentDescription = null,
                                    tint = BellTextPrimary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (worlds.isNotEmpty()) selectedWorld else "No Worlds",
                                    color = BellTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
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

                if (mainMode == "browse") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (filterByGameVersion) Color(0x26FFFFFF) else Color(0x12FFFFFF))
                            .border(
                                1.dp,
                                if (filterByGameVersion) BellBorderActive else BellBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                filterByGameVersion = !filterByGameVersion
                                currentPage = 1
                                performSearch(1)
                            }
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (filterByGameVersion) Icons.Outlined.FilterAlt else Icons.Outlined.FilterAltOff,
                                contentDescription = null,
                                tint = BellTextPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (filterByGameVersion) "MC ${currentInst.mcVersion}" else "ALL VERSIONS",
                                color = BellTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.5.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                } else {
                    BellPrimaryButton(
                        text = tr("mods.import_btn").uppercase(),
                        icon = Icons.Outlined.FileUpload,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val mimeTypes = getMimeTypesForType(activeType)
                            filePicker.launch(mimeTypes)
                        }
                    )
                }
            }
        }

        // =====================================================================
        // KOLOM KANAN: TOP SEARCH + BENTO GRID 2 KOLOM
        // =====================================================================
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            // Top Header Strip (Mode Switcher + Search Input)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mode Segmented Pill
                Row(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(BellGlassSidebar)
                        .border(1.dp, BellBorder, RoundedCornerShape(10.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    val isBrowse = mainMode == "browse"
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(7.dp))
                            .background(if (isBrowse) BellPillWhite else Color.Transparent)
                            .clickable { mainMode = "browse" }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "DISCOVER",
                            color = if (isBrowse) BellInkDark else BellTextSecondary,
                            fontWeight = FontWeight.Black,
                            fontSize = 9.sp,
                            letterSpacing = 0.7.sp
                        )
                    }

                    val isInstalled = mainMode == "installed"
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(7.dp))
                            .background(if (isInstalled) BellPillWhite else Color.Transparent)
                            .clickable { mainMode = "installed" }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "LIBRARY (${installedItems.size})",
                            color = if (isInstalled) BellInkDark else BellTextSecondary,
                            fontWeight = FontWeight.Black,
                            fontSize = 9.sp,
                            letterSpacing = 0.7.sp
                        )
                    }
                }

                // Search Bar Full-Flex
                BellSearchBar(
                    value = if (mainMode == "browse") browseQuery else installedSearch,
                    onValueChange = {
                        if (mainMode == "browse") onQueryChanged(it) else installedSearch = it
                    },
                    placeholder = if (mainMode == "browse") tr("mods.search_placeholder")
                    else tr("mods.search_installed_placeholder", installedItems.size),
                    modifier = Modifier.weight(1f)
                )
            }

            // Content Area (2-Column Bento Grid)
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
                        CircularProgressIndicator(color = BellTextPrimary, modifier = Modifier.size(26.dp))
                    }
                } else if (filteredInstalled.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Outlined.FolderOpen,
                                contentDescription = null,
                                tint = BellTextMuted,
                                modifier = Modifier.size(34.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (activeType == "datapacks" && worlds.isEmpty())
                                    "No worlds found in this instance."
                                else if (installedSearch.isBlank())
                                    "Library is empty for $activeType"
                                else "No matching items found",
                                color = BellTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            BellPrimaryButton(
                                text = "DISCOVER CONTENT",
                                icon = Icons.Outlined.Explore,
                                onClick = { mainMode = "browse" }
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 14.dp)
                    ) {
                        itemsIndexed(
                            filteredInstalled,
                            key = { index, item -> "${item.id}_${item.filename}_$index" }
                        ) { _, item ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (item.isEnabled) BellGlassCard else Color(0x73131318))
                                    .border(1.dp, BellBorder, RoundedCornerShape(12.dp))
                                    .padding(10.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0x1AFFFFFF))
                                                .border(1.dp, BellBorder, RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = item.name.take(2).uppercase(),
                                                fontWeight = FontWeight.Black,
                                                color = if (item.isEnabled) BellTextPrimary else BellTextMuted,
                                                fontSize = 11.sp
                                            )
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
                                                modifier = Modifier.scale(0.72f),
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = BellInkDark,
                                                    checkedTrackColor = BellPillWhite,
                                                    uncheckedThumbColor = BellTextSecondary,
                                                    uncheckedTrackColor = Color(0x26FFFFFF)
                                                )
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(RoundedCornerShape(7.dp))
                                                    .background(Color(0x1AFFFFFF))
                                                    .clickable { itemToDelete = item },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Delete,
                                                    contentDescription = tr("common.delete"),
                                                    tint = Color(0xFFF87171),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }

                                    Column {
                                        Text(
                                            text = item.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (item.isEnabled) BellTextPrimary else BellTextMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${formatBytes(item.sizeBytes)} · ${item.filename}",
                                            fontSize = 9.sp,
                                            color = BellTextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // BROWSE GRID 2 KOLOM
                if (isSearching) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = BellTextPrimary, modifier = Modifier.size(26.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(tr("common.loading").uppercase(), color = BellTextSecondary, fontSize = 9.5.sp, letterSpacing = 0.8.sp)
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
                                modifier = Modifier.size(34.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "FAILED TO LOAD FROM MODRINTH",
                                color = BellTextPrimary,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                letterSpacing = 0.6.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = searchErrorMessage ?: "Network error occurred",
                                color = BellTextSecondary,
                                fontSize = 10.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            BellPrimaryButton(
                                text = tr("common.retry").uppercase(),
                                icon = Icons.Outlined.Refresh,
                                onClick = { performSearch(currentPage) }
                            )
                        }
                    }
                } else if (searchHits.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Outlined.SearchOff,
                                contentDescription = null,
                                tint = BellTextMuted,
                                modifier = Modifier.size(34.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (filterByGameVersion) {
                                    "No results for MC ${currentInst.mcVersion} (${currentInst.loader.uppercase()})"
                                } else {
                                    "No results found"
                                },
                                color = BellTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            if (filterByGameVersion) {
                                Spacer(modifier = Modifier.height(12.dp))
                                BellPrimaryButton(
                                    text = tr("mods.btn_search_all_versions").uppercase(),
                                    icon = Icons.Outlined.FilterAltOff,
                                    onClick = {
                                        filterByGameVersion = false
                                        currentPage = 1
                                        performSearch(1)
                                    }
                                )
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        itemsIndexed(searchHits, key = { index, hit -> "${hit.projectId}_$index" }) { _, hit ->
                            val isInstalled = installedItems.any { isModInstalled(it, hit) }
                            val isDownloading = downloadingProjectIds.containsKey(hit.projectId)
                            val downloadStatus: String = downloadingProjectIds[hit.projectId].orEmpty()

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(13.dp))
                                    .background(BellGlassCard)
                                    .border(1.dp, BellBorder, RoundedCornerShape(13.dp))
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
                                    }
                                    .padding(10.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Baris Atas Kartu: Ikon + Badge Downloads
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        NuxNetworkImage(
                                            model = hit.iconUrl,
                                            contentDescription = hit.title,
                                            modifier = Modifier
                                                .size(38.dp)
                                                .border(1.dp, BellBorder, RoundedCornerShape(9.dp)),
                                            fallbackInitials = hit.title.take(2).uppercase(),
                                            shape = RoundedCornerShape(9.dp)
                                        )

                                        Column(horizontalAlignment = Alignment.End) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .background(Color(0x1AFFFFFF))
                                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = formatCompactNumber(hit.downloads),
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = BellTextPrimary
                                                )
                                            }
                                            hit.categories?.firstOrNull()?.let { firstCat ->
                                                Spacer(modifier = Modifier.height(3.dp))
                                                Text(
                                                    text = firstCat.uppercase(),
                                                    fontSize = 7.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = BellTextMuted,
                                                    letterSpacing = 0.4.sp
                                                )
                                            }
                                        }
                                    }

                                    // Judul & Deskripsi Singkat
                                    Column {
                                        Text(
                                            text = hit.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = BellTextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = hit.description ?: "No description available",
                                            fontSize = 9.5.sp,
                                            color = BellTextSecondary,
                                            maxLines = 2,
                                            lineHeight = 13.sp,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.height(26.dp)
                                        )
                                    }

                                    // Tombol Aksi Full-Width di Bagian Bawah Kartu
                                    if (isDownloading) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(30.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0x1AFFFFFF)),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CircularProgressIndicator(
                                                color = BellTextPrimary,
                                                modifier = Modifier.size(12.dp),
                                                strokeWidth = 1.8.dp
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = downloadStatus.ifBlank { "DOWNLOADING" }.uppercase(),
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = BellTextSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    } else if (isInstalled) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(30.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0x1AFFFFFF))
                                                .border(1.dp, BellBorder, RoundedCornerShape(8.dp)),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Check,
                                                contentDescription = null,
                                                tint = BellTextPrimary,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "INSTALLED",
                                                color = BellTextPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 8.5.sp,
                                                letterSpacing = 0.6.sp
                                            )
                                        }
                                    } else {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(30.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(BellPillWhite)
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
                                                },
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Download,
                                                contentDescription = null,
                                                tint = BellInkDark,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = if (hit.projectType == "modpack") "GET PACK" else "INSTALL",
                                                color = BellInkDark,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 8.5.sp,
                                                letterSpacing = 0.7.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Pagination Bar (Spanning 2 Columns)
                        item(span = { GridItemSpan(2) }) {
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
                                        .height(30.dp)
                                        .clip(CircleShape)
                                        .background(BellGlassCard)
                                        .border(1.dp, BellBorder, CircleShape)
                                        .clickable(enabled = currentPage > 1) { performSearch(currentPage - 1) }
                                        .padding(horizontal = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = tr("common.previous").uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        letterSpacing = 0.6.sp,
                                        color = if (currentPage > 1) BellTextPrimary else BellTextMuted
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = "$currentPage / $totalPages",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp,
                                    color = BellTextSecondary
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Box(
                                    modifier = Modifier
                                        .height(30.dp)
                                        .clip(CircleShape)
                                        .background(BellGlassCard)
                                        .border(1.dp, BellBorder, CircleShape)
                                        .clickable(enabled = currentPage < totalPages) { performSearch(currentPage + 1) }
                                        .padding(horizontal = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = tr("common.next").uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        letterSpacing = 0.6.sp,
                                        color = if (currentPage < totalPages) BellTextPrimary else BellTextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DELETE CONFIRMATION DIALOG ---
    itemToDelete?.let { item ->
        NuxDialog(onDismissRequest = { itemToDelete = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
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
                    text = if (activeType == "modpacks") "DELETE MODPACK?" else "DELETE ${item.category.uppercase()}?",
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    letterSpacing = 0.6.sp,
                    color = BellTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (activeType == "modpacks") {
                        "Are you sure you want to delete modpack \"${item.name}\"? All mods installed by this pack will also be removed."
                    } else {
                        "Are you sure you want to delete \"${item.name}\" from this instance?"
                    },
                    fontSize = 11.sp,
                    color = BellTextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(BellGlassElevated)
                            .border(1.dp, BellBorder, RoundedCornerShape(9.dp))
                            .clickable { itemToDelete = null },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tr("common.cancel").uppercase(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 0.6.sp,
                            color = BellTextPrimary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(Color(0xFFDC2626))
                            .clickable {
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
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tr("common.delete").uppercase(),
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp,
                            letterSpacing = 0.6.sp,
                            color = BellTextPrimary
                        )
                    }
                }
            }
        }
    }

    // --- MODPACK IMPORT PROGRESS DIALOG ---
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
                        .background(BellGlassElevated)
                        .border(1.dp, BellBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = BellTextPrimary,
                        strokeWidth = 2.5.dp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "IMPORTING MODPACK",
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    letterSpacing = 0.7.sp,
                    color = BellTextPrimary
                )

                if (modpackImportName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = modpackImportName,
                        fontSize = 11.sp,
                        color = BellTextSecondary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (modpackImportStatus.isNotBlank()) modpackImportStatus else "Parsing mod list and downloading files...",
                    fontSize = 11.sp,
                    color = BellTextSecondary,
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
                        color = BellPillWhite,
                        trackColor = Color(0x26FFFFFF)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$modpackImportCurrent / $modpackImportTotal mod files",
                        fontSize = 10.sp,
                        color = BellTextSecondary
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = BellPillWhite,
                        trackColor = Color(0x26FFFFFF)
                    )
                }
            }
        }
    }

    // --- MODRINTH PROJECT DETAIL MODAL ---
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
                    .background(BellGlassElevated)
                    .border(1.dp, BellBorder, modalOuterShape)
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
                                    .size(46.dp)
                                    .clip(subCardShape)
                                    .background(Color(0x1AFFFFFF))
                                    .border(1.dp, BellBorder, subCardShape),
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
                                        color = BellTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(BellPillWhite)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = hit.projectType.uppercase(),
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Black,
                                            color = BellInkDark
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "by ${hit.author ?: "Community"}",
                                        fontSize = 10.sp,
                                        color = BellTextSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "·", color = BellTextMuted, fontSize = 10.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = formatCompactNumber(hit.downloads),
                                        fontSize = 10.sp,
                                        color = BellTextSecondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(BellGlassCard)
                                .border(1.dp, BellBorder, CircleShape)
                                .clickable(enabled = !isDetailInstalling) { detailModalHit = null },
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

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isTabOverview = detailTab == "overview"
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isTabOverview) BellPillWhite else BellGlassCard)
                                .border(
                                    1.dp,
                                    if (isTabOverview) BellPillWhite else BellBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { detailTab = "overview" },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "OVERVIEW & GALLERY",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.6.sp,
                                color = if (isTabOverview) BellInkDark else BellTextSecondary
                            )
                        }

                        val isTabVersions = detailTab == "versions"
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isTabVersions) BellPillWhite else BellGlassCard)
                                .border(
                                    1.dp,
                                    if (isTabVersions) BellPillWhite else BellBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { detailTab = "versions" },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "COMPATIBLE VERSIONS (${compatibleVersions.size})",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.6.sp,
                                color = if (isTabVersions) BellInkDark else BellTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(modalInnerShape)
                            .background(BellGlassInput)
                            .border(1.dp, BellBorder, modalInnerShape)
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
                                    CircularProgressIndicator(color = BellTextPrimary, modifier = Modifier.size(26.dp))
                                }
                            } else if (detailTab == "overview") {
                                val galleryList = detailProject?.gallery ?: emptyList()
                                if (galleryList.isNotEmpty()) {
                                    Text(
                                        text = "SCREENSHOTS",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        color = BellTextMuted,
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
                                                    .background(BellGlassCard)
                                                    .border(1.dp, BellBorder, RoundedCornerShape(8.dp))
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
                                    text = "DESCRIPTION",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    color = BellTextMuted,
                                    letterSpacing = 0.8.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = detailProject?.description ?: hit.description ?: "No description summary available.",
                                    fontSize = 11.5.sp,
                                    color = BellTextPrimary,
                                    lineHeight = 17.sp
                                )
                            } else {
                                if (compatibleVersions.isEmpty()) {
                                    Text(
                                        text = "No release matching Minecraft ${selectedInstance?.mcVersion ?: ""} (${(selectedInstance?.loader ?: "Vanilla").uppercase()}).",
                                        color = BellTextSecondary,
                                        fontSize = 10.sp
                                    )
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        compatibleVersions.forEach { ver ->
                                            val isPicked = selectedVersion?.id == ver.id
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isPicked) Color(0x26FFFFFF) else BellGlassCard)
                                                    .border(
                                                        width = 1.dp,
                                                        color = if (isPicked) BellPillWhite else BellBorder,
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable { selectedVersion = ver }
                                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = ver.name.ifBlank { ver.versionNumber },
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 11.5.sp,
                                                            color = BellTextPrimary,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = "Version: ${ver.versionNumber} · MC: ${ver.gameVersions.joinToString(", ")}",
                                                            fontSize = 9.sp,
                                                            color = BellTextSecondary
                                                        )
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(Color(0x1AFFFFFF))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = ver.versionType.uppercase(),
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = BellTextPrimary
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

                    Spacer(modifier = Modifier.height(10.dp))

                    if (isDetailInstalling) {
                        val installingLabel: String = detailProgressText.ifBlank { "Downloading and installing..." }
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = BellTextPrimary, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = installingLabel,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp,
                                color = BellTextPrimary
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(BellGlassCard)
                                    .border(1.dp, BellBorder, RoundedCornerShape(9.dp))
                                    .clickable { detailModalHit = null },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tr("common.close").uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    letterSpacing = 0.7.sp,
                                    color = BellTextSecondary
                                )
                            }

                            if (isHitInstalled) {
                                Box(
                                    modifier = Modifier
                                        .weight(2f)
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(Color(0x1AFFFFFF))
                                        .border(1.dp, BellBorder, RoundedCornerShape(9.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = tr("mods.already_installed").uppercase(),
                                        color = BellTextPrimary,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.5.sp,
                                        letterSpacing = 0.7.sp
                                    )
                                }
                            } else {
                                val canInstall = compatibleVersions.isNotEmpty()
                                Box(
                                    modifier = Modifier
                                        .weight(2f)
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(if (canInstall) BellPillWhite else Color(0x1AFFFFFF))
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
                                            tint = if (canInstall) BellInkDark else BellTextMuted,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = (if (hit.projectType == "modpack") tr("mods.install_modpack") else tr("mods.install_to_instance")).uppercase(),
                                            color = if (canInstall) BellInkDark else BellTextMuted,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 10.5.sp,
                                            letterSpacing = 0.7.sp
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

    // --- MODAL PREVIEW GALERI GAMBAR ---
    previewGalleryUrl?.let { url ->
        NuxDialog(onDismissRequest = { previewGalleryUrl = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SCREENSHOTS",
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        letterSpacing = 0.7.sp,
                        color = BellTextPrimary
                    )
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(BellGlassCard)
                            .border(1.dp, BellBorder, CircleShape)
                            .clickable { previewGalleryUrl = null },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close", tint = BellTextPrimary, modifier = Modifier.size(14.dp))
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
                        .border(1.dp, BellBorder, RoundedCornerShape(8.dp)),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }
    }
}

@Composable
private fun BellPrimaryButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(BellPillWhite)
            .clickable { onClick() }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = BellInkDark,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                color = BellInkDark,
                fontWeight = FontWeight.Black,
                fontSize = 9.5.sp,
                letterSpacing = 0.7.sp
            )
        }
    }
}

@Composable
private fun BellSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(BellGlassSidebar)
            .border(1.dp, BellBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            tint = BellTextMuted,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(7.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    color = BellTextMuted,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    color = BellTextPrimary,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(BellTextPrimary),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (value.isNotEmpty()) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = tr("common.delete"),
                tint = BellTextSecondary,
                modifier = Modifier
                    .size(13.dp)
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
