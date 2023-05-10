package com.example.filemanager

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.Manifest
import android.os.Environment
import android.window.OnBackInvokedDispatcher
import androidx.fragment.app.Fragment
import androidx.lifecycle.coroutineScope
import com.example.filemanager.db.FileDatabase
import com.example.filemanager.db.FileEntity
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileFilter

interface FragmentLauncher {
    fun launchFragment(fragment: Fragment)
}

interface DbProvider {
    suspend fun getChangedItems(): List<FileEntity>
}

class MainActivity : AppCompatActivity(), FragmentLauncher, DbProvider {
    private lateinit var db: FileDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        db = FileDatabase.getDb(this)
        if (checkExternalStoragePermission()) {
            lifecycle.coroutineScope.launch {
                addFilesToDb()
            }
            if (savedInstanceState == null) {
                launchFragment(FilesListFragment())
            }
        }
    }
    override suspend fun getChangedItems(): List<FileEntity>{
        return db.getDao().getChangedFiles()
    }

    suspend fun addFilesToDb(startPath: String = Environment.getExternalStorageDirectory().absolutePath) {
        if((File(startPath).listFiles()?.size ?:0) == 0) return
        val childDirs = File(startPath).listFiles(FileFilter {
            it.isDirectory
        })
        val childFiles = File(startPath).listFiles(FileFilter {
            it.isFile
        })
        for (file in childFiles!!) {
            val cur_file = FileEntity(
                path = file.absolutePath,
                hash = file.hashCode(),
                isChanged = false)
            val db_file = db.getDao().getFileByPath(cur_file.path)
            if(db_file != null){
                if(db_file.hash != cur_file.hash){
                    cur_file.isChanged = true
                    db.getDao().updateFile(cur_file)
                }
            }else{
                db.getDao().insertFile(cur_file)
            }
        }
        for(dir in childDirs!!){
            addFilesToDb(dir.absolutePath)
        }
    }

    override fun launchFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .addToBackStack(null)
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    override fun getOnBackInvokedDispatcher(): OnBackInvokedDispatcher {
        val cur_frag = (R.id.fragment_container as ListChanger)
        File(cur_frag.path).parentFile?.let { cur_frag.newPathList(it.absolutePath) }
        return super.getOnBackInvokedDispatcher()
    }

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_LONG).show()
                launchFragment(FilesListFragment())
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_LONG).show()
            }
        }

    private fun checkExternalStoragePermission(): Boolean {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        } else {
            requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        return false
    }
}