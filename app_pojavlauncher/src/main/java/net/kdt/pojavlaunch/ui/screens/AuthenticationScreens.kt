package net.kdt.pojavlaunch.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.BaseActivity
import net.kdt.pojavlaunch.ui.theme.PojavTheme

@Composable
fun SelectAuthScreen(
    onBack: () -> Unit,
    onMicrosoftClick: () -> Unit,
    onLocalClick: () -> Unit,
    onElyByClick: () -> Unit
) {
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(0.85f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header centered, back button removed
                Text(
                    text = "Select Login Method",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )

                Spacer(Modifier.height(48.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    AuthMethodButton(
                        modifier = Modifier.weight(1f),
                        title = stringResource(id = R.string.auth_select_microsoft),
                        onClick = onMicrosoftClick
                    )
                    AuthMethodButton(
                        modifier = Modifier.weight(1f),
                        title = stringResource(id = R.string.auth_select_elyby),
                        onClick = onElyByClick
                    )
                    AuthMethodButton(
                        modifier = Modifier.weight(1f),
                        title = stringResource(id = R.string.auth_select_local),
                        onClick = onLocalClick
                    )
                }
            }
        }
    }
}

@Composable
fun AuthMethodButton(
    modifier: Modifier = Modifier,
    title: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp), // standard high emphasis height
        shape = CircleShape, // Pill shape
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun LocalLoginScreen(
    onBack: () -> Unit,
    onLoginClick: (String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    val isPreview = LocalInspectionMode.current
    val backgroundBitmap = if (isPreview) null else BaseActivity.getBackgroundBitmap()
    val hasBackground = backgroundBitmap != null

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
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

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Header centered, back button removed
                Text(
                    text = stringResource(id = R.string.auth_select_local),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )

                Spacer(Modifier.height(32.dp))

                Text(
                    text = stringResource(id = R.string.login_online_username_hint),
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Start).padding(start = 4.dp, bottom = 4.dp),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Username") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedPlaceholderColor = Color.LightGray
                    )
                )

                Spacer(Modifier.height(32.dp))

                Button(
                    onClick = { onLoginClick(username) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = CircleShape, // Pill shape
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = stringResource(id = R.string.login_online_login_label).uppercase(),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, device = "spec:width=1280dp,height=800dp,orientation=landscape")
@Composable
fun SelectAuthScreenPreview() {
    PojavTheme(dynamicColor = true) {
        SelectAuthScreen({}, {}, {}, {})
    }
}

@Preview(showBackground = true, device = "spec:width=1280dp,height=800dp,orientation=landscape")
@Composable
fun LocalLoginScreenPreview() {
    PojavTheme(dynamicColor = true) {
        LocalLoginScreen({}, {})
    }
}
