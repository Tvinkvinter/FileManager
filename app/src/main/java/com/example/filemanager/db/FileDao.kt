package com.example.filemanager.db

import androidx.room.*

@Dao
interface FileDao {

    @Insert
    suspend fun insertFile(fileEntity: FileEntity)

    @Delete
    suspend fun deleteFile(fileEntity: FileEntity)

    @Update
    suspend fun updateFile(fileEntity: FileEntity)

    @Query("SELECT * FROM files")
    suspend fun getAllFiles(): List<FileEntity>

    @Query("SELECT * FROM files WHERE path = :path")
    suspend fun getFileByPath(path: String): FileEntity?

    @Query("SELECT * FROM files WHERE isChanged = true")
    suspend fun getChangedFiles(): List<FileEntity>
}