package com.joshw.voicereader.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.joshw.voicereader.data.Book
import com.joshw.voicereader.tts.AndroidTtsEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.toUrl
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import java.io.File

class ReaderViewModel(application: Application) : AndroidViewModel(application) {

    private val httpClient = DefaultHttpClient()
    private val assetRetriever = AssetRetriever(
        contentResolver = application.contentResolver,
        httpClient = httpClient
    )
    private val publicationOpener = PublicationOpener(
        publicationParser = DefaultPublicationParser(
            application,
            httpClient = httpClient,
            assetRetriever = assetRetriever,
            pdfFactory = null
        )
    )

    private val ttsEngine = AndroidTtsEngine(application)

    // UI state
    private val _chapters = MutableStateFlow<List<Chapter>>(emptyList())
    val chapters: StateFlow<List<Chapter>> = _chapters.asStateFlow()

    private val _currentChapterIndex = MutableStateFlow(0)
    val currentChapterIndex: StateFlow<Int> = _currentChapterIndex.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    data class Chapter(val title: String, val content: String)

    fun loadBook(book: Book) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isLoading.value = true
                val file = File(book.filePath)
                val asset = assetRetriever.retrieve(file.toUrl()).getOrNull()
                    ?: run {
                        _error.value = "Could not open book file"
                        _isLoading.value = false
                        return@launch
                    }

                val publication = publicationOpener.open(
                    asset = asset,
                    allowUserInteraction = false
                ).getOrNull() ?: run {
                    _error.value = "Could not parse EPUB"
                    _isLoading.value = false
                    return@launch
                }

                val extractedChapters = mutableListOf<Chapter>()

                publication.readingOrder.forEachIndexed { index, link ->
                    try {
                        val resource = publication.get(link)
                        val bytes = resource?.read()?.getOrNull() ?: return@forEachIndexed
                        val html = String(bytes, Charsets.UTF_8)
                        val plainText = stripHtml(html)

                        if (plainText.length > 50) { // skip near-empty resources
                            val title = link.title
                                ?: publication.tableOfContents.getOrNull(index)?.title
                                ?: "Chapter ${index + 1}"
                            extractedChapters.add(Chapter(title, plainText))
                        }
                    } catch (e: Exception) {
                        // skip unreadable resources silently
                    }
                }

                _chapters.value = extractedChapters
                _isLoading.value = false

            } catch (e: Exception) {
                _error.value = "Failed to load book: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun speakCurrentChapter() {
        val chapter = _chapters.value.getOrNull(_currentChapterIndex.value) ?: return
        _isSpeaking.value = true
        ttsEngine.onUtteranceCompleted = {
            _isSpeaking.value = false
        }
        ttsEngine.speak(chapter.content)
    }

    fun stopSpeaking() {
        ttsEngine.stop()
        _isSpeaking.value = false
    }

    fun nextChapter() {
        stopSpeaking()
        val next = _currentChapterIndex.value + 1
        if (next < _chapters.value.size) {
            _currentChapterIndex.value = next
        }
    }

    fun previousChapter() {
        stopSpeaking()
        val prev = _currentChapterIndex.value - 1
        if (prev >= 0) {
            _currentChapterIndex.value = prev
        }
    }

    private fun stripHtml(html: String): String {
        return html
            .replace(Regex("<style[^>]*>.*?</style>", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("<script[^>]*>.*?</script>", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("<[^>]+>"), " ")
            .replace(Regex("&nbsp;"), " ")
            .replace(Regex("&amp;"), "&")
            .replace(Regex("&lt;"), "<")
            .replace(Regex("&gt;"), ">")
            .replace(Regex("&quot;"), "\"")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    override fun onCleared() {
        super.onCleared()
        ttsEngine.shutdown()
    }
}