package com.zanuaimi.unimanager.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zanuaimi.unimanager.data.InstalledAppScanner
import com.zanuaimi.unimanager.data.model.AppVisibility
import com.zanuaimi.unimanager.data.model.AppearanceSettings
import com.zanuaimi.unimanager.data.model.InstalledApp
import com.zanuaimi.unimanager.data.model.RefreshSettings
import com.zanuaimi.unimanager.data.model.RegisteredApp
import com.zanuaimi.unimanager.data.model.ReleaseInfo
import com.zanuaimi.unimanager.data.model.ScreenState
import com.zanuaimi.unimanager.data.repository.AppRegistryRepository
import com.zanuaimi.unimanager.data.repository.InstalledAppRepository
import com.zanuaimi.unimanager.data.repository.SettingsRepository
import com.zanuaimi.unimanager.data.repository.UpdateRepository
import com.zanuaimi.unimanager.ui.theme.UniManagerTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject

class MainViewModel : ViewModel()

class AppsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRegistryRepository(application)
    private val settingsRepository = SettingsRepository(application)
    private val _state = MutableStateFlow<ScreenState<List<RegisteredApp>>>(ScreenState.Loading)
    val state: StateFlow<ScreenState<List<RegisteredApp>>> = _state.asStateFlow()
    private val _refreshSettings = MutableStateFlow(RefreshSettings())
    val refreshSettings: StateFlow<RefreshSettings> = _refreshSettings.asStateFlow()

    init {
        reloadSettings()
        refresh()
    }

    fun reloadSettings() {
        viewModelScope.launch {
            _refreshSettings.value = settingsRepository.read()
        }
    }

    fun refresh(forceScan: Boolean = false) {
        viewModelScope.launch {
            _state.value = ScreenState.Loading
            runCatching {
                if (forceScan) {
                    InstalledAppScanner.scan(getApplication())
                } else {
                    InstalledAppScanner.scanIfNeeded(getApplication())
                }
                repository.getAll()
            }
                .onSuccess { apps -> _state.value = if (apps.isEmpty()) ScreenState.Empty("No managed apps yet") else ScreenState.Success(apps) }
                .onFailure { _state.value = ScreenState.Error(it.message ?: "Unable to load apps") }
        }
    }

    fun remove(packageName: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch { onComplete(repository.remove(packageName)); refresh() }
    }
}

data class PickerUiState(
    val visibility: AppVisibility = AppVisibility.USER,
    val query: String = "",
    val state: ScreenState<List<InstalledApp>> = ScreenState.Loading,
    val registrationMessage: String? = null,
)

class ManualAppPickerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = InstalledAppRepository(application)
    private val registry = AppRegistryRepository(application)
    private val _state = MutableStateFlow(PickerUiState())
    val state: StateFlow<PickerUiState> = _state.asStateFlow()
    private var cachedVisibility: AppVisibility? = null
    private var cachedApps: List<InstalledApp> = emptyList()

    init { refresh() }

    fun setVisibility(visibility: AppVisibility) {
        _state.update { it.copy(visibility = visibility) }
        refresh()
    }

    fun setQuery(query: String) {
        _state.update { it.copy(query = query) }
        if (cachedVisibility == _state.value.visibility) {
            publishFiltered(query)
        } else {
            refresh()
        }
    }

    fun refresh() {
        val snapshot = _state.value
        viewModelScope.launch {
            _state.update { it.copy(state = ScreenState.Loading, registrationMessage = null) }
            if (cachedVisibility == snapshot.visibility && cachedApps.isNotEmpty()) {
                publishFiltered(snapshot.query)
                return@launch
            }
            runCatching { repository.list(snapshot.visibility, "") }
                .onSuccess { apps ->
                    cachedVisibility = snapshot.visibility
                    cachedApps = apps
                    publishFiltered(snapshot.query)
                }
                .onFailure { error -> _state.update { it.copy(state = ScreenState.Error(error.message ?: "Unable to scan installed apps")) } }
        }
    }

    private fun publishFiltered(query: String) {
        val normalized = query.trim()
        val filtered = if (normalized.isBlank()) cachedApps else cachedApps.filter { app ->
            app.label.contains(normalized, true) || app.info.packageName.contains(normalized, true)
        }
        _state.update { it.copy(state = if (filtered.isEmpty()) ScreenState.Empty("No matching installed apps") else ScreenState.Success(filtered)) }
    }

    fun register(app: InstalledApp, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val metadata = repository.registration(app)
            if (metadata == null) {
                _state.update { it.copy(registrationMessage = "${app.label} does not contain UniManager integration metadata.") }
                return@launch
            }
            val registered = registry.register(JSONObject(metadata))
            if (registered) onSuccess() else _state.update { it.copy(registrationMessage = "UniManager could not save this app entry.") }
        }
    }

    fun dismissRegistrationMessage() {
        _state.update { it.copy(registrationMessage = null) }
    }
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)
    private val _state = MutableStateFlow(RefreshSettings())
    val state: StateFlow<RefreshSettings> = _state.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    private val _appearance = MutableStateFlow(AppearanceSettings())
    val appearance: StateFlow<AppearanceSettings> = _appearance.asStateFlow()

    init { viewModelScope.launch { _state.value = repository.read(); _appearance.value = repository.readAppearance() } }

    fun update(settings: RefreshSettings) {
        if (settings.cooldownValue <= 0 || settings.rawCooldownSeconds() < InstalledAppScanner.MIN_REFRESH_COOLDOWN_SECONDS) {
            _message.value = "Cooldown must be at least 30 seconds"
            return
        }
        _state.value = settings
        _message.value = "Saved"
        viewModelScope.launch { repository.save(settings) }
    }

    fun updateAppearance(settings: AppearanceSettings) {
        _appearance.value = settings
        UniManagerTheme.setAppearance(settings)
        viewModelScope.launch { repository.saveAppearance(settings) }
    }
}

data class AboutUiState(
    val release: ReleaseInfo? = null,
    val checking: Boolean = false,
    val downloading: Boolean = false,
    val message: String = "Checking for updates...",
)

class AboutViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = UpdateRepository(application)
    private val _state = MutableStateFlow(AboutUiState())
    val state: StateFlow<AboutUiState> = _state.asStateFlow()

    init { checkForUpdates() }

    fun checkForUpdates() {
        viewModelScope.launch {
            _state.update { it.copy(checking = true, message = "Checking for updates...") }
            val release = runCatching { repository.latest() }.getOrNull()
            val status = when {
                release == null -> "Unable to check for updates right now."
                release.apkUrl.isNullOrBlank() -> "No installable update is available."
                repository.compareVersions(release.version, com.zanuaimi.unimanager.BuildConfig.VERSION_NAME) > 0 -> "Update available: version ${release.version}"
                else -> "You are up to date."
            }
            _state.value = AboutUiState(release, false, false, status)
        }
    }

    fun download(onSuccess: (java.io.File) -> Unit) {
        val url = _state.value.release?.apkUrl ?: return
        viewModelScope.launch {
            _state.update { it.copy(downloading = true, message = "Downloading update...") }
            runCatching { repository.download(url) }
                .onSuccess { file -> _state.update { it.copy(downloading = false, message = "Download complete. Opening installer...") }; onSuccess(file) }
                .onFailure { _state.update { it.copy(downloading = false, message = "Download failed. Please try again.") } }
        }
    }
}

class AppDetailsViewModel(application: Application, val packageName: String) : AndroidViewModel(application) {
    private val repository = AppRegistryRepository(application)
    private val _app = MutableStateFlow<RegisteredApp?>(null)
    val app: StateFlow<RegisteredApp?> = _app.asStateFlow()
    private val _configuration = MutableStateFlow(JSONObject())
    val configuration: StateFlow<JSONObject> = _configuration.asStateFlow()

    init {
        viewModelScope.launch {
            _app.value = repository.get(packageName)
            _configuration.value = repository.configuration(packageName)
        }
    }

    fun save(values: JSONObject, onComplete: () -> Unit) { viewModelScope.launch { repository.updateConfiguration(packageName, values); onComplete() } }
}

class AppDetailsViewModelFactory(
    private val application: Application,
    private val packageName: String,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        AppDetailsViewModel(application, packageName) as T
}
