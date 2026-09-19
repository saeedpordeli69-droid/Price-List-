package com.priceyar.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    companion object {
        private const val REQUEST_CREATE_FILE = 1001
        private const val REQUEST_OPEN_FILE = 1002
    }

    private var pendingExportJson: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = true
        webView.settings.allowContentAccess = true

        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()

        webView.addJavascriptInterface(
            AndroidBridge(),
            "AndroidBridge"
        )

        setContentView(webView)

        webView.loadUrl(
            "file:///android_asset/index.html"
        )
    }

    inner class AndroidBridge {

        @JavascriptInterface
        fun saveData(json: String) {

            getSharedPreferences(
                "priceyar",
                MODE_PRIVATE
            )
                .edit()
                .putString("data", json)
                .apply()

            updateWidget()
        }

        @JavascriptInterface
        fun loadData(): String {

            return getSharedPreferences(
                "priceyar",
                MODE_PRIVATE
            )
                .getString("data", "") ?: ""
        }

        @JavascriptInterface
        fun exportPriceList(
            json: String,
            fileName: String
        ) {

            pendingExportJson = json

            val intent =
                Intent(Intent.ACTION_CREATE_DOCUMENT).apply {

                    addCategory(
                        Intent.CATEGORY_OPENABLE
                    )

                    type =
                        "application/json"

                    putExtra(
                        Intent.EXTRA_TITLE,
                        fileName
                    )
                }

            startActivityForResult(
                intent,
                REQUEST_CREATE_FILE
            )
        }

        @JavascriptInterface
        fun importPriceList() {

            val intent =
                Intent(Intent.ACTION_OPEN_DOCUMENT).apply {

                    addCategory(
                        Intent.CATEGORY_OPENABLE
                    )

                    type = "application/json"

                    putExtra(
                        Intent.EXTRA_MIME_TYPES,
                        arrayOf(
                            "application/json",
                            "text/plain",
                            "*/*"
                        )
                    )
                }

            startActivityForResult(
                intent,
                REQUEST_OPEN_FILE
            )
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        intentData: Intent?
    ) {
        super.onActivityResult(
            requestCode,
            resultCode,
            intentData
        )

        if (
            resultCode != Activity.RESULT_OK
        ) {

            if (
                requestCode ==
                REQUEST_CREATE_FILE
            ) {

                pendingExportJson = null

                webView.evaluateJavascript(
                    "window.exportCancelled && window.exportCancelled();",
                    null
                )
            }

            return
        }

        val uri =
            intentData?.data
                ?: return

        when (requestCode) {

            REQUEST_CREATE_FILE -> {
                saveExportFile(uri)
            }

            REQUEST_OPEN_FILE -> {
                readImportFile(uri)
            }
        }
    }

    private fun saveExportFile(
        uri: Uri
    ) {

        try {

            val json =
                pendingExportJson

            if (json == null) {

                webView.evaluateJavascript(
                    "window.exportFailed && window.exportFailed();",
                    null
                )

                return
            }

            contentResolver
                .openOutputStream(uri)
                ?.use { output ->

                    output.write(
                        json.toByteArray(
                            Charsets.UTF_8
                        )
                    )

                    output.flush()
                }

            pendingExportJson = null

            webView.evaluateJavascript(
                "window.exportFinished && window.exportFinished();",
                null
            )

        } catch (e: Exception) {

            pendingExportJson = null

            webView.evaluateJavascript(
                "window.exportFailed && window.exportFailed();",
                null
            )
        }
    }

    private fun readImportFile(
        uri: Uri
    ) {

        try {

            val text =
                contentResolver
                    .openInputStream(uri)
                    ?.use { input ->

                        input.readBytes()
                            .toString(
                                Charsets.UTF_8
                            )
                    }

            if (
                text.isNullOrBlank()
            ) {

                webView.evaluateJavascript(
                    "window.importFailed && window.importFailed();",
                    null
                )

                return
            }

            val safeText =
                org.json.JSONObject.quote(
                    text
                )

            webView.evaluateJavascript(
                "window.receiveImportedPriceList($safeText);",
                null
            )

        } catch (e: Exception) {

            webView.evaluateJavascript(
                "window.importFailed && window.importFailed();",
                null
            )
        }
    }

    private fun updateWidget() {

        val manager =
            AppWidgetManager.getInstance(this)

        val component =
            ComponentName(
                this,
                PriceWidgetProvider::class.java
            )

        val ids =
            manager.getAppWidgetIds(
                component
            )

        for (id in ids) {

            PriceWidgetProvider.updateWidget(
                this,
                manager,
                id
            )
        }
    }
}
