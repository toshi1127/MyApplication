package com.example.user.myapplication.util

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.widget.ImageView
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat

/**
 * Created by user on 2018/08/07.
 */
class imageLoader(Img: ImageView) {
    var bitmap: Bitmap = Bitmap.createScaledBitmap(getBitmapFromImageView(Img), 640, 480, false)

    private fun getBitmapFromImageView(view: ImageView): Bitmap {
        view.getDrawingCache(true)
        return (view.drawable as BitmapDrawable)?.let { it.bitmap }
    }

    fun getImageMat(): Mat {
        val imageMat = Mat(bitmap!!.height, bitmap!!.width, CvType.CV_8UC1).apply { Utils.bitmapToMat(bitmap, this) }
        println("画像の生成")
        return imageMat
    }
}