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

    private val backgroundColor = Color.rgb(32, 33, 36)
    private val searchColor = Color.rgb(23, 23, 23)
    private val cardColor = Color.rgb(48, 49, 52)
    private val textColor = Color.rgb(232, 234, 237)
    private val secondaryColor = Color.rgb(154, 160, 166)
    private val borderColor = Color.rgb(74, 76, 80)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = backgroundColor
        window.navigationBarColor = backgroundColor
        window.decorView.systemUiVisibility = 0

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(18), dp(20), dp(18), dp(14))
        root.setBackgroundColor(backgroundColor)

        val title = TextView(this)
        title.text = "جستجوی قیمت کالا"
        title.textSize = 22f
        title.setTextColor(textColor)
        title.typeface = Typeface.DEFAULT_BOLD
        title.gravity = Gravity.CENTER
        title.setPadding(0, dp(8), 0, dp(18))

        root.addView(
            title,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        input = EditText(this)
        input.hint = "جستجوی کالا"
        input.setHintTextColor(secondaryColor)
        input.setTextColor(textColor)
        input.textSize = 17f
        input.setSingleLine(true)
        input.setPadding(dp(18), 0, dp(18), 0)
        input.gravity = Gravity.CENTER_VERTICAL
        input.imeOptions = EditorInfo.IME_ACTION_SEARCH

        val searchBackground = GradientDrawable()
        searchBackground.setColor(searchColor)
        searchBackground.cornerRadius = dp(28).toFloat()
        searchBackground.setStroke(dp(1), borderColor)

        input.background = searchBackground

        root.addView(
            input,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(56)
            )
        )

        val scrollView = ScrollView(this)
        scrollView.isFillViewport = true

        resultsLayout = LinearLayout(this)
        resultsLayout.orientation = LinearLayout.VERTICAL

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

        input.setOnEditorActionListener { _, actionId, _ ->

            if (actionId == EditorInfo.IME_ACTION_SEARCH) {

                searchProducts(
                    input.text.toString().trim()
                )

                true

            } else {

                false
            }
        }
    }

    override fun onBackPressed() {

        val imm =
            getSystemService(
                Context.INPUT_METHOD_SERVICE
            ) as InputMethodManager

        imm.hideSoftInputFromWindow(
            input.windowToken,
            0
        )

        finish()
    }

    private fun searchProducts(query: String) {

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

        val infoMode =
            PriceWidgetProvider.isInfoModeEnabled(this)

        try {

            val rootObject =
                org.json.JSONObject(json)

            val stores =
                rootObject.optJSONArray("stores")
                    ?: return

            if (stores.length() == 0) {
                return
            }

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

            val store =
                stores.optJSONObject(
                    activeStoreIndex
                ) ?: return

            val items =
                store.optJSONArray("items")
                    ?: return

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
                    name.lowercase().contains(
                        normalizedQuery
                    )
                ) {

                    val price =
                        item.optString(
                            "price",
                            ""
                        )

                    var buyPrice =
                        item.optString(
                            "buyPrice",
                            ""
                        )

                    if (buyPrice.isBlank()) {
                        buyPrice =
                            item.optString(
                                "buy",
                                ""
                            )
                    }

                    if (buyPrice.isBlank()) {
                        buyPrice =
                            item.optString(
                                "purchasePrice",
                                ""
                            )
                    }

                    var description =
                        item.optString(
                            "description",
                            ""
                        )

                    if (description.isBlank()) {
                        description =
                            item.optString(
                                "desc",
                                ""
                            )
                    }

                    if (description.isBlank()) {
                        description =
                            item.optString(
                                "details",
                                ""
                            )
                    }

                    addResult(
                        name,
                        price,
                        buyPrice,
                        description,
                        infoMode
                    )

                    found++
                }
            }

            if (found == 0) {
                showMessage("کالایی پیدا نشد.")
            }

        } catch (_: Exception) {

            showMessage(
                "خطا در خواندن اطلاعات کالاها."
            )
        }
    }

    private fun addResult(
        name: String,
        price: String,
        buyPrice: String,
        description: String,
        infoMode: Boolean
    ) {

        val card = LinearLayout(this)

        card.orientation = LinearLayout.VERTICAL
        card.gravity = Gravity.CENTER_VERTICAL

        card.setPadding(
            dp(18),
            dp(12),
            dp(18),
            dp(12)
        )

        val background = GradientDrawable()

        background.setColor(cardColor)
        background.cornerRadius = dp(16).toFloat()
        background.setStroke(
            dp(1),
            borderColor
        )

        card.background = background

        val nameText = TextView(this)

        nameText.text = name
        nameText.textSize = 16f
        nameText.setTextColor(textColor)
        nameText.typeface = Typeface.DEFAULT_BOLD

        card.addView(
            nameText,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        if (price.isNotBlank()) {

            val priceText = TextView(this)

            priceText.text =
                "قیمت فروش: $price تومان"

            priceText.textSize = 15f
            priceText.setTextColor(textColor)

            priceText.setPadding(
                0,
                dp(4),
                0,
                0
            )

            card.addView(
                priceText,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }

        if (
            infoMode &&
            buyPrice.isNotBlank()
        ) {

            val buyPriceText = TextView(this)

            buyPriceText.text =
                "قیمت خرید: $buyPrice تومان"

            buyPriceText.textSize = 15f
            buyPriceText.setTextColor(textColor)

            buyPriceText.setPadding(
                0,
                dp(4),
                0,
                0
            )

            card.addView(
                buyPriceText,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }

        if (
            infoMode &&
            description.isNotBlank()
        ) {

            val descriptionText =
                TextView(this)

            descriptionText.text =
                description

            descriptionText.textSize = 14f
            descriptionText.setTextColor(
                secondaryColor
            )

            descriptionText.setPadding(
                0,
                dp(8),
                0,
                0
            )

            descriptionText.maxLines = 10
            descriptionText.ellipsize = null

            card.addView(
                descriptionText,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }

        val params =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
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

        card.setOnClickListener {

            updateWidget(
                name,
                price
            )

            finish()
        }
    }

    private fun showMessage(message: String) {

        val text = TextView(this)

        text.text = message
        text.textSize = 15f
        text.setTextColor(secondaryColor)
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

    private fun dp(value: Int): Int {

        return (
            value *
                resources
                    .displayMetrics
                    .density
            ).toInt()
    }
}
