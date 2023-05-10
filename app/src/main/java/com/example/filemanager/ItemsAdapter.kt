package com.example.filemanager


import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.filemanager.databinding.ListItemBinding
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ItemsAdapter(
    private val listChanger: ListChanger,
    private val context: Context
) :
    RecyclerView.Adapter<ItemsAdapter.ItemsViewHolder>() {

    var changedPaths: List<String> = emptyList()
        @SuppressLint("NotifyDataSetChanged")
        set(newValue) {
            field = newValue
            notifyDataSetChanged()
        }
    var items: List<File> = emptyList()
        @SuppressLint("NotifyDataSetChanged")
        set(newValue) {
            field = newValue.filter { it.name[0] != '.' }
            notifyDataSetChanged()
        }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemsViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ListItemBinding.inflate(inflater, parent, false)

        return ItemsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemsViewHolder, position: Int) {
        val item = items[position]
        Log.i("onBindViewHolder", "Called")
        with(holder.binding) {
            holder.itemView.tag = item
            itemNameTextView.text = item.name
            if (!item.isDirectory) {
                if (item.absolutePath in changedPaths)
                    itemNameTextView.setTextColor(context.getColor(R.color.yellow))
                else itemNameTextView.setTextColor(context.getColor(R.color.black))

                itemExtensionTextView.text = item.extension.uppercase()
                itemSizeTextView.text = context.getString(R.string.file_size, item.length() / 1024)
                itemSizeTextView.visibility = View.VISIBLE
                itemExtensionTextView.visibility = View.VISIBLE
                iconView.setImageResource(
                    when (item.extension.lowercase()) {
                        in listOf("png", "jpg", "jpeg", "tiff", "raw") -> R.drawable.ic_image_file
                        in listOf("mp4", "mov", "avi") -> R.drawable.ic_video_file
                        in listOf("mp3", "aac") -> R.drawable.ic_audio_file
                        in listOf("txt", "rtf") -> R.drawable.ic_txt_file
                        else -> R.drawable.ic_any_file
                    }
                )
            } else {
                iconView.setImageResource(R.drawable.ic_folder)
                itemSizeTextView.visibility = View.GONE
                itemExtensionTextView.text = item.extension.uppercase()
                itemExtensionTextView.visibility = View.GONE
            }
            val attr = Files.readAttributes(item.toPath(), BasicFileAttributes::class.java)
            itemDateTextView.text =
                attr.creationTime()
                    .toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDateTime()
                    .format(
                        DateTimeFormatter.ofPattern("dd/MM/yyyy")
                    )
        }
        holder.itemView.setOnClickListener {
            val selectedItem = it.tag as File
            if (selectedItem.isDirectory) {
                listChanger.newPathList(selectedItem.absolutePath)
            }
        }
    }

    class ItemsViewHolder(
        val binding: ListItemBinding
    ) : RecyclerView.ViewHolder(binding.root)
}
