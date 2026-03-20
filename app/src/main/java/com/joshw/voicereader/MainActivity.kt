package com.joshw.voicereader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.joshw.voicereader.data.Book
import com.joshw.voicereader.ui.LibraryScreen
import com.joshw.voicereader.ui.theme.VoiceReaderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VoiceReaderTheme {
                // Temporary empty list — we'll wire up the database shortly
                LibraryScreen(
                    books = emptyList<Book>(),
                    onImportClick = { /* coming soon */ },
                    onBookClick = { /* coming soon */ }
                )
            }
        }
    }
}