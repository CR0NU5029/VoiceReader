package com.joshw.voicereader.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.joshw.voicereader.data.AppDatabase
import com.joshw.voicereader.data.Book
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import org.readium.r2.shared.util.toUrl
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val bookDao = db.bookDao()

    // Readium components for metadata extraction
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

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    val books: StateFlow<List<Book>> = bookDao.getAllBooks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun importBook(uri: Uri) {
        viewModelScope.launch {
            val context = getApplication<Application>()

            // Copy the EPUB into app's private storage
            val fileName = "book_${System.currentTimeMillis()}.epub"
            val destFile = File(context.filesDir, fileName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            // Use Readium 3.x API to extract metadata
            var title = destFile.nameWithoutExtension
            var author = "Unknown Author"

            try {
                val fileUrl = destFile.toUrl()
                val asset = assetRetriever.retrieve(fileUrl).getOrNull()

                if (asset != null) {
                    val publication = publicationOpener.open(
                        asset = asset,
                        allowUserInteraction = false
                    ).getOrNull()

                    publication?.let {
                        title = it.metadata.title ?: destFile.nameWithoutExtension
                        author = it.metadata.authors
                            .joinToString(", ") { a -> a.name }
                            .ifBlank { "Unknown Author" }
                    }
                }
            } catch (e: Exception) {
                // Keep the filename fallback
            }

            val book = Book(
                title = title,
                author = author,
                filePath = destFile.absolutePath
            )

            // Check for duplicate
            val existingCount = bookDao.countByTitleAndAuthor(title, author)
            if (existingCount > 0) {
                destFile.delete()
                _toastMessage.value = "\"$title\" is already in your library"
                delay(3500)
                _toastMessage.value = null
                return@launch
            }

            bookDao.insertBook(book)
        }
    }

    fun deleteBook(book: Book) {
        viewModelScope.launch {
            File(book.filePath).delete()
            bookDao.deleteBook(book)
        }
    }
}