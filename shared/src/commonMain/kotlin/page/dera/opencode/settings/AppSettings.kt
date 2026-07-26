package page.dera.opencode.settings

import com.russhwolf.settings.Settings

class AppSettings {
    private val settings: Settings = Settings()

    var serverUrl: String?
        get() = settings.getStringOrNull(KEY_SERVER_URL)
        set(value) {
            if (value != null) settings.putString(KEY_SERVER_URL, value)
            else settings.remove(KEY_SERVER_URL)
        }

    var serverPassword: String?
        get() = settings.getStringOrNull(KEY_SERVER_PASSWORD)
        set(value) {
            if (value != null) settings.putString(KEY_SERVER_PASSWORD, value)
            else settings.remove(KEY_SERVER_PASSWORD)
        }

    var wakatimeApiUrl: String?
        get() = settings.getStringOrNull(KEY_WAKATIME_API_URL)
        set(value) {
            if (value != null) settings.putString(KEY_WAKATIME_API_URL, value)
            else settings.remove(KEY_WAKATIME_API_URL)
        }

    var wakatimeApiKey: String?
        get() = settings.getStringOrNull(KEY_WAKATIME_API_KEY)
        set(value) {
            if (value != null) settings.putString(KEY_WAKATIME_API_KEY, value)
            else settings.remove(KEY_WAKATIME_API_KEY)
        }

    val isWakatimeConfigured: Boolean
        get() = !wakatimeApiKey.isNullOrBlank()

    var latestSessionsLimit: Int
        get() {
            val raw = settings.getInt(KEY_LATEST_SESSIONS_LIMIT, DEFAULT_LATEST_SESSIONS_LIMIT)
            return raw.coerceIn(0, 20)
        }
        set(value) { settings.putInt(KEY_LATEST_SESSIONS_LIMIT, value.coerceIn(0, 20)) }

    var pinnedFolders: List<String>
        get() {
            val raw = settings.getStringOrNull(KEY_PINNED_FOLDERS) ?: return emptyList()
            return raw.split(",").filter { it.isNotBlank() }
        }
        set(value) {
            if (value.isEmpty()) settings.remove(KEY_PINNED_FOLDERS)
            else settings.putString(KEY_PINNED_FOLDERS, value.joinToString(","))
        }

    fun togglePinnedFolder(path: String) {
        val current = pinnedFolders.toMutableList()
        if (current.contains(path)) current.remove(path) else current.add(path)
        pinnedFolders = current
    }

    fun isPinned(path: String): Boolean = pinnedFolders.contains(path)

    var expandDiffsByDefault: Boolean
        get() = settings.getBoolean(KEY_EXPAND_DIFFS_BY_DEFAULT, true)
        set(value) { settings.putBoolean(KEY_EXPAND_DIFFS_BY_DEFAULT, value) }

    var portScannerEndpoint: String?
        get() = settings.getStringOrNull(KEY_PORT_SCANNER_ENDPOINT)
        set(value) {
            if (value != null) settings.putString(KEY_PORT_SCANNER_ENDPOINT, value)
            else settings.remove(KEY_PORT_SCANNER_ENDPOINT)
        }

    var urlPattern: String
        get() = settings.getString(KEY_URL_PATTERN, DEFAULT_URL_PATTERN)
        set(value) { settings.putString(KEY_URL_PATTERN, value) }

    val isConfigured: Boolean
        get() = !serverUrl.isNullOrBlank()

    fun clear() {
        settings.remove(KEY_SERVER_URL)
        settings.remove(KEY_SERVER_PASSWORD)
    }

    companion object {
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_SERVER_PASSWORD = "server_password"
        private const val KEY_PINNED_FOLDERS = "pinned_folders"
        private const val KEY_LATEST_SESSIONS_LIMIT = "latest_sessions_limit"
        private const val DEFAULT_LATEST_SESSIONS_LIMIT = 3
        private const val KEY_WAKATIME_API_URL = "wakatime_api_url"
        private const val KEY_WAKATIME_API_KEY = "wakatime_api_key"
        private const val KEY_EXPAND_DIFFS_BY_DEFAULT = "expand_diffs_by_default"
        private const val KEY_PORT_SCANNER_ENDPOINT = "port_scanner_endpoint"
        private const val KEY_URL_PATTERN = "url_pattern"
        private const val DEFAULT_URL_PATTERN = "http://localhost:<port>"
    }
}
