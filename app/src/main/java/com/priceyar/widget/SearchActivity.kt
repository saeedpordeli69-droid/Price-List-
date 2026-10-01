package com.priceyar.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class SearchActivity : Activity {

    private lateinit var input: EditText
    private lateinit var resultsLayout: LinearLayout

    // رنگ‌بندی حالت تاریک
    private val backgroundColor = Color.rgb(32, 33, 36)
    private val searchColor = Color.rgb(23, 23, 23)
    private val cardColor = Color.rgb(48, 49, 52)

    private val textColor = Color.rgb(232, 234, 237)
    private val secondaryColor = Color.rgb(154, 160, 166)
    private val borderColor = Color.rgb(74, 76, 80)
    private val accentColor = Color.rgb(138, 180, 248)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = backgroundColor
        window.navigationBarColor = backgroundColor
        window.decorView.systemUiVisibility = 0

        // صفحه اصلی
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL

        root.setPadding(
            dp(18),
            dp(20),
            dp(18),
            dp(14)
        )

        root.setBackgroundColor(backgroundColor)

        // عنوان
        val title = TextView(this)

        title.text = "جستجوی قیمت کالا"
        title.textSize = 22f
        title.setTextColor(textColor)
        title.typeface = Typeface.DEFAULT_BOLD
        title.gravity = Gravity.CENTER

        title.setPadding(
            0,
            dp(8),
            0,
            dp(18)
        )

        root.addView(
            title,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        // نوار جستجو
        input = EditText(this)

        input.hint = "جستجوی کالا"
        input.setHintTextColor(secondaryColor)
        input.setTextColor(textColor)
        input.textSize = 17f

        input.setSingleLine(true)

        input.setPadding(
            dp(18),
            0,
            dp(18),
            0
        )

        input.gravity = Gravity.CENTER_VERTICAL
        input.imeOptions = EditorInfo.IME_ACTION_SEARCH

        val searchBackground = GradientDrawable()

        searchBackground.setColor(searchColor)

        searchBackground.cornerRadius =
            dp(28).toFloat()

        searchBackground.setStroke(
            dp(1),
            borderColor
        )

        input.background = searchBackground

        root.addView(
            input,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(56)
            )
        )

        // فضای نتایج
        // هیچ متن راهنمایی در ابتدا نمایش داده نمی‌شود.
        val scrollView = ScrollView(this)

        scrollView.isFillViewport = true

        resultsLayout = LinearLayout(this)

        resultsLayout.orientation =
            LinearLayout.VERTICAL

        scrollView.addView(
            resultsLayout,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        root.addView(
            scrollView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(root)

        // باز کردن خودکار کیبورد
        input.requestFocus()

        input.postDelayed({

            val imm =
                getSystemService(
                    Context.INPUT_METHOD_SERVICE
                ) as InputMethodManager

            imm.showSoftInput(
                input,
                InputMethodManager.SHOW_IMPLICIT
            )

        }, 250)

        // جستجو هنگام تایپ
        input.addTextChangedListener(
            object : TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {

                    searchProducts(
                        s?.toString()?.trim() ?: ""
                    )
                }

                override fun afterTextChanged(
                    s: Editable?
                ) {
                }
            }
        )

        // دکمه Search کیبورد
        input.setOnEditorActionListener {
                _,
                actionId,
                _ ->

            if (
                actionId ==
                EditorInfo.IME_ACTION_SEARCH
            ) {

                searchProducts(
                    input.text.toString().trim()
                )

                true

            } else {

                false
            }
        }
    }

    /**
     * جستجوی کالاها
     */
    private fun searchProducts(
        query: String
    ) {

        resultsLayout.removeAllViews()

        if (query.isBlank()) {
            return
        }

        val prefs =
            getSharedPreferences(
                "priceyar",
                Context.MODE_PRIVATE
            )

        val json =
            prefs.getString(
                "data",
                null
            )

        if (json.isNullOrBlank()) {
            return
        }

        try {

            val rootObject =
                org.json.JSONObject(json)

            val stores =
                rootObject.optJSONArray(
                    "stores"
                ) ?: return

            var activeStoreIndex =
                rootObject.optInt(
                    "activeStore",
                    0
                )

            if (
                activeStoreIndex < 0 ||
                activeStoreIndex >= stores.length()
            ) {
                activeStoreIndex = 0
            }

            if (stores.length() == 0) {
                return
            }

            val store =
                stores.optJSONObject(
                    activeStoreIndex
                ) ?: return

            val items =
                store.optJSONArray(
                    "items"
                ) ?: return

            val normalizedQuery =
                query.trim().lowercase()

            var found = 0

            for (i in 0 until items.length()) {

                val item =
                    items.optJSONObject(i)
                        ?: continue

                val name =
                    item.optString(
                        "name",
                        ""
                    )

                if (name.isBlank()) {
                    continue
                }

                if (
                    name.lowercase()
                        .contains(normalizedQuery)
                ) {

                    val price =
                        item.optString(
                            "price",
                            ""
                        )

                    addResult(
                        name,
                        price
                    )

                    found++
                }
            }

            if (found == 0) {

                showMessage(
                    "کالایی پیدا نشد."
                )
            }

        } catch (_: Exception) {

            showMessage(
                "خطا در خواندن اطلاعات کالاها."
            )
        }
    }

    /**
     * اضافه کردن نتیجه جستجو
     */
    private fun addResult(
        name: String,
        price: String
    ) {

        val card = TextView(this)

        val resultText =
            if (price.isNotBlank()) {

                "$name  •  $price تومان"

            } else {

                name
            }

        card.text = resultText
        card.textSize = 16f
        card.setTextColor(textColor)
        card.gravity = Gravity.CENTER_VERTICAL

        card.setPadding(
            dp(18),
            dp(16),
            dp(18),
            dp(16)
        )

        val background =
            GradientDrawable()

        background.setColor(cardColor)

        background.cornerRadius =
            dp(16).toFloat()

        background.setStroke(
            dp(1),
            borderColor
        )

        card.background = background

        val params =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(58)
            )

        params.setMargins(
            0,
            dp(8),
            0,
            0
        )

        resultsLayout.addView(
            card,
            params
        )

        // انتخاب کالا
        card.setOnClickListener {

            updateWidget(
                name,
                price
            )

            finish()
        }
    }

    /**
     * نمایش پیام فقط در صورت نیاز
     */
    private fun showMessage(
        message: String
    ) {

        val text =
            TextView(this)

        text.text = message
        text.textSize = 15f
        text.setTextColor(
            secondaryColor
        )

        text.gravity = Gravity.CENTER

        text.setPadding(
            dp(10),
            dp(30),
            dp(10),
            dp(10)
        )

        resultsLayout.addView(
            text,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
    }

    /**
     * نمایش نتیجه انتخاب‌شده روی ویجت
     */
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
            manager.getAppWidgetIds(
                component
            )

        for (id in ids) {

            PriceWidgetProvider.showResult(
                this,
                manager,
                id,
                name,
                price
            )
        }
    }

    private fun dp(
        value: Int
    ): Int {

        return (
            value *
                resources.displayMetrics.density
            ).toInt()
    }
}

این نسخه دو نوشته اضافی را ندارد و هنگام باز شدن صفحه فقط عنوان و نوار جستجو را نشان می‌دهد.
