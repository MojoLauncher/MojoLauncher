package net.kdt.pojavlaunch.ui.screens

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.BaseActivity
import net.kdt.pojavlaunch.multirt.Runtime
import net.kdt.pojavlaunch.ui.theme.PojavTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstanceEditorScreen(
    onBack: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onIconClick: () -> Unit,
    onVersionClick: () -> Unit,
    onControlClick: () -> Unit,
    
    instanceIcon: Drawable?,
    name: String,
    onNameChange: (String) -> Unit,
    versionId: String,
    controlLayout: String,
    jvmArgs: String,
    onJvmArgsChange: (String) -> Unit,
    sharedData: Boolean,
    onSharedDataChange: (Boolean) -> Unit,
    
    availableRuntimes: List<Runtime>,
    selectedRuntime: Runtime?,
    onRuntimeSelected: (Runtime) -> Unit,
    
    rendererDisplayNames: List<String>,
    selectedRendererIndex: Int,
    onRendererSelected: (Int) -> Unit
) {
    val scrollState = rememberScrollState()
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
            // Left Column: All Edit Instance Options
            Surface(
                modifier = Modifier
                    .weight(0.75f)
                    .fillMaxHeight()
                    .padding(end = 8.dp),
                color = Color.Transparent
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(4.dp)
                ) {
                    // Header and back button removed per request

                    // Icon and Name Section
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .clickable { onIconClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            if (instanceIcon != null) {
                                val bitmap = remember(instanceIcon) { instanceIcon.toBitmap().asImageBitmap() }
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = "Instance Icon",
                                    modifier = Modifier.fillMaxSize().padding(8.dp),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_px_java),
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            
                            // Edit Overlay
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(24.dp)
                                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f), RoundedCornerShape(topStart = 8.dp))
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp).align(Alignment.Center),
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = onNameChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Instance Name", fontSize = 12.sp) },
                        placeholder = { Text(stringResource(id = R.string.unnamed)) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Selectors
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Version",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.padding(start = 4.dp)
                        )
                        EditorSelectorButton(
                            text = versionId.ifEmpty { stringResource(id = R.string.version_select_hint) },
                            onClick = onVersionClick
                        )

                        Text(
                            text = "Control Layout",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.padding(start = 4.dp)
                        )
                        EditorSelectorButton(
                            text = controlLayout.ifEmpty { stringResource(id = R.string.use_global_default) },
                            onClick = onControlClick
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Technical Settings
                    OutlinedTextField(
                        value = jvmArgs,
                        onValueChange = onJvmArgsChange,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                        label = { Text("JVM Arguments", fontSize = 12.sp) },
                        placeholder = { Text(stringResource(id = R.string.use_global_default)) },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Java Runtime",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                    
                    EditorDropdown(
                        options = availableRuntimes.map { it.name },
                        selectedOption = selectedRuntime?.name ?: "<Default>",
                        onOptionSelected = { name ->
                            availableRuntimes.find { it.name == name }?.let { onRuntimeSelected(it) }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Renderer",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                    
                    EditorDropdown(
                        options = rendererDisplayNames,
                        selectedOption = if (selectedRendererIndex in rendererDisplayNames.indices) rendererDisplayNames[selectedRendererIndex] else "Default",
                        onOptionSelected = { name ->
                            onRendererSelected(rendererDisplayNames.indexOf(name))
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Shared Data
                    Surface(
                        onClick = { onSharedDataChange(!sharedData) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = sharedData,
                                onCheckedChange = onSharedDataChange,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary,
                                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (sharedData) stringResource(id = R.string.instance_shared_data_on) else stringResource(id = R.string.instance_shared_data_off),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Right Column: Save and Delete Buttons Only
            Surface(
                modifier = Modifier
                    .weight(0.25f)
                    .fillMaxHeight(),
                color = Color.Transparent
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // High Emphasis Pill Shape Delete Button
                    Button(
                        onClick = onDelete,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // High Emphasis Pill Shape Save Button
                    Button(
                        onClick = onSave,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun EditorSelectorButton(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun EditorDropdown(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth().height(44.dp),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedOption,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    painter = painterResource(id = R.drawable.spinner_arrow),
                    contentDescription = null,
                    modifier = Modifier.size(12.dp).alpha(0.6f),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface).width(240.dp)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, color = MaterialTheme.colorScheme.onSurface) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
