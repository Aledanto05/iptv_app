M3U IPTV - progetto Android nativo

Questa è una base seria stile IPTV:
- Android nativo (Kotlin)
- Media3 ExoPlayer per stream HLS/M3U8
- URL M3U
- file picker M3U
- testo M3U incollato
- ricerca
- gruppi
- preferiti
- player integrato

Come compilare:
1. Apri la cartella in Android Studio oppure in un builder Android/Gradle compatibile.
2. Lascia fare il sync.
3. Build > Build APK(s)

Nota:
- Alcuni stream IPTV usano formati o header particolari: in quei casi serve rifinire DataSourceFactory o usare proxy.
- Se vuoi un passo ancora più serio, i prossimi upgrade sono:
  1) logo canali
  2) EPG
  3) storico ultimi canali
  4) fullscreen player
  5) drawer laterale stile IPTV
