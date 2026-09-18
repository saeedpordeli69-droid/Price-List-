package com.priceyar.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = true

        webView.webViewClient = WebViewClient()

        webView.addJavascriptInterface(
            AndroidBridge(),
            "AndroidBridge"
        )

        setContentView(webView)

        webView.loadUrl("file:///android_asset/index.html")
    }

    inner class AndroidBridge {

        @JavascriptInterface
        fun saveData(json: String) {
            getSharedPreferences("priceyar", MODE_PRIVATE)
                .edit()
                .putString("data", json)
                .apply()

            updateWidget()
        }

        @JavascriptInterface
        fun loadData(): String {
            return getSharedPreferences("priceyar", MODE_PRIVATE)
                .getString("data", "") ?: ""
        }
    }

    private fun updateWidget() {
        val manager = AppWidgetManager.getInstance(this)

        val component = ComponentName(
            this,
            PriceWidgetProvider::class.java
        )

        val ids = manager.getAppWidgetIds(component)

        for (id in ids) {
            PriceWidgetProvider.updateWidget(
                this,
                manager,
                id
            )
        }
    }
}
