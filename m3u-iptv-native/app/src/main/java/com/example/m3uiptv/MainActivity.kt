package com.example.m3uiptv

import android.os.Bundle
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.m3uiptv.databinding.ActivityMainBinding
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var player: ExoPlayer

    private var channels: List<Channel> = emptyList()
    private var filteredChannels: List<Channel> = emptyList()
    private var selectedIndex: Int = -1

    // INSERISCI QUI IL TUO URL AUTORIZZATO
    private val DEFAULT_M3U_URL = "http://45.155.225.210:80//get.php?username=gennarofico&password=sasy&type=m3u_plus"

    private lateinit var adapter: ChannelAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupPlayer()
        setupRecycler()
        setupSearch()

        loadDefaultPlaylist()
    }

    private fun setupPlayer() {
        player = ExoPlayer.Builder(this).build()
        binding.playerView.player = player
        binding.playerView.useController = false
    }

    private fun setupRecycler() {
        adapter = ChannelAdapter(
            items = emptyList(),
            isSelected = { channel ->
                selectedIndex in filteredChannels.indices && filteredChannels[selectedIndex].id == channel.id
            },
            onFocused = { channel ->
                val idx = filteredChannels.indexOfFirst { it.id == channel.id }
                if (idx != -1) {
                    selectedIndex = idx
                    updateSelectedInfo()
                }
            },
            onClicked = { channel ->
                val idx = filteredChannels.indexOfFirst { it.id == channel.id }
                if (idx != -1) {
                    selectedIndex = idx
                    playSelectedChannel()
                }
            }
        )

        binding.channelRecycler.layoutManager = LinearLayoutManager(this)
        binding.channelRecycler.adapter = adapter
    }

    private fun setupSearch() {
        binding.searchInput.doAfterTextChanged {
            applyFilter(it?.toString().orEmpty())
        }
    }

    private fun loadDefaultPlaylist() {
        if (DEFAULT_M3U_URL.isBlank() || DEFAULT_M3U_URL == "METTI_QUI_IL_TUO_URL") {
            showError("Inserisci il tuo URL in MainActivity.kt")
            return
        }

        thread {
            try {
                val text = URL(DEFAULT_M3U_URL).readText()
                val parsed = M3UParser.parse(text)

                runOnUiThread {
                    if (parsed.isEmpty()) {
                        showError("Nessun canale trovato nella playlist.")
                        return@runOnUiThread
                    }

                    hideError()
                    channels = parsed
                    filteredChannels = parsed
                    selectedIndex = 0
                    adapter.update(filteredChannels)
                    playSelectedChannel()
                    binding.channelRecycler.post {
                        binding.channelRecycler.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    showError("Errore nel caricamento automatico della playlist.")
                }
            }
        }
    }

    private fun applyFilter(query: String) {
        filteredChannels = if (query.isBlank()) {
            channels
        } else {
            val q = query.trim().lowercase()
            channels.filter {
                it.name.lowercase().contains(q) ||
                it.group.lowercase().contains(q) ||
                it.metaName.lowercase().contains(q)
            }
        }

        selectedIndex = if (filteredChannels.isNotEmpty()) 0 else -1
        adapter.update(filteredChannels)

        if (selectedIndex != -1) {
            updateSelectedInfo()
        } else {
            binding.currentTitle.text = "Nessun canale selezionato"
            binding.currentGroup.text = ""
        }
    }

    private fun playSelectedChannel() {
        if (selectedIndex !in filteredChannels.indices) return

        val channel = filteredChannels[selectedIndex]
        binding.currentTitle.text = channel.name
        binding.currentGroup.text = channel.group

        val mediaItem = MediaItem.fromUri(channel.url)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true

        adapter.notifyDataSetChanged()
        scrollToSelection()
    }

    private fun updateSelectedInfo() {
        if (selectedIndex !in filteredChannels.indices) return
        val channel = filteredChannels[selectedIndex]
        binding.currentTitle.text = channel.name
        binding.currentGroup.text = channel.group
        adapter.notifyDataSetChanged()
    }

    private fun scrollToSelection() {
        if (selectedIndex !in filteredChannels.indices) return
        binding.channelRecycler.scrollToPosition(selectedIndex)
    }

    private fun showError(msg: String) {
        binding.errorText.text = msg
        binding.errorText.visibility = android.view.View.VISIBLE
    }

    private fun hideError() {
        binding.errorText.visibility = android.view.View.GONE
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                    if (filteredChannels.isNotEmpty()) {
                        selectedIndex = (selectedIndex + 1).coerceAtMost(filteredChannels.lastIndex)
                        playSelectedChannel()
                        return true
                    }
                }

                KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_CHANNEL_UP -> {
                    if (filteredChannels.isNotEmpty()) {
                        selectedIndex = (selectedIndex - 1).coerceAtLeast(0)
                        playSelectedChannel()
                        return true
                    }
                }

                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                    playSelectedChannel()
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onStop() {
        super.onStop()
        player.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }
}
