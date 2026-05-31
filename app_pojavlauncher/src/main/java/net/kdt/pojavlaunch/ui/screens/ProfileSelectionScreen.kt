package net.kdt.pojavlaunch.ui.screens

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
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
import androidx.core.graphics.drawable.toBitmap
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.BaseActivity
import net.kdt.pojavlaunch.instances.Instance
import net.kdt.pojavlaunch.instances.InstanceIconProvider
import net.kdt.pojavlaunch.ui.theme.PojavTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileSelectionScreen(
    onAddClick: () -> Unit,
    onImportClick: () -> Unit,
    onCreateClick: () -> Unit,
    onSelectDirClick: () -> Unit,
    onEditClick: (Instance) -> Unit,
    onDeleteClick: (Instance) -> Unit,
    onSearch: (String) -> Unit,
    onSelect: (Instance) -> Unit,
    onFilterChange: (Boolean, Boolean, Boolean) -> Unit,
    profiles: List<Instance>,
    selectedPathName: String,
    searchQuery: String,
    showReleases: Boolean,
    showSnapshots: Boolean,
    showModded: Boolean,
    isLoading: Boolean
) {
    val leftScrollState = rememberScrollState()
    val listState = rememberLazyListState()
    var isFiltersExpanded by remember { mutableStateOf(true) }

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
            // Left Column: Navigation and Actions
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
                    // Header and Search bar removed per request
                    
                    // Vertical Filter Menu
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)), RoundedCornerShape(14.dp))
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isFiltersExpanded = !isFiltersExpanded }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Filters",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            Icon(
                                painter = painterResource(id = R.drawable.spinner_arrow),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(12.dp)
                                    .rotate(if (isFiltersExpanded) 0f else -90f),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                        
                        AnimatedVisibility(
                            visible = isFiltersExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                ProfileFilterItem("Releases", showReleases) { onFilterChange(it, showSnapshots, showModded) }
                                ProfileFilterItem("Snapshots", showSnapshots) { onFilterChange(showReleases, it, showModded) }
                                ProfileFilterItem("Modded", showModded) { onFilterChange(showReleases, showSnapshots, it) }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Buttons - High Emphasis (Filled Pill Shape)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProfileActionButton(
                            text = stringResource(id = R.string.main_select_instance_directory),
                            icon = R.drawable.ic_px_folder,
                            onClick = onSelectDirClick
                        )
                        
                        ProfileActionButton(
                            text = stringResource(id = R.string.import_local_modpack),
                            icon = R.drawable.ic_px_download,
                            onClick = onImportClick
                        )
                        
                        ProfileActionButton(
                            text = stringResource(id = R.string.create_instance),
                            icon = R.drawable.ic_add,
                            onClick = onCreateClick
                        )
                    }
                }
            }

            // Right Column: Profile List
            Surface(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight(),
                color = Color.Transparent
            ) {
                AnimatedContent(
                    targetState = isLoading to profiles,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(220, delayMillis = 90)) + 
                         slideInHorizontally(initialOffsetX = { it / 4 }))
                        .togetherWith(fadeOut(animationSpec = tween(90)) + 
                         slideOutHorizontally(targetOffsetX = { -it / 4 }))
                    },
                    label = "listSwitch"
                ) { (loading, currentProfiles) ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center),
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 8.dp)
                            ) {
                                items(currentProfiles, key = { it.mInstanceRoot?.name ?: it.hashCode() }) { profile ->
                                    ProfileItem(
                                        instance = profile,
                                        isSelected = profile.mInstanceRoot?.name == selectedPathName,
                                        onClick = { onSelect(profile) },
                                        onEditClick = { onEditClick(profile) },
                                        onDeleteClick = { onDeleteClick(profile) }
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

@Composable
fun ProfileFilterItem(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Surface(
        onClick = { onCheckedChange(!checked) },
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .padding(horizontal = 4.dp),
        shape = RoundedCornerShape(10.dp),
        color = if (checked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent,
        contentColor = if (checked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    ) {
        Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.padding(horizontal = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = if (checked) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                if (checked) {
                    Icon(
                        painter = painterResource(id = R.drawable.pref_toggle_tick),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileActionButton(
    text: String,
    icon: Int,
    onClick: () -> Unit
) {
    // ✅ High emphasis Filled Button with Pill Shape
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ProfileItem(
    instance: Instance,
    isSelected: Boolean,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val context = LocalContext.current
    val iconPainter = remember(instance) {
        BitmapPainter(InstanceIconProvider.fetchIcon(context.resources, instance).toBitmap().asImageBitmap())
    }

    val displayName = remember(instance) {
        var name = instance.name
        if (name == null || name.isEmpty() || name == "New") {
            name = instance.versionId ?: "Unknown"
        } else {
            name = "$name (${instance.versionId})"
        }
        name
    }

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = isSelected,
                onClick = null,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary,
                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Image(
                painter = iconPainter,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = displayName,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Edit",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
