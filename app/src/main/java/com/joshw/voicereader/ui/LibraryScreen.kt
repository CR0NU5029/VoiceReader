package com.joshw.voicereader.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.joshw.voicereader.data.Book
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.Surface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentHeight
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    books: List<Book>,
    onImportClick: () -> Unit,
    onBookClick: (Book) -> Unit,
    onDeleteBook: (Book) -> Unit,
    toastMessage: String? = null
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("My Library") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = onImportClick) {
                    Icon(Icons.Default.Add, contentDescription = "Import book")
                }
            }
        ) { paddingValues ->
            if (books.isEmpty()) {
                EmptyLibraryMessage(
                    modifier = Modifier.Companion.padding(paddingValues)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.Companion.padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(books, key = { it.id }) { book ->
                        BookCard(
                            book = book,
                            onClick = { onBookClick(book) },
                            onDelete = { onDeleteBook(book) }  // ← new
                        )
                    }
                }
            }
        }
        // Toast overlay — sits above the scaffold at the bottom
        AnimatedVisibility(
            visible = toastMessage != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        ) {
            toastMessage?.let { LibraryToast(message = it) }
        }
    }
}

@Composable
fun EmptyLibraryMessage(modifier: Modifier = Modifier.Companion) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Companion.Center
    ) {
        Column(horizontalAlignment = Alignment.Companion.CenterHorizontally) {
            Text(
                text = "No books yet",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.Companion.height(8.dp))
            Text(
                text = "Tap the + button to import an EPUB",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookCard(book: Book, onClick: () -> Unit, onDelete: (Book) -> Unit) {
    var cardWidth by remember { mutableStateOf(0f) }
    val maxSwipe = cardWidth * 0.2f
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .onSizeChanged { cardWidth = it.width.toFloat() }
    ) {
        // Delete background — only visible when swiping
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CardDefaults.shape)
                .background(MaterialTheme.colorScheme.errorContainer)
                .padding(end = 20.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
        }

        // The card itself — slides left and stops at 20%
        Card(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        scope.launch {
                            val newOffset = (offsetX.value + delta).coerceIn(-maxSwipe, 0f)
                            offsetX.snapTo(newOffset)
                        }
                    },
                    onDragStopped = {
                        scope.launch {
                            if (offsetX.value <= -maxSwipe * 0.99f) {
                                // User reached the hard stop — delete
                                onDelete(book)
                                offsetX.snapTo(0f)
                            } else {
                                // Didn't reach the stop — spring back
                                offsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f)
                                )
                            }
                        }
                    }
                )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (book.percentComplete > 0f) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { book.percentComplete },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun LibraryToast(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.secondaryContainer,
            tonalElevation = 4.dp,
            shadowElevation = 4.dp
        ) {
            Text(
                text = message,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}