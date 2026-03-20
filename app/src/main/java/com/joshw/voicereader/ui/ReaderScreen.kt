package com.joshw.voicereader.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joshw.voicereader.data.Book

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    book: Book,
    viewModel: ReaderViewModel,
    onBack: () -> Unit
) {
    val chapters by viewModel.chapters.collectAsStateWithLifecycle()
    val currentChapterIndex by viewModel.currentChapterIndex.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    // Load book when screen opens
    LaunchedEffect(book.id) {
        viewModel.loadBook(book)
    }

    val currentChapter = chapters.getOrNull(currentChapterIndex)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = currentChapter?.title ?: book.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.stopSpeaking()
                        onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        bottomBar = {
            ReaderControls(
                isSpeaking = isSpeaking,
                onPlay = { viewModel.speakCurrentChapter() },
                onStop = { viewModel.stopSpeaking() },
                onPrevious = { viewModel.previousChapter() },
                onNext = { viewModel.nextChapter() },
                hasPrevious = currentChapterIndex > 0,
                hasNext = currentChapterIndex < chapters.size - 1
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                error != null -> {
                    Text(
                        text = error ?: "Unknown error",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                currentChapter != null -> {
                    Text(
                        text = currentChapter.content,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = MaterialTheme.typography.bodyLarge.fontSize * 1.6
                    )
                }
            }
        }
    }
}

@Composable
fun ReaderControls(
    isSpeaking: Boolean,
    onPlay: () -> Unit,
    onStop: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    hasPrevious: Boolean,
    hasNext: Boolean
) {
    Surface(
        tonalElevation = 4.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPrevious,
                enabled = hasPrevious
            ) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous chapter")
            }

            FilledIconButton(
                onClick = if (isSpeaking) onStop else onPlay,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = if (isSpeaking) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isSpeaking) "Stop" else "Play",
                    modifier = Modifier.size(32.dp)
                )
            }

            IconButton(
                onClick = onNext,
                enabled = hasNext
            ) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next chapter")
            }
        }
    }
}