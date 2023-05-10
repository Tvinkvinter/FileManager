package com.example.filemanager.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "files",
    indices = [Index("path", unique = true)]
)
class FileEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Int? = null,

    @ColumnInfo()
    var path: String,

    @ColumnInfo
    var hash: String,

    @ColumnInfo
    var isChanged: Boolean
)