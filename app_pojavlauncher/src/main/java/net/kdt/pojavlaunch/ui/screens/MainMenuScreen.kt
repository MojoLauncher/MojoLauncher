package net.kdt.pojavlaunch.ui.screens

import android.graphics.drawable.Drawable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.authenticator.accounts.Accounts
import net.kdt.pojavlaunch.instances.Instance
import net.kdt.pojavlaunch.instances.InstanceIconProvider
import net.kdt.pojavlaunch.instances.Instances
import net.kdt.pojavlaunch.ui.theme.PojavTheme

@Composable
fun rememberDrawablePainter(drawable: Drawable?): Painter {
    return remember(drawable) {
        if (drawable == null) {
            object : Painter() {
                override val intrinsicSize: Size get() = Size.Unspecified
                override fun DrawScope.onDraw() {}
            }
        } else {
            object : Painter() {
                override val intrinsicSize: Size
                    get() = Size(
                        drawable.intrinsicWidth.toFloat(),
                        drawable.intrinsicHeight.toFloat()
                    )

                override fun DrawScope.onDraw() {
                    drawIntoCanvas { canvas ->
                        drawable.setBounds(0, 0, size.width.toInt(), size.height.toInt())
                        drawable.draw(canvas.nativeCanvas)
                    }
                }
            }
        }
    }
}

@Composable
fun MainMenuRevamp(
    onEditProfileClick: () -> Unit,
    onCustomControlsClick: () -> Unit,
    onInstallJarClick: () -> Unit,
    onShareLogsClick: () -> Unit,
    onOpenFilesClick: () -> Unit,
    onWikiClick: () -> Unit,
    onSocialMediaClick: () -> Unit,
    onPlayClick: () -> Unit,
    onTerminateClick: () -> Unit,
    onInstanceSelect: () -> Unit
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current

    // ✅ Refresh instance name correctly using side effect
    var selectedInstance by remember {
        mutableStateOf<Instance?>(
            if (isPreview) null
            else try { Instances.loadSelectedInstance() } catch (e: Exception) { null }
        )
    }

    // Refresh every time the screen is composed/entered
    SideEffect {
        if (!isPreview) {
            val instance = try { Instances.loadSelectedInstance() } catch (e: Exception) { null }
            if (selectedInstance != instance) {
                selectedInstance = instance
            }
        }
    }

    val currentAccount = remember {
        mutableStateOf(if (isPreview) null else Accounts.current)
    }

    val instanceIcon = remember(selectedInstance) {
        if (selectedInstance != null)
            InstanceIconProvider.fetchIcon(context.resources, selectedInstance!!)
        else null
    }

    val headInteractionSource = remember { MutableInteractionSource() }
    val isHeadPressed by headInteractionSource.collectIsPressedAsState()
    val headScale by animateFloatAsState(
        targetValue = if (isHeadPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "headScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Column(
                modifier = Modifier
                    .weight(0.66f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionCard(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        title = stringResource(id = R.string.mcl_tab_wiki),
                        icon = Icons.Rounded.Info,
                        onClick = onWikiClick
                    )
                    ActionCard(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        title = stringResource(id = R.string.mcl_button_social_media),
                        icon = Icons.Rounded.Share,
                        onClick = onSocialMediaClick
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                )

                ActionCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    title = stringResource(id = R.string.mcl_option_customcontrol),
                    icon = Icons.Rounded.Build,
                    onClick = onCustomControlsClick
                )
                ActionCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    title = stringResource(id = R.string.main_install_jar_file),
                    icon = Icons.Rounded.Add,
                    onClick = onInstallJarClick
                )
                ActionCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    title = stringResource(id = R.string.main_share_logs),
                    icon = Icons.AutoMirrored.Rounded.Send,
                    onClick = onShareLogsClick
                )
                ActionCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    title = stringResource(id = R.string.mcl_button_open_directory),
                    icon = Icons.Rounded.Search,
                    onClick = onOpenFilesClick
                )
            }

            Surface(
                modifier = Modifier
                    .weight(0.34f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.1f),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                ),
                tonalElevation = 2.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .scale(headScale)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(
                                    interactionSource = headInteractionSource,
                                    indication = null,
                                    onClick = {}
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            val skinHead = currentAccount.value?.skinFacePlain
                            if (skinHead != null) {
                                Image(
                                    bitmap = skinHead.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = currentAccount.value?.username ?: "Steve",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = "Minecraft Account",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 9.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Edit Profile - Low emphasis
                        OutlinedButton(
                            onClick = onEditProfileClick,
                            shape = CircleShape,
                            modifier = Modifier.height(32.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Edit Profile",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // ✅ Correctly display the selected instance name or "No Instance"
                    OutlinedButton(
                        onClick = onInstanceSelect,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (instanceIcon != null) {
                                Image(
                                    painter = rememberDrawablePainter(instanceIcon),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_px_home),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            
                            Column(modifier = Modifier.weight(1f)) {
                                // ✅ Fix: Handle empty/null name by showing "UNNAMED" as requested
                                val name = selectedInstance?.name
                                val instanceDisplayName = if (selectedInstance == null) {
                                    stringResource(id = R.string.no_instance)
                                } else if (name.isNullOrBlank()) {
                                    "UNNAMED"
                                } else {
                                    name
                                }

                                Text(
                                    text = instanceDisplayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = selectedInstance?.versionId ?: stringResource(id = R.string.version_select_hint),
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // High Emphasis Pill Shape PLAY Button - Dynamic Primary
                        Button(
                            onClick = onPlayClick,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = CircleShape,
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(id = R.string.main_play).uppercase(),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        // Terminate button - High emphasis (Filled) Pill Shape
                        Button(
                            onClick = onTerminateClick,
                            modifier = Modifier.size(44.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            contentPadding = PaddingValues(0.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Clear,
                                contentDescription = "Terminate",
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActionCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    // Action buttons - Low emphasis (Outlined)
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        contentPadding = PaddingValues(horizontal = 14.dp),
    )
    {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        )
        {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(showBackground = true, device = "spec:width=1280dp,height=800dp,orientation=landscape")
@Composable
fun MainMenuRevampPreview() {
    PojavTheme(dynamicColor = true) {
        MainMenuRevamp(
            onEditProfileClick = {},
            onCustomControlsClick = {},
            onInstallJarClick = {},
            onShareLogsClick = {},
            onOpenFilesClick = {},
            onWikiClick = {},
            onSocialMediaClick = {},
            onPlayClick = {},
            onTerminateClick = {},
            onInstanceSelect = {}
        )
    }
}
