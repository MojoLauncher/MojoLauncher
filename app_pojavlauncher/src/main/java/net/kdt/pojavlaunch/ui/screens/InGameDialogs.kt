package net.kdt.pojavlaunch.ui.screens

import android.graphics.Color as AndroidColor
import android.view.LayoutInflater
import android.widget.EditText
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.edit
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.EfficientAndroidLWJGLKeycode
import net.kdt.pojavlaunch.colorselector.*
import net.kdt.pojavlaunch.customcontrols.ControlData
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.ui.theme.PojavTheme

@Composable
fun QuickSettingsDialog(
    onDismissRequest: () -> Unit,
    onResolutionChanged: () -> Unit,
    onGyroStateChanged: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = stringResource(id = R.string.quick_setting_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                var resolution by remember { mutableFloatStateOf(LauncherPreferences.PREF_SCALE_FACTOR) }
                var gyroEnabled by remember { mutableStateOf(LauncherPreferences.PREF_ENABLE_GYRO) }
                var gyroInvertX by remember { mutableStateOf(LauncherPreferences.PREF_GYRO_INVERT_X) }
                var gyroInvertY by remember { mutableStateOf(LauncherPreferences.PREF_GYRO_INVERT_Y) }
                var gyroSensitivity by remember { mutableFloatStateOf(LauncherPreferences.PREF_GYRO_SENSITIVITY) }
                var mouseSpeed by remember { mutableFloatStateOf(LauncherPreferences.PREF_MOUSESPEED) }
                var gesturesDisabled by remember { mutableStateOf(LauncherPreferences.PREF_DISABLE_GESTURES) }
                var gestureDelay by remember { mutableIntStateOf(LauncherPreferences.PREF_LONGPRESS_TRIGGER) }

                SettingsSlider(
                    label = stringResource(id = R.string.mcl_setting_title_resolution_scaler),
                    value = resolution,
                    onValueChange = { 
                        resolution = it
                        LauncherPreferences.PREF_SCALE_FACTOR = it
                        LauncherPreferences.DEFAULT_PREF?.edit { putInt("resolutionRatio", (it * 100).toInt()) }
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
                        LauncherPreferences.DEFAULT_PREF?.edit { putBoolean("enableGyro", it) }
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
                            LauncherPreferences.DEFAULT_PREF?.edit { putBoolean("gyroInvertX", it) }
                            onGyroStateChanged()
                        }
                    )
                    SettingsSwitch(
                        label = stringResource(id = R.string.preference_gyro_invert_y_axis),
                        checked = gyroInvertY,
                        onCheckedChange = {
                            gyroInvertY = it
                            LauncherPreferences.PREF_GYRO_INVERT_Y = it
                            LauncherPreferences.DEFAULT_PREF?.edit { putBoolean("gyroInvertY", it) }
                            onGyroStateChanged()
                        }
                    )
                    SettingsSlider(
                        label = stringResource(id = R.string.preference_gyro_sensitivity_title),
                        value = gyroSensitivity,
                        onValueChange = {
                            gyroSensitivity = it
                            LauncherPreferences.PREF_GYRO_SENSITIVITY = it
                            LauncherPreferences.DEFAULT_PREF?.edit { putInt("gyroSensitivity", (it * 100).toInt()) }
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
                        LauncherPreferences.DEFAULT_PREF?.edit { putInt("mousespeed", (it * 100).toInt()) }
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
                        LauncherPreferences.DEFAULT_PREF?.edit { putBoolean("disableGestures", it) }
                    }
                )

                if (!gesturesDisabled) {
                    SettingsSlider(
                        label = stringResource(id = R.string.mcl_setting_title_longpresstrigger),
                        value = gestureDelay.toFloat(),
                        onValueChange = {
                            gestureDelay = it.toInt()
                            LauncherPreferences.PREF_LONGPRESS_TRIGGER = it.toInt()
                            LauncherPreferences.DEFAULT_PREF?.edit { putInt("timeLongPressTrigger", it.toInt()) }
                        },
                        valueRange = 100f..1000f,
                        displayValue = "${gestureDelay}ms"
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Close")
            }
        }
    )
}

@Composable
fun ButtonEditDialog(
    controlData: ControlData,
    onDismissRequest: () -> Unit,
    onSaveRequest: () -> Unit
) {
    var showBgColorPicker by remember { mutableStateOf(false) }
    var showStrokeColorPicker by remember { mutableStateOf(false) }

    if (showBgColorPicker) {
        ColorPickerDialog(
            initialColor = Color(controlData.bgColor),
            onDismissRequest = { showBgColorPicker = false },
            onColorSelected = {
                controlData.bgColor = it.toArgb()
                onSaveRequest()
                showBgColorPicker = false
            }
        )
    }

    if (showStrokeColorPicker) {
        ColorPickerDialog(
            initialColor = Color(controlData.strokeColor),
            onDismissRequest = { showStrokeColorPicker = false },
            onColorSelected = {
                controlData.strokeColor = it.toArgb()
                onSaveRequest()
                showStrokeColorPicker = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = "Edit Control",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                var name by remember { mutableStateOf(controlData.name ?: "") }
                var opacity by remember { mutableFloatStateOf(controlData.opacity) }
                var strokeWidth by remember { mutableFloatStateOf(controlData.strokeWidth) }
                var cornerRadius by remember { mutableFloatStateOf(controlData.cornerRadius) }
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

                ColorSelectorItem(
                    label = "Background Color",
                    color = Color(controlData.bgColor),
                    onClick = { showBgColorPicker = true }
                )

                ColorSelectorItem(
                    label = "Stroke Color",
                    color = Color(controlData.strokeColor),
                    onClick = { showStrokeColorPicker = true }
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
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Close")
            }
        }
    )
}

@Composable
fun ColorSelectorItem(
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, fontSize = 12.sp)
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
        )
    }
}

@Composable
fun ColorPickerDialog(
    initialColor: Color,
    onDismissRequest: () -> Unit,
    onColorSelected: (Color) -> Unit
) {
    var currentColorInt by remember { mutableIntStateOf(initialColor.toArgb()) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Select Color") },
        text = {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                factory = { ctx ->
                    LayoutInflater.from(ctx).inflate(R.layout.dialog_color_selector, null).apply {
                        val hueView = findViewById<HueView>(R.id.color_selector_hue_view)
                        val svView = findViewById<SVRectangleView>(R.id.color_selector_rectangle_view)
                        val alphaView = findViewById<AlphaView>(R.id.color_selector_alpha_view)
                        val colorView = findViewById<ColorSideBySideView>(R.id.color_selector_color_view)
                        val hexEdit = findViewById<EditText>(R.id.color_selector_hex_edit)

                        val hsv = FloatArray(3)
                        AndroidColor.colorToHSV(currentColorInt, hsv)
                        val hueTemplate = floatArrayOf(hsv[0], 1f, 1f)
                        var alphaSelected = AndroidColor.alpha(currentColorInt)

                        fun updateColor(notify: Boolean) {
                            val color = AndroidColor.HSVToColor(alphaSelected, hsv)
                            if (notify) currentColorInt = color
                            colorView.setColor(color)
                            hexEdit.setText(String.format("%08X", color))
                        }

                        hueView.setHue(hsv[0])
                        hueView.setHueSelectionListener(object : HueSelectionListener {
                            override fun onHueSelected(hue: Float) {
                                hsv[0] = hue
                                hueTemplate[0] = hue
                                svView.setColor(AndroidColor.HSVToColor(hueTemplate), true)
                                updateColor(true)
                            }
                        })

                        svView.setColor(AndroidColor.HSVToColor(hueTemplate), false)
                        svView.setLuminosityIntensity(hsv[2], hsv[1])
                        svView.setRectSelectionListener(object : RectangleSelectionListener {
                            override fun onLuminosityIntensityChanged(luminosity: Float, intensity: Float) {
                                hsv[1] = intensity
                                hsv[2] = luminosity
                                updateColor(true)
                            }
                        })

                        alphaView.setAlpha(alphaSelected)
                        alphaView.setAlphaSelectionListener(object : AlphaSelectionListener {
                            override fun onAlphaSelected(alpha: Int) {
                                alphaSelected = alpha
                                updateColor(true)
                            }
                        })
                        
                        updateColor(false)
                    }
                }
            )
        },
        confirmButton = {
            Button(onClick = { onColorSelected(Color(currentColorInt)) }) {
                Text("Select")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
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
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("K${index + 1}: ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(currentName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
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
            .height(36.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, fontSize = 12.sp)
        Switch(
            checked = checked, 
            onCheckedChange = onCheckedChange,
            modifier = Modifier.scale(0.8f),
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
            Text(text = label, style = MaterialTheme.typography.bodySmall, fontSize = 12.sp)
            Text(text = displayValue, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier
                .height(24.dp)
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
            controlData = ControlData(name = "Preview"),
            onDismissRequest = {},
            onSaveRequest = {}
        )
    }
}
