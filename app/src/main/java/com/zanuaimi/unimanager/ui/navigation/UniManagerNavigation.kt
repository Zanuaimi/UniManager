package com.zanuaimi.unimanager.ui.navigation

import android.content.Context
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Slider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.zanuaimi.unimanager.BuildConfig
import com.zanuaimi.unimanager.R
import com.zanuaimi.unimanager.data.AppRegistry
import com.zanuaimi.unimanager.data.model.AppVisibility
import com.zanuaimi.unimanager.data.model.InstalledApp
import com.zanuaimi.unimanager.data.model.RefreshSettings
import com.zanuaimi.unimanager.data.model.RegisteredApp
import com.zanuaimi.unimanager.data.model.ScreenState
import com.zanuaimi.unimanager.data.repository.AppIconRepository
import com.zanuaimi.unimanager.ui.components.AppIcon
import com.zanuaimi.unimanager.ui.components.ExpandableCard
import com.zanuaimi.unimanager.ui.components.LoadingOrMessage
import com.zanuaimi.unimanager.ui.components.NoticeCard
import com.zanuaimi.unimanager.ui.components.ScreenHeader
import com.zanuaimi.unimanager.viewmodel.AboutViewModel
import com.zanuaimi.unimanager.viewmodel.AppDetailsViewModel
import com.zanuaimi.unimanager.viewmodel.AppDetailsViewModelFactory
import com.zanuaimi.unimanager.viewmodel.AppsViewModel
import com.zanuaimi.unimanager.viewmodel.ManualAppPickerViewModel
import com.zanuaimi.unimanager.viewmodel.SettingsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private const val APPS = "apps"
private const val BACKUPS = "backups"
private const val SETTINGS = "settings"
private const val ABOUT = "about"

private fun routeOrder(route: String?): Int = when {
    route == null || route.startsWith(APPS) -> 0
    route.startsWith(BACKUPS) || route.startsWith("picker") || route.startsWith("details") -> 1
    route.startsWith(SETTINGS) || route.startsWith("strings") -> 2
    route.startsWith(ABOUT) -> 3
    else -> 0
}

@Composable
fun UniManagerNavigation(navController: NavHostController = rememberNavController()) {
    val currentRoute by navController.currentBackStackEntryAsState()
    val routeName = currentRoute?.destination?.route
    val mainRoute = routeName?.substringBefore('/')
    val showBottomBar = mainRoute in setOf(APPS, BACKUPS, SETTINGS, ABOUT)
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                Box(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(30.dp),
                        shadowElevation = 12.dp,
                        tonalElevation = 4.dp,
                    ) {
                        Row(Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    listOf(
                        Triple(APPS, "Apps", Icons.Default.Apps),
                        Triple(BACKUPS, "Backups", Icons.Default.Build),
                        Triple(SETTINGS, "Settings", Icons.Default.Settings),
                        Triple(ABOUT, "About", Icons.Default.Info),
                    ).forEach { (route, label, icon) ->
                        val selected = mainRoute == route
                        Surface(
                            onClick = { navController.navigate(route) { popUpTo(APPS); launchSingleTop = true } },
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                            shape = CircleShape,
                            modifier = Modifier.animateContentSize(),
                        ) {
                            Row(Modifier.padding(horizontal = 13.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(icon, label, Modifier.size(21.dp))
                                AnimatedVisibility(
                                    visible = selected,
                                    enter = expandHorizontally() + fadeIn(),
                                    exit = shrinkHorizontally() + fadeOut(),
                                ) {
                                    Text(label, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 6.dp))
                                }
                            }
                        }
                    }
                        }
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController,
            startDestination = APPS,
            modifier = Modifier.padding(padding).fillMaxSize(),
            enterTransition = {
                val movingForward = routeOrder(initialState.destination.route) < routeOrder(targetState.destination.route)
                if (movingForward) slideInHorizontally(initialOffsetX = { -it })
                else slideInHorizontally(initialOffsetX = { it })
            },
            exitTransition = {
                val movingForward = routeOrder(initialState.destination.route) < routeOrder(targetState.destination.route)
                if (movingForward) slideOutHorizontally(targetOffsetX = { it })
                else slideOutHorizontally(targetOffsetX = { -it })
            },
        ) {
            composable(APPS) { AppsScreen(navController) }
            composable(BACKUPS) { PlaceholderScreen("Backups", "Coming Soon") }
            composable(SETTINGS) { SettingsScreen() }
            composable(ABOUT) { AboutScreen() }
            composable("picker") { ManualAppPickerScreen(navController) }
            composable("no-integration") { NoIntegrationScreen { navController.popBackStack(APPS, false) } }
            composable("details/{packageName}", arguments = listOf(navArgument("packageName") { type = NavType.StringType })) { entry ->
                AppDetailsScreen(Uri.decode(entry.arguments?.getString("packageName").orEmpty()), navController)
            }
            composable("strings/{packageName}/{key}", arguments = listOf(
                navArgument("packageName") { type = NavType.StringType },
                navArgument("key") { type = NavType.StringType },
            )) { entry ->
                MultiPartStringsScreen(
                    Uri.decode(entry.arguments?.getString("packageName").orEmpty()),
                    Uri.decode(entry.arguments?.getString("key").orEmpty()),
                    navController,
                )
            }
        }
    }
}

@Composable
private fun AppsScreen(navController: NavHostController, viewModel: AppsViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refreshSettings by viewModel.refreshSettings.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    var deleteTarget by remember { mutableStateOf<RegisteredApp?>(null) }
    var refreshing by remember { mutableStateOf(false) }
    var pullDistance by remember { mutableStateOf(0f) }
    val scroll = rememberScrollState()
    LaunchedEffect(refreshing) {
        if (refreshing) {
            viewModel.refresh(forceScan = true)
            delay(350)
            refreshing = false
            pullDistance = 0f
        }
    }
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.reloadSettings()
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    Box(Modifier.fillMaxSize()) {
        val listModifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 18.dp)
            .verticalScroll(scroll)
            .then(
                if (refreshSettings.pullToRefresh) {
                    Modifier.pointerInput(Unit) {
                        var distance = 0f
                        detectVerticalDragGestures(
                            onVerticalDrag = { _, drag ->
                                if (scroll.value == 0 && drag > 0f) {
                                    distance += drag
                                    pullDistance = distance.coerceIn(0f, 140f)
                                }
                            },
                            onDragEnd = { if (distance > 100f && scroll.value == 0) refreshing = true; distance = 0f; if (!refreshing) pullDistance = 0f },
                            onDragCancel = { distance = 0f; pullDistance = 0f },
                        )
                    }
                } else {
                    Modifier
                },
            )
        Column(
            listModifier,
        ) {
            ScreenHeader("UniManager")
            Text("Your patched apps, settings, and runtime profiles", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            Spacer(Modifier.height(16.dp))
            when (val current = state) {
                ScreenState.Loading -> LoadingOrMessage("Loading apps...")
                is ScreenState.Empty -> NoticeCard(current.message, "Patch an app with a UniPatches patch that has UniManager integration enabled. It will appear here automatically.")
                is ScreenState.Error -> NoticeCard("Unable to load apps", current.message)
                is ScreenState.Success -> current.data.forEach { app -> RegisteredAppCard(app, navController) { deleteTarget = app } }
            }
            Spacer(Modifier.height(80.dp))
        }
        FloatingActionButton(
            onClick = { navController.navigate("picker") },
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
        ) { Text("+", fontSize = 24.sp) }
        if (pullDistance > 0f || refreshing) {
            Box(Modifier.align(Alignment.TopCenter).padding(top = 10.dp)) {
                if (refreshing) {
                    CircularProgressIndicator(Modifier.size(34.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 3.dp)
                } else {
                    Box(
                        Modifier.size(34.dp)
                            .alpha((pullDistance / 100f).coerceIn(0f, 1f))
                            .background(MaterialTheme.colorScheme.primaryContainer, androidx.compose.foundation.shape.CircleShape),
                    )
                }
            }
        }
    }
    deleteTarget?.let { app ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Remove app entry?") },
            text = { Text("This removes the UniManager profile for ${app.label}. It does not uninstall or undo the patch.") },
            confirmButton = { TextButton(onClick = { viewModel.remove(app.packageName); deleteTarget = null }) { Text("Remove") } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun RegisteredAppCard(app: RegisteredApp, navController: NavHostController, onDelete: () -> Unit) {
    val context = LocalContext.current
    val drawable = remember(app.packageName) { AppIconRepository(context).load(app.packageName) }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            AppIcon(drawable, "${app.label} icon", Modifier.size(54.dp))
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(app.label, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(app.packageName, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Text("v${app.version.ifBlank { "unknown" }}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                if (app.statusLabel != "Registered") {
                    Text(app.statusLabel, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            IconButton(
                enabled = app.isEditable,
                onClick = { navController.navigate("details/${Uri.encode(app.packageName)}") },
            ) { Icon(Icons.Default.Settings, "Configure ${app.label}") }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Remove ${app.label}") }
        }
    }
}

@Composable
private fun ManualAppPickerScreen(navController: NavHostController, viewModel: ManualAppPickerViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var searchVisible by rememberSaveable { mutableStateOf(false) }
    state.registrationMessage?.let { message ->
        NoIntegrationScreen(
            message = message,
            buttonLabel = "Back to installed apps",
            onHome = viewModel::dismissRegistrationMessage,
        )
        return
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to Apps") }
            Text("Installed Apps", fontWeight = FontWeight.Bold, fontSize = 21.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = { searchVisible = !searchVisible }) { Icon(Icons.Default.Search, "Search installed apps") }
            FilterButton(state.visibility) { viewModel.setVisibility(it) }
        }
        androidx.compose.animation.AnimatedVisibility(searchVisible) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                label = { Text("Search app name or package") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            )
        }
        when (val current = state.state) {
            ScreenState.Loading -> LoadingOrMessage("Scanning installed apps...")
            is ScreenState.Empty -> NoticeCard("No matching installed apps", "Try another search or change the visibility filter.")
            is ScreenState.Error -> NoticeCard("Unable to scan installed apps", current.message)
            is ScreenState.Success -> Column(Modifier.verticalScroll(rememberScrollState())) { current.data.forEach { app -> InstalledAppCard(app) { viewModel.register(app) { navController.popBackStack() } } } }
        }
    }
}

@Composable
private fun FilterButton(current: AppVisibility, onChange: (AppVisibility) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) { Text(current.name.lowercase().replaceFirstChar(Char::uppercase)) }
        ThemedDropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            AppVisibility.entries.forEachIndexed { index, option ->
                if (index > 0) ThemedDropdownDivider()
                DropdownMenuItem(text = { Text(option.name.lowercase().replaceFirstChar(Char::uppercase)) }, onClick = { expanded = false; onChange(option) })
            }
        }
    }
}

@Composable
private fun ThemedDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)),
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
        content = content,
    )
}

@Composable
private fun ThemedDropdownDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), thickness = 1.dp)
}

@Composable
private fun InstalledAppCard(app: InstalledApp, onClick: () -> Unit) {
    val context = LocalContext.current
    val drawable = remember(app.info.packageName) { AppIconRepository(context).load(app.info.packageName) }
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            AppIcon(drawable, "${app.label} icon", Modifier.size(56.dp))
            Column(Modifier.padding(start = 14.dp)) {
                Text(app.label, fontWeight = FontWeight.Bold)
                Text(app.info.packageName, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Text("v${app.version.ifBlank { "unknown" }}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val settings by viewModel.state.collectAsStateWithLifecycle()
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()
    val dynamicColorAvailable by viewModel.dynamicColorAvailable.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    var value by remember(settings.cooldownValue) { mutableStateOf(settings.cooldownValue.toString()) }
    var unitMenu by remember { mutableStateOf(false) }
    var colorSetMenu by remember { mutableStateOf(false) }
    var accentMenu by remember { mutableStateOf(false) }
    var customAccent by remember(appearance.customAccent) { mutableStateOf(appearance.customAccent) }
    val accentChoices = listOf("Red" to "#FF5656", "Blue" to "#5599FF", "Yellow" to "#FFCC33", "Green" to "#55CC88", "Black" to "#111111", "Aqua" to "#33CCCC")
    val colorSetChoices = buildList {
        add("unipatches" to "UniPatches colorset")
        if (dynamicColorAvailable) add("dynamic" to "Dynamic Color")
        add("custom" to "Custom Accent Color")
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        ScreenHeader("Settings")
        Spacer(Modifier.height(12.dp))
        ExpandableCard("Appearance") {
            Text("Color set", fontWeight = FontWeight.Bold)
            Box {
                OutlinedButton(onClick = { colorSetMenu = true }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                    Text(when (appearance.colorSet) { "dynamic" -> "Dynamic Color"; "custom" -> "Custom Accent Color"; else -> "UniPatches colorset" })
                }
                ThemedDropdownMenu(expanded = colorSetMenu, onDismissRequest = { colorSetMenu = false }) {
                    colorSetChoices.forEachIndexed { index, (key, label) ->
                        if (index > 0) ThemedDropdownDivider()
                        DropdownMenuItem(text = { Text(label) }, onClick = { colorSetMenu = false; viewModel.updateAppearance(appearance.copy(colorSet = key)) })
                    }
                }
            }
            if (appearance.colorSet == "custom") {
                Text("Custom accent color", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
                Box {
                    OutlinedButton(onClick = { accentMenu = true }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                        Text(accentChoices.firstOrNull { it.second.equals(customAccent, true) }?.first ?: "Custom color")
                    }
                    ThemedDropdownMenu(expanded = accentMenu, onDismissRequest = { accentMenu = false }) {
                        accentChoices.forEachIndexed { index, (label, color) ->
                            if (index > 0) ThemedDropdownDivider()
                            DropdownMenuItem(text = { Text(label) }, onClick = {
                                accentMenu = false
                                customAccent = color
                                viewModel.updateAppearance(appearance.copy(colorSet = "custom", customAccent = color))
                            })
                        }
                        ThemedDropdownDivider()
                        DropdownMenuItem(text = { Text("Custom color editor") }, onClick = { accentMenu = false })
                    }
                }
                OutlinedTextField(customAccent, { customAccent = it }, label = { Text("#RRGGBB") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), singleLine = true)
                Button(onClick = { viewModel.updateAppearance(appearance.copy(customAccent = customAccent)) }, modifier = Modifier.padding(top = 8.dp)) { Text("Set color") }
            } else {
                Text("Custom accent choices apply only when Custom Accent Color is selected.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        ExpandableCard("Refresh feature") {
            SettingSwitch("Enable Refresh upon pulling down from top of list", "Allow a manual pull-down gesture to scan for newly registered or updated patched apps.", settings.pullToRefresh) { viewModel.update(settings.copy(pullToRefresh = it)) }
            SettingSwitch("Enable automatic refreshing", "Automatically scan installed apps when UniManager opens or returns to the foreground.", settings.automaticRefresh) { viewModel.update(settings.copy(automaticRefresh = it)) }
            Text("Automatic refresh cooldown", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
            Text("Wait this long between automatic scans. Minimum 30 seconds.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value, { value = it.filter(Char::isDigit) }, label = { Text("Value") }, modifier = Modifier.weight(1f), singleLine = true)
                Spacer(Modifier.size(8.dp))
                Box {
                    OutlinedButton(onClick = { unitMenu = true }) { Text(settings.cooldownUnit) }
                    ThemedDropdownMenu(unitMenu, { unitMenu = false }) {
                        listOf("s", "m", "h", "d").forEachIndexed { index, item ->
                            if (index > 0) ThemedDropdownDivider()
                            DropdownMenuItem(text = { Text(item) }, onClick = { unitMenu = false; viewModel.update(settings.copy(cooldownValue = value.toLongOrNull() ?: 0, cooldownUnit = item)) })
                        }
                    }
                }
            }
            Button(onClick = { viewModel.update(settings.copy(cooldownValue = value.toLongOrNull() ?: 0)) }, modifier = Modifier.padding(top = 8.dp)) { Text("Save refresh settings") }
            message?.let { Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 6.dp)) }
        }
    }
}

@Composable
private fun SettingSwitch(title: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title); Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) }
        Switch(checked, onCheckedChange)
    }
}

@Composable
private fun AboutScreen(viewModel: AboutViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val updateAvailable = state.updateAvailable && state.release?.apkUrl?.isNotBlank() == true
    LaunchedEffect(state.timeoutEvent) {
        if (state.timeoutEvent != 0L) {
            Toast.makeText(context, "Update check timed out. Please try again.", Toast.LENGTH_LONG).show()
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        ScreenHeader("About")
        Spacer(Modifier.height(12.dp))
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                AppIcon(context.getDrawable(R.drawable.unipatches_icon_inset), "UniManager app logo", Modifier.size(92.dp))
                Text(context.getString(R.string.app_name), fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Text("Version ${BuildConfig.VERSION_NAME}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("by Zanuaimi", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                Button(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Zanuaimi/UniManager"))) }, modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) { Text("UniManager GitHub Repository") }
            }
        }
        Spacer(Modifier.height(12.dp))
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Updates", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    state.message,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                )
                Button(
                    onClick = {
                        if (updateAvailable) viewModel.download { file -> installDownloadedApk(context, file) }
                        else viewModel.checkForUpdates()
                    },
                    enabled = !state.checking && !state.downloading,
                    modifier = Modifier.animateContentSize().padding(top = 12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (state.checking || state.downloading) {
                            CircularProgressIndicator(Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.SystemUpdateAlt, contentDescription = null)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            when {
                                state.checking -> "Checking for updates..."
                                state.downloading -> "Downloading update..."
                                updateAvailable -> "Download and install update"
                                else -> "Check for updates"
                            },
                        )
                    }
                }
            }
        }
    }
}

private fun installDownloadedApk(context: Context, file: java.io.File) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
        context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")))
        return
    }
    // UpdateFileProvider serves only files from cacheDir/updates. Build its URI directly
    // instead of calling AndroidX FileProvider, which requires FILE_PROVIDER_PATHS metadata
    // that the custom provider intentionally does not use.
    val uri = Uri.Builder()
        .scheme("content")
        .authority("${context.packageName}.fileprovider")
        .appendPath(file.name)
        .build()
    val installIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        clipData = ClipData.newRawUri("UniManager update", uri)
        putExtra(Intent.EXTRA_TITLE, file.name)
    }
    try {
        context.startActivity(installIntent)
    } catch (_: android.content.ActivityNotFoundException) {
        // Preserve compatibility with third-party installers that only expose
        // a generic APK viewer instead of ACTION_INSTALL_PACKAGE.
        context.startActivity(installIntent.apply { action = Intent.ACTION_VIEW })
    }
}

@Composable
private fun AppDetailsScreen(packageName: String, navController: NavHostController, viewModel: AppDetailsViewModel = viewModel(factory = AppDetailsViewModelFactory(LocalContext.current.applicationContext as android.app.Application, packageName))) {
    val app by viewModel.app.collectAsStateWithLifecycle()
    val storedConfiguration by viewModel.configuration.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var saveMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmation by rememberSaveable { mutableStateOf<String?>(null) }
    var isSaving by rememberSaveable { mutableStateOf(false) }
    var configuration by remember { mutableStateOf<JSONObject?>(null) }
    var originalConfiguration by remember { mutableStateOf<JSONObject?>(null) }
    var presetDrafts by remember(packageName) { mutableStateOf<Map<String, String>>(emptyMap()) }
    val navigationScope = rememberCoroutineScope()
    LaunchedEffect(storedConfiguration.toString(), app?.raw?.toString()) {
        val raw = app?.raw ?: return@LaunchedEffect
        if (storedConfiguration.length() == 0) return@LaunchedEffect
        val stored = JSONObject(storedConfiguration.toString())
        val selectedPreset = stored.optString("runtimeOverlaySelectedPreset", "custom")
        val initial = JSONObject(stored.toString())
        if (selectedPreset != "custom") {
            ConfigurationKeyLabels.presetConfiguration(raw, selectedPreset)?.let { preset ->
                overlayPresetConfiguration(initial, preset)
            }
        }
        if (configuration == null) {
            originalConfiguration = JSONObject(initial.toString())
            configuration = initial
        }
    }
    val values = configuration ?: JSONObject()
    val original = originalConfiguration
    val hasChanges = original != null && values.toString() != original.toString()

    fun discardChanges(goBack: Boolean) {
        original?.let { configuration = JSONObject(it.toString()) }
        presetDrafts = emptyMap()
        saveMessage = null
        if (goBack) {
            navigationScope.launch {
                delay(220)
                navController.popBackStack()
            }
        }
    }

    BackHandler {
        if (hasChanges) confirmation = "discard-back" else navController.popBackStack()
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            ScreenHeader(app?.label ?: "App", onBack = {
                if (hasChanges) confirmation = "discard-back" else navController.popBackStack()
            })
            if (app == null) { LoadingOrMessage("Loading configuration..."); return@Column }
            Text(packageName, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            if (!app!!.isEditable) {
                NoticeCard(app!!.statusLabel, app!!.statusDetail)
            }
            OutlinedTextField(query, { query = it }, label = { Text("Search settings") }, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), singleLine = true)
            Box(Modifier.weight(1f).fillMaxWidth()) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    val keys = values.keys().asSequence().toList().filter { key ->
                        if (key == "runtimeOverlaySelectedPresetVersion") {
                            false
                        } else {
                            val label = ConfigurationKeyLabels.label(app?.raw, key)
                            query.isBlank() || key.contains(query, true) || label.contains(query, true)
                        }
                    }.sortedWith(compareBy { if (it == "runtimeOverlaySelectedPreset") 0 else 1 })
                    if (keys.isEmpty()) NoticeCard("No configurable capabilities", "This app registered successfully, but it did not report manager-editable settings.")
                    buildConfigurationTree(app?.raw, keys).forEach { group ->
                        ConfigurationGroupContent(
                            group = group,
                            values = values,
                            app = app?.raw,
                            packageName = packageName,
                            navController = navController,
                            onChange = { key, value ->
                                if (key == "runtimeOverlaySelectedPreset") {
                                    val previousId = values.optString(key, "custom")
                                    val nextId = value.toString()
                                    val updatedDrafts = presetDrafts.toMutableMap().apply {
                                        put(previousId, values.toString())
                                    }
                                    val next = updatedDrafts[nextId]?.let(::JSONObject) ?: JSONObject(values.toString())
                                    if (nextId != "custom") {
                                        ConfigurationKeyLabels.presetConfiguration(app?.raw, nextId)?.let { preset ->
                                            overlayPresetConfiguration(next, preset)
                                        }
                                    }
                                    next.put(key, nextId)
                                    ConfigurationKeyLabels.presetVersion(app?.raw, nextId)?.let {
                                        next.put("runtimeOverlaySelectedPresetVersion", it)
                                    }
                                    presetDrafts = updatedDrafts
                                    configuration = next
                                } else {
                                    values.put(key, value)
                                    configuration = JSONObject(values.toString())
                                }
                            },
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                    saveMessage?.let { NoticeCard("Unable to save configuration", it) }
                    Spacer(Modifier.height(96.dp))
                }
            }
        }

        AnimatedVisibility(
            visible = hasChanges && app?.isEditable == true,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .imePadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        enabled = !isSaving,
                        onClick = { confirmation = "discard-stay" },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Discard Changes")
                    }
                    Button(
                        enabled = !isSaving,
                        onClick = { confirmation = "save" },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Save Changes")
                    }
                }
            }
        }
    }

    when (confirmation) {
        "discard-stay", "discard-back" -> AlertDialog(
            onDismissRequest = { confirmation = null },
            title = { Text("Discard Changes?") },
            text = { Text("Your changes will be removed and the last saved configuration will be restored. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    val goBack = confirmation == "discard-back"
                    confirmation = null
                    discardChanges(goBack)
                }) { Text("Yes, discard") }
            },
            dismissButton = { TextButton(onClick = { confirmation = null }) { Text("Keep editing") } },
        )
        "save" -> AlertDialog(
            onDismissRequest = { confirmation = null },
            title = { Text("Save Changes?") },
            text = { Text("These settings will be saved to UniManager and used by the patched app the next time it starts. The current app session will not be changed.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmation = null
                    saveMessage = null
                    isSaving = true
                    val snapshot = JSONObject(values.toString())
                    viewModel.save(snapshot) { saved ->
                        isSaving = false
                        if (saved) {
                            originalConfiguration = JSONObject(snapshot.toString())
                            presetDrafts = emptyMap()
                            navigationScope.launch {
                                delay(220)
                                navController.popBackStack()
                            }
                        } else {
                            saveMessage = "The app registry rejected this update. Reopen the app details and try again."
                        }
                    }
                }, enabled = !isSaving) { Text(if (isSaving) "Saving..." else "Yes, save") }
            },
            dismissButton = { TextButton(onClick = { confirmation = null }) { Text("Keep editing") } },
        )
    }
}

private fun overlayPresetConfiguration(target: JSONObject, preset: JSONObject) {
    val keys = preset.keys()
    while (keys.hasNext()) {
        val key = keys.next()
        target.put(key, preset.get(key))
    }
}

private class ConfigurationGroup(val title: String) {
    val settings = mutableListOf<String>()
    val children = linkedMapOf<String, ConfigurationGroup>()
}

private fun buildConfigurationTree(app: JSONObject?, keys: List<String>): List<ConfigurationGroup> {
    val roots = linkedMapOf<String, ConfigurationGroup>()
    keys.forEach { key ->
        var children = roots
        var current: ConfigurationGroup? = null
        ConfigurationKeyLabels.hierarchy(app, key).forEach { segment ->
            val group = children.getOrPut(segment) { ConfigurationGroup(segment) }
            current = group
            children = group.children
        }
        current?.settings?.add(key)
    }
    return roots.values.toList()
}

@Composable
private fun ConfigurationGroupContent(
    group: ConfigurationGroup,
    values: JSONObject,
    app: JSONObject?,
    packageName: String,
    navController: NavHostController,
    onChange: (String, Any) -> Unit,
) {
    ExpandableCard(group.title) {
        group.settings.forEachIndexed { index, key ->
            ConfigurationSetting(
                key,
                ConfigurationKeyLabels.leafLabel(app, key),
                values,
                app,
                onChange = { onChange(key, it) },
                onEditList = { navController.navigate("strings/${Uri.encode(packageName)}/${Uri.encode(key)}") },
            )
            if (index < group.settings.lastIndex) Spacer(Modifier.height(14.dp))
        }
        group.children.values.forEach { child ->
            if (group.settings.isNotEmpty()) Spacer(Modifier.height(10.dp))
            ConfigurationGroupContent(child, values, app, packageName, navController, onChange)
        }
    }
}

@Composable
private fun ConfigurationSetting(key: String, label: String, configuration: JSONObject, app: JSONObject?, onChange: (Any) -> Unit, onEditList: () -> Unit) {
    val value = configuration.opt(key)
    var showColorEditor by rememberSaveable(key) { mutableStateOf(false) }
    val choices = if (key == "runtimeOverlaySelectedPreset") {
        ConfigurationKeyLabels.presetChoices(app)
    } else {
        ConfigurationKeyLabels.choices(app, key)
    }
    val descriptorType = ConfigurationKeyLabels.type(app, key)
    if (value is Boolean || descriptorType == "boolean" || key == "block_ads" || key == "block_hosts") {
        SettingSwitch(label, "Managed by the patch capability.", configuration.optBoolean(key), { onChange(it) })
    } else if (value is JSONArray || descriptorType == "list" || value is String && value.startsWith("[")) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f)); OutlinedButton(onClick = onEditList) { Text("Edit list") }
        }
    } else if (choices != null) {
        var expanded by rememberSaveable(key) { mutableStateOf(false) }
        val current = configuration.optString(key)
        val selected = choices.firstOrNull { it.value == current }
        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Box(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(selected?.label ?: current.ifBlank { "Choose an option" }, modifier = Modifier.weight(1f))
                    Text("▾")
                }
                ThemedDropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                    choices.forEachIndexed { index, choice ->
                        if (index > 0) ThemedDropdownDivider()
                        DropdownMenuItem(
                            text = { Text(choice.label) },
                            onClick = { expanded = false; onChange(choice.value) },
                        )
                    }
                }
            }
        }
    } else {
        val isFolder = key.contains("folder", true)
        val isFile = isFolder || key.contains("file", true) || key.contains("image", true) || key.contains("path", true)
        val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { onChange(it.toString()) } }
        val folderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri -> uri?.let { onChange(it.toString()) } }
        Column(Modifier.fillMaxWidth()) {
            if (key.contains("color", true) || configuration.optString(key).startsWith("#")) {
                val previewColor = runCatching { Color(android.graphics.Color.parseColor(configuration.optString(key))) }.getOrDefault(MaterialTheme.colorScheme.surfaceVariant)
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).background(previewColor, RoundedCornerShape(8.dp)))
                    OutlinedButton(onClick = { showColorEditor = true }, modifier = Modifier.padding(start = 10.dp)) { Text("Edit color") }
                }
                Spacer(Modifier.height(8.dp))
            }
            OutlinedTextField(
                configuration.optString(key),
                { onChange(coerceEditedValue(value, descriptorType, it)) },
                label = { Text(label) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                singleLine = true,
            )
            if (isFile) {
                OutlinedButton(
                    onClick = { if (isFolder) folderLauncher.launch(null) else fileLauncher.launch(arrayOf("*/*")) },
                    modifier = Modifier.padding(top = 2.dp, bottom = 4.dp),
                ) { Text(if (isFolder) "Choose folder" else "Choose file") }
            }
        }
    }
    if (showColorEditor) {
        ColorEditorDialog(
            initialValue = configuration.optString(key),
            onDismiss = { showColorEditor = false },
            onApply = { color -> onChange(color); showColorEditor = false },
        )
    }
}

private fun coerceEditedValue(original: Any?, descriptorType: String?, input: String): Any = when {
    descriptorType == "integer" -> input.toIntOrNull() ?: original ?: input
    descriptorType == "long" -> input.toLongOrNull() ?: original ?: input
    descriptorType == "float" -> input.toFloatOrNull() ?: original ?: input
    descriptorType == "double" || descriptorType == "number" -> input.toDoubleOrNull() ?: original ?: input
    original is Int -> input.toIntOrNull() ?: original
    original is Long -> input.toLongOrNull() ?: original
    original is Float -> input.toFloatOrNull() ?: original
    original is Double -> input.toDoubleOrNull() ?: original
    else -> input
}

private data class RgbColor(val red: Int, val green: Int, val blue: Int) {
    fun hex(): String = "#%02X%02X%02X".format(red, green, blue)
}

private fun parseRgb(value: String): RgbColor {
    val color = runCatching { android.graphics.Color.parseColor(value) }.getOrDefault(android.graphics.Color.GRAY)
    return RgbColor(android.graphics.Color.red(color), android.graphics.Color.green(color), android.graphics.Color.blue(color))
}

@Composable
private fun ColorEditorDialog(initialValue: String, onDismiss: () -> Unit, onApply: (String) -> Unit) {
    var rgb by remember(initialValue) { mutableStateOf(parseRgb(initialValue)) }
    var hex by remember(initialValue) { mutableStateOf(rgb.hex()) }

    fun update(next: RgbColor) {
        rgb = next
        hex = next.hex()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Color editor") },
        text = {
            Column {
                Box(Modifier.fillMaxWidth().height(52.dp).background(Color(rgb.red, rgb.green, rgb.blue), RoundedCornerShape(10.dp)))
                OutlinedTextField(
                    value = hex,
                    onValueChange = { candidate ->
                        hex = candidate
                        runCatching { parseRgb(candidate) }.onSuccess { parsed -> if (candidate.matches(Regex("#[0-9A-Fa-f]{6}"))) rgb = parsed }
                    },
                    label = { Text("Hex color") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
                RgbChannel("Red", rgb.red, { update(rgb.copy(red = it)) })
                RgbChannel("Green", rgb.green, { update(rgb.copy(green = it)) })
                RgbChannel("Blue", rgb.blue, { update(rgb.copy(blue = it)) })
            }
        },
        confirmButton = { Button(onClick = { onApply(rgb.hex()) }) { Text("Apply") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun RgbChannel(name: String, value: Int, onValueChange: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(name, modifier = Modifier.weight(1f))
            OutlinedTextField(
                value = value.toString(),
                onValueChange = { input -> input.toIntOrNull()?.let { onValueChange(it.coerceIn(0, 255)) } },
                singleLine = true,
                modifier = Modifier.width(78.dp),
            )
        }
        Slider(value = value.toFloat(), onValueChange = { onValueChange(it.toInt()) }, valueRange = 0f..255f, steps = 254)
    }
}

@Composable
private fun MultiPartStringsScreen(packageName: String, key: String, navController: NavHostController, viewModel: AppDetailsViewModel = viewModel(factory = AppDetailsViewModelFactory(LocalContext.current.applicationContext as android.app.Application, packageName))) {
    val app by viewModel.app.collectAsStateWithLifecycle()
    val storedConfiguration by viewModel.configuration.collectAsStateWithLifecycle()
    val values = remember(storedConfiguration.toString(), key) { mutableStateOf(decodeStringList(storedConfiguration.opt(key))) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        ScreenHeader(ConfigurationKeyLabels.label(app?.raw, key), onBack = { navController.popBackStack() })
        Text("Add, edit, or remove one string per icon part.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 10.dp)) {
            values.value.forEachIndexed { index, item ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(item, { text -> values.value = values.value.toMutableList().also { it[index] = text } }, label = { Text("Part ${index + 1}") }, modifier = Modifier.weight(1f))
                    IconButton(onClick = { values.value = values.value.toMutableList().also { it.removeAt(index) } }) { Icon(Icons.Default.Build, "Remove string") }
                }
            }
        }
        OutlinedButton(onClick = { values.value = values.value + "" }, modifier = Modifier.fillMaxWidth()) { Text("Add string") }
        Button(
            onClick = {
                viewModel.save(JSONObject().put(key, JSONArray(values.value.filter(String::isNotBlank)))) { saved ->
                    if (saved) navController.popBackStack()
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) { Text("Save list") }
    }
}

private fun decodeStringList(value: Any?): List<String> = when (value) {
    is JSONArray -> (0 until value.length()).mapNotNull { value.optString(it).takeIf(String::isNotBlank) }
    is String -> runCatching { decodeStringList(JSONArray(value)) }.getOrElse { value.lines().filter(String::isNotBlank) }
    else -> listOf("")
}

@Composable
private fun PlaceholderScreen(title: String, message: String) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun NoIntegrationScreen(
    title: String = "No UniManager integration",
    message: String = "This installed app does not contain UniManager integration metadata, so no app entry was added.",
    buttonLabel: String = "Home",
    onHome: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 21.sp)
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
        Button(onClick = onHome, modifier = Modifier.padding(top = 18.dp)) { Text(buttonLabel) }
    }
}
