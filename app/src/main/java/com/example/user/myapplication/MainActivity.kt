package com.example.user.myapplication

import android.app.Activity
import android.view.View
import android.Manifest
import android.os.Environment
import android.content.pm.PackageManager
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import android.widget.*
import android.support.v4.content.FileProvider
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.calib3d.Calib3d.findHomography
import org.opencv.calib3d.Calib3d.RANSAC
import org.opencv.features2d.AKAZE
import org.opencv.features2d.ORB
import org.opencv.features2d.DescriptorMatcher
import org.opencv.features2d.Features2d
import org.opencv.imgproc.Imgproc

import java.text.SimpleDateFormat
import java.io.FileDescriptor
import java.io.File
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity(),AdapterView.OnItemSelectedListener {

    var selectItem: String = "ORB"
    var Score: Double? = null
    var distance: Int = 10
    var inliersCounter: Int = 0
    var PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE )
    var ImgUri: Uri? = null
    var filePath: String? = null
    var RANSACMatched: MutableList<DMatch> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        checkPermission()

        var algorithmsSpinner:Spinner = extractionAlgorithms
        var distanceSpinner:Spinner = distanceValues

        var algorithmList = ArrayAdapter.createFromResource(this, R.array.extractionAlgorithmList, android.R.layout.simple_spinner_item)
        var distanceValueList = ArrayAdapter.createFromResource(this, R.array.distanceValues, android.R.layout.simple_spinner_item)

        algorithmsSpinner!!.setOnItemSelectedListener(this)
        algorithmList.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        algorithmsSpinner!!.setAdapter(algorithmList)

        distanceSpinner!!.setOnItemSelectedListener(this)
        distanceValueList.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        distanceSpinner!!.setAdapter(distanceValueList)

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

        load_cameraImage.setOnClickListener {
            // 保存先のフォルダーを作成
            val cameraFolder = File(
                    Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_PICTURES), "IMG")
            cameraFolder.mkdirs()

            // 保存ファイル名
            val fileName = SimpleDateFormat(
                    "ddHHmmss", Locale.US).format(Date())
            filePath = String.format("%s/%s.jpg", cameraFolder.path, fileName)

            // capture画像のファイルパス
            var cameraFile = File(filePath)
            ImgUri = FileProvider.getUriForFile(
                    this,
                    getApplicationContext().getPackageName() + ".fileprovider",
                    cameraFile)

            val intent = Intent()
            intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE)
            intent.putExtra(MediaStore.EXTRA_OUTPUT,ImgUri)
            startActivityForResult(intent, RESULT_CAMERA)

            //そのまま取り込む方法（画像の解像度が下がる）
            /*
            val intent = Intent()
            intent.action = MediaStore.ACTION_IMAGE_CAPTURE
            startActivityForResult(intent, RESULT_CAMERA)*/
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

                // アルゴリズムはORB or AKAZEで
                val algorithm = if (selectItem == "ORB") ORB.create() else AKAZE.create()

                // 特徴点抽出
                val keypoint1 = MatOfKeyPoint().apply { algorithm.detect(scene1, this) }
                val keypoint2 = MatOfKeyPoint().apply { algorithm.detect(scene2, this) }

                // 特徴量記述
                val descriptor1 = Mat().apply { algorithm.compute(scene1, keypoint1, this) }
                val descriptor2 = Mat().apply { algorithm.compute(scene2, keypoint2, this) }

                // マッチング (アルゴリズムにはBruteForceを使用)
                val matcher = DescriptorMatcher.create("BruteForce-Hamming")

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
                        if(backward.distance <= distance){
                            matches_list.add(forward)
                            count++
                        }
                    }
                }

                val matches = MatOfDMatch().apply { this.fromList(matches_list) }

                //RANSACによるモデルの推定
                var pts1: MutableList<Point> = mutableListOf()
                var pts2: MutableList<Point> = mutableListOf()
                for(mat in matches_list) {
                    pts1.add(keypoint1.toList().get(mat.queryIdx).pt)
                    pts2.add(keypoint2.toList().get(mat.trainIdx).pt)
                }

                var pts1After = MatOfPoint2f()
                pts1After.fromList(pts1)
                var pts2After = MatOfPoint2f()
                pts2After.fromList(pts2)

                var inliers = Mat()

                var RansacMatch = findHomography(pts2After, pts1After, RANSAC, 1.0, inliers, 2000, 0.995)

                if (RansacMatch.empty()){
                    return@setOnClickListener
                }

                for (i in 0 until matches_list.size) {
                    val values = inliers.get(i, 0)
                    if (values[0] == 1.0) {
                        inliersCounter++
                        RANSACMatched.add(matches_list.get(i))
                    }
                }

                Score = inliersCounter.toDouble()/matches_list.size.toDouble()

                println("Score" + Score)
                val RANSACMatches = MatOfDMatch().apply { this.fromList(RANSACMatched) }

                // 結果画像の背景真っ黒になるのを防ぐ
                val scene1rgb = Mat().apply { Imgproc.cvtColor(scene1, this, Imgproc.COLOR_RGBA2RGB, 1) }
                val scene2rgb = Mat().apply { Imgproc.cvtColor(scene2, this, Imgproc.COLOR_RGBA2RGB, 1) }

                // マッチ結果を出力
                val dest = scene1.clone().apply {
                    Features2d.drawMatches(scene1rgb, keypoint1, scene2rgb, keypoint2, RANSACMatches, this)
                }

                val result_btm: Bitmap = Bitmap.createBitmap(dest.cols(), dest.rows(), Bitmap.Config.ARGB_8888)
                        .apply { Utils.matToBitmap(dest, this) }

                // マッチング結果画像の出力
                result_img.setImageBitmap(result_btm)

                // マッチング数を出力
                count_txt.text = "マッチング数: ${count}"

                // Scoreを出力
                score.text = "Score: ${Score}"

            } catch(e: NullPointerException) {
                e.printStackTrace()
            }
        }

    }

    override fun onItemSelected(arg0: AdapterView<*>, arg1: View, position: Int, id: Long) {
        when (position) {
            0 -> {
                if(arg0.getItemAtPosition(position) == "ORB"){
                    selectItem = "ORB"
                    Log.d("selectItem", "ORB")
                } else {
                    distance = Integer.parseInt(arg0.getItemAtPosition(position) as String)
                }
            }
            1 -> {
                if(arg0.getItemAtPosition(position) == "AKAZE"){
                    selectItem = "AKAZE"
                    Log.d("selectItem", "AKAZE")
                } else {
                    distance = Integer.parseInt(arg0.getItemAtPosition(position) as String)
                }
            }
            else -> {
                distance = Integer.parseInt(arg0.getItemAtPosition(position) as String)
            }
        }
    }

    override fun onNothingSelected(arg0: AdapterView<*>) {
        selectItem = "ORB"
        distance = 10
    }

    // startActivityForResultで呼ばれる
    override fun onActivityResult(requestCode: Int, resultCode: Int, resultdata: Intent?) {
        if((requestCode == RESULT_PICK_IMAGEFILE1 || requestCode == RESULT_PICK_IMAGEFILE2)
                && resultCode == Activity.RESULT_OK) {
            val image_view: ImageView =
                    if(requestCode == RESULT_PICK_IMAGEFILE1) src_img1
                    else src_img2

            if(resultdata?.data != null) {
                try {
                    val uri: Uri = resultdata.data
                    image_view.setImageBitmap(getImages(uri))
                } catch(e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        if (requestCode == RESULT_CAMERA) {
            var image_view: ImageView = src_img1
            image_view.setImageURI(ImgUri)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,permissions: Array<out String>, grantResults: IntArray){
        if(requestCode == REQUEST_PERMISSION){
            if (grantResults.size> 0) {
                for (i in 0 until permissions.size-1) {
                    if (permissions[i].equals(Manifest.permission.CAMERA)) {
                        if (grantResults[i] === PackageManager.PERMISSION_GRANTED) {
                            // 許可された
                            Log.d("permission", "CAMERA")
                        } else {
                            // それでも拒否された時の対応
                        }
                    } else if (permissions[i].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        if (grantResults[i] === PackageManager.PERMISSION_GRANTED) {
                            // 許可された
                            Log.d("permission", "WRITE_EXTERNAL_STORAGE")
                        } else {
                            // それでも拒否された時の対応
                        }
                    } else if (permissions[i].equals(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                        if (grantResults[i] === PackageManager.PERMISSION_GRANTED) {
                            // 許可された
                            Log.d("permission", "READ_EXTERNAL_STORAGE")
                        } else {
                            // それでも拒否された時の対応
                        }
                    }
                }
            }
        }
    }

    private fun getImages(uri: Uri): Bitmap{
        val parcelFileDesc: ParcelFileDescriptor = getContentResolver().openFileDescriptor(uri, "r")
        if(parcelFileDesc != null) {
            val fDesc: FileDescriptor = parcelFileDesc.fileDescriptor
            val bmp: Bitmap = BitmapFactory.decodeFileDescriptor(fDesc)
            parcelFileDesc.close()
            return bmp
        } else{
            throw error("parcelFileDesc is not exist")
        }
    }

    // BitmapをImageViewから取得する
    private fun getBitmapFromImageView(view: ImageView): Bitmap {
        view.getDrawingCache(true)
        return (view.drawable as BitmapDrawable)?.let { it.bitmap }
    }

    private fun checkPermission(){
        Log.d("permission", "checkPermissionsArray")
        if (checkPermissionsArray(PERMISSIONS)){
            Log.d("permission", "finish_checkPermissionsArray")
            return
        } else {
            Log.d("permission", "requestPermission")
            requestPermission()
        }
    }

    private fun requestPermission() {
        Log.d("permission", "shouldShowRequestPermissionRationale")
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Log.d("permission", "requestPermissions")
            ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_PERMISSION)
        }
        ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_PERMISSION)
    }

    private fun checkPermissionsArray(permissions: Array<String>): Boolean {
        Log.d("permission", "checkSelfPermission")
        for (permission in permissions) {
            val permissionRequest = ActivityCompat.checkSelfPermission(this, permission)
            if (permissionRequest != PackageManager.PERMISSION_GRANTED)
                return false
        }
        return true
    }

    companion object {
        private val RESULT_PICK_IMAGEFILE1: Int = 1001
        private val RESULT_PICK_IMAGEFILE2: Int = 1002
        private val RESULT_CAMERA: Int = 1003
        private val REQUEST_PERMISSION: Int = 1004
    }
}