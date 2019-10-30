package com.zhihu.matisse.ui

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.zhihu.matisse.R
import com.zomato.photofilters.utils.ThumbnailCallback
import com.zomato.photofilters.utils.ThumbnailItem

class ThumbnailsAdapter(private val dataSet: List<ThumbnailItem>,
                        private val thumbnailCallback: ThumbnailCallback)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): RecyclerView.ViewHolder {
        Log.v(TAG, "On Create View Holder Called")
        return ThumbnailsViewHolder(LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.list_thumbnail_item, viewGroup, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, i: Int) {
        val thumbnailItem = dataSet[i]
        Log.v(TAG, "On Bind View Called")
        val thumbnailsViewHolder = holder as ThumbnailsViewHolder
        thumbnailsViewHolder.name.text = thumbnailItem.filter.name
        thumbnailsViewHolder.thumbnail.setImageBitmap(thumbnailItem.image)
        thumbnailsViewHolder.thumbnail.scaleType = ImageView.ScaleType.FIT_CENTER
        thumbnailsViewHolder.thumbnail.setOnClickListener {
            if (lastPosition != i) {
                thumbnailCallback.onThumbnailClick(thumbnailItem.filter)
                lastPosition = i
            }
        }
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    class ThumbnailsViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        var thumbnail: ImageView = v.findViewById(R.id.thumbnail)
        var name: TextView = v.findViewById(R.id.name)

    }

    companion object {
        private const val TAG = "ThumbnailsAdapter"
        private var lastPosition = -1
    }
}
