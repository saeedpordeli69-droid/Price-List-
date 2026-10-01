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

class SearchActivity : Activity() {

    private lateinit var input: EditText
    private lateinit var resultsLayout: LinearLayout

    // Chrome Dark Mode style
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

        // کادر جستجو
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

        input.imeOptions =
            EditorInfo.IME_ACTION_SEARCH

        val searchBackground =
            GradientDrawable()

        // مشکی‌تر از صفحه
        searchBackground.setColor(
            searchColor
        )

        searchBackground.cornerRadius =
            dp(28).toFloat()

        searchBackground.setStroke(
            dp(1),
            borderColor
        )

        input.background =
            searchBackground

        root.addView(
            input,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(56)
            )
        )

        // توضیح کوچک
        val subtitle = TextView(this)

        subtitle.text =
            "نام کالا را وارد کنید تا قیمت آن نمایش داده شود."

        subtitle.textSize = 14f

        subtitle.setTextColor(
            secondaryColor
        )

        subtitle.setPadding(
            dp(8),
            dp(14),
            dp(8),
            dp(12)
        )

        root.addView(
            subtitle,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        // نتایج
        val scrollView =
            ScrollView(this)

        scrollView.isFillViewport =
            true

        resultsLayout =
            LinearLayout(this)

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

        showMessage(
            "برای جستجو، نام کالا را وارد کنید."
        )

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

        // جستجوی لحظه‌ای
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
                    search(
                        s.toString()
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

                val imm =
                    getSystemService(
                        Context.INPUT_METHOD_SERVICE
                    ) as InputMethodManager

                imm.hideSoftInputFromWindow(
                    input.windowToken,
                    0
                )

                search(
                    input.text.toString()
                )

                true

            } else {

                false
            }
        }
    }

    private fun search(
        query: String
    ) {

        val q =
            query.trim()

        if (q.isEmpty()) {

            showMessage(
                "برای جستجو، نام کالا را وارد کنید."
            )

            return
        }

        val json =
            getSharedPreferences(
                "priceyar",
                MODE_PRIVATE
            ).getString(
                "data",
                ""
            ) ?: ""

        if (json.isEmpty()) {

            showMessage(
                "هنوز اطلاعاتی ذخیره نشده است."
            )

            return
        }

        try {

            val data =
                org.json.JSONObject(json)

            val stores =
                data.optJSONArray(
                    "stores"
                )

            if (
                stores == null ||
                stores.length() == 0
            ) {

                showMessage(
                    "فروشگاهی برای جستجو وجود ندارد."
                )

                return
            }

            val active =
                data.optInt(
                    "active",
                    0
                )

            if (
                active < 0 ||
                active >= stores.length()
            ) {

                showMessage(
                    "فروشگاه انتخاب‌شده معتبر نیست."
                )

                return
            }

            val store =
                stores.getJSONObject(
                    active
                )

            val items =
                store.optJSONArray(
                    "items"
                )

            if (
                items == null ||
                items.length() == 0
            ) {

                showMessage(
                    "در این فروشگاه کالایی ثبت نشده است."
                )

                return
            }

            resultsLayout.removeAllViews()

            var found = 0

            for (
                i in 0 until items.length()
            ) {

                val item =
                    items.optJSONObject(i)
                        ?: continue

                val name =
                    item.optString(
                        "name",
                        ""
                    )

                if (
                    name.contains(
                        q,
                        ignoreCase = true
                    )
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
                    "کالایی با این نام پیدا نشد."
                )
            }

        } catch (_: Exception) {

            showMessage(
                "خطا در خواندن اطلاعات کالاها."
            )
        }
    }

    private fun addResult(
        name: String,
        price: String
    ) {

        val card =
            LinearLayout(this)

        card.orientation =
            LinearLayout.VERTICAL

        card.setPadding(
            dp(18),
            dp(15),
            dp(18),
            dp(15)
        )

        val cardBackground =
            GradientDrawable()

        cardBackground.setColor(
            cardColor
        )

        cardBackground.cornerRadius =
            dp(16).toFloat()

        cardBackground.setStroke(
            dp(1),
            borderColor
        )

        card.background =
            cardBackground

        val nameView =
            TextView(this)

        nameView.text =
            name

        nameView.textSize =
            18f

        nameView.setTextColor(
            textColor
        )

        nameView.typeface =
            Typeface.DEFAULT_BOLD

        val priceView =
            TextView(this)

        priceView.text =
            if (price.isNotBlank()) {
                "$price تومان"
            } else {
                "قیمت ثبت نشده"
            }

        priceView.textSize =
            16f

        priceView.setTextColor(
            accentColor
        )

        priceView.setPadding(
            0,
            dp(8),
            0,
            0
        )

        card.addView(
            nameView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        card.addView(
            priceView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        card.setOnClickListener {

            updateWidget(
                name,
                price
            )

            finish()
        }

        val params =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        params.bottomMargin =
            dp(10)

        resultsLayout.addView(
            card,
            params
        )
    }

    private fun showMessage(
        message: String
    ) {

        resultsLayout.removeAllViews()

        val messageView =
            TextView(this)

        messageView.text =
            message

        messageView.textSize =
            16f

        messageView.setTextColor(
            secondaryColor
        )

        messageView.gravity =
            Gravity.CENTER

        messageView.setPadding(
            dp(12),
            dp(30),
            dp(12),
            dp(30)
        )

        resultsLayout.addView(
            messageView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
    }

    private fun updateWidget(
        name: String,
        price: String
    ) {

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
