package page.dera.opencode.ui

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.flow.drop

@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun WebViewContent(url: String, controls: BrowserControls?) {
    var webView by remember { mutableStateOf<WebView?>(null) }

    if (controls != null) {
        LaunchedEffect(controls) {
            snapshotFlow { controls.trigger }
                .drop(1)
                .collect {
                    when (controls.command) {
                        BrowserCommand.GO_BACK -> webView?.goBack()
                        BrowserCommand.GO_FORWARD -> webView?.goForward()
                        BrowserCommand.RELOAD -> webView?.reload()
                        null -> {}
                    }
                    controls.clear()
                }
        }
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                webViewClient = WebViewClient()
                webView = this
                loadUrl(url)
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}
