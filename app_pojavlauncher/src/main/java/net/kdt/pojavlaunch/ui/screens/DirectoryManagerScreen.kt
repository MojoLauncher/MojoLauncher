package net.kdt.pojavlaunch.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import kotlinx.coroutines.launch
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.BaseActivity
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DirectoryManagerScreen(
    onBack: () -> Unit,
    title: String,
    breadcrumbs: List<Pair<String, File>>,
    entries: List<File>,
    selectedFile: File?,
    statusText: String,
    onEntryClick: (File) -> Unit,
    onEntryLongClick: (File) -> Unit,
    onCrumbClick: (File) -> Unit,
    onUpClick: () -> Unit,
    onUploadClick: () -> Unit,
    onNewFolderClick: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    val leftScrollState = rememberScrollState()
    val listState = rememberLazyListState()

    val isPreview = LocalInspectionMode.current
    
    // ✅ Fix: Don't render background if not in preview to avoid overlapping with LauncherScreen
    val backgroundBitmap = if (isPreview) BaseActivity.getBackgroundBitmap() else null
    val hasBackground = backgroundBitmap != null

    Box(modifier = Modifier.fillMaxSize()) {
        // Shared Background Logic (Preview only)
        if (isPreview) {
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
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // Left Column: Actions and Path
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
                    // Path Breadcrumbs
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        androidx.compose.foundation.layout.FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            breadcrumbs.forEachIndexed { index, (name, file) ->
                                Text(
                                    text = name,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable { onCrumbClick(file) }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                                if (index < breadcrumbs.size - 1) {
                                    Text(
                                        text = "/",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Buttons - High Emphasis Pill Shape
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FileActionButton(
                            text = "Up",
                            icon = R.drawable.ic_px_home, 
                            onClick = onUpClick
                        )
                        FileActionButton(
                            text = "Upload",
                            icon = R.drawable.ic_px_download,
                            onClick = onUploadClick
                        )
                        FileActionButton(
                            text = "New folder",
                            icon = R.drawable.ic_add,
                            onClick = onNewFolderClick
                        )
                        FileActionButton(
                            text = "Rename",
                            icon = R.drawable.ic_px_edit,
                            onClick = onRenameClick,
                            enabled = selectedFile != null
                        )
                        FileActionButton(
                            text = "Delete",
                            icon = R.drawable.ic_px_trash,
                            onClick = onDeleteClick,
                            enabled = selectedFile != null,
                            isError = true
                        )
                    }

                    if (statusText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = statusText,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }

            // Right Column: File List
            Surface(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight(),
                color = Color.Transparent
            ) {
                AnimatedContent(
                    targetState = entries,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(220, delayMillis = 90)) + 
                         slideInHorizontally(initialOffsetX = { it / 4 }))
                        .togetherWith(fadeOut(animationSpec = tween(90)) + 
                         slideOutHorizontally(targetOffsetX = { -it / 4 }))
                    },
                    label = "fileSwitch"
                ) { currentEntries ->
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(currentEntries, key = { it.absolutePath }) { entry ->
                            FileEntryItem(
                                file = entry,
                                isSelected = selectedFile == entry,
                                onClick = { onEntryClick(entry) },
                                onLongClick = { onEntryLongClick(entry) }
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FileActionButton(
    text: String,
    icon: Int,
    onClick: () -> Unit,
    enabled: Boolean = true,
    isError: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp),
        shape = CircleShape,
        colors = if (isError) {
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            )
        } else {
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        },
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileEntryItem(
    file: File,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = if (file.isDirectory) R.drawable.ic_px_folder else R.drawable.ic_px_file),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = file.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (isSelected) {
                Icon(
                    painter = painterResource(id = R.drawable.pref_toggle_tick),
                    contentDescription = "Selected",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
