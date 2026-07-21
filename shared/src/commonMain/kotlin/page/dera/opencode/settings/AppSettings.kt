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

    val isConfigured: Boolean
        get() = !serverUrl.isNullOrBlank()

    fun clear() {
        settings.remove(KEY_SERVER_URL)
        settings.remove(KEY_SERVER_PASSWORD)
    }

    companion object {
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_SERVER_PASSWORD = "server_password"
    }
}
