/*
 * Copyright 2017 Zhihu Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zhihu.matisse.internal.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.util.Size
import com.daasuu.mp4compose.FillModeCustomItem
import com.daasuu.mp4compose.filter.GlFilter
import com.zhihu.matisse.ui.widget.GesturePlayerTextureView
import com.zhihu.matisse.ui.widget.SceneCropColor
import kotlin.math.roundToInt

class Utils {
    companion object {
        fun spanCount(context: Context, gridExpectedSize: Int): Int {
            val screenWidth = context.resources.displayMetrics.widthPixels
            val expected = screenWidth.toFloat() / gridExpectedSize.toFloat()
            var spanCount = expected.roundToInt()
            if (spanCount == 0) {
                spanCount = 1
            }
            return spanCount
        }

        fun getFillMode(playerTextureView: GesturePlayerTextureView, path: String?): FillModeCustomItem {
            val resolution = getVideoResolution(path)
            return FillModeCustomItem(
                    playerTextureView!!.scaleX,
                    playerTextureView!!.rotation,
                    playerTextureView!!.translationX / playerTextureView!!.baseWidthSize * 2f,
                    playerTextureView!!.translationY / playerTextureView!!.baseWidthSize * 2f,
                    resolution.width.toFloat(),
                    resolution.height.toFloat()
            )
        }


        fun getFill(sceneCropColor: SceneCropColor = SceneCropColor.WHITE): GlFilter {
            val glFilter = GlFilter()
            val clearColorItem = sceneCropColor.clearColorItem
            glFilter.setClearColor(clearColorItem.red, clearColorItem.green, clearColorItem.blue, clearColorItem.alpha)
            return glFilter
        }

        fun exportMp4ToGallery(context: Context, filePath: String) {
            val values = ContentValues(2)
            values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            values.put(MediaStore.Video.Media.DATA, filePath)
            context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                    Uri.parse("file://$filePath")))
        }

        fun getVideoResolution(path: String?): Size {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(path)
            val width = Integer.valueOf(
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            )
            val height = Integer.valueOf(
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            )
            retriever.release()
            val rotation = getVideoRotation(path)
            return if (rotation == 90 || rotation == 270) {
                Size(height, width)
            } else Size(width, height)
        }


        fun getVideoRotation(videoFilePath: String?): Int {
            val mediaMetadataRetriever = MediaMetadataRetriever()
            mediaMetadataRetriever.setDataSource(videoFilePath)
            val orientation = mediaMetadataRetriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION
            )
            return Integer.valueOf(orientation)
        }
    }
}
