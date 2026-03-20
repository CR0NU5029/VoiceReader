package com.joshw.voicereader.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY dateAdded DESC")
    fun getAllBooks(): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBookById(id: Long): Book?

    @Query("SELECT COUNT(*) FROM books WHERE title = :title AND author = :author")
    suspend fun countByTitleAndAuthor(title: String, author: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: Book): Long

    @Delete
    suspend fun deleteBook(book: Book)

    @Update
    suspend fun updateBook(book: Book)
}