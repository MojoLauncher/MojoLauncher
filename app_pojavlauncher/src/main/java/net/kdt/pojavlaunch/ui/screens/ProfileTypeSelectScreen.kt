package net.kdt.pojavlaunch.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
fun ProfileTypeSelectScreen(
    onBack: () -> Unit,
    onVanillaClick: () -> Unit,
    onOptifineClick: () -> Unit,
    onFabricClick: () -> Unit,
    onForgeClick: () -> Unit,
    onQuiltClick: () -> Unit,
    onNeoForgeClick: () -> Unit,
    onLegacyFabricClick: () -> Unit,
    onModpackClick: () -> Unit,
    onBTAClick: () -> Unit
) {
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Create New Profile",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Vanilla Group
                item(span = { GridItemSpan(2) }) {
                    CategoryHeader(title = stringResource(id = R.string.create_profile_vanilla_like_versions))
                }

                item {
                    ProfileTypeButton(
                        text = stringResource(id = R.string.create_instance_vanilla),
                        onClick = onVanillaClick
                    )
                }
                item {
                    ProfileTypeButton(
                        text = stringResource(id = R.string.mod_dl_install_optifine),
                        onClick = onOptifineClick
                    )
                }

                // Modded Group
                item(span = { GridItemSpan(2) }) {
                    CategoryHeader(title = stringResource(id = R.string.create_profile_modded_versions))
                }

                item {
                    ProfileTypeButton(
                        text = stringResource(id = R.string.modloader_dl_install_fabric_instance),
                        onClick = onFabricClick
                    )
                }
                item {
                    ProfileTypeButton(
                        text = stringResource(id = R.string.modloader_dl_install_quilt_instance),
                        onClick = onQuiltClick
                    )
                }
                item {
                    ProfileTypeButton(
                        text = stringResource(id = R.string.modloader_dl_install_legacy_fabric_instance),
                        onClick = onLegacyFabricClick
                    )
                }
                item {
                    ProfileTypeButton(
                        text = stringResource(id = R.string.modloader_dl_install_forge_instance),
                        onClick = onForgeClick
                    )
                }
                item {
                    ProfileTypeButton(
                        text = stringResource(id = R.string.modloader_dl_install_neoforge_instance),
                        onClick = onNeoForgeClick
                    )
                }
                item {
                    ProfileTypeButton(
                        text = stringResource(id = R.string.modpack_install_instance_button),
                        onClick = onModpackClick
                    )
                }
                item {
                    ProfileTypeButton(
                        text = stringResource(id = R.string.create_bta_instance),
                        onClick = onBTAClick
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryHeader(title: String) {
    Column(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    }
}

@Composable
fun ProfileTypeButton(
    text: String,
    onClick: () -> Unit
) {
    // ✅ High emphasis Filled Button with Pill Shape
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview(showBackground = true, device = "spec:width=1280dp,height=800dp,orientation=landscape")
@Composable
fun ProfileTypeSelectScreenPreview() {
    PojavTheme(dynamicColor = true) {
        ProfileTypeSelectScreen(
            onBack = {},
            onVanillaClick = {},
            onOptifineClick = {},
            onFabricClick = {},
            onForgeClick = {},
            onQuiltClick = {},
            onNeoForgeClick = {},
            onLegacyFabricClick = {},
            onModpackClick = {},
            onBTAClick = {}
        )
    }
}
