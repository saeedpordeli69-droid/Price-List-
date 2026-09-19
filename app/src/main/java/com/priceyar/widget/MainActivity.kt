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
import androidx.core.content.FileProvider
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    private var pageLoaded = false
    private var pendingIncomingUri: Uri? = null

    companion object {
        private const val REQUEST_OPEN_FILE = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = true
        webView.settings.allowContentAccess = true

        webView.webViewClient = object : WebViewClient() {

            override fun onPageFinished(
                view: WebView?,
                url: String?
            ) {
                super.onPageFinished(view, url)

                pageLoaded = true
                processPendingIncomingFile()
            }
        }

        webView.webChromeClient = WebChromeClient()

        webView.addJavascriptInterface(
            AndroidBridge(),
            "AndroidBridge"
        )

        setContentView(webView)

        webView.loadUrl(
            "file:///android_asset/index.html"
        )

        handleIncomingIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        setIntent(intent)

        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(
        incomingIntent: Intent?
    ) {
        if (incomingIntent == null) {
            return
        }

        val action = incomingIntent.action

        var uri: Uri? = null

        if (action == Intent.ACTION_VIEW) {

            uri = incomingIntent.data

        } else if (action == Intent.ACTION_SEND) {

            uri =
                if (android.os.Build.VERSION.SDK_INT >= 33) {
                    incomingIntent.getParcelableExtra(
                        Intent.EXTRA_STREAM,
                        Uri::class.java
                    )
                } else {
                    @Suppress("DEPRECATION")
                    incomingIntent.getParcelableExtra<Uri>(
                        Intent.EXTRA_STREAM
                    )
                }
        }

        if (uri != null) {

            pendingIncomingUri = uri

            processPendingIncomingFile()
        }
    }

    private fun processPendingIncomingFile() {

        if (!pageLoaded) {
            return
        }

        val uri = pendingIncomingUri
            ?: return

        pendingIncomingUri = null

        readImportFile(uri)
    }

    inner class AndroidBridge {

        @JavascriptInterface
        fun saveData(json: String) {

            getSharedPreferences(
                "priceyar",
                MODE_PRIVATE
            )
                .edit()
                .putString(
                    "data",
                    json
                )
                .apply()

            updateWidget()
        }

        @JavascriptInterface
        fun loadData(): String {

            return getSharedPreferences(
                "priceyar",
                MODE_PRIVATE
            )
                .getString(
                    "data",
                    ""
                ) ?: ""
        }

        @JavascriptInterface
        fun sharePriceList(
            json: String,
            fileName: String
        ) {

            try {

                val sharedDirectory =
                    File(
                        cacheDir,
                        "shared"
                    )

                if (!sharedDirectory.exists()) {
                    sharedDirectory.mkdirs()
                }

                val safeFileName =
                    sanitizeFileName(fileName)

                val file =
                    File(
                        sharedDirectory,
                        safeFileName
                    )

                file.writeText(
                    json,
                    Charsets.UTF_8
                )

                val uri =
                    FileProvider.getUriForFile(
                        this@MainActivity,
                        "${packageName}.fileprovider",
                        file
                    )

                val sendIntent =
                    Intent(
                        Intent.ACTION_SEND
                    ).apply {

                        type =
                            "application/json"

                        putExtra(
                            Intent.EXTRA_STREAM,
                            uri
                        )

                        putExtra(
                            Intent.EXTRA_TEXT,
                            "لیست جدید قیمت‌ها برای به‌روزرسانی لمس کنید."
                        )

                        addFlags(
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )

                        clipData =
                            android.content.ClipData.newRawUri(
                                "PriceYar",
                                uri
                            )
                    }

                val chooser =
                    Intent.createChooser(
                        sendIntent,
                        "ارسال لیست قیمت با"
                    )

                startActivity(chooser)

                webView.evaluateJavascript(
                    "window.shareStarted && window.shareStarted();",
                    null
                )

            } catch (e: Exception) {

                webView.evaluateJavascript(
                    "window.shareFailed && window.shareFailed();",
                    null
                )
            }
        }

        /*
         * برای سازگاری با نسخه‌های قبلی HTML
         * اگر هنوز exportPriceList صدا زده شود،
         * همان اشتراک‌گذاری جدید انجام می‌شود.
         */
        @JavascriptInterface
        fun exportPriceList(
            json: String,
            fileName: String
        ) {
            sharePriceList(
                json,
                fileName
            )
        }

        @JavascriptInterface
        fun importPriceList() {

            val intent =
                Intent(
                    Intent.ACTION_OPEN_DOCUMENT
                ).apply {

                    addCategory(
                        Intent.CATEGORY_OPENABLE
                    )

                    type =
                        "application/json"

                    putExtra(
                        Intent.EXTRA_MIME_TYPES,
                        arrayOf(
                            "application/json",
                            "text/json",
                            "text/plain",
                            "application/octet-stream",
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
            requestCode != REQUEST_OPEN_FILE
        ) {
            return
        }

        if (
            resultCode != Activity.RESULT_OK
        ) {
            return
        }

        val uri =
            intentData?.data
                ?: return

        readImportFile(uri)
    }

    private fun readImportFile(
        uri: Uri
    ) {

        try {

            val text =
                contentResolver
                    .openInputStream(uri)
                    ?.use { input ->

                        input
                            .readBytes()
                            .toString(
                                Charsets.UTF_8
                            )
                    }

            if (text.isNullOrBlank()) {

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

    private fun sanitizeFileName(
        fileName: String
    ): String {

        var result =
            fileName.replace(
                Regex("[\\\\/:*?\"<>|]"),
                "_"
            )

        if (
            !result
                .lowercase()
                .endsWith(".json")
        ) {
            result += ".json"
        }

        return result
    }

    private fun updateWidget() {

        val manager =
            AppWidgetManager.getInstance(
                this
            )

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
