package com.example.m3uiptv

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.m3uiptv.databinding.ItemChannelTvBinding

class ChannelAdapter(
    private var items: List<Channel>,
    private val isSelected: (Channel) -> Boolean,
    private val onFocused: (Channel) -> Unit,
    private val onClicked: (Channel) -> Unit
) : RecyclerView.Adapter<ChannelAdapter.ChannelVH>() {

    inner class ChannelVH(val binding: ItemChannelTvBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelVH {
        val binding = ItemChannelTvBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChannelVH(binding)
    }

    override fun onBindViewHolder(holder: ChannelVH, position: Int) {
        val channel = items[position]
        val b = holder.binding

        b.channelName.text = channel.name
        b.channelGroup.text = channel.group

        fun paint(view: View, focused: Boolean) {
            when {
                focused -> {
                    view.setBackgroundColor(Color.parseColor("#2A3C63"))
                    view.scaleX = 1.04f
                    view.scaleY = 1.04f
                }
                isSelected(channel) -> {
                    view.setBackgroundColor(Color.parseColor("#1E2E4D"))
                    view.scaleX = 1f
                    view.scaleY = 1f
                }
                else -> {
                    view.setBackgroundColor(Color.TRANSPARENT)
                    view.scaleX = 1f
                    view.scaleY = 1f
                }
            }
        }

        paint(b.root, b.root.hasFocus())

        b.root.setOnFocusChangeListener { v, hasFocus ->
            paint(v, hasFocus)
            if (hasFocus) onFocused(channel)
        }

        b.root.setOnClickListener {
            onClicked(channel)
        }
    }

    override fun getItemCount(): Int = items.size

    fun update(newItems: List<Channel>) {
        items = newItems
        notifyDataSetChanged()
    }
}
