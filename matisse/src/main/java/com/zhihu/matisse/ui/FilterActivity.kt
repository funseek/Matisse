package com.zhihu.matisse.ui

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.zhihu.matisse.Matisse
import com.zhihu.matisse.R
import com.zomato.photofilters.FilterPack
import com.zomato.photofilters.imageprocessors.Filter
import com.zomato.photofilters.utils.ThumbnailCallback
import com.zomato.photofilters.utils.ThumbnailItem
import com.zomato.photofilters.utils.ThumbnailsManager
import kotlinx.android.synthetic.main.activity_post_filter.*
import kotlinx.android.synthetic.main.filter_images.view.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

class FilterActivity : AppCompatActivity(), ThumbnailCallback, OnViewPagerImageLoad {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Matisse_Zhihu)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_filter)

        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        next.setOnClickListener {
            setResult(Activity.RESULT_OK)
            finish()
        }

        initUIWidgets(Matisse.obtainResult(intent))
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initUIWidgets(mSelected: MutableList<Uri>) {
        initViewPager(mSelected)
        initHorizontalList()
    }

    private fun initViewPager(mSelected: MutableList<Uri>) {
        val adapter = FilterImageAdapter(this, mSelected, this)
        viewPager.adapter = adapter
        viewPager.offscreenPageLimit = adapter.count
        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(p0: Int) {
            }

            override fun onPageScrolled(p0: Int, p1: Float, p2: Int) {
            }

            override fun onPageSelected(p0: Int) {
                Log.v("FilterActivity", "onPageSelected $p0")
                bindDataToAdapter(p0)
            }
        })
    }

    private fun initHorizontalList() {
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.HORIZONTAL
        layoutManager.scrollToPosition(0)
        thumbnails_view.layoutManager = layoutManager
        thumbnails_view.setHasFixedSize(true)
    }

    override fun onImageLoad(p: Int) {
        bindDataToAdapter(p)
    }

    private fun bindDataToAdapter(p: Int) {
        if (viewPager.currentItem != p) return

        val size = resources.getDimension(R.dimen.thumbnail_size)
        val resource = (viewPager.adapter as FilterImageAdapter).bitmaps[viewPager.currentItem]
                ?: return
        val resizeBm = Bitmap.createScaledBitmap(resource, size.toInt(), size.toInt(), false)
        ThumbnailsManager.clearThumbs()

        FilterPack.getFilterPack(this).forEach { filter ->
            val thumbnailItem = ThumbnailItem()
            thumbnailItem.image = resizeBm
            thumbnailItem.filter = filter
            ThumbnailsManager.addThumb(thumbnailItem)
        }
        val adapter = ThumbnailsAdapter(ThumbnailsManager.processThumbs(this), this)
        thumbnails_view.adapter = adapter
    }

    override fun onThumbnailClick(filter: Filter) {
        val bm = (viewPager.adapter as FilterImageAdapter).bitmaps[viewPager.currentItem]!!
        val bm2 = bm.copy(bm.config, true)
        viewPager.getChildAt(viewPager.currentItem)?.findViewById<ImageView>(R.id.imageView)
                ?.setImageBitmap(filter.processFilter(bm2))
        val uri = (viewPager.adapter as FilterImageAdapter).images[viewPager.currentItem]
        saveFilteredBitmap(uri, bm2)
    }

    private fun saveFilteredBitmap(uri: Uri, bitmap: Bitmap) {
        try {
            // Initialize a new file instance to save bitmap object
            val file = File(uri.path)
            // Compress the bitmap and save in jpg format
            val stream: OutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            stream.flush()
            stream.close()
            Log.i("FilterActivity", "saveFilteredBitmap $uri")
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    companion object {
        init {
            System.loadLibrary("NativeImageProcessor")
        }
    }

    class FilterImageAdapter(private val context: Context, val images: List<Uri>,
                             private val listener: OnViewPagerImageLoad) : PagerAdapter() {
        var bitmaps: LinkedHashMap<Int, Bitmap?> = LinkedHashMap()
        override fun isViewFromObject(view: View, `object`: Any): Boolean {
            return view == `object`
        }

        override fun getCount(): Int {
            return images.size
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val view = layoutInflater.inflate(R.layout.filter_images, container, false)
            Glide.with(context).asBitmap().load(images[position]).addListener(object : RequestListener<Bitmap> {
                override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Bitmap>?,
                                          isFirstResource: Boolean): Boolean {
                    return false
                }

                override fun onResourceReady(resource: Bitmap?, model: Any?, target: Target<Bitmap>?,
                                             dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                    view.imageView.setImageBitmap(resource)
                    bitmaps[position] = resource
                    listener.onImageLoad(position)
                    return true
                }

            }).into(view.imageView)
            container.addView(view)
            return view
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            container.removeView(`object` as View)
        }
    }
}

interface OnViewPagerImageLoad {
    fun onImageLoad(p: Int)
}

