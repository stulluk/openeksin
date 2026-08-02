package com.drejo.openeksin.ui.topic

import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.drejo.openeksin.R
import com.drejo.openeksin.data.model.Topic
import com.drejo.openeksin.ui.theme.LocalEkColors
import com.drejo.openeksin.ui.theme.TextSizes

private class TopicAdapter(
    private val onClick: (Topic) -> Unit,
) : ListAdapter<Topic, TopicAdapter.Holder>(Diff) {

    var rankBadgeColor: Int = 0
    var rankBadgeTextColor: Int = 0
    var titleColor: Int = 0
    var dividerColor: Int = 0
    var titleSizeSp: Float = 14f
    var countSizeSp: Float = 12f

    object Diff : DiffUtil.ItemCallback<Topic>() {
        override fun areItemsTheSame(a: Topic, b: Topic) = a.link == b.link
        override fun areContentsTheSame(a: Topic, b: Topic) = a == b
    }

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val count: TextView = view.findViewById(R.id.topic_count)
        val title: TextView = view.findViewById(R.id.topic_title)
        val divider: View = view.findViewById(R.id.topic_divider)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_topic, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val topic = getItem(position)
        holder.title.text = topic.title
        holder.title.setTextColor(titleColor)
        holder.title.textSize = titleSizeSp
        holder.title.setTypeface(holder.title.typeface, Typeface.BOLD)
        holder.divider.setBackgroundColor(dividerColor)
        if (topic.entryCount.isEmpty()) {
            holder.count.visibility = View.GONE
        } else {
            holder.count.visibility = View.VISIBLE
            holder.count.text = topic.entryCount
            holder.count.textSize = countSizeSp
            holder.count.setTypeface(holder.count.typeface, Typeface.BOLD)
            holder.count.setTextColor(rankBadgeTextColor)
            holder.count.background = GradientDrawable().apply {
                cornerRadius = holder.itemView.resources.displayMetrics.density * 4f
                setColor(rankBadgeColor)
            }
        }
        holder.itemView.setOnClickListener { onClick(topic) }
    }
}

@Composable
fun NativeTopicList(
    topics: List<Topic>,
    onTopicClick: (Topic) -> Unit,
    onNearEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ek = LocalEkColors.current
    val rankBadge = ek.rankBadge.toArgb()
    val rankBadgeText = ek.rankBadgeText.toArgb()
    val titleColor = ek.mainText.toArgb()
    val dividerColor = ek.divider.toArgb()
    val titleSizeSp = TextSizes.TopicTitle.value
    val countSizeSp = TextSizes.TopicCount.value
    val nearEndCallback = remember(onNearEnd) { onNearEnd }

    AndroidView(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds(),
        factory = { ctx ->
            val adapter = TopicAdapter(onTopicClick)
            RecyclerView(ctx).apply {
                layoutManager = LinearLayoutManager(ctx)
                setHasFixedSize(true)
                itemAnimator = null
                clipToPadding = true
                clipChildren = true
                this.adapter = adapter
                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        if (dy <= 0) return
                        val lm = recyclerView.layoutManager as LinearLayoutManager
                        val last = lm.findLastVisibleItemPosition()
                        val total = lm.itemCount
                        if (total > 0 && last >= total - 3) {
                            nearEndCallback()
                        }
                    }
                })
            }
        },
        update = { rv ->
            val adapter = rv.adapter as TopicAdapter
            adapter.rankBadgeColor = rankBadge
            adapter.rankBadgeTextColor = rankBadgeText
            adapter.titleColor = titleColor
            adapter.dividerColor = dividerColor
            adapter.titleSizeSp = titleSizeSp
            adapter.countSizeSp = countSizeSp
            adapter.submitList(topics.toList())
        },
    )
}
