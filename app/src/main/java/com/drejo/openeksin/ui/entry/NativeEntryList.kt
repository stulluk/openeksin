package com.drejo.openeksin.ui.entry

import android.graphics.drawable.GradientDrawable
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.drejo.openeksin.R
import com.drejo.openeksin.data.model.Entry
import com.drejo.openeksin.ui.theme.LocalEkColors
import com.drejo.openeksin.ui.theme.TextSizes
import com.drejo.openeksin.util.PerfTrace

private const val COLLAPSED_LINES = 8
private const val LOAD_MORE_THRESHOLD = 3
private const val BOUNDARY_SCROLL_THRESHOLD_PX = 72

internal class EntryAdapter(
    private val onLinkClick: (String) -> Unit,
    private val onAuthorClick: (String) -> Unit,
    private val onMenuClick: (Entry) -> Unit,
) : ListAdapter<Entry, EntryAdapter.Holder>(Diff) {

    var cardBgColor: Int = 0
    var mainTextColor: Int = 0
    var secondaryTextColor: Int = 0
    var readMoreColor: Int = 0
    var dividerColor: Int = 0
    var bodySizeSp: Float = 15f
    var metaSizeSp: Float = 14f

    private val expandedIds = mutableSetOf<String>()
    private val overflowIds = mutableSetOf<String>()

    object Diff : DiffUtil.ItemCallback<Entry>() {
        override fun areItemsTheSame(a: Entry, b: Entry) = a.id == b.id
        override fun areContentsTheSame(a: Entry, b: Entry) = a == b
    }

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val card: LinearLayout = view.findViewById(R.id.entry_card)
        val favRow: View = view.findViewById(R.id.entry_fav_row)
        val favCount: TextView = view.findViewById(R.id.entry_fav_count)
        val favDivider: View = view.findViewById(R.id.entry_fav_divider)
        val body: TextView = view.findViewById(R.id.entry_body)
        val readMoreDivider: View = view.findViewById(R.id.entry_read_more_divider)
        val readMore: TextView = view.findViewById(R.id.entry_read_more)
        val footerDivider: View = view.findViewById(R.id.entry_footer_divider)
        val author: TextView = view.findViewById(R.id.entry_author)
        val date: TextView = view.findViewById(R.id.entry_date)
        val menu: ImageButton = view.findViewById(R.id.entry_menu)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_entry, parent, false)
        return Holder(view).also { h ->
            h.body.movementMethod = LinkMovementMethod.getInstance()
            h.body.includeFontPadding = false
        }
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val entry = getItem(position)
        val expanded = entry.id in expandedIds

        holder.card.background = GradientDrawable().apply {
            cornerRadius = holder.itemView.resources.displayMetrics.density * 4f
            setColor(cardBgColor)
        }
        holder.body.setTextColor(mainTextColor)
        holder.body.textSize = bodySizeSp
        holder.author.setTextColor(mainTextColor)
        holder.author.textSize = metaSizeSp
        holder.date.setTextColor(mainTextColor)
        holder.date.textSize = metaSizeSp
        holder.readMore.setTextColor(readMoreColor)
        holder.readMore.textSize = metaSizeSp
        holder.favCount.setTextColor(secondaryTextColor)
        holder.favCount.textSize = metaSizeSp
        holder.favDivider.setBackgroundColor(dividerColor)
        holder.readMoreDivider.setBackgroundColor(dividerColor)
        holder.footerDivider.setBackgroundColor(dividerColor)

        if (entry.favoriteCount.isNotEmpty() && entry.favoriteCount != "0") {
            holder.favRow.visibility = View.VISIBLE
            holder.favDivider.visibility = View.VISIBLE
            holder.favCount.text = entry.favoriteCount
        } else {
            holder.favRow.visibility = View.GONE
            holder.favDivider.visibility = View.GONE
        }

        holder.body.bindEntryBody(
            entry = entry,
            expanded = expanded,
            collapsedLines = COLLAPSED_LINES,
            onLinkClick = onLinkClick,
            onOverflow = {
                if (overflowIds.add(entry.id)) {
                    notifyItemChanged(position)
                }
            },
        )

        val showReadMore = !expanded && (entry.id in overflowIds || entry.content.length > 280)
        holder.readMore.visibility = if (showReadMore) View.VISIBLE else View.GONE
        holder.readMoreDivider.visibility = holder.readMore.visibility
        holder.readMore.setOnClickListener {
            expandedIds.add(entry.id)
            notifyItemChanged(position)
        }

        holder.author.text = entry.author
        holder.date.text = entry.date
        holder.author.setOnClickListener { onAuthorClick(entry.author) }
        holder.menu.setColorFilter(mainTextColor)
        holder.menu.setOnClickListener { onMenuClick(entry) }
    }
}

@Composable
internal fun NativeEntryList(
    entries: List<Entry>,
    topicLink: String,
    scrollPage: Int,
    canGoPrev: Boolean,
    canGoNext: Boolean,
    onNearEnd: () -> Unit,
    onRequestPrevPage: () -> Unit,
    onRequestNextPage: () -> Unit,
    onLinkClick: (String) -> Unit,
    onAuthorClick: (String) -> Unit,
    onMenuClick: (Entry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ek = LocalEkColors.current
    val cardBgArgb = ek.entryCardBg.toArgb()
    val mainText = ek.mainText.toArgb()
    val secondary = ek.secondaryText.toArgb()
    val readMore = ek.readMore.toArgb()
    val divider = ek.divider.toArgb()
    val bodySp = TextSizes.EntryBody.value
    val metaSp = TextSizes.EntryAuthor.value
    val nearEnd = remember(onNearEnd) { onNearEnd }
    val prevPage = remember(onRequestPrevPage) { onRequestPrevPage }
    val nextPage = remember(onRequestNextPage) { onRequestNextPage }
    val linkClick = remember(onLinkClick) { onLinkClick }
    val authorClick = remember(onAuthorClick) { onAuthorClick }
    val menuClick = remember(onMenuClick) { onMenuClick }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            val adapter = EntryAdapter(linkClick, authorClick, menuClick)
            var boundaryPullTop = 0
            var boundaryPullBottom = 0
            RecyclerView(ctx).apply {
                layoutManager = LinearLayoutManager(ctx)
                setHasFixedSize(false)
                itemAnimator = null
                overScrollMode = View.OVER_SCROLL_NEVER
                this.adapter = adapter
                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                        val canPrev = rv.getTag(R.id.tag_can_go_prev) as? Boolean ?: false
                        val canNext = rv.getTag(R.id.tag_can_go_next) as? Boolean ?: false
                        val onPrev = rv.getTag(R.id.tag_on_prev) as? () -> Unit
                        val onNext = rv.getTag(R.id.tag_on_next) as? () -> Unit
                        if (dy < 0 && !rv.canScrollVertically(-1) && canPrev) {
                            boundaryPullTop -= dy
                            if (boundaryPullTop >= BOUNDARY_SCROLL_THRESHOLD_PX) {
                                boundaryPullTop = 0
                                onPrev?.invoke()
                            }
                        } else if (dy < 0) {
                            boundaryPullTop = 0
                        }
                        if (dy > 0 && !rv.canScrollVertically(1) && canNext) {
                            boundaryPullBottom += dy
                            if (boundaryPullBottom >= BOUNDARY_SCROLL_THRESHOLD_PX) {
                                boundaryPullBottom = 0
                                onNext?.invoke()
                            }
                        } else if (dy > 0) {
                            boundaryPullBottom = 0
                        }
                        val lm = rv.layoutManager as LinearLayoutManager
                        val last = lm.findLastVisibleItemPosition()
                        val total = lm.itemCount
                        if (total > 0 && last >= total - LOAD_MORE_THRESHOLD) {
                            PerfTrace.logScroll("near_end", "last=$last total=$total")
                            (rv.getTag(R.id.tag_on_near_end) as? () -> Unit)?.invoke()
                        }
                    }
                })
            }
        },
        update = { rv ->
            rv.setTag(R.id.tag_on_near_end, nearEnd)
            rv.setTag(R.id.tag_can_go_prev, canGoPrev)
            rv.setTag(R.id.tag_can_go_next, canGoNext)
            rv.setTag(R.id.tag_on_prev, prevPage)
            rv.setTag(R.id.tag_on_next, nextPage)
            val adapter = rv.adapter as EntryAdapter
            adapter.cardBgColor = cardBgArgb
            adapter.mainTextColor = mainText
            adapter.secondaryTextColor = secondary
            adapter.readMoreColor = readMore
            adapter.dividerColor = divider
            adapter.bodySizeSp = bodySp
            adapter.metaSizeSp = metaSp
            // Copy the list: submitList skips updates when the same List instance is reused
            // (mutableStateListOf is mutated in place via addAll).
            adapter.submitList(entries.toList()) {
                if (entries.isNotEmpty()) {
                    PerfTrace.markEntryListVisible(topicLink, scrollPage, entries.size)
                }
                val lm = rv.layoutManager as LinearLayoutManager
                val last = lm.findLastVisibleItemPosition()
                val total = lm.itemCount
                if (total > 0 && last >= total - LOAD_MORE_THRESHOLD) {
                    (rv.getTag(R.id.tag_on_near_end) as? () -> Unit)?.invoke()
                }
            }
            val lastPage = rv.getTag(R.id.tag_scroll_page) as? Int
            if (lastPage != scrollPage) {
                rv.setTag(R.id.tag_scroll_page, scrollPage)
                rv.scrollToPosition(0)
            }
        },
    )
}
