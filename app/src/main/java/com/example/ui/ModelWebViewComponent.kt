package com.example.ui

import android.view.View
import android.webkit.WebView
import android.webkit.WebSettings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.device.AndroidBridge
import com.example.device.DeviceActionManager

@Composable
fun ModelWebViewComponent(
    modifier: Modifier = Modifier,
    isVisible: Boolean = false,
    url: String = "https://www.meshy.ai/3d-models/Modern-Muse-in-Blue-Light-01a12c74-a3dd-76e7-85e2-72b2d597bc97",
    onDiagnosticLog: (String) -> Unit = {}
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                id = com.example.R.id.modelWebView
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                val actionManager = DeviceActionManager(context)
                val bridge = AndroidBridge(
                    actionManager = actionManager,
                    onLog = onDiagnosticLog
                )
                addJavascriptInterface(bridge, "AndroidBridge")

                loadUrl(url)
            }
        },
        update = { webView ->
            webView.visibility = if (isVisible) View.VISIBLE else View.GONE
            if (isVisible && webView.url != url) {
                webView.loadUrl(url)
            }
        }
    )
}
