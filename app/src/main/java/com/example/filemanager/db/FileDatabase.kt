package com.example.filemanager.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [FileEntity::class], version = 1)
abstract class FileDatabase : RoomDatabase() {
    abstract fun getDao(): FileDao
    companion object {
        fun getDb(context: Context): FileDatabase{
            return Room.databaseBuilder(
                context.applicationContext,
                FileDatabase::class.java,
                "files.db"
            ).build()
        }
    }
}