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
    }
}
