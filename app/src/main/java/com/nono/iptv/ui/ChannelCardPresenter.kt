package com.nono.iptv.ui

import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import com.bumptech.glide.Glide
import com.nono.iptv.R
import com.nono.iptv.data.ChannelEntity

/**
 * Renders a [ChannelEntity] as a Leanback [ImageCardView] with its logo, name,
 * and channel number. Logos are loaded off the main thread by Glide.
 */
class ChannelCardPresenter : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val cardView = object : ImageCardView(parent.context) {}.apply {
            isFocusable = true
            isFocusableInTouchMode = true
            setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)
        }
        return ViewHolder(cardView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val channel = item as ChannelEntity
        val card = viewHolder.view as ImageCardView
        card.titleText = channel.name
        card.contentText = buildString {
            append("#").append(channel.channelNumber)
            if (channel.isFavorite) append("  ★")
        }

        val placeholder = ContextCompat.getDrawable(card.context, R.drawable.default_logo)
        card.mainImageView?.let { imageView ->
            if (!channel.logoUrl.isNullOrBlank()) {
                Glide.with(card.context)
                    .load(channel.logoUrl)
                    .placeholder(placeholder)
                    .error(placeholder)
                    .centerInside()
                    .into(imageView)
            } else {
                imageView.setImageDrawable(placeholder)
            }
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val card = viewHolder.view as ImageCardView
        card.mainImageView?.let { Glide.with(card.context).clear(it) }
        card.mainImage = null
    }

    companion object {
        private const val CARD_WIDTH = 320
        private const val CARD_HEIGHT = 180
    }
}
