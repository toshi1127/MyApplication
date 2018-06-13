package com.example.user.myapplication

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import android.widget.ImageView
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.features2d.AKAZE
import org.opencv.features2d.DescriptorMatcher
import org.opencv.features2d.Features2d
import org.opencv.imgproc.Imgproc
import java.io.FileDescriptor
import java.io.IOException

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        if(!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "error_openCV")
        }

        // 画像選択ボタン1のリスナー
        select_img1_btn.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.setType("*/*")
            startActivityForResult(intent, RESULT_PICK_IMAGEFILE1)
        }

        // 画像選択ボタン2のリスナー
        select_img2_btn.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.setType("*/*")
            startActivityForResult(intent, RESULT_PICK_IMAGEFILE2)
        }

        // 決定ボタンのリスナー
        decition_btn.setOnClickListener {
            try {
                // src_img1の画像をMatに
                var bitmap: Bitmap = Bitmap.createScaledBitmap(getBitmapFromImageView(src_img1), 640, 480, false)
                val scene1 = Mat(bitmap!!.height, bitmap!!.width, CvType.CV_8UC1).apply { Utils.bitmapToMat(bitmap, this) }

                // src_img2の画像をMatに
                bitmap = Bitmap.createScaledBitmap(getBitmapFromImageView(src_img2), 640, 480, false)
                val scene2 = Mat(bitmap!!.height, bitmap!!.width, CvType.CV_8UC1).apply { Utils.bitmapToMat(bitmap, this) }

                // アルゴリズムはAKZEで
                val algorithm: AKAZE = AKAZE.create()

                // 特徴点抽出
                val keypoint1 = MatOfKeyPoint().apply { algorithm.detect(scene1, this) }
                val keypoint2 = MatOfKeyPoint().apply { algorithm.detect(scene2, this) }

                // 特徴量記述
                val descriptor1 = Mat().apply { algorithm.compute(scene1, keypoint1, this) }
                val descriptor2 = Mat().apply { algorithm.compute(scene2, keypoint2, this) }

                // マッチング (アルゴリズムにはBruteForceを使用)
                val matcher = DescriptorMatcher.create("BruteForce")

                var matches_list: MutableList<DMatch> = mutableListOf()
                val match12 = MatOfDMatch().apply { matcher.match(descriptor1, descriptor2, this) }
                val match21 = MatOfDMatch().apply { matcher.match(descriptor2, descriptor1, this) }

                // クロスチェック(1→2と2→1の両方でマッチしたものだけを残して精度を高める)
                val size: Int = match12.toArray().size - 1
                val match12_array = match12.toArray()
                val match21_array = match21.toArray()
                var count: Int = 0
                for(i in 0..size) {
                    val forward: DMatch =match12_array[i]
                    val backward: DMatch = match21_array[forward.trainIdx]
                    if(backward.trainIdx == forward.queryIdx) {
                        matches_list.add(forward)
                        count++
                    }
                }

                val matches = MatOfDMatch().apply { this.fromList(matches_list) }

                // 結果画像の背景真っ黒になるのを防ぐ
                val scene1rgb = Mat().apply { Imgproc.cvtColor(scene1, this, Imgproc.COLOR_RGBA2RGB, 1) }
                val scene2rgb = Mat().apply { Imgproc.cvtColor(scene2, this, Imgproc.COLOR_RGBA2RGB, 1) }

                // マッチ結果を出力
                val dest = scene1.clone().apply {
                    Features2d.drawMatches(scene1rgb, keypoint1, scene2rgb, keypoint2, matches, this)
                }

                val result_btm: Bitmap = Bitmap.createBitmap(dest.cols(), dest.rows(), Bitmap.Config.ARGB_8888)
                        .apply { Utils.matToBitmap(dest, this) }

                // マッチング結果画像の出力
                result_img.setImageBitmap(result_btm)

                // マッチング数を出力
                count_txt.text = "マッチング数: ${count}"

            } catch(e: NullPointerException) {
                e.printStackTrace()
            }
        }

    }

    // 画像を選択したときの動き
    override fun onActivityResult(requestCode: Int, resultCode: Int, resultdata: Intent?) {
        if((requestCode == RESULT_PICK_IMAGEFILE1 || requestCode == RESULT_PICK_IMAGEFILE2)
                && resultCode == Activity.RESULT_OK) {
            val image_view: ImageView =
                    if(requestCode == RESULT_PICK_IMAGEFILE1) src_img1
                    else src_img2

            if(resultdata?.data != null) {
                try {
                    val uri: Uri = resultdata.data
                    val parcelFileDesc: ParcelFileDescriptor = getContentResolver().openFileDescriptor(uri, "r")
                    if(parcelFileDesc != null) {
                        val fDesc: FileDescriptor = parcelFileDesc.fileDescriptor
                        val bmp: Bitmap = BitmapFactory.decodeFileDescriptor(fDesc)
                        parcelFileDesc.close()
                        image_view.setImageBitmap(bmp)
                    }
                } catch(e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    // BitmapをImageViewから取得する
    private fun getBitmapFromImageView(view: ImageView): Bitmap {
        view.getDrawingCache(true)
        return (view.drawable as BitmapDrawable)?.let { it.bitmap }
    }

    companion object {
        private val RESULT_PICK_IMAGEFILE1: Int = 1001
        private val RESULT_PICK_IMAGEFILE2: Int = 1002
    }
}