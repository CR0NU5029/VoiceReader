package com.joshw.voicereader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joshw.voicereader.data.Book
import com.joshw.voicereader.ui.LibraryScreen
import com.joshw.voicereader.ui.LibraryViewModel
import com.joshw.voicereader.ui.ReaderScreen
import com.joshw.voicereader.ui.ReaderViewModel
import com.joshw.voicereader.ui.theme.VoiceReaderTheme

sealed class Screen {
    object Library : Screen()
    data class Reader(val book: Book) : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VoiceReaderTheme {
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Library) }

                when (val screen = currentScreen) {
                    is Screen.Library -> {
                        val viewModel: LibraryViewModel = viewModel()
                        val books by viewModel.books.collectAsStateWithLifecycle()
                        val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

                        val filePicker = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.GetContent()
                        ) { uri ->
                            uri?.let { viewModel.importBook(it) }
                        }

                        LibraryScreen(
                            books = books,
                            onImportClick = { filePicker.launch("application/epub+zip") },
                            onBookClick = { book -> currentScreen = Screen.Reader(book) },
                            onDeleteBook = { viewModel.deleteBook(it) },
                            toastMessage = toastMessage
                        )
                    }

                    is Screen.Reader -> {
                        val viewModel: ReaderViewModel = viewModel()
                        ReaderScreen(
                            book = screen.book,
                            viewModel = viewModel,
                            onBack = { currentScreen = Screen.Library }
                        )
                    }
                }
            }
        }
    }
}