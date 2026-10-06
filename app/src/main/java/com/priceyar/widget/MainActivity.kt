package com.priceyar.widget

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.TextView
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

    // دکمه برگشت گوشی فقط یک لایه از رابط برنامه را می‌بندد و هرگز برنامه را نمی‌بندد.
    override fun onBackPressed() {
        if (!pageLoaded) return

        webView.evaluateJavascript(
            "(window.handleAndroidBack && window.handleAndroidBack()) || true;",
            null
        )
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

    /*
     * پنجره انتخاب نوع ارسال
     *
     * طراحی Glassmorphism ساده و سبک است تا روی همه نسخه‌های اندروید
     * بدون وابستگی جدید کار کند.
     *
     * حالت Dark و Light بر اساس تم فعلی دستگاه تشخیص داده می‌شود.
     */
    private fun showShareModeDialog() {
        showGlassShareModeDialog { mode ->
            webView.evaluateJavascript(
                "window.AndroidBridge && " +
                    "window.AndroidBridge.requestShareMode && " +
                    "window.AndroidBridge.requestShareMode(\"$mode\");",
                null
            )
        }
    }

    private fun showShareModeDialogForData(
        json: String,
        fileName: String
    ) {
        showGlassShareModeDialog { mode ->
            if (mode == "seller") {
                sharePriceListFile(
                    json,
                    fileName,
                    "ویژه فروشندگان"
                )
            } else {
                sharePriceListFile(
                    json,
                    fileName,
                    "ویژه مدیریت"
                )
            }
        }
    }

    private fun showGlassShareModeDialog(
        onModeSelected: (String) -> Unit
    ) {
        val isDark = (resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES

        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(
                dp(22),
                dp(20),
                dp(22),
                dp(18)
            )
            background = roundedBackground(
                if (isDark) "#E9151C29" else "#F2FFFFFF",
                if (isDark) "#4058D5FF" else "#5058B9E8",
                24f
            )
        }

        val title = TextView(this).apply {
            text = "ارسال لیست قیمت"
            textSize = 21f
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(
                Color.parseColor(
                    if (isDark) "#F5FAFF" else "#142033"
                )
            )
        }

        dialogView.addView(
            title,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val subtitle = TextView(this).apply {
            text = "نوع دسترسی گیرنده را انتخاب کنید"
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, dp(7), 0, dp(16))
            setTextColor(
                Color.parseColor(
                    if (isDark) "#AFC2D9" else "#607086"
                )
            )
        }

        dialogView.addView(
            subtitle,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val sellerButton = createShareModeButton(
            icon = "👤",
            title = "ویژه فروشندگان",
            description = "فقط قیمت فروش کالاها",
            isDark = isDark,
            accent = "#42B8FF"
        )

        val managementButton = createShareModeButton(
            icon = "🔐",
            title = "ویژه مدیریت",
            description = "قیمت‌ها و اطلاعات مدیریتی",
            isDark = isDark,
            accent = "#63D5A2"
        )

        dialogView.addView(
            sellerButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(72)
            ).apply {
                bottomMargin = dp(10)
            }
        )

        dialogView.addView(
            managementButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(72)
            )
        )

        val cancelButton = TextView(this).apply {
            text = "انصراف"
            textSize = 14f
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(
                Color.parseColor(
                    if (isDark) "#AFC2D9" else "#506176"
                )
            )
            setPadding(0, dp(16), 0, dp(4))
            isClickable = true
            isFocusable = true
        }

        dialogView.addView(
            cancelButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48)
            )
        )

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawable(
            android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)
        )

        sellerButton.setOnClickListener {
            dialog.dismiss()
            onModeSelected("seller")
        }

        managementButton.setOnClickListener {
            dialog.dismiss()
            onModeSelected("management")
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawable(
                android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)
            )

            dialog.window?.setDimAmount(
                if (isDark) 0.72f else 0.45f
            )

            dialog.window?.setLayout(
                (resources.displayMetrics.widthPixels * 0.88f).toInt(),
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        dialog.show()

        dialog.window?.setBackgroundDrawable(
            android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)
        )

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.88f).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun createShareModeButton(
        icon: String,
        title: String,
        description: String,
        isDark: Boolean,
        accent: String
    ): LinearLayout {

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(
                dp(14),
                dp(8),
                dp(14),
                dp(8)
            )
            background = roundedBackground(
                if (isDark) "#CC202A38" else "#DFFFFFFF",
                accent,
                18f
            )
            isClickable = true
            isFocusable = true
        }

        val iconView = TextView(this).apply {
            text = icon
            textSize = 24f
            gravity = Gravity.CENTER
        }

        container.addView(
            iconView,
            LinearLayout.LayoutParams(
                dp(44),
                dp(52)
            ).apply {
                marginEnd = dp(8)
            }
        )

        val textContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val titleView = TextView(this).apply {
            text = title
            textSize = 15f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor(accent))
        }

        val descriptionView = TextView(this).apply {
            text = description
            textSize = 11.5f
            setPadding(0, dp(3), 0, 0)
            setTextColor(
                Color.parseColor(
                    if (isDark) "#AFC0D3" else "#66768A"
                )
            )
        }

        textContainer.addView(titleView)
        textContainer.addView(descriptionView)

        container.addView(
            textContainer,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        val arrow = TextView(this).apply {
            text = "‹"
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(
                Color.parseColor(
                    if (isDark) "#9EB4CB" else "#718197"
                )
            )
        }

        container.addView(
            arrow,
            LinearLayout.LayoutParams(
                dp(28),
                dp(48)
            )
        )

        return container
    }

    private fun roundedBackground(
        fillColor: String,
        strokeColor: String,
        radius: Float
    ): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.parseColor(fillColor))
            setStroke(
                dp(1),
                Color.parseColor(strokeColor)
            )
            cornerRadius = dp(radius.toInt()).toFloat()
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
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

                val rawJson = getSharedPreferences(
                    "priceyar",
                    MODE_PRIVATE
                ).getString("data", "") ?: ""

                if (rawJson.isBlank()) return@post

                val json = try {

                    val rawRoot =
                        org.json.JSONObject(rawJson)

                    val packageRoot =
                        org.json.JSONObject()

                    packageRoot.put(
                        "type",
                        "priceyar-price-list"
                    )

                    packageRoot.put(
                        "version",
                        1
                    )

                    packageRoot.put(
                        "createdAt",
                        System.currentTimeMillis()
                    )

                    packageRoot.put(
                        "shareMode",
                        mode
                    )

                    packageRoot.put(
                        "stores",
                        rawRoot.optJSONArray("stores")
                            ?: org.json.JSONArray()
                    )

                    packageRoot.put(
                        "deleted",
                        rawRoot.optJSONObject("deleted")
                            ?: org.json.JSONObject()
                    )

                    packageRoot.toString(2)

                } catch (e: Exception) {
                    return@post
                }

                sharePriceListFile(
                    json,
                    "PriceYar-PriceList.json",
                    if (mode == "seller")
                        "ویژه فروشندگان"
                    else
                        "ویژه مدیریت"
                )
            }
        }

        @JavascriptInterface
        fun sharePriceList(
            json: String,
            fileName: String
        ) {
            runOnUiThread {
                showShareModeDialogForData(
                    json,
                    fileName
                )
            }
        }

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
                Intent(Intent.ACTION_OPEN_DOCUMENT).apply {

                    addCategory(
                        Intent.CATEGORY_OPENABLE
                    )

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

            startActivityForResult(
                intent,
                REQUEST_OPEN_FILE
            )
        }
    }

    private fun prepareSellerJson(
        json: String
    ): String {

        return try {

            val root =
                org.json.JSONObject(json)

            root.put(
                "shareMode",
                "seller"
            )

            val stores =
                root.optJSONArray("stores")

            if (stores != null) {

                for (i in 0 until stores.length()) {

                    val store =
                        stores.optJSONObject(i)
                            ?: continue

                    val items =
                        store.optJSONArray("items")
                            ?: continue

                    for (j in 0 until items.length()) {

                        val item =
                            items.optJSONObject(j)
                                ?: continue

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

            val outputJson =
                if (modeName == "ویژه فروشندگان") {

                    prepareSellerJson(json)

                } else {

                    try {

                        val root =
                            org.json.JSONObject(json)

                        root.put(
                            "shareMode",
                            "management"
                        )

                        root.toString(2)

                    } catch (e: Exception) {
                        json
                    }
                }

            val sharedDirectory =
                File(cacheDir, "shared")

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
                outputJson,
                Charsets.UTF_8
            )

            val uri =
                FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    file
                )

            val sendIntent =
                Intent(Intent.ACTION_SEND).apply {

                    type = "application/json"

                    putExtra(
                        Intent.EXTRA_STREAM,
                        uri
                    )

                    putExtra(
                        Intent.EXTRA_TEXT,
                        "لیست قیمت - $modeName"
                    )

                    addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )

                    clipData =
                        android.content.ClipData
                            .newRawUri(
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
                "window.shareStarted && " +
                    "window.shareStarted();",
                null
            )

        } catch (e: Exception) {

            webView.evaluateJavascript(
                "window.shareFailed && " +
                    "window.shareFailed();",
                null
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

        if (requestCode != REQUEST_OPEN_FILE) {
            return
        }

        if (resultCode != Activity.RESULT_OK) {
            return
        }

        val uri =
            intentData?.data ?: return

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
                        input.readBytes()
                            .toString(Charsets.UTF_8)
                    }

            if (text.isNullOrBlank()) {

                webView.evaluateJavascript(
                    "window.importFailed && " +
                        "window.importFailed();",
                    null
                )

                return
            }

            val safeText =
                org.json.JSONObject.quote(text)

            webView.evaluateJavascript(
                "window.receiveImportedPriceList($safeText);",
                null
            )

        } catch (e: Exception) {

            webView.evaluateJavascript(
                "window.importFailed && " +
                    "window.importFailed();",
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

        if (!result.lowercase().endsWith(".json")) {
            result += ".json"
        }

        return result
    }

    private fun updateWidget() {
        PriceWidgetProvider.updateWidget(this)
    }
}
