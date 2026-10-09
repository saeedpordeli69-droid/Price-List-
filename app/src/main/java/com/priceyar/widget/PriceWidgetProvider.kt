val searchIntent = Intent(
    context,
    SearchActivity::class.java
).apply {
    putExtra("opened_from_widget", true)
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

val searchPending = PendingIntent.getActivity(
    context,
    id + 10,
    searchIntent,
    PendingIntent.FLAG_UPDATE_CURRENT or
        PendingIntent.FLAG_IMMUTABLE
)

views.setOnClickPendingIntent(
    R.id.search_button,
    searchPending
)
