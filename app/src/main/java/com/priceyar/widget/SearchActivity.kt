package com.priceyar.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.Color
import android.view.Gravity

class SearchActivity : Activity() {

    private lateinit var input: EditText
    private lateinit var result: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(30, 40, 30, 30)
        root.setBackgroundColor(Color.BLACK)

        input = EditText(this)
        input.hint = "نام کالا را جستجو کن"
        input.setHintTextColor(Color.GRAY)
        input.setTextColor(Color.WHITE)
        input.setTextSize(18f)
        input.setSingleLine(true)

        result = TextView(this)
        result.setTextColor(Color.WHITE)
        result.setTextSize(22f)
        result.gravity = Gravity.CENTER
        result.setPadding(10, 40, 10, 40)

        root.addView(
            input,
            LinearLayout.LayoutParams(
                -1,
                60
            )
        )

        root.addView(
            result,
            LinearLayout.LayoutParams(
                -1,
                -2
            )
        )

        setContentView(root)

        input.requestFocus()

        input.setOnEditorActionListener { _, _, _ ->
            search()
            true
        }

        input.setOnKeyListener { _, keyCode, event ->
            if (keyCode == android.view.KeyEvent.KEYCODE_ENTER &&
                event.action == android.view.KeyEvent.ACTION_UP) {
                search()
                true
            } else {
                false
            }
        }
    }

    private fun search() {

        val q = input.text.toString().trim()

        if (q.isEmpty()) {
            result.text = "نام کالا را وارد کن"
            return
        }

        val json = getSharedPreferences(
            "priceyar",
            MODE_PRIVATE
        ).getString("data", "") ?: ""

        if (json.isEmpty()) {
            result.text = "هنوز اطلاعاتی ذخیره نشده"
            return
        }

        try {

            val stores =
                org.json.JSONObject(json)
                    .getJSONArray("stores")

            val active =
                org.json.JSONObject(json)
                    .getInt("active")

            val items =
                stores
                    .getJSONObject(active)
                    .getJSONArray("items")

            var found = false

            for (i in 0 until items.length()) {

                val item = items.getJSONObject(i)

                val name =
                    item.optString("name")

                if (name.contains(q, true)) {

                    val price =
                        item.optString("price")

                    result.text =
                        "$name\n\n$price"

                    found = true

                    updateWidget(
                        name,
                        price
                    )

                    break
                }
            }

            if (!found) {
                result.text = "کالایی پیدا نشد"
            }

        } catch (_: Exception) {

            result.text =
                "خطا در خواندن اطلاعات"
        }
    }

    private fun updateWidget(
        name: String,
        price: String
    ) {

        val manager =
            AppWidgetManager.getInstance(this)

        val component =
            ComponentName(
                this,
                PriceWidgetProvider::class.java
            )

        val ids =
            manager.getAppWidgetIds(component)

        for (id in ids) {

            PriceWidgetProvider
                .showResult(
                    this,
                    manager,
                    id,
                    name,
                    price
                )
        }
    }
}
