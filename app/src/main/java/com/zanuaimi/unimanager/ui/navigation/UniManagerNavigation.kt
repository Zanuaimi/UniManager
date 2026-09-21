package com.zanuaimi.unimanager.ui.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
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
import com.zanuaimi.unimanager.ui.theme.UniManagerTheme
import com.zanuaimi.unimanager.viewmodel.AboutViewModel
import com.zanuaimi.unimanager.viewmodel.AppDetailsViewModel
import com.zanuaimi.unimanager.viewmodel.AppDetailsViewModelFactory
import com.zanuaimi.unimanager.viewmodel.AppsViewModel
import com.zanuaimi.unimanager.viewmodel.ManualAppPickerViewModel
import com.zanuaimi.unimanager.viewmodel.SettingsViewModel
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject

private const val APPS = "apps"
private const val BACKUPS = "backups"
private const val SETTINGS = "settings"
private const val ABOUT = "about"

@Composable
fun UniManagerNavigation(navController: NavHostController = rememberNavController()) {
    val currentRoute by navController.currentBackStackEntryAsState()
    val routeName = currentRoute?.destination?.route
    val mainRoute = routeName?.substringBefore('/')
    val showBottomBar = mainRoute in setOf(APPS, BACKUPS, SETTINGS, ABOUT)
    Scaffold(
        containerColor = UniManagerTheme.palette.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = UniManagerTheme.palette.surface) {
                    listOf(
                        Triple(APPS, "Apps", Icons.Default.Apps),
                        Triple(BACKUPS, "Backups", Icons.Default.Build),
                        Triple(SETTINGS, "Settings", Icons.Default.Settings),
                        Triple(ABOUT, "About", Icons.Default.Info),
                    ).forEach { (route, label, icon) ->
                        NavigationBarItem(
                            selected = mainRoute == route,
                            onClick = { navController.navigate(route) { popUpTo(APPS); launchSingleTop = true } },
                            icon = { Icon(icon, label) },
                            label = { Text(label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(navController, startDestination = APPS, modifier = Modifier.padding(padding)) {
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
    val scroll = rememberScrollState()
    LaunchedEffect(refreshing) {
        if (refreshing) { viewModel.refresh(forceScan = true); delay(350); refreshing = false }
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
                            onVerticalDrag = { _, drag -> distance += drag },
                            onDragEnd = { if (distance > 100f && scroll.value == 0) refreshing = true; distance = 0f },
                            onDragCancel = { distance = 0f },
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
            containerColor = UniManagerTheme.palette.accentDark,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
        ) { Text("+", fontSize = 24.sp) }
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
        colors = CardDefaults.cardColors(containerColor = UniManagerTheme.palette.surface),
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            AppIcon(drawable, "${app.label} icon", Modifier.size(54.dp))
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(app.label, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Row(Modifier.fillMaxWidth()) {
                    Text(app.packageName, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text(app.version, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
            IconButton(onClick = { navController.navigate("details/${Uri.encode(app.packageName)}") }) { Icon(Icons.Default.Settings, "Configure ${app.label}") }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Build, "Remove ${app.label}") }
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
        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            AppVisibility.entries.forEach { option -> DropdownMenuItem(text = { Text(option.name) }, onClick = { expanded = false; onChange(option) }) }
        }
    }
}

@Composable
private fun InstalledAppCard(app: InstalledApp, onClick: () -> Unit) {
    val context = LocalContext.current
    val drawable = remember(app.info.packageName) { AppIconRepository(context).load(app.info.packageName) }
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = UniManagerTheme.palette.surface), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
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
    val message by viewModel.message.collectAsStateWithLifecycle()
    var value by remember(settings.cooldownValue) { mutableStateOf(settings.cooldownValue.toString()) }
    var unitMenu by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        ScreenHeader("Settings")
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
                    DropdownMenu(unitMenu, { unitMenu = false }) { listOf("s", "m", "h", "d").forEach { item -> DropdownMenuItem(text = { Text(item) }, onClick = { unitMenu = false; viewModel.update(settings.copy(cooldownValue = value.toLongOrNull() ?: 0, cooldownUnit = item)) }) } }
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
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        ScreenHeader("About")
        Spacer(Modifier.height(12.dp))
        Card(colors = CardDefaults.cardColors(containerColor = UniManagerTheme.palette.surface), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                AppIcon(context.getDrawable(R.mipmap.ic_launcher), "UniManager app logo", Modifier.size(92.dp))
                Text(context.getString(R.string.app_name), fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Text("Version ${BuildConfig.VERSION_NAME}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Zanuaimi/UniManager"))) }, modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) { Text("UniManager repository  GitHub") }
            }
        }
        Spacer(Modifier.height(12.dp))
        Card(colors = CardDefaults.cardColors(containerColor = UniManagerTheme.palette.surface), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text("Updates", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(state.message, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                if (state.release?.apkUrl != null && state.message.startsWith("Update available")) {
                    Button(onClick = { viewModel.download { file -> installDownloadedApk(context, file) } }, enabled = !state.downloading, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) { Text(if (state.downloading) "Downloading..." else "Update now") }
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
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    context.startActivity(Intent(Intent.ACTION_VIEW).apply { setDataAndType(uri, "application/vnd.android.package-archive"); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK) })
}

@Composable
private fun AppDetailsScreen(packageName: String, navController: NavHostController, viewModel: AppDetailsViewModel = viewModel(factory = AppDetailsViewModelFactory(LocalContext.current.applicationContext as android.app.Application, packageName))) {
    val app by viewModel.app.collectAsStateWithLifecycle()
    val storedConfiguration by viewModel.configuration.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var configuration by remember { mutableStateOf<JSONObject?>(null) }
    LaunchedEffect(storedConfiguration) { configuration = JSONObject(storedConfiguration.toString()) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        ScreenHeader(app?.label ?: "App", onBack = { navController.popBackStack() })
        if (app == null) { LoadingOrMessage("Loading configuration..."); return@Column }
        Text(packageName, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        OutlinedTextField(query, { query = it }, label = { Text("Search settings") }, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), singleLine = true)
        val values = configuration ?: JSONObject()
        val keys = values.keys().asSequence().toList().filter { query.isBlank() || it.contains(query, true) }
        if (keys.isEmpty()) NoticeCard("No configurable capabilities", "This app registered successfully, but it did not report manager-editable settings.")
        keys.groupBy { it.substringBefore('.') }.forEach { (group, groupKeys) ->
            ExpandableCard(group.replaceFirstChar(Char::uppercase)) {
                groupKeys.forEach { key -> ConfigurationSetting(key, values, onChange = { values.put(key, it); configuration = JSONObject(values.toString()) }, onEditList = { navController.navigate("strings/${Uri.encode(packageName)}/${Uri.encode(key)}") }) }
            }
            Spacer(Modifier.height(10.dp))
        }
        Button(onClick = { viewModel.save(values) { navController.popBackStack() } }, modifier = Modifier.fillMaxWidth()) { Text("Save configuration") }
    }
}

@Composable
private fun ConfigurationSetting(key: String, configuration: JSONObject, onChange: (Any) -> Unit, onEditList: () -> Unit) {
    val value = configuration.opt(key)
    val label = key.replace('_', ' ').replace('.', ' ').replaceFirstChar(Char::uppercase)
    if (value is Boolean || key == "block_ads" || key == "block_hosts") {
        SettingSwitch(label, "Managed by the patch capability.", configuration.optBoolean(key), { onChange(it) })
    } else if (value is JSONArray || value is String && value.startsWith("[")) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f)); OutlinedButton(onClick = onEditList) { Text("Edit list") }
        }
    } else {
        val isFolder = key.contains("folder", true)
        val isFile = isFolder || key.contains("file", true) || key.contains("image", true) || key.contains("path", true)
        val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { onChange(it.toString()) } }
        val folderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri -> uri?.let { onChange(it.toString()) } }
        Column(Modifier.fillMaxWidth()) {
            if (key.contains("color", true) || configuration.optString(key).startsWith("#")) {
                val previewColor = runCatching { Color(android.graphics.Color.parseColor(configuration.optString(key))) }.getOrDefault(MaterialTheme.colorScheme.surfaceVariant)
                Box(Modifier.size(36.dp).background(previewColor, RoundedCornerShape(6.dp)))
            }
            OutlinedTextField(configuration.optString(key), { onChange(it) }, label = { Text(label) }, modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), singleLine = true)
            if (isFile) {
                OutlinedButton(
                    onClick = { if (isFolder) folderLauncher.launch(null) else fileLauncher.launch(arrayOf("*/*")) },
                    modifier = Modifier.padding(bottom = 6.dp),
                ) { Text(if (isFolder) "Choose folder" else "Choose file") }
            }
        }
    }
}

@Composable
private fun MultiPartStringsScreen(packageName: String, key: String, navController: NavHostController, viewModel: AppDetailsViewModel = viewModel(factory = AppDetailsViewModelFactory(LocalContext.current.applicationContext as android.app.Application, packageName))) {
    val storedConfiguration by viewModel.configuration.collectAsStateWithLifecycle()
    val values = remember(storedConfiguration.toString(), key) { mutableStateOf(decodeStringList(storedConfiguration.opt(key))) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        ScreenHeader(key.replace('_', ' ').replaceFirstChar(Char::uppercase), onBack = { navController.popBackStack() })
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
        Button(onClick = { viewModel.save(JSONObject().put(key, JSONArray(values.value.filter(String::isNotBlank)))) { navController.popBackStack() } }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Save list") }
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
