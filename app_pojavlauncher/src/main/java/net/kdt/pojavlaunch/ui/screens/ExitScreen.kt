package net.kdt.pojavlaunch.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.ui.theme.PojavTheme

@Composable
fun ExitScreen(
    title: String,
    logs: String,
    onShareClick: () -> Unit,
    onCopyClick: () -> Unit,
    onRestartClick: () -> Unit,
    onOpenCrashReport: (String) -> Unit = {}
) {
    val isPreview = LocalInspectionMode.current
    val ignoreNotch = if (isPreview) true else LauncherPreferences.PREF_IGNORE_NOTCH
    
    // Detect crash report in logs
    val crashReportPath = remember(logs) {
        val marker = "#@!@# Game crashed! Crash report saved to: #@!@# "
        val index = logs.indexOf(marker)
        if (index != -1) {
            logs.substring(index + marker.length).trim().substringBefore("\n")
        } else null
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        val layoutModifier = if (ignoreNotch) {
            Modifier.fillMaxSize()
        } else {
            Modifier.fillMaxSize()
                .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
        }

        Row(
            modifier = layoutModifier.padding(16.dp)
        ) {
            // Log Area
            Box(
                modifier = Modifier
                    .weight(0.7f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.8f))
                    .padding(8.dp)
            ) {
                val scrollState = rememberScrollState()
                
                // Automatically scroll to bottom when text size changes (meaning logs loaded)
                LaunchedEffect(scrollState.maxValue) {
                    if (!isPreview && scrollState.maxValue > 0) {
                        scrollState.scrollTo(scrollState.maxValue)
                    }
                }

                Text(
                    text = logs,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState),
                    color = Color(0xFFCCCCCC),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Control Panel
            Column(
                modifier = Modifier
                    .weight(0.3f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (crashReportPath != null) "Game Crashed" else title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (crashReportPath != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                if (crashReportPath != null) {
                    Button(
                        onClick = { onOpenCrashReport(crashReportPath) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_px_file),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View crash report")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Button(
                    onClick = onShareClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = CircleShape
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_px_sharelog),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(id = R.string.main_share_logs))
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onCopyClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = CircleShape
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_px_file),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copy logs")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ✅ Improved Restart Button - Pill shape and primary colors
                Button(
                    onClick = onRestartClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_px_refresh),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(id = R.string.global_restart).uppercase(), fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Preview(showBackground = true, device = "spec:width=1280dp,height=800dp,orientation=landscape")
@Composable
fun ExitScreenPreview() {
    PojavTheme {
        ExitScreen(
            title = "Game exited with code 1",
            logs = "[10:57:55] [main/INFO]: Loading Minecraft...\n".repeat(20),
            onShareClick = {},
            onCopyClick = {},
            onRestartClick = {}
        )
    }
}

@Preview(showBackground = true, device = "spec:width=1280dp,height=800dp,orientation=landscape")
@Composable
fun ExitScreenCrashPreview() {
    PojavTheme {
        ExitScreen(
            title = "Game exited with code 1",
            logs = "[10:57:55] [main/INFO]: Loading Minecraft...\n#@!@# Game crashed! Crash report saved to: #@!@# /storage/emulated/0/crash.txt",
            onShareClick = {},
            onCopyClick = {},
            onRestartClick = {}
        )
    }
}
