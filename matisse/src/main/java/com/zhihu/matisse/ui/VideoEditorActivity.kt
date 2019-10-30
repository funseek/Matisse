package com.zhihu.matisse.ui

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Point
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout.LayoutParams
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.daasuu.mp4compose.FillMode
import com.daasuu.mp4compose.composer.Mp4Composer
import com.zhihu.matisse.R
import com.zhihu.matisse.internal.entity.Item
import com.zhihu.matisse.internal.utils.PathUtils
import com.zhihu.matisse.internal.utils.Utils
import com.zhihu.matisse.ui.widget.GesturePlayerTextureView
import com.zhihu.matisse.ui.widget.SceneCropColor
import kotlinx.android.synthetic.main.activity_video_trim.*
import java.io.File
import java.util.*

class VideoEditorActivity : AppCompatActivity() {
    companion object {
        internal const val PATH_ARG = "PATH_ARG"
    }

    private lateinit var item: Item
    private lateinit var srcPath: Uri
    private var baseWidthSize: Float = 0.toFloat()
    private var playerTextureView: GesturePlayerTextureView? = null
    private var clearColorDialog: AlertDialog? = null
    private var sceneCropColor = SceneCropColor.WHITE

    private val windowHeight: Int
        get() {
            val size = Point()
            (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getSize(size)
            return size.x
        }

    private val videoFilePath: String
        get() = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                .absolutePath + "/necosta_" + Date().time + ".mp4"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_trim)

        if (intent == null) {
            finish()
            return
        }
        item = intent.getParcelableExtra(PATH_ARG)
        srcPath = item.uri

        btn_rotate?.setOnClickListener { playerTextureView!!.updateRotate() }
        done?.setOnClickListener { codec() }

        btn_color_change?.setOnClickListener { v ->
            if (clearColorDialog == null) {
                val builder = AlertDialog.Builder(v.context)
                builder.setTitle(getString(R.string.video_background_color_title))
                builder.setOnDismissListener { clearColorDialog = null }

                val items = SceneCropColor.values()
                val charList = arrayOfNulls<CharSequence>(items.size)
                var i = 0
                val n = items.size
                while (i < n) {
                    charList[i] = items[i].name
                    i++
                }
                builder.setItems(charList) { _, item ->
                    sceneCropColor = items[item]
                    layout_crop_trim_video?.setBackgroundColor(ContextCompat.getColor(v.context,
                            sceneCropColor.colorRes))
                }
                clearColorDialog = builder.show()
            } else {
                clearColorDialog!!.dismiss()
            }
        }

        initPlayer()
    }

    override fun onResume() {
        super.onResume()
        if (playerTextureView != null) {
            playerTextureView!!.play()
        }
    }

    override fun onPause() {
        super.onPause()
        if (playerTextureView != null) {
            playerTextureView!!.pause()
        }
    }

    private fun initPlayer() {
        playerTextureView = GesturePlayerTextureView(applicationContext, srcPath, timeSelector)

        val layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        layoutParams.gravity = Gravity.CENTER

        playerTextureView!!.layoutParams = layoutParams
        baseWidthSize = windowHeight.toFloat()
        playerTextureView!!.setBaseWidthSize(baseWidthSize)

        layout_crop_trim_video?.addView(playerTextureView)
    }

    private fun codec() {
        val progress = ProgressDialog.show(this, null, "Please wait...")
        btn_rotate?.isEnabled = false
        btn_color_change?.isEnabled = false

        val file = File(ContextWrapper(this).cacheDir, "${Calendar.getInstance().timeInMillis}.mp4")
        val timeCrop = timeSelector.getCurrent()

        Mp4Composer(PathUtils.getPath(this, srcPath), file.path)
                .size(720, 720)
                .trim(timeCrop[0], timeCrop[1])
                .filter(Utils.getFill(sceneCropColor))
                .fillMode(FillMode.CUSTOM)
                .customFillMode(Utils.getFillMode(playerTextureView!!, srcPath.path))
                .listener(object : Mp4Composer.Listener {
                    override fun onProgress(progress: Double) {
                    }

                    override fun onCompleted() {
                        progress.dismiss()
//                        exportMp4ToGallery(applicationContext, videoPath)
                        runOnUiThread {
                            val intent = Intent()
                            item.uri = Uri.fromFile(file)
                            intent.putExtra(PATH_ARG, item)
                            setResult(Activity.RESULT_OK, intent)
                            finish()
                        }
                    }

                    override fun onCanceled() {}

                    override fun onFailed(exception: Exception) {
                        progress.dismiss()
                    }
                })
                .start()
    }
}
