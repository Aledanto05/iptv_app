package com.example.m3uiptv

import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
    private lateinit var store: PrefStore

    private var channels: List<Channel> = emptyList()
    private var filtered: List<Channel> = emptyList()
    private var favorites: MutableSet<String> = mutableSetOf()
    private var selectedId: String? = null

    private var currentGroup: String = "Tutti i gruppi"
    private var currentSort: String = "Per nome"
    private var currentTab: String = "Tutti"

    private lateinit var adapter: ChannelAdapter

    private val filePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                val text = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                binding.rawInput.setText(text)
                loadChannels(M3UParser.parse(text))
            } catch (_: Exception) {
                showError("Non sono riuscito a leggere il file selezionato.")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        store = PrefStore(this)
        favorites = store.getStrings("favorites")

        setupPlayer()
        setupRecycler()
        setupSpinners()
        restoreInputs()
        setupActions()
        applyFilters()
    }

    private fun setupPlayer() {
        player = ExoPlayer.Builder(this).build()
        binding.playerView.player = player
    }

    private fun setupRecycler() {
        adapter = ChannelAdapter(
            items = emptyList(),
            favoritesProvider = { favorites },
            onClick = { channel ->
                selectedId = channel.id
                playChannel(channel)
                updateCurrentInfo()
                adapter.notifyDataSetChanged()
            },
            onToggleFavorite = { channel ->
                toggleFavorite(channel)
            }
        )
        binding.channelRecycler.layoutManager = LinearLayoutManager(this)
        binding.channelRecycler.adapter = adapter
    }

    private fun setupSpinners() {
        val sortAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("Per nome", "Per gruppo"))
        binding.sortSpinner.adapter = sortAdapter

        val tabAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("Tutti", "Preferiti"))
        binding.tabSpinner.adapter = tabAdapter
    }

    private fun restoreInputs() {
        binding.urlInput.setText(store.getText("url"))
        binding.proxyInput.setText(store.getText("proxy"))
        binding.rawInput.setText(store.getText("raw"))
    }

    private fun setupActions() {
        binding.loadUrlButton.setOnClickListener {
            val m3uUrl = binding.urlInput.text.toString().trim()
            val proxy = binding.proxyInput.text.toString().trim()

            store.saveText("url", m3uUrl)
            store.saveText("proxy", proxy)

            if (m3uUrl.isBlank()) {
                showError("Inserisci un link M3U valido.")
                return@setOnClickListener
            }

            thread {
                try {
                    val finalUrl = if (proxy.isNotBlank()) proxy + Uri.encode(m3uUrl) else m3uUrl
                    val text = URL(finalUrl).readText()
                    val parsed = M3UParser.parse(text)
                    runOnUiThread { loadChannels(parsed) }
                } catch (_: Exception) {
                    runOnUiThread {
                        showError("Impossibile leggere l'URL direttamente. Prova con file M3U, testo incollato o proxy CORS.")
                    }
                }
            }
        }

        binding.pickFileButton.setOnClickListener {
            filePicker.launch("*/*")
        }

        binding.loadTextButton.setOnClickListener {
            val raw = binding.rawInput.text.toString()
            store.saveText("raw", raw)
            if (raw.isBlank()) {
                showError("Incolla il contenuto della playlist M3U.")
                return@setOnClickListener
            }
            loadChannels(M3UParser.parse(raw))
        }

        binding.searchInput.doAfterTextChanged {
            applyFilters()
        }

        binding.groupSpinner.setOnItemSelectedListener(SimpleItemSelectedListener { selected ->
            currentGroup = selected
            applyFilters()
        })

        binding.sortSpinner.setOnItemSelectedListener(SimpleItemSelectedListener { selected ->
            currentSort = selected
            applyFilters()
        })

        binding.tabSpinner.setOnItemSelectedListener(SimpleItemSelectedListener { selected ->
            currentTab = selected
            applyFilters()
        })

        binding.favoriteButton.setOnClickListener {
            val current = channels.find { it.id == selectedId } ?: return@setOnClickListener
            toggleFavorite(current)
        }
    }

    private fun loadChannels(parsed: List<Channel>) {
        if (parsed.isEmpty()) {
            showError("Nessun canale trovato nella playlist.")
            return
        }
        hideError()
        channels = parsed
        selectedId = parsed.first().id
        favorites.clear()
        store.saveStrings("favorites", favorites)
        refreshGroupSpinner()
        applyFilters()
        playChannel(parsed.first())
        updateCurrentInfo()
        Toast.makeText(this, "Canali caricati: ${parsed.size}", Toast.LENGTH_SHORT).show()
    }

    private fun refreshGroupSpinner() {
        val groups = listOf("Tutti i gruppi") + channels.map { it.group.ifBlank { "Senza gruppo" } }.distinct().sorted()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, groups)
        binding.groupSpinner.adapter = adapter
    }

    private fun applyFilters() {
        var list = channels.toList()

        if (currentGroup != "Tutti i gruppi") {
            list = list.filter { it.group == currentGroup }
        }

        val q = binding.searchInput.text.toString().trim().lowercase()
        if (q.isNotBlank()) {
            list = list.filter {
                it.name.lowercase().contains(q) ||
                it.group.lowercase().contains(q) ||
                it.metaName.lowercase().contains(q)
            }
        }

        if (currentTab == "Preferiti") {
            list = list.filter { favorites.contains(it.id) }
        }

        list = when (currentSort) {
            "Per gruppo" -> list.sortedWith(compareBy({ it.group }, { it.name }))
            else -> list.sortedBy { it.name }
        }

        filtered = list
        adapter.update(filtered)
        updateCurrentInfo()
    }

    private fun playChannel(channel: Channel) {
        val mediaItem = MediaItem.fromUri(channel.url)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true
    }

    private fun updateCurrentInfo() {
        val current = channels.find { it.id == selectedId }
        if (current == null) {
            binding.currentTitle.text = "Nessun canale selezionato"
            binding.currentGroup.text = ""
            binding.favoriteButton.text = "Aggiungi preferito"
            return
        }

        binding.currentTitle.text = current.name
        binding.currentGroup.text = current.group
        binding.favoriteButton.text = if (favorites.contains(current.id)) "Rimuovi preferito" else "Aggiungi preferito"
    }

    private fun toggleFavorite(channel: Channel) {
        if (favorites.contains(channel.id)) {
            favorites.remove(channel.id)
        } else {
            favorites.add(channel.id)
        }
        store.saveStrings("favorites", favorites)
        adapter.notifyDataSetChanged()
        updateCurrentInfo()
    }

    private fun showError(msg: String) {
        binding.errorText.text = msg
        binding.errorText.visibility = android.view.View.VISIBLE
    }

    private fun hideError() {
        binding.errorText.visibility = android.view.View.GONE
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
