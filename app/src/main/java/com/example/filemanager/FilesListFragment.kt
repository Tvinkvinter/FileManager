package com.example.filemanager

import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.coroutineScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.filemanager.databinding.FragmentFilesListBinding
import kotlinx.coroutines.launch
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes

interface ListChanger {
    val curPath: String
    fun newPathList(newPath: String)
}

class FilesListFragment : Fragment(), ListChanger {
    private lateinit var binding: FragmentFilesListBinding
    private lateinit var adapter: ItemsAdapter
    override var curPath: String = Environment.getExternalStorageDirectory().path
    private var prevPath = curPath
    private var currentSortingParameter = 0 // sorting by filename
    private var currentSortingOrder = true // sorting from large to small


    override fun onAttach(context: Context) {
        super.onAttach(context)
        val callback = object : OnBackPressedCallback(
            true // default to enabled
        ) {
            override fun handleOnBackPressed() {
                Log.i("CallBack", "Call")
                when (curPath) {
                    Environment.getExternalStorageDirectory().path -> requireActivity().finish()
                    "new_files" -> newPathList(prevPath)
                    else -> File(curPath).parentFile?.let { newPathList(it.absolutePath) }
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(
            this, // LifecycleOwner
            callback
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFilesListBinding.inflate(inflater)
        adapter = ItemsAdapter(
            this as ListChanger,
            requireContext()
        )

        val layoutManager = LinearLayoutManager(context)
        binding.recycleView.layoutManager = layoutManager
        binding.recycleView.adapter = adapter

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setSpinner()

        val root = File(curPath)
        val filesAndFolders = root.listFiles()

        if (filesAndFolders != null) {
            adapter.items = filesAndFolders.toList()
            lifecycle.coroutineScope.launch {
                adapter.changedPaths =
                    (requireActivity() as DbProvider).getChangedItems().map { it.path }
            }
            binding.totalCountText.text = getString(R.string.total_item_count, filesAndFolders.size)
        }


        binding.fabNewFiles.setOnClickListener {
            val newItemList = mutableListOf<File>()
            for (path in adapter.changedPaths) {
                newItemList.add(File(path))
            }
            adapter.items = newItemList
            sortRecycler()
            binding.totalCountText.text = getString(
                R.string.total_item_count,
                adapter.changedPaths.size
            )
            prevPath = curPath
            curPath = "new_files"
        }

    }

    private fun setSpinner() {
        val spinner = binding.sortingSpinner

        val data = listOf(
            getString(R.string.filename_parameter),
            getString(R.string.time_created_parameter),
            getString(R.string.size_parameter),
            getString(R.string.extension_parameter)
        )

        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            R.layout.spinner_row,
            R.id.text_parameter,
            data
        )
        spinner.adapter = spinnerAdapter
        spinner.setSelection(0)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                currentSortingParameter = p2
                sortRecycler()
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                Log.i("spinner", "nothing selected")
            }
        }

        binding.changeOrderButton.setOnClickListener {
            currentSortingOrder = !currentSortingOrder
            if (currentSortingOrder) (it as ImageView).setImageResource(R.drawable.ic_arrow_upward)
            else (it as ImageView).setImageResource(R.drawable.ic_arrow_downward)
            sortRecycler()
        }
    }

    override fun newPathList(newPath: String) {
        lifecycle.coroutineScope.launch {
            adapter.changedPaths =
                (requireActivity() as DbProvider).getChangedItems().map { it.path }
        }
        adapter.items = File(newPath).listFiles()?.toList() ?: listOf()
        sortRecycler()
        binding.totalCountText.text = getString(R.string.total_item_count, adapter.itemCount)
        curPath = newPath
    }

    private fun sortRecycler() {
        adapter.items = when (currentSortingParameter) {
            0 -> if (currentSortingOrder) adapter.items.sortedBy { it.name } else adapter.items.sortedByDescending { it.name }
            1 -> {
                if (currentSortingOrder) adapter.items.sortedBy {
                    Files.readAttributes(it.toPath(), BasicFileAttributes::class.java)
                        .creationTime()
                } else adapter.items.sortedByDescending {
                    Files.readAttributes(it.toPath(), BasicFileAttributes::class.java)
                        .creationTime()
                }
            }
            2 -> if (currentSortingOrder) adapter.items.sortedBy { it.length() } else adapter.items.sortedByDescending { it.length() }
            3 -> if (currentSortingOrder) adapter.items.sortedBy { it.extension } else adapter.items.sortedByDescending { it.extension }
            else -> {
                adapter.items
            }
        }
    }
}