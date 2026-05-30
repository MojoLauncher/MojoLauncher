package net.kdt.pojavlaunch.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.EfficientAndroidLWJGLKeycode
import net.kdt.pojavlaunch.customcontrols.ControlData
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.ui.theme.PojavTheme

@Composable
fun SideSheetLayout(
    title: String,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    // Custom "Side Sheet" using a Box aligned to the right
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.32f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismissRequest
            ),
        contentAlignment = Alignment.CenterEnd
    ) {
        Surface(
            modifier = Modifier
                .padding(16.dp) // Floating horizontal rectangle look
                .width(280.dp) 
                .heightIn(max = 450.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {} // Consume clicks to prevent dialog from closing
                ),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false) 
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
fun QuickSettingsDialog(
    onDismissRequest: () -> Unit,
    onResolutionChanged: () -> Unit,
    onGyroStateChanged: () -> Unit
) {
    // Use Dialog for proper event handling and focus
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        SideSheetLayout(
            title = stringResource(id = R.string.quick_setting_title),
            onDismissRequest = onDismissRequest
        ) {
            var resolution by remember { mutableStateOf(LauncherPreferences.PREF_SCALE_FACTOR) }
            var gyroEnabled by remember { mutableStateOf(LauncherPreferences.PREF_ENABLE_GYRO) }
            var gyroInvertX by remember { mutableStateOf(LauncherPreferences.PREF_GYRO_INVERT_X) }
            var gyroInvertY by remember { mutableStateOf(LauncherPreferences.PREF_GYRO_INVERT_Y) }
            var gyroSensitivity by remember { mutableStateOf(LauncherPreferences.PREF_GYRO_SENSITIVITY) }
            var mouseSpeed by remember { mutableStateOf(LauncherPreferences.PREF_MOUSESPEED) }
            var gesturesDisabled by remember { mutableStateOf(LauncherPreferences.PREF_DISABLE_GESTURES) }
            var gestureDelay by remember { mutableStateOf(LauncherPreferences.PREF_LONGPRESS_TRIGGER) }

            SettingsSlider(
                label = stringResource(id = R.string.mcl_setting_title_resolution_scaler),
                value = resolution,
                onValueChange = { 
                    resolution = it
                    LauncherPreferences.PREF_SCALE_FACTOR = it
                    LauncherPreferences.DEFAULT_PREF?.edit()?.putInt("resolutionRatio", (it * 100).toInt())?.apply()
                    onResolutionChanged()
                },
                valueRange = 0.25f..1f,
                displayValue = "${(resolution * 100).toInt()}%"
            )

            SettingsSwitch(
                label = stringResource(id = R.string.preference_enable_gyro_title),
                checked = gyroEnabled,
                onCheckedChange = {
                    gyroEnabled = it
                    LauncherPreferences.PREF_ENABLE_GYRO = it
                    LauncherPreferences.DEFAULT_PREF?.edit()?.putBoolean("enableGyro", it)?.apply()
                    onGyroStateChanged()
                }
            )

            if (gyroEnabled) {
                SettingsSwitch(
                    label = stringResource(id = R.string.preference_gyro_invert_x_axis),
                    checked = gyroInvertX,
                    onCheckedChange = {
                        gyroInvertX = it
                        LauncherPreferences.PREF_GYRO_INVERT_X = it
                        LauncherPreferences.DEFAULT_PREF?.edit()?.putBoolean("gyroInvertX", it)?.apply()
                        onGyroStateChanged()
                    }
                )
                SettingsSwitch(
                    label = stringResource(id = R.string.preference_gyro_invert_y_axis),
                    checked = gyroInvertY,
                    onCheckedChange = {
                        gyroInvertY = it
                        LauncherPreferences.PREF_GYRO_INVERT_Y = it
                        LauncherPreferences.DEFAULT_PREF?.edit()?.putBoolean("gyroInvertY", it)?.apply()
                        onGyroStateChanged()
                    }
                )
                SettingsSlider(
                    label = stringResource(id = R.string.preference_gyro_sensitivity_title),
                    value = gyroSensitivity,
                    onValueChange = {
                        gyroSensitivity = it
                        LauncherPreferences.PREF_GYRO_SENSITIVITY = it
                        LauncherPreferences.DEFAULT_PREF?.edit()?.putInt("gyroSensitivity", (it * 100).toInt())?.apply()
                    },
                    valueRange = 0.1f..4f,
                    displayValue = "${(gyroSensitivity * 100).toInt()}%"
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            SettingsSlider(
                label = stringResource(id = R.string.mcl_setting_title_mousespeed),
                value = mouseSpeed,
                onValueChange = {
                    mouseSpeed = it
                    LauncherPreferences.PREF_MOUSESPEED = it
                    LauncherPreferences.DEFAULT_PREF?.edit()?.putInt("mousespeed", (it * 100).toInt())?.apply()
                },
                valueRange = 0.25f..3f,
                displayValue = "${(mouseSpeed * 100).toInt()}%"
            )

            SettingsSwitch(
                label = stringResource(id = R.string.mcl_disable_gestures),
                checked = gesturesDisabled,
                onCheckedChange = {
                    gesturesDisabled = it
                    LauncherPreferences.PREF_DISABLE_GESTURES = it
                    LauncherPreferences.DEFAULT_PREF?.edit()?.putBoolean("disableGestures", it)?.apply()
                }
            )

            if (!gesturesDisabled) {
                SettingsSlider(
                    label = stringResource(id = R.string.mcl_setting_title_longpresstrigger),
                    value = gestureDelay.toFloat(),
                    onValueChange = {
                        gestureDelay = it.toInt()
                        LauncherPreferences.PREF_LONGPRESS_TRIGGER = it.toInt()
                        LauncherPreferences.DEFAULT_PREF?.edit()?.putInt("timeLongPressTrigger", it.toInt())?.apply()
                    },
                    valueRange = 100f..1000f,
                    displayValue = "${gestureDelay}ms"
                )
            }
        }
    }
}

@Composable
fun ButtonEditDialog(
    controlData: ControlData,
    onDismissRequest: () -> Unit,
    onSaveRequest: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        SideSheetLayout(
            title = "Edit Control",
            onDismissRequest = onDismissRequest
        ) {
            var name by remember { mutableStateOf(controlData.name ?: "") }
            var opacity by remember { mutableStateOf(controlData.opacity) }
            var strokeWidth by remember { mutableStateOf(controlData.strokeWidth) }
            var cornerRadius by remember { mutableStateOf(controlData.cornerRadius) }
            var isToggle by remember { mutableStateOf(controlData.isToggle) }
            var passThru by remember { mutableStateOf(controlData.passThruEnabled) }
            var isSwipeable by remember { mutableStateOf(controlData.isSwipeable) }
            var displayInGame by remember { mutableStateOf(controlData.displayInGame) }
            var displayInMenu by remember { mutableStateOf(controlData.displayInMenu) }

            OutlinedTextField(
                value = name,
                onValueChange = { newName ->
                    name = newName
                    controlData.name = newName
                    onSaveRequest()
                },
                label = { Text("Name", fontSize = 10.sp) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
            )

            SettingsSlider(
                label = "Opacity",
                value = opacity,
                onValueChange = { 
                    opacity = it
                    controlData.opacity = it
                    onSaveRequest()
                },
                valueRange = 0f..1f,
                displayValue = "${(opacity * 100).toInt()}%"
            )

            SettingsSlider(
                label = "Stroke Width",
                value = strokeWidth,
                onValueChange = { 
                    strokeWidth = it
                    controlData.strokeWidth = it
                    onSaveRequest()
                },
                valueRange = 0f..20f,
                displayValue = "${strokeWidth.toInt()}dp"
            )

            SettingsSlider(
                label = "Corner Radius",
                value = cornerRadius,
                onValueChange = { 
                    cornerRadius = it
                    controlData.cornerRadius = it
                    onSaveRequest()
                },
                valueRange = 0f..100f, 
                displayValue = "${cornerRadius.toInt()}%"
            )

            SettingsSwitch(
                label = "Toggleable", 
                checked = isToggle, 
                onCheckedChange = { 
                    isToggle = it
                    controlData.isToggle = it
                    onSaveRequest()
                }
            )
            
            SettingsSwitch(
                label = "Pass-through", 
                checked = passThru, 
                onCheckedChange = { 
                    passThru = it
                    controlData.passThruEnabled = it
                    onSaveRequest()
                }
            )
            
            SettingsSwitch(
                label = "Swipeable", 
                checked = isSwipeable, 
                onCheckedChange = { 
                    isSwipeable = it
                    controlData.isSwipeable = it
                    onSaveRequest()
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text("Visibility", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            
            SettingsSwitch(
                label = "In Game", 
                checked = displayInGame, 
                onCheckedChange = { 
                    displayInGame = it
                    controlData.displayInGame = it
                    onSaveRequest()
                }
            )
            
            SettingsSwitch(
                label = "In Menus", 
                checked = displayInMenu, 
                onCheckedChange = { 
                    displayInMenu = it
                    controlData.displayInMenu = it
                    onSaveRequest()
                }
            )
            
            Text("Mapping", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            controlData.keycodes.forEachIndexed { index, code ->
                KeycodePicker(index, code) { newCode ->
                    controlData.keycodes[index] = newCode
                    onSaveRequest()
                }
            }
        }
    }
}

@Composable
fun KeycodePicker(index: Int, currentCode: Int, onCodeSelected: (Int) -> Unit) {
    val isInPreview = androidx.compose.ui.platform.LocalInspectionMode.current
    val keyNames = remember { if (isInPreview) arrayOf("KEY_A", "KEY_B") else EfficientAndroidLWJGLKeycode.generateKeyName() }
    val specialNames = remember { if (isInPreview) listOf("SPECIAL_1") else ControlData.buildSpecialButtonArray() ?: emptyList() }
    
    var expanded by remember { mutableStateOf(false) }
    val currentName = remember(currentCode) {
        if (currentCode < 0) {
             specialNames.getOrNull(-(currentCode + 1)) ?: "Unknown"
        } else {
             if (isInPreview) "KEY_A" else keyNames.getOrNull(EfficientAndroidLWJGLKeycode.getIndexByValue(currentCode)) ?: "Unknown"
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedCard(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("K${index + 1}: ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(currentName ?: "Unknown", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
        }
        
        DropdownMenu(
            expanded = expanded, 
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 240.dp)
        ) {
            (specialNames + keyNames.filterNotNull()).forEachIndexed { i, name ->
                DropdownMenuItem(
                    text = { Text(name ?: "", fontSize = 11.sp) },
                    onClick = {
                        if (!isInPreview) {
                            val newCode = if (i < specialNames.size) {
                                -(i + 1)
                            } else {
                                EfficientAndroidLWJGLKeycode.getValueByIndex(i - specialNames.size).toInt()
                            }
                            onCodeSelected(newCode)
                        }
                        expanded = false
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun SettingsSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, fontSize = 11.sp)
        Switch(
            checked = checked, 
            onCheckedChange = onCheckedChange,
            modifier = Modifier.scale(0.65f),
            thumbContent = if (checked) {
                {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize)
                    )
                }
            } else null,
            colors = SwitchDefaults.colors(
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                uncheckedBorderColor = Color.Transparent,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedIconColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}

@Composable
fun SettingsSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    displayValue: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, style = MaterialTheme.typography.bodySmall, fontSize = 11.sp)
            Text(text = displayValue, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, fontSize = 10.sp)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier
                .height(18.dp)
                .scale(0.85f)
        )
    }
}

@Preview(device = "spec:width=1280dp,height=800dp,orientation=landscape")
@Composable
fun QuickSettingsDialogPreview() {
    PojavTheme(dynamicColor = false) {
        QuickSettingsDialog(
            onDismissRequest = {},
            onResolutionChanged = {},
            onGyroStateChanged = {}
        )
    }
}

@Preview(device = "spec:width=1280dp,height=800dp,orientation=landscape")
@Composable
fun ButtonEditDialogPreview() {
    PojavTheme(dynamicColor = false) {
        ButtonEditDialog(
            controlData = ControlData("Preview"),
            onDismissRequest = {},
            onSaveRequest = {}
        )
    }
}
