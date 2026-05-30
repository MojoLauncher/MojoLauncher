package net.kdt.pojavlaunch.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.BaseActivity
import net.kdt.pojavlaunch.ui.theme.PojavTheme

enum class ContentInstallerType(val projectType: String, val labelRes: Int) {
    MODS("mod", R.string.installer_mods),
    SHADERS("shader", R.string.installer_shaders),
    RESOURCES("resourcepack", R.string.installer_packs)
}

data class ModrinthProject(
    val id: String,
    val title: String,
    val description: String,
    val iconUrl: String?,
    val iconBitmap: Bitmap? = null
)

data class ModrinthVersion(
    val name: String,
    val url: String,
    val filename: String?,
    val gameVersions: List<String>,
    val loaders: List<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentInstallerScreen(
    onBack: () -> Unit,
    onOpenDownloads: () -> Unit,
    onInstallLocal: () -> Unit,
    onSearch: (String, ContentInstallerType, version: String?, loader: String?) -> Unit,
    onProjectClick: (ModrinthProject) -> Unit,
    onVersionClick: (ModrinthVersion) -> Unit,
    projects: List<ModrinthProject>,
    isLoading: Boolean,
    statusText: String,
    selectedVersion: String?,
    selectedLoader: String?,
    onVersionFilterChange: (String?) -> Unit,
    onLoaderFilterChange: (String?) -> Unit,
    instanceVersion: String?,
    instanceLoader: String?,
    // For Version Detail View
    viewingProject: ModrinthProject? = null,
    projectVersions: List<ModrinthVersion> = emptyList(),
    availableProjectMCVersions: List<String> = emptyList(),
    selectedProjectMCVersion: String? = null,
    onProjectMCVersionClick: (String) -> Unit = {},
    onBackToProjects: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(ContentInstallerType.MODS) }
    
    val leftScrollState = rememberScrollState()

    val isPreview = LocalInspectionMode.current
    val backgroundBitmap = if (isPreview) null else BaseActivity.getBackgroundBitmap()
    val hasBackground = backgroundBitmap != null

    Box(modifier = Modifier.fillMaxSize()) {
        if (backgroundBitmap != null) {
            Image(
                bitmap = backgroundBitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        }

        Box(modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = if (hasBackground) 0.4f else 0f))
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // Left Column: Navigation and Filters
            Surface(
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight()
                    .padding(end = 8.dp),
                color = Color.Transparent
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(leftScrollState)
                        .padding(4.dp)
                ) {
                    if (viewingProject == null) {
                        // Back button and title
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack, 
                                    contentDescription = "Back", 
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Content Installer",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Type Selection
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ContentInstallerType.entries.forEach { type ->
                                val isSelected = selectedType == type
                                Surface(
                                    onClick = { 
                                        selectedType = type
                                        onSearch(searchQuery, selectedType, selectedVersion, selectedLoader)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .padding(2.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else Color.Transparent,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = stringResource(id = type.labelRes),
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Quick Actions
                        Row(modifier = Modifier.fillMaxWidth()) {
                            InstallerImageButton(
                                onClick = onOpenDownloads,
                                icon = R.drawable.ic_px_folder,
                                contentDescription = "Downloads",
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            InstallerImageButton(
                                onClick = onInstallLocal,
                                icon = R.drawable.ic_px_file,
                                contentDescription = "Install local",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Search Field
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { 
                                searchQuery = it
                                onSearch(it, selectedType, selectedVersion, selectedLoader)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            placeholder = { Text(stringResource(id = R.string.installer_search_hint), fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                onSearch(searchQuery, selectedType, selectedVersion, selectedLoader)
                            })
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Inline Vertical Menus
                        VerticalFilterMenu(
                            title = "Minecraft Version",
                            options = listOf("Any") + (if (instanceVersion != null) listOf(instanceVersion) else emptyList()),
                            selectedOption = selectedVersion ?: "Any",
                            onOptionSelected = {
                                val newValue = if (it == "Any") null else it
                                onVersionFilterChange(newValue)
                                onSearch(searchQuery, selectedType, newValue, selectedLoader)
                            }
                        )

                        if (selectedType == ContentInstallerType.MODS) {
                            Spacer(modifier = Modifier.height(12.dp))
                            VerticalFilterMenu(
                                title = "Mod Loader",
                                options = listOf("Any", "fabric", "forge", "quilt", "neoforge"),
                                labels = listOf("Any", "Fabric", "Forge", "Quilt", "NeoForge"),
                                selectedOption = selectedLoader ?: "Any",
                                onOptionSelected = {
                                    val newValue = if (it == "Any") null else it
                                    onLoaderFilterChange(newValue)
                                    onSearch(searchQuery, selectedType, selectedVersion, newValue)
                                }
                            )
                        }
                    } else {
                        // Viewing Project Detail (Left Panel)
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val iconBitmap = viewingProject.iconBitmap
                            if (iconBitmap != null) {
                                Image(
                                    bitmap = iconBitmap.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_px_download),
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = viewingProject.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = viewingProject.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            Button(
                                onClick = onBackToProjects,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Back")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Status Text
                    Text(
                        text = statusText,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            // Right Column: Content List
            Surface(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight(),
                color = Color.Transparent
            ) {
                AnimatedContent(
                    targetState = viewingProject to selectedProjectMCVersion,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(220, delayMillis = 90)) + 
                         slideInHorizontally(initialOffsetX = { it / 4 }))
                        .togetherWith(fadeOut(animationSpec = tween(90)) + 
                         slideOutHorizontally(targetOffsetX = { -it / 4 }))
                    },
                    label = "screenSwitch"
                ) { (project, mcVersion) ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(32.dp)
                                    .align(Alignment.Center),
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 8.dp)
                            ) {
                                if (project == null) {
                                    items(projects) { p ->
                                        ProjectItemView(
                                            project = p,
                                            onClick = { onProjectClick(p) }
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                    }
                                } else {
                                    if (mcVersion == null) {
                                        // Step 1: Minecraft Version Selection for the project
                                        item {
                                            Text(
                                                "Select Minecraft Version",
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                                            )
                                        }
                                        items(availableProjectMCVersions) { v ->
                                            val isCompatible = instanceVersion != null && v.contains(instanceVersion, ignoreCase = true)
                                            SubVersionItemView(
                                                text = v,
                                                isCompatible = isCompatible,
                                                onClick = { onProjectMCVersionClick(v) }
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                        }
                                    } else {
                                        // Step 2: Project Version Selection (Files)
                                        item {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(bottom = 8.dp)
                                            ) {
                                                TextButton(onClick = { onProjectMCVersionClick("") }) { // Clear sub-filter
                                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(Modifier.width(4.dp))
                                                    Text("Change Version", fontSize = 12.sp)
                                                }
                                                Text(
                                                    "Files for $mcVersion",
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                        val filteredVersions = projectVersions.filter { it.gameVersions.contains(mcVersion) }
                                        items(filteredVersions) { version ->
                                            val isCompatible = instanceVersion != null && version.gameVersions.any { it.contains(instanceVersion, ignoreCase = true) } &&
                                                               (instanceLoader == null || version.loaders.any { it.equals(instanceLoader, ignoreCase = true) })
                                            
                                            VersionItemView(
                                                version = version,
                                                isCompatible = isCompatible,
                                                onClick = { onVersionClick(version) }
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
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

@Composable
fun VerticalFilterMenu(
    title: String,
    options: List<String>,
    labels: List<String>? = null,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f)), RoundedCornerShape(14.dp))
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
        
        options.forEachIndexed { index, option ->
            val label = labels?.get(index) ?: option
            val isSelected = option.equals(selectedOption, ignoreCase = true) || label.equals(selectedOption, ignoreCase = true)
            
            Surface(
                onClick = { onOptionSelected(option) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .padding(horizontal = 4.dp),
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent,
                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            ) {
                Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.padding(horizontal = 10.dp)) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun SubVersionItemView(
    text: String,
    isCompatible: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isCompatible) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, if (isCompatible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
    ) {
        Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.padding(16.dp)) {
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = if (isCompatible) FontWeight.Bold else FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun InstallerImageButton(
    onClick: () -> Unit,
    icon: Int,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = contentDescription,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun ProjectItemView(
    project: ModrinthProject,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val iconBitmap = project.iconBitmap
            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.ic_px_java),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = project.description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun VersionItemView(
    version: ModrinthVersion,
    isCompatible: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (isCompatible) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, if (isCompatible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_px_download),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (isCompatible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = version.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val infoText = buildString {
                    if (version.loaders.isNotEmpty()) {
                        append(version.loaders.joinToString(", "))
                        append("  •  ")
                    }
                    append(version.gameVersions.joinToString(", "))
                }
                Text(
                    text = infoText,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 800, heightDp = 400)
@Composable
fun ContentInstallerScreenPreview() {
    PojavTheme(dynamicColor = true) {
        ContentInstallerScreen(
            onBack = {},
            onOpenDownloads = {},
            onInstallLocal = {},
            onSearch = { _, _, _, _ -> },
            onProjectClick = {},
            onVersionClick = {},
            projects = listOf(
                ModrinthProject("1", "Sodium", "A search for performance", null),
                ModrinthProject("2", "Iris Shaders", "A modern shaders mod", null)
            ),
            isLoading = false,
            statusText = "Found 2 results",
            selectedVersion = "1.20.1",
            selectedLoader = "Fabric",
            onVersionFilterChange = {},
            onLoaderFilterChange = {},
            instanceVersion = "1.20.1",
            instanceLoader = "fabric"
        )
    }
}
