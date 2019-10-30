package com.zhihu.matisse.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import android.view.Surface
import android.view.TextureView
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ClippingMediaSource
import com.google.android.exoplayer2.source.LoopingMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.video.VideoListener
import java.util.concurrent.TimeUnit

@SuppressLint("ViewConstructor")
open class PlayerTextureView(context: Context, path: Uri, private val timeSelector: TimeSelectorView?)
    : TextureView(context, null, 0), TextureView.SurfaceTextureListener, VideoListener {

    private val VIDEO_LIMIT_US = TimeUnit.SECONDS.toMicros(30) // ３０秒
    private var duration = TimeUnit.SECONDS.toMicros(1)
    private val player: SimpleExoPlayer?
    protected var videoAspect = DEFAULT_ASPECT

    init {
        timeSelector?.apply {
            selectionStartListener = { start, left, right ->
                if (start) pause()
                else {
                    play()
                    onSelectionChanged(context, path, left, right)
                }
            }
            initDuration(context, path)
        }

        // SimpleExoPlayer
        player = ExoPlayerFactory.newSimpleInstance(context)
        player!!.addVideoListener(this)

        // Prepare the player with the source.
        player.prepare(createLoopingMediaSource(context, path))
        surfaceTextureListener = this
    }

    private fun initDuration(context: Context, path: Uri) {
        val retriever = MediaMetadataRetriever()
        //use one of overloaded setDataSource() functions to set your data source
        retriever.setDataSource(context, path)
        val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        duration = TimeUnit.MILLISECONDS.toMicros(java.lang.Long.parseLong(time))
        timeSelector?.setLimit(VIDEO_LIMIT_US.toFloat() / duration)
        timeSelector?.duration = duration
        Log.d(TAG, "duration = $duration")
        retriever.release()
    }

    private fun createLoopingMediaSource(context: Context, path: Uri): MediaSource {
        // Produces DataSource instances through which media data is loaded.
        val dataSourceFactory = DefaultDataSourceFactory(context, "No-Agent")

        // This is the MediaSource representing the media to be played.
        val videoSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(path)

        return LoopingMediaSource(videoSource)
    }

    private fun onSelectionChanged(context: Context, path: Uri, left: Float?, right: Float?) {
        // Produces DataSource instances through which media data is loaded.
        val dataSourceFactory = DefaultDataSourceFactory(context, "No-Agent")

        // This is the MediaSource representing the media to be played.
        var videoSource: MediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(path)

        val start = (left!! * duration).toLong()
        val end = (right!! * duration).toLong()
        if (start != C.TIME_UNSET) videoSource = ClippingMediaSource(videoSource, start, end)

        player?.prepare(LoopingMediaSource(videoSource))
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        if (videoAspect == DEFAULT_ASPECT) return

        val measuredWidth = measuredWidth
        val viewHeight = (measuredWidth / videoAspect).toInt()
        Log.d(TAG, "onMeasure videoAspect = $videoAspect")
        Log.d(TAG, "onMeasure viewWidth = $measuredWidth viewHeight = $viewHeight")

        setMeasuredDimension(measuredWidth, viewHeight)
    }


    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        Log.d(TAG, "onSurfaceTextureAvailable width = $width height = $height")

        //3. bind the player to the view
        player!!.setVideoSurface(Surface(surface))
        player.playWhenReady = true
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {

    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        player!!.stop()
        player.release()
        return false
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {

    }

    override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
        Log.d(TAG, "width = " + width + " height = " + height + " unappliedRotationDegrees = "
                + unappliedRotationDegrees + " pixelWidthHeightRatio = " + pixelWidthHeightRatio)
        videoAspect = width.toFloat() / height * pixelWidthHeightRatio
        Log.d(TAG, "videoAspect = $videoAspect")
        requestLayout()
    }

    override fun onSurfaceSizeChanged(width: Int, height: Int) {

    }

    override fun onRenderedFirstFrame() {

    }

    fun play() {
        player!!.playWhenReady = true
    }

    fun pause() {
        player!!.playWhenReady = false
    }

    companion object {

        private val TAG = PlayerTextureView::class.java.simpleName
        protected val DEFAULT_ASPECT = -1f
    }

}
