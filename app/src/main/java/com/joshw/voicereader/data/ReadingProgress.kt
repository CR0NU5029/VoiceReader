package com.joshw.voicereader.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reading_progress")
data class ReadingProgress(
    @PrimaryKey
    val bookId: Long,
    val locatorJson: String = "",  // Readium saves position as JSON
    val percentComplete: Float = 0f,
    val lastRead: Long = System.currentTimeMillis()
)