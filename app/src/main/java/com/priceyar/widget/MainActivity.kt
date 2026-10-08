package com.priceyar.widget

import android.app.Activity
import android.app.AlertDialog
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
    private var pendingOpenAdd = false
    private var pendingChooseShareMode = false
    private var pendingOpenLedger = false

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
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                pageLoaded = true
                processPendingIncomingFile()
                processPendingOpenAdd()
                processPendingChooseShareMode()
                processPendingOpenLedger()
            }
        }

        webView.webChromeClient = WebChromeClient()
        webView.addJavascriptInterface(AndroidBridge(), "AndroidBridge")
        setContentView(webView)

        webView.loadUrl("file:///android_asset/index.html")
        handleIncomingIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    // ابتدا لایه داخلی WebView را می‌بندد؛ اگر لایه‌ای باز نبود، خود برنامه بسته می‌شود.
    override fun onBackPressed() {
        if (!pageLoaded) {
            super.onBackPressed()
            return
        }

        webView.evaluateJavascript(
            "(window.handleAndroidBack && window.handleAndroidBack()) || false;"
        ) { result ->
            if (result == "false") {
                super.onBackPressed()
            }
        }
    }

    private fun handleIncomingIntent(incomingIntent: Intent?) {
        if (incomingIntent == null) return

        if (incomingIntent.action == PriceWidgetProvider.ACTION_ADD_PRODUCT) {
            pendingOpenAdd = true
            processPendingOpenAdd()
        }

        if (incomingIntent.action == PriceWidgetProvider.ACTION_CHOOSE_SHARE_MODE) {
            pendingChooseShareMode = true
            processPendingChooseShareMode()
        }

        if (incomingIntent.getBooleanExtra("openLedger", false)) {
            pendingOpenLedger = true
            processPendingOpenLedger()
        }

        val action = incomingIntent.action
        var uri: Uri? = null

        if (action == Intent.ACTION_VIEW) {
            uri = incomingIntent.data
        } else if (action == Intent.ACTION_SEND) {
            uri = if (android.os.Build.VERSION.SDK_INT >= 33) {
                incomingIntent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                incomingIntent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            }
        }

        if (uri != null) {
            pendingIncomingUri = uri
            processPendingIncomingFile()
        }
    }

    private fun processPendingOpenAdd() {
        if (!pageLoaded || !pendingOpenAdd) return
        pendingOpenAdd = false
        webView.evaluateJavascript(
            "window.openAdd && window.openAdd();",
            null
        )
    }

    private fun processPendingOpenLedger() {
        if (!pageLoaded || !pendingOpenLedger) return
        pendingOpenLedger = false
        webView.evaluateJavascript(
            "window.openLedger && window.openLedger();",
            null
        )
    }

    private fun processPendingChooseShareMode() {
        if (!pageLoaded || !pendingChooseShareMode) return
        pendingChooseShareMode = false
        showShareModeDialog()
    }

    private fun showShareModeDialog() {
        showShareModeDialogWithoutData()
    }

    private fun showShareModeDialogWithoutData() {
        AlertDialog.Builder(this)
            .setTitle("ارسال لیست قیمت")
            .setItems(
                arrayOf(
                    "👤 ویژه فروشندگان",
                    "🔐 ویژه مدیریت"
                )
            ) { _, which ->
                when (which) {
                    0 -> webView.evaluateJavascript(
                        "window.AndroidBridge && window.AndroidBridge.requestShareMode && window.AndroidBridge.requestShareMode(\"seller\");",
                        null
                    )
                    1 -> webView.evaluateJavascript(
                        "window.AndroidBridge && window.AndroidBridge.requestShareMode && window.AndroidBridge.requestShareMode(\"management\");",
                        null
                    )
                }
            }
            .setNegativeButton("انصراف", null)
            .show()
    }

    private fun processPendingIncomingFile() {
        if (!pageLoaded) return
        val uri = pendingIncomingUri ?: return
        pendingIncomingUri = null
        readImportFile(uri)
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

        @JavascriptInterface
        fun requestShareMode(mode: String) {
            webView.post {
                val rawJson = getSharedPreferences("priceyar", MODE_PRIVATE)
                    .getString("data", "") ?: ""

                if (rawJson.isBlank()) return@post

                val json = try {
                    val rawRoot = org.json.JSONObject(rawJson)
                    val packageRoot = org.json.JSONObject()

                    packageRoot.put("type", "priceyar-price-list")
                    packageRoot.put("version", 1)
                    packageRoot.put("createdAt", System.currentTimeMillis())
                    packageRoot.put("shareMode", mode)
                    packageRoot.put(
                        "stores",
                        rawRoot.optJSONArray("stores") ?: org.json.JSONArray()
                    )
                    packageRoot.put(
                        "deleted",
                        rawRoot.optJSONObject("deleted") ?: org.json.JSONObject()
                    )

                    packageRoot.toString(2)
                } catch (e: Exception) {
                    return@post
                }

                sharePriceListFile(
                    json,
                    "PriceYar-PriceList.json",
                    if (mode == "seller") "ویژه فروشندگان" else "ویژه مدیریت"
                )
            }
        }

        @JavascriptInterface
        fun sharePriceList(json: String, fileName: String) {
            runOnUiThread {
                showShareModeDialogForData(json, fileName)
            }
        }

        @JavascriptInterface
        fun exportPriceList(json: String, fileName: String) {
            sharePriceList(json, fileName)
        }

        @JavascriptInterface
        fun importPriceList() {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/json"
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

            startActivityForResult(intent, REQUEST_OPEN_FILE)
        }
    }

    private fun showShareModeDialogForData(json: String, fileName: String) {
        AlertDialog.Builder(this)
            .setTitle("ارسال لیست قیمت")
            .setItems(
                arrayOf(
                    "👤 ویژه فروشندگان",
                    "🔐 ویژه مدیریت"
                )
            ) { _, which ->
                when (which) {
                    0 -> sharePriceListFile(json, fileName, "ویژه فروشندگان")
                    1 -> sharePriceListFile(json, fileName, "ویژه مدیریت")
                }
            }
            .setNegativeButton("انصراف", null)
            .show()
    }

    private fun prepareSellerJson(json: String): String {
        return try {
            val root = org.json.JSONObject(json)
            root.put("shareMode", "seller")

            val stores = root.optJSONArray("stores")
            if (stores != null) {
                for (i in 0 until stores.length()) {
                    val store = stores.optJSONObject(i) ?: continue
                    val items = store.optJSONArray("items") ?: continue

                    for (j in 0 until items.length()) {
                        val item = items.optJSONObject(j) ?: continue
                        item.remove("buy")
                        item.remove("buyPrice")
                        item.remove("purchasePrice")
                    }
                }
            }

            root.toString(2)
        } catch (e: Exception) {
            json
        }
    }

    private fun sharePriceListFile(
        json: String,
        fileName: String,
        modeName: String
    ) {
        try {
            val outputJson = if (modeName == "ویژه فروشندگان") {
                prepareSellerJson(json)
            } else {
                try {
                    val root = org.json.JSONObject(json)
                    root.put("shareMode", "management")
                    root.toString(2)
                } catch (e: Exception) {
                    json
                }
            }

            val sharedDirectory = File(cacheDir, "shared")
            if (!sharedDirectory.exists()) sharedDirectory.mkdirs()

            val safeFileName = sanitizeFileName(fileName)
            val file = File(sharedDirectory, safeFileName)
            file.writeText(outputJson, Charsets.UTF_8)

            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "لیست قیمت - $modeName")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData = android.content.ClipData.newRawUri("PriceYar", uri)
            }

            val chooser = Intent.createChooser(sendIntent, "ارسال لیست قیمت با")
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

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        intentData: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, intentData)

        if (requestCode != REQUEST_OPEN_FILE) return
        if (resultCode != Activity.RESULT_OK) return

        val uri = intentData?.data ?: return
        readImportFile(uri)
    }

    private fun readImportFile(uri: Uri) {
        try {
            val text = contentResolver
                .openInputStream(uri)
                ?.use { input -> input.readBytes().toString(Charsets.UTF_8) }

            if (text.isNullOrBlank()) {
                webView.evaluateJavascript(
                    "window.importFailed && window.importFailed();",
                    null
                )
                return
            }

            val safeText = org.json.JSONObject.quote(text)
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

    private fun sanitizeFileName(fileName: String): String {
        var result = fileName.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        if (!result.lowercase().endsWith(".json")) result += ".json"
        return result
    }

    private fun updateWidget() {
        PriceWidgetProvider.updateWidget(this)
    }
}
