package com.example.pdf_generator.adapters

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.pdf_generator.Listener.ItemClickListner
import com.example.pdf_generator.R

class ClickedImagePreviewAdapter(val itemClickListner: ItemClickListner):RecyclerView.Adapter<ClickedImagePreviewAdapter.ClickedImageViewHolder>() {

    class ClickedImageViewHolder(view: View): RecyclerView.ViewHolder(view){
        val image: ImageView=view.findViewById(R.id.image)
        val deleteBtn: ImageView =view.findViewById(R.id.delete_image_button)
    }
    private val differCallback=object: DiffUtil.ItemCallback<Bitmap>(){
        override fun areContentsTheSame(oldItem: Bitmap, newItem: Bitmap): Boolean {
            return oldItem.sameAs(newItem)
        }

        override fun areItemsTheSame(oldItem: Bitmap, newItem: Bitmap): Boolean {
            return oldItem.sameAs(newItem)
        }
    }

    val differ= AsyncListDiffer(this, differCallback)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ClickedImageViewHolder {
        return ClickedImageViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.clicked_image_item_list, parent, false))
    }

    override fun onBindViewHolder(
        holder: ClickedImageViewHolder,
        position: Int
    ) {
        val item=differ.currentList[position]
        holder.image.setImageBitmap(item)
        holder.deleteBtn.setOnClickListener { itemClickListner.onDeleteBtnClick(it, position)}

    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }
}