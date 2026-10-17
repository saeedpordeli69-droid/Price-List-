package com.priceyar.widget

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class WidgetShareActivity : Activity() {

    private lateinit var webView: WebView
    private var pageLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rawJson = getSharedPreferences("priceyar", MODE_PRIVATE)
            .getString("data", "") ?: ""

        if (rawJson.isBlank()) {
            showMessage("اطلاعاتی برای ارسال وجود ندارد")
            return
        }

        val root = try {
            JSONObject(rawJson)
        } catch (_: Exception) {
            showMessage("اطلاعات لیست معتبر نیست")
            return
        }

        if ((root.optJSONArray("stores") ?: JSONArray()).length() == 0) {
            showMessage("ابتدا یک فروشگاه بسازید یا یک لیست وارد کنید")
            return
        }

        webView = WebView(this)
        webView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        webView.setBackgroundColor(Color.TRANSPARENT)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = true
        webView.settings.allowContentAccess = true
        webView.webChromeClient = WebChromeClient()
        webView.addJavascriptInterface(WidgetBridge(), "AndroidBridge")
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (pageLoaded) return
                pageLoaded = true
                webView.evaluateJavascript(
                    "window.openShareMenu && window.openShareMenu();",
                    null
                )
            }
        }

        setContentView(webView)
        webView.loadUrl("file:///android_asset/index.html")
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        finish()
    }

    inner class WidgetBridge {
        @JavascriptInterface
        fun loadData(): String {
            return getSharedPreferences("priceyar", MODE_PRIVATE)
                .getString("data", "") ?: ""
        }

        @JavascriptInterface
        fun saveData(json: String) {
            getSharedPreferences("priceyar", MODE_PRIVATE)
                .edit()
                .putString("data", json)
                .apply()
            runOnUiThread { PriceWidgetProvider.updateWidget(this@WidgetShareActivity) }
        }

        @JavascriptInterface
        fun sharePriceList(json: String, fileName: String) {
            shareByJsonMode(json, fileName)
        }

        @JavascriptInterface
        fun sharePriceListDirect(json: String, fileName: String, mode: String) {
            runOnUiThread {
                shareList(json, fileName, if (mode == "seller") "seller" else "management")
            }
        }

        @JavascriptInterface
        fun exportPriceList(json: String, fileName: String) {
            shareByJsonMode(json, fileName)
        }

        @JavascriptInterface
        fun closeShareActivity() {
            runOnUiThread { finish() }
        }
    }

    private fun shareByJsonMode(json: String, fileName: String) {
        val mode = try {
            if (JSONObject(json).optString("shareMode") == "seller") "seller" else "management"
        } catch (_: Exception) {
            "management"
        }
        runOnUiThread { shareList(json, fileName, mode) }
    }

    private fun shareList(json: String, fileName: String, mode: String) {
        try {
            val root = JSONObject(json)
            root.put("type", "priceyar-price-list")
            root.put("version", root.optInt("version", 3))
            root.put("shareMode", mode)

            // نسخه فروشندگان نباید هیچ‌کدام از فیلدهای قیمت خرید را داشته باشد.
            if (mode == "seller") {
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
                    val ledger = root.optJSONObject("ledger")
                    if (ledger != null) {
                        ledger.put("shareMode", "seller")
                        val accounts = ledger.optJSONArray("accounts") ?: JSONArray()
                        val creditors = JSONArray()
                        for (i in 0 until accounts.length()) {
                            val account = accounts.optJSONObject(i) ?: continue
                            if (account.optString("type") == "creditor") creditors.put(account)
                        }
                        ledger.put("accounts", creditors)
                    }
                }
            }

            val sharedDirectory = File(cacheDir, "shared")
            if (!sharedDirectory.exists() && !sharedDirectory.mkdirs()) {
                throw IllegalStateException("ساخت پوشه ارسال ناموفق بود")
            }

            val safeName = sanitizeFileName(fileName)
            val file = File(sharedDirectory, safeName)
            file.writeText(root.toString(2), Charsets.UTF_8)

            val uri: Uri = FileProvider.getUriForFile(
                this,
                "$packageName.fileprovider",
                file
            )

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(
                    Intent.EXTRA_TEXT,
                    if (mode == "seller") "لیست قیمت - ویژه فروشندگان" else "لیست قیمت - ویژه مدیریت"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData = ClipData.newRawUri("PriceYar", uri)
            }

            startActivity(Intent.createChooser(sendIntent, "ارسال لیست قیمت با"))
            finish()
        } catch (_: Exception) {
            showMessage("ارسال لیست انجام نشد")
        }
    }

    private fun sanitizeFileName(fileName: String): String {
        var result = fileName.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        if (!result.lowercase().endsWith(".json")) result += ".json"
        return result
    }
}
