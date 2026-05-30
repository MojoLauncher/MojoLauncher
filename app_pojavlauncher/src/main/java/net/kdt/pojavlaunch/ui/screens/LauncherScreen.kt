package net.kdt.pojavlaunch.ui.screens

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import com.kdt.mcgui.ProgressLayout
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.BaseActivity
import net.kdt.pojavlaunch.authenticator.AuthType
import net.kdt.pojavlaunch.authenticator.accounts.Accounts
import net.kdt.pojavlaunch.authenticator.accounts.MinecraftAccount
import net.kdt.pojavlaunch.authenticator.listener.LoginListener
import net.kdt.pojavlaunch.extra.ExtraConstants
import net.kdt.pojavlaunch.extra.ExtraCore
import net.kdt.pojavlaunch.extra.ExtraListener
import net.kdt.pojavlaunch.fragments.MainMenuFragment
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper
import net.kdt.pojavlaunch.progresskeeper.ProgressListener
import net.kdt.pojavlaunch.ui.theme.PojavTheme

/** Data class to track task progress in Compose state */
class TaskProgress(
    val key: String,
    initialProgress: Int = 0,
    initialText: String = ""
) {
    var progress by mutableIntStateOf(initialProgress)
    var text by mutableStateOf(initialText)
}

@Composable
fun TaskProgressItem(task: TaskProgress) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = task.text,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${task.progress}%",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { task.progress.toFloat() / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

@Composable
fun ProgressCard(
    modifier: Modifier = Modifier,
    topBarHeight: androidx.compose.ui.unit.Dp
) {
    val isPreview = LocalInspectionMode.current
    val context = LocalContext.current
    
    // Map to keep track of active tasks using Compose state
    val activeTasks = remember { mutableStateOf(mutableStateMapOf<String, TaskProgress>()) }
    var isExpanded by remember { mutableStateOf(true) }

    if (!isPreview) {
        DisposableEffect(Unit) {
            val keys = arrayOf(
                ProgressLayout.UNPACK_RUNTIME, ProgressLayout.DOWNLOAD_MINECRAFT,
                ProgressLayout.DOWNLOAD_VERSION_LIST, ProgressLayout.AUTHENTICATE,
                ProgressLayout.INSTALL_MODPACK, ProgressLayout.EXTRACT_COMPONENTS,
                ProgressLayout.EXTRACT_SINGLE_FILES, ProgressLayout.INSTANCE_INSTALL,
                ProgressLayout.CONTENT_INSTALL
            )
            
            val listeners = keys.map { key ->
                val listener = object : ProgressListener {
                    override fun onProgressStarted() {
                        (context as? FragmentActivity)?.runOnUiThread {
                            activeTasks.value[key] = TaskProgress(key)
                        }
                    }

                    @SuppressLint("LocalContextGetResourceValueCall")
                    override fun onProgressUpdated(progress: Int, resid: Int, vararg va: Any?) {
                        (context as? FragmentActivity)?.runOnUiThread {
                            val task = activeTasks.value.getOrPut(key) { TaskProgress(key) }
                            task.progress = progress
                            task.text = if (resid > 0) context.getString(resid, *va)
                                       else if (va.isNotEmpty() && va[0] != null) va[0].toString()
                                       else ""
                        }
                    }

                    override fun onProgressEnded() {
                        (context as? FragmentActivity)?.runOnUiThread {
                            activeTasks.value.remove(key)
                        }
                    }
                }
                ProgressKeeper.addListener(key, listener)
                key to listener
            }

            onDispose {
                listeners.forEach { (key, listener) ->
                    ProgressKeeper.removeListener(key, listener)
                }
            }
        }
    } else {
        LaunchedEffect(Unit) {
            activeTasks.value["dl"] = TaskProgress("dl", 45, "Downloading Assets...")
            activeTasks.value["auth"] = TaskProgress("auth", 90, "Authenticating...")
        }
    }

    if (activeTasks.value.isEmpty() && !isPreview) return

    ElevatedCard(
        modifier = modifier
            .width(dimensionResource(id = R.dimen._280sdp))
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp).copy(alpha = 0.98f)
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_px_progress),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(id = R.string.progresslayout_tasks_in_progress, activeTasks.value.size),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Icon(
                    painter = painterResource(id = R.drawable.spinner_arrow),
                    contentDescription = null,
                    modifier = Modifier
                        .size(18.dp)
                        .alpha(0.6f),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                        .heightIn(max = 300.dp),
                    contentPadding = PaddingValues(top = 4.dp)
                ) {
                    items(activeTasks.value.values.toList(), key = { it.key }) { task ->
                        TaskProgressItem(task)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSelector(
    modifier: Modifier = Modifier,
    topBarHeight: androidx.compose.ui.unit.Dp,
    onLoginProgressUpdate: (Float) -> Unit,
    onLoginActiveUpdate: (Boolean) -> Unit
) {
    val isPreview = LocalInspectionMode.current
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var accounts by remember { mutableStateOf<List<MinecraftAccount>>(emptyList()) }
    var currentAccount by remember { mutableStateOf<MinecraftAccount?>(if (isPreview) null else Accounts.current) }
    
    var accountToDelete by remember { mutableStateOf<MinecraftAccount?>(null) }
    var maxLoginSteps by remember { mutableIntStateOf(5) }

    val refreshAccountsList = {
        try {
            accounts = Accounts.load().accounts.filterNotNull()
            currentAccount = Accounts.current
        } catch (e: Exception) {
            accounts = emptyList()
        }
    }

    val loginListener = remember {
        object : LoginListener {
            override fun onLoginDone(account: MinecraftAccount?) {
                onLoginProgressUpdate(1f)
                onLoginActiveUpdate(false)
                Accounts.current = account
                refreshAccountsList()
            }

            override fun onLoginError(errorMessage: Throwable?) {
                onLoginProgressUpdate(1f)
                onLoginActiveUpdate(false)
                Toast.makeText(context, errorMessage?.message ?: "Login failed", Toast.LENGTH_LONG).show()
            }

            override fun onLoginProgress(step: Int) {
                onLoginProgressUpdate(step.toFloat() / maxLoginSteps)
                onLoginActiveUpdate(true)
            }

            override fun setMaxLoginProgress(max: Int) {
                maxLoginSteps = max
            }
        }
    }

    val mojangListener = remember {
        object : ExtraListener<Array<String?>?> {
            override fun onValueSet(key: String?, value: Array<String?>?): Boolean {
                if (value != null) {
                    try {
                        val acc = Accounts.upsertByUsername { it.apply {
                            username = value[0] ?: "Steve"
                            authType = AuthType.LOCAL
                        }}
                        loginListener.onLoginDone(acc)
                    } catch(e: Exception) { loginListener.onLoginError(e) }
                }
                return false
            }
        }
    }

    val microsoftListener = remember {
        object : ExtraListener<String?> {
            override fun onValueSet(key: String?, value: String?): Boolean {
                onLoginActiveUpdate(true)
                AuthType.MICROSOFT.createAuth()?.createAccount(loginListener, value)
                return false
            }
        }
    }

    val elyByListener = remember {
        object : ExtraListener<String?> {
            override fun onValueSet(key: String?, value: String?): Boolean {
                onLoginActiveUpdate(true)
                AuthType.ELY_BY.createAuth()?.createAccount(loginListener, value)
                return false
            }
        }
    }

    val refreshListener = remember {
        ExtraListener<Boolean?> { _, _ ->
            refreshAccountsList()
            false
        }
    }

    LaunchedEffect(Unit) {
        if (!isPreview) {
            refreshAccountsList()
            ExtraCore.addExtraListener(ExtraConstants.REFRESH_ACCOUNT_SPINNER, refreshListener)
            ExtraCore.addExtraListener(ExtraConstants.MOJANG_LOGIN_TODO, mojangListener)
            ExtraCore.addExtraListener(ExtraConstants.MICROSOFT_LOGIN_TODO, microsoftListener)
            ExtraCore.addExtraListener(ExtraConstants.ELYBY_LOGIN_TODO, elyByListener)
        }
    }

    if (accountToDelete != null) {
        AlertDialog(
            onDismissRequest = { accountToDelete = null },
            confirmButton = {
                TextButton(onClick = {
                    accountToDelete?.let {
                        Accounts.delete(it)
                        refreshAccountsList()
                    }
                    accountToDelete = null
                }) {
                    Text(stringResource(id = R.string.global_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { accountToDelete = null }) {
                    Text(stringResource(id = android.R.string.cancel))
                }
            },
            title = { Text(stringResource(id = R.string.global_error)) },
            text = { Text(stringResource(id = R.string.warning_remove_account)) }
        )
    }

    Box(modifier = modifier) {
        FilledTonalButton(
            onClick = { expanded = true },
            modifier = Modifier
                .height(topBarHeight - 8.dp) 
                .wrapContentWidth()
                .padding(horizontal = 4.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            val head = currentAccount?.skinFace
            if (head != null) {
                Image(
                    bitmap = head.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.ic_px_home),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
            }
            
            Spacer(Modifier.width(10.dp))
            
            Text(
                text = currentAccount?.username ?: "Select Account",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(Modifier.width(6.dp))
            
            Icon(
                painter = painterResource(id = R.drawable.ic_px_alt_sliders),
                contentDescription = null,
                modifier = Modifier.size(16.dp).alpha(0.5f)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(280.dp) 
                .background(MaterialTheme.colorScheme.surface)
        ) {
            accounts.forEach { account ->
                DropdownMenuItem(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    text = { 
                        Text(
                            account.username, 
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        ) 
                    },
                    onClick = {
                        Accounts.current = account
                        currentAccount = account
                        expanded = false
                        ExtraCore.setValue(ExtraConstants.REFRESH_ACCOUNT_SPINNER, true)
                        if (account.authType.requiresLogin() && System.currentTimeMillis() > account.expiresAt) {
                            onLoginActiveUpdate(true)
                            account.authType.createAuth()?.refreshAccount(loginListener, account)
                        }
                    },
                    leadingIcon = {
                        val head = account.skinFace
                        if (head != null) {
                            Image(
                                bitmap = head.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.size(32.dp)
                            )
                        } else {
                            Icon(painterResource(id = R.drawable.ic_px_home), null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onSurface)
                        }
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val authIcon = account.authType.iconResource
                            if (authIcon != 0) {
                                Icon(painterResource(id = authIcon), null, Modifier.size(20.dp), tint = Color.Unspecified)
                            }
                            Spacer(Modifier.width(12.dp))
                            IconButton(
                                onClick = {
                                    accountToDelete = account
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_px_trash), 
                                    contentDescription = null, 
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                )
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            
            DropdownMenuItem(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                text = { Text("Add Account", color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp) },
                onClick = {
                    expanded = false
                    ExtraCore.setValue(ExtraConstants.SELECT_AUTH_METHOD, true)
                },
                leadingIcon = { 
                    Icon(
                        painter = painterResource(id = R.drawable.ic_add), 
                        contentDescription = null, 
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    ) 
                }
            )
        }
    }
}

@Composable
fun TopBarButton(
    onClick: () -> Unit,
    icon: Int,
    label: String,
    topBarHeight: androidx.compose.ui.unit.Dp,
    isSelected: Boolean = false,
    isSpecialActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    val defaultContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val activeColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
    
    val finalContainerColor = if (isSelected || isSpecialActive) activeColor else defaultContainerColor

    FilledTonalButton(
        onClick = onClick,
        modifier = modifier
            .height(topBarHeight - 16.dp)
            .padding(horizontal = 4.dp)
            .animateContentSize(), // Smooth extension
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = finalContainerColor,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = label,
            modifier = Modifier.size(18.dp)
        )
        
        // ✅ Improved label animation to avoid cutoff
        AnimatedVisibility(
            visible = isSelected,
            enter = fadeIn() + expandHorizontally(),
            exit = fadeOut() + shrinkHorizontally()
        ) {
            Row {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = label, 
                    fontSize = 13.sp, 
                    fontWeight = FontWeight.Bold, 
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.wrapContentWidth(unbounded = true)
                )
            }
        }
    }
}

@Composable
fun LauncherScreen(
    onHomeClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onInstallerClick: () -> Unit,
    onFilesClick: () -> Unit,
    onProgressClick: () -> Unit,
    fragmentManager: androidx.fragment.app.FragmentManager?,
    isProgressVisible: Boolean,
    isTaskRunning: Boolean
) {
    val isPreview = LocalInspectionMode.current
    val backgroundBitmap = if (isPreview) null else BaseActivity.getBackgroundBitmap()
    val topBarHeight = dimensionResource(id = R.dimen._50sdp)
    
    val hasBackground = backgroundBitmap != null

    // State for login progress shared with AccountSelector
    var loginProgress by remember { mutableFloatStateOf(1f) }
    var isLoggingIn by remember { mutableStateOf(false) }
    
    // Track selected category to support "clicking active button makes it not extended"
    var selectedCategory by remember { mutableIntStateOf(-1) }

    // Sync selectedCategory with external visibility states
    LaunchedEffect(isProgressVisible) {
        if (!isProgressVisible && selectedCategory == 0) selectedCategory = -1
    }

    // Determine if any screen is open to replace account selector with close button
    val isAnyScreenOpen = selectedCategory != -1 || isProgressVisible

    Box(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
    ) {
        if (backgroundBitmap != null) {
            Image(
                bitmap = backgroundBitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Box(modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = if (hasBackground) 0.4f else 0f))
        )

        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(topBarHeight)
                    // Make top bar semi-transparent to show background through it
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = if (hasBackground) 0.4f else 1f))
            ) {
                // Apply horizontal display cutout padding to keep UI elements safe from the notch
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Replace account spinner with "Home" button when screen is open with animation
                    Box(modifier = Modifier.padding(start = 8.dp)) {
                        AnimatedContent(
                            targetState = isAnyScreenOpen,
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(220, delayMillis = 90)) + 
                                 slideInHorizontally(initialOffsetX = { -it / 2 }))
                                .togetherWith(fadeOut(animationSpec = tween(90)) + 
                                 slideOutHorizontally(targetOffsetX = { -it / 2 }))
                            },
                            label = "homeSwitch"
                        ) { targetAnyOpen ->
                            if (targetAnyOpen) {
                                TopBarButton(
                                    onClick = {
                                        selectedCategory = -1
                                        if (isProgressVisible) onProgressClick()
                                        onHomeClick() 
                                    },
                                    icon = R.drawable.ic_px_home,
                                    label = "Home",
                                    topBarHeight = topBarHeight,
                                    isSelected = true 
                                )
                            } else {
                                AccountSelector(
                                    topBarHeight = topBarHeight,
                                    onLoginProgressUpdate = { loginProgress = it },
                                    onLoginActiveUpdate = { isLoggingIn = it }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Row(
                        modifier = Modifier.padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TopBarButton(
                            onClick = { 
                                if (selectedCategory != 0) {
                                    selectedCategory = 0
                                    if (!isProgressVisible) onProgressClick()
                                }
                            },
                            isSelected = false,
                            isSpecialActive = isTaskRunning || selectedCategory == 0 || isProgressVisible,
                            icon = R.drawable.ic_px_progress,
                            label = "Tasks",
                            topBarHeight = topBarHeight
                        )

                        TopBarButton(
                            onClick = { 
                                if (selectedCategory != 1) {
                                    if (isProgressVisible) onProgressClick()
                                    selectedCategory = 1
                                    onFilesClick() 
                                }
                            },
                            isSelected = selectedCategory == 1,
                            icon = R.drawable.ic_px_folder,
                            label = "Files",
                            topBarHeight = topBarHeight
                        )

                        TopBarButton(
                            onClick = { 
                                if (selectedCategory != 2) {
                                    if (isProgressVisible) onProgressClick()
                                    selectedCategory = 2
                                    onInstallerClick() 
                                }
                            },
                            isSelected = selectedCategory == 2,
                            icon = R.drawable.ic_px_download,
                            label = "Installer",
                            topBarHeight = topBarHeight
                        )

                        TopBarButton(
                            onClick = { 
                                if (selectedCategory != 3) {
                                    if (isProgressVisible) onProgressClick()
                                    selectedCategory = 3
                                    onSettingsClick() 
                                }
                            },
                            isSelected = selectedCategory == 3,
                            icon = R.drawable.ic_px_alt_sliders,
                            label = "Settings",
                            topBarHeight = topBarHeight
                        )
                    }
                }

                LinearProgressIndicator(
                    progress = { if (isLoggingIn && loginProgress < 1f) loginProgress else 0f },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(1.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent
                )
            }

            AndroidView(
                factory = { ctx ->
                    FragmentContainerView(ctx).apply {
                        id = R.id.container_fragment
                        val activity = ctx as? FragmentActivity
                        val manager = activity?.supportFragmentManager
                        if (manager != null && manager.findFragmentById(R.id.container_fragment) == null) {
                            manager.beginTransaction()
                                .replace(R.id.container_fragment, MainMenuFragment(), MainMenuFragment.TAG)
                                .commit()
                        }
                    }
                },
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        }
        
        if (isProgressVisible) {
            ProgressCard(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = topBarHeight + 12.dp, end = 12.dp)
                    .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)),
                topBarHeight = topBarHeight
            )
        }
    }
}

@Preview(showBackground = true, device = "spec:width=1280dp,height=800dp,orientation=landscape")
@Composable
fun LauncherScreenPreview() {
    PojavTheme(dynamicColor = true) {
        LauncherScreen(
            onHomeClick = {},
            onSettingsClick = {},
            onInstallerClick = {},
            onFilesClick = {},
            onProgressClick = {},
            fragmentManager = null,
            isProgressVisible = true,
            isTaskRunning = true
        )
    }
}
