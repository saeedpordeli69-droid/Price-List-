package com.priceyar.widget

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
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

    // دکمه برگشت گوشی:
    // اگر یک لایه داخل برنامه باز باشد، همان لایه بسته می‌شود.
    // اگر هیچ لایه‌ای باز نباشد، خود برنامه بسته می‌شود.
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
        if (!pageLoaded) return

        val uri = pendingIncomingUri ?: return

        pendingIncomingUri = null

        readImportFile(uri)
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
        showGlassShareModeDialogWithoutData()
    }

    private fun showGlassShareModeDialogWithoutData() {

        val dialog = createGlassShareDialog(
            title = "ارسال لیست قیمت"
        )

        val container = dialog.viewContainer

        container.addView(
            createShareModeButton(
                title = "👤  ویژه فروشندگان",
                subtitle = "ارسال قیمت فروش بدون اطلاعات خرید",
                accent = "#55D8FF"
            ) {

                dialog.alert.dismiss()

                webView.evaluateJavascript(
                    "window.AndroidBridge && " +
                        "window.AndroidBridge.requestShareMode && " +
                        "window.AndroidBridge.requestShareMode(\"seller\");",
                    null
                )
            }
        )

        container.addView(
            createShareModeButton(
                title = "🔐  ویژه مدیریت",
                subtitle = "ارسال لیست کامل همراه اطلاعات مدیریت",
                accent = "#8FA7FF"
            ) {

                dialog.alert.dismiss()

                webView.evaluateJavascript(
                    "window.AndroidBridge && " +
                        "window.AndroidBridge.requestShareMode && " +
                        "window.AndroidBridge.requestShareMode(\"management\");",
                    null
                )
            }
        )

        container.addView(
            createDialogCancelButton {
                dialog.alert.dismiss()
            }
        )

        dialog.alert.show()

        dialog.alert.window?.setBackgroundDrawable(
            android.graphics.drawable.ColorDrawable(
                Color.TRANSPARENT
            )
        )

        dialog.alert.window?.setLayout(
            dp(340),
            android.view.WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun showShareModeDialogForData(
        json: String,
        fileName: String
    ) {

        val dialog = createGlassShareDialog(
            title = "ارسال لیست قیمت"
        )

        val container = dialog.viewContainer

        container.addView(
            createShareModeButton(
                title = "👤  ویژه فروشندگان",
                subtitle = "فقط قیمت فروش برای فروشندگان",
                accent = "#55D8FF"
            ) {

                dialog.alert.dismiss()

                sharePriceListFile(
                    json,
                    fileName,
                    "ویژه فروشندگان"
                )
            }
        )

        container.addView(
            createShareModeButton(
                title = "🔐  ویژه مدیریت",
                subtitle = "لیست کامل برای مدیریت",
                accent = "#8FA7FF"
            ) {

                dialog.alert.dismiss()

                sharePriceListFile(
                    json,
                    fileName,
                    "ویژه مدیریت"
                )
            }
        )

        container.addView(
            createDialogCancelButton {
                dialog.alert.dismiss()
            }
        )

        dialog.alert.show()

        dialog.alert.window?.setBackgroundDrawable(
            android.graphics.drawable.ColorDrawable(
                Color.TRANSPARENT
            )
        )

        dialog.alert.window?.setLayout(
            dp(340),
            android.view.WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private data class GlassDialogParts(
        val alert: AlertDialog,
        val viewContainer: LinearLayout
    )

    private fun createGlassShareDialog(
        title: String
    ): GlassDialogParts {

        val root = LinearLayout(this).apply {

            orientation = LinearLayout.VERTICAL

            gravity = Gravity.CENTER_HORIZONTAL

            setPadding(
                dp(20),
                dp(20),
                dp(20),
                dp(16)
            )

            background = roundedBackground(
                "#F018202D",
                "#4058D5FF",
                26f,
                1
            )
        }

        val titleView = TextView(this).apply {

            text = title

            gravity = Gravity.CENTER

            setTextColor(
                Color.parseColor("#F5FAFF")
            )

            setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                20f
            )

            setTypeface(
                null,
                android.graphics.Typeface.BOLD
            )

            setPadding(
                0,
                dp(2),
                0,
                dp(16)
            )
        }

        root.addView(
            titleView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val subtitle = TextView(this).apply {

            text = "نوع لیست موردنظر را انتخاب کنید"

            gravity = Gravity.CENTER

            setTextColor(
                Color.parseColor("#AFC1D8")
            )

            setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                13f
            )

            setPadding(
                0,
                0,
                0,
                dp(14)
            )
        }

        root.addView(
            subtitle,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val options = LinearLayout(this).apply {

            orientation = LinearLayout.VERTICAL

            gravity = Gravity.CENTER_HORIZONTAL
        }

        root.addView(
            options,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val alert = AlertDialog.Builder(this)
            .setView(root)
            .create()

        alert.setOnShowListener {

            alert.window?.setBackgroundDrawable(
                android.graphics.drawable.ColorDrawable(
                    Color.TRANSPARENT
                )
            )

            alert.window?.setLayout(
                dp(340),
                android.view.WindowManager.LayoutParams.WRAP_CONTENT
            )
        }

        return GlassDialogParts(
            alert = alert,
            viewContainer = options
        )
    }

    private fun createShareModeButton(
        title: String,
        subtitle: String,
        accent: String,
        onClick: () -> Unit
    ): TextView {

        val box = TextView(this).apply {

            text = "$title\n$subtitle"

            gravity = Gravity.CENTER_VERTICAL

            setTextColor(
                Color.parseColor("#F5FAFF")
            )

            setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                15f
            )

            setTypeface(
                null,
                android.graphics.Typeface.BOLD
            )

            setPadding(
                dp(16),
                dp(15),
                dp(16),
                dp(15)
            )

            isClickable = true
            isFocusable = true

            background = roundedBackground(
                "#CC111925",
                accent,
                18f,
                1
            )

            setOnClickListener {
                onClick()
            }
        }

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        params.setMargins(
            0,
            dp(4),
            0,
            dp(4)
        )

        box.layoutParams = params

        return box
    }

    private fun createDialogCancelButton(
        onClick: () -> Unit
    ): TextView {

        return TextView(this).apply {

            text = "انصراف"

            gravity = Gravity.CENTER

            setTextColor(
                Color.parseColor("#AFC1D8")
            )

            setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                14f
            )

            setTypeface(
                null,
                android.graphics.Typeface.BOLD
            )

            setPadding(
                dp(10),
                dp(14),
                dp(10),
                dp(6)
            )

            isClickable = true
            isFocusable = true

            setOnClickListener {
                onClick()
            }
        }
    }

    private fun roundedBackground(
        fillColor: String,
        strokeColor: String,
        radiusDp: Float,
        strokeWidthDp: Int
    ): GradientDrawable {

        return GradientDrawable().apply {

            shape = GradientDrawable.RECTANGLE

            cornerRadius =
                dp(radiusDp).toFloat()

            setColor(
                Color.parseColor(fillColor)
            )

            setStroke(
                dp(strokeWidthDp),
                Color.parseColor(strokeColor)
            )
        }
    }

    private fun dp(value: Int): Int {

        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    private fun dp(value: Float): Int {

        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value,
            resources.displayMetrics
        ).toInt()
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
        fun requestShareMode(mode: String) {

            webView.post {

                val rawJson =
                    getSharedPreferences(
                        "priceyar",
                        MODE_PRIVATE
                    )
                        .getString(
                            "data",
                            ""
                        ) ?: ""

                if (rawJson.isBlank()) {
                    return@post
                }

                val json = try {

                    val rawRoot =
                        org.json.JSONObject(
                            rawJson
                        )

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
                        rawRoot.optJSONArray(
                            "stores"
                        ) ?: org.json.JSONArray()
                    )

                    packageRoot.put(
                        "deleted",
                        rawRoot.optJSONObject(
                            "deleted"
                        ) ?: org.json.JSONObject()
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
                Intent(
                    Intent.ACTION_OPEN_DOCUMENT
                ).apply {

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
                root.optJSONArray(
                    "stores"
                )

            if (stores != null) {

                for (i in 0 until stores.length()) {

                    val store =
                        stores.optJSONObject(i)
                            ?: continue

                    val items =
                        store.optJSONArray(
                            "items"
                        ) ?: continue

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
                File(
                    cacheDir,
                    "shared"
                )

            if (!sharedDirectory.exists()) {
                sharedDirectory.mkdirs()
            }

            val safeFileName =
                sanitizeFileName(
                    fileName
                )

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
                Intent(
                    Intent.ACTION_SEND
                ).apply {

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

            startActivity(
                chooser
            )

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
                        input.readBytes()
                            .toString(
                                Charsets.UTF_8
                            )
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
                org.json.JSONObject.quote(
                    text
                )

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
        PriceWidgetProvider.updateWidget(
            this
        )
    }
}
