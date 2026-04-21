package com.example.m3uiptv

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.m3uiptv.databinding.ItemChannelBinding

class ChannelAdapter(
    private var items: List<Channel>,
    private val favoritesProvider: () -> Set<String>,
    private val onClick: (Channel) -> Unit,
    private val onToggleFavorite: (Channel) -> Unit
) : RecyclerView.Adapter<ChannelAdapter.ChannelVH>() {

    inner class ChannelVH(val binding: ItemChannelBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelVH {
        val binding = ItemChannelBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChannelVH(binding)
    }

    override fun onBindViewHolder(holder: ChannelVH, position: Int) {
        val channel = items[position]
        val b = holder.binding
        b.channelName.text = channel.name
        b.channelGroup.text = channel.group
        b.favoriteStar.text = if (favoritesProvider().contains(channel.id)) "★" else "☆"

        b.root.setOnClickListener { onClick(channel) }
        b.favoriteStar.setOnClickListener { onToggleFavorite(channel) }
    }

    override fun getItemCount(): Int = items.size

    fun update(newItems: List<Channel>) {
        items = newItems
        notifyDataSetChanged()
    }
}
