package com.priceyar.widget

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class WidgetShareActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rawJson = getSharedPreferences(
            "priceyar",
            MODE_PRIVATE
        ).getString("data", "") ?: ""

        if (rawJson.isBlank()) {
            showMessage("اطلاعاتی برای ارسال وجود ندارد")
            return
        }

        try {
            JSONObject(rawJson)
        } catch (_: Exception) {
            showMessage("اطلاعات لیست معتبر نیست")
            return
        }

        AlertDialog.Builder(this)
            .setTitle("ارسال لیست قیمت")
            .setItems(
                arrayOf(
                    "ویژه فروشندگان",
                    "ویژه مدیریت"
                )
            ) { _, which ->
                when (which) {
                    0 -> shareList(rawJson, "seller")
                    1 -> shareList(rawJson, "management")
                    else -> finish()
                }
            }
            .setOnCancelListener {
                finish()
            }
            .show()
    }

    private fun showMessage(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()

        finish()
    }

    private fun shareList(
        rawJson: String,
        mode: String
    ) {
        try {
            val root = JSONObject(rawJson)

            val packageRoot = JSONObject().apply {
                put("type", "priceyar-price-list")
                put("version", 1)
                put("createdAt", System.currentTimeMillis())
                put("shareMode", mode)

                put(
                    "stores",
                    root.optJSONArray("stores") ?: JSONArray()
                )

                put(
                    "deleted",
                    root.optJSONObject("deleted") ?: JSONObject()
                )
            }

            // حذف قیمت خرید برای نسخه ویژه فروشندگان
            if (mode == "seller") {
                val stores = packageRoot.optJSONArray("stores")

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
            }

            val sharedDirectory = File(cacheDir, "shared")

            if (!sharedDirectory.exists() &&
                !sharedDirectory.mkdirs()
            ) {
                throw IllegalStateException(
                    "ساخت پوشه ارسال ناموفق بود"
                )
            }

            val file = File(
                sharedDirectory,
                "PriceYar-PriceList.json"
            )

            file.writeText(
                packageRoot.toString(2),
                Charsets.UTF_8
            )

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
                    if (mode == "seller") {
                        "لیست قیمت - ویژه فروشندگان"
                    } else {
                        "لیست قیمت - ویژه مدیریت"
                    }
                )

                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                clipData = ClipData.newRawUri(
                    "PriceYar",
                    uri
                )
            }

            val chooser = Intent.createChooser(
                sendIntent,
                "ارسال لیست قیمت با"
            )

            startActivity(chooser)
            finish()

        } catch (_: Exception) {
            showMessage("ارسال لیست انجام نشد")
        }
    }
}
