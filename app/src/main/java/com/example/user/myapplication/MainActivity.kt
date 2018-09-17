package com.example.user.myapplication

import android.app.Activity
import android.view.View
import android.widget.ImageView
import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.Manifest
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.content.Intent
import android.graphics.Matrix
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.media.ExifInterface
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.widget.ImageView.ScaleType
import android.widget.RelativeLayout.LayoutParams
import android.widget.*
import android.support.v4.content.FileProvider
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.calib3d.Calib3d.findHomography
import org.opencv.calib3d.Calib3d.RANSAC
import org.opencv.features2d.DescriptorMatcher
import org.opencv.features2d.Features2d
import org.opencv.imgproc.Imgproc
import org.nield.kotlinstatistics.median

import java.text.SimpleDateFormat
import java.io.FileDescriptor
import java.io.File
import java.io.IOException
import java.util.*

import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI

import com.example.user.myapplication.util.featureDrawer
import com.example.user.myapplication.util.imageLoader
import com.example.user.myapplication.match.normalMatching
import com.example.user.myapplication.calculateChange.calculateChange
import kotlinx.coroutines.experimental.launch

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
    var cameraFilePath: String? = null
    var RANSACMatched: MutableList<DMatch> = mutableListOf()

    var Img1: Bitmap? = null
    var Img2: Bitmap? = null

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

        val median = sequenceOf(1.0, 3.0, 5.0).median()
        println("median" + median) // prints "3.0"

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
            cameraFilePath = cameraFile.path
            ImgUri = FileProvider.getUriForFile(
                    this,
                    getApplicationContext().getPackageName() + ".fileprovider",
                    cameraFile)

            val intent = Intent()
            intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE)
            intent.putExtra(MediaStore.EXTRA_OUTPUT,ImgUri)
            startActivityForResult(intent, RESULT_CAMERA)
        }

        // 決定ボタンのリスナー
        decition_btn.setOnClickListener {
            try {
                // src_img1の画像をMatに
                val scene1 = Mat(Img1!!.height, Img1!!.width, CvType.CV_8UC1).apply { Utils.bitmapToMat(Img1, this) }

                // src_img2の画像をMatに
                val scene2 = Mat(Img2!!.height, Img2!!.width, CvType.CV_8UC1).apply { Utils.bitmapToMat(Img2, this) }

                // 角度を変更するためのクラス
                val calculateChanger = calculateChange(scene1, scene2)

                // 特徴点抽出
                val featureMatcher = featureDrawer(selectItem, scene1, scene2)
                val (keypoint1, keypoint2) = featureMatcher.featureExtractions()
                val (descriptor1, descriptor2) = featureMatcher.featureDraws(keypoint1, keypoint2)

                val matchAlg = DescriptorMatcher.BRUTEFORCE_HAMMING

                val normalMatch = normalMatching(matchAlg, distance, keypoint1, keypoint2)

                var matches_list : MutableList<DMatch> ?= null
                var count: Int ?= null
                var pts1After: MatOfPoint2f ?= null
                var pts2After: MatOfPoint2f ?= null
                var calculateChangeImage = Mat()
                var inliers = Mat()

                launch(UI) {
                    val(matches_list2, count2) = featurePointMatchsResult(normalMatch, descriptor1, descriptor2).await()
                    matches_list = matches_list2
                    count = count2

                    val(pts1After2, pts2After2) = filterMatchesResult(normalMatch, matches_list2).await()
                    pts1After = pts1After2
                    pts2After = pts2After2

                    var RansacMatch = findHomography(pts2After, pts1After, RANSAC, 5.0, inliers, 2000, 0.995)

//                    Score = inliersCounter.toDouble()/matches_list!!.size.toDouble()
//
//                    println("Score" + Score)

                    // ホモグラフィ変換
                    calculateChangeImage = calculateChangeResult(calculateChanger, RansacMatch).await()

                    val keypoint = featureMatcher.featureExtraction(calculateChangeImage)
                    val descriptor = featureMatcher.featureDraw(keypoint, calculateChangeImage)

                    val normalMatch = normalMatching(matchAlg, distance, keypoint, keypoint2)
                    val (matches_list3, count3) = featurePointMatchsResult(normalMatch, descriptor, descriptor2).await()
                    matches_list = matches_list3
                    count = count3

                    val (pts1After3, pts2After3) = filterMatchesResult(normalMatch, matches_list3).await()
                    pts1After = pts1After3
                    pts2After = pts2After3

                    findHomography(pts2After, pts1After, RANSAC, 5.0, inliers, 2000, 0.995)
                    for (i in 0 until matches_list!!.size) {
                        val values = inliers.get(i, 0)
                        if (values[0] == 1.0) {
                            inliersCounter++
                            RANSACMatched.add(matches_list!!.get(i))
                        }
                    }
                    Score = inliersCounter.toDouble()/matches_list!!.size.toDouble()

                    println("Score" + Score)
                    val RANSACMatches = MatOfDMatch().apply { this.fromList(RANSACMatched) }


                    // 結果画像の背景真っ黒になるのを防ぐ
                    val scene1rgb = Mat().apply { Imgproc.cvtColor(calculateChangeImage, this, Imgproc.COLOR_RGBA2RGB, 1) }
                    val scene2rgb = Mat().apply { Imgproc.cvtColor(scene2, this, Imgproc.COLOR_RGBA2RGB, 1) }

                    // マッチ結果を出力
                    val dest = scene1.clone().apply {
                        Features2d.drawMatches(scene1rgb, keypoint, scene2rgb, keypoint2, RANSACMatches, this)
                    }

                    val result_btm: Bitmap = Bitmap.createBitmap(dest.cols(), dest.rows(), Bitmap.Config.ARGB_8888).apply { Utils.matToBitmap(dest, this) }

                    // val resizeesultImg = Mat(((dest.cols())*0.7).toInt(), ((dest.rows())*0.7).toInt(), CvType.CV_8UC1)
                    // Imgproc.resize( dest, resizeesultImg, scene1.size() )

                    // val result_btm: Bitmap = Bitmap.createBitmap(dest.cols(), dest.rows(), Bitmap.Config.ARGB_8888).apply { Utils.matToBitmap(dest, this) }

                    // マッチング結果画像の出力
                    result_img.setImageBitmap(result_btm)

                    // マッチング数を出力
                    count_txt.text = "マッチング数: ${count}"

                    // Scoreを出力
                    score.text = "Score: ${Score}"

                }

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
                    if (requestCode == RESULT_PICK_IMAGEFILE1) {
                        Img1 = getImages(uri)
                    } else {
                        Img2 = getImages(uri)
                    }
                    image_view.setImageBitmap(getImages(uri))
                    val exifInterface = ExifInterface(getPathFromUri(uri))//ここで落ちている
                    var orientation: Int = (exifInterface.getAttribute(ExifInterface.TAG_ORIENTATION)).toInt()
                    println(orientation)
                    // var wm: WindowManager = getSystemService(WINDOW_SERVICE) as WindowManager
                    // val disp = wm.defaultDisplay
                    // var viewWidth = disp.getWidth()
                    // setMatrix(image_view, getImages(uri), orientation, viewWidth)

                } catch(e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        if (requestCode == RESULT_CAMERA) {
            var image_view: ImageView = src_img1
            val exifInterface = ExifInterface(cameraFilePath)// FilePathで正常に動作する。
            var orientation: Int = (exifInterface.getAttribute(ExifInterface.TAG_ORIENTATION)).toInt()
            image_view.setImageURI(ImgUri)
            println(orientation)
            // var wm: WindowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            // val disp = wm.defaultDisplay
            // var viewWidth = disp.getWidth()
            // setMatrix(image_view, getImages(ImgUri!!), orientation, viewWidth)
        }
    }

    fun getPathFromUri(uri : Uri) : String? {
        val isAfterKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
        // DocumentProvider
        Log.d("URI","uri:${uri.authority}")
        if (isAfterKitKat && DocumentsContract.isDocumentUri(this, uri)) {
            if ("com.android.externalstorage.documents" == uri.authority) {
                // ExternalStorageProvider
                val split = DocumentsContract.getDocumentId(uri).split(":")
                val type = split[0]
                if ("primary".equals(type, true)) {
                    return "${Environment.getExternalStorageDirectory()}/${split[1]}"
                }else {
                    return "/stroage/$type/${split[1]}"
                }
            }else if ("com.android.providers.downloads.documents" == uri.authority) {
                // DownloadsProvider
                val id = DocumentsContract.getDocumentId(uri)
                val contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), id.toLong())
                return getDataColumn(contentUri, null, null)
            }else if ("com.android.providers.media.documents" == uri.authority) {
                // MediaProvider
                val split = DocumentsContract.getDocumentId(uri).split(":")
                return getDataColumn(MediaStore.Files.getContentUri("external"), "_id=?", Array(1, {split[1]}))
            }
        }else if ("content".equals(uri.scheme, true)) {
            //MediaStore
            return getDataColumn(uri, null, null)
        }else if ("file".equals(uri.scheme, true)) {
            // File
            return uri.path
        }
        return null
    }

    fun getDataColumn(uri : Uri, selection : String?, selectionArgs : Array<String>?) : String? {
        var cursor : Cursor? = null
        val projection = Array(1, {MediaStore.Files.FileColumns.DATA})
        try {
            cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndexOrThrow(projection[0]))
            }
        } finally {
            if (cursor != null)
                cursor.close()
        }
        return null
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
    private fun featurePointMatchsResult(normalMatch: normalMatching, descriptor1: Mat, descriptor2: Mat): Deferred<Pair<MutableList<DMatch>, Int>> = async(CommonPool) {
        val (matches_list, count) = normalMatch.featurePointMatchs(descriptor1, descriptor2)
        return@async Pair(matches_list, count)
    }

    private fun filterMatchesResult(normalMatch: normalMatching, matches_list: MutableList<DMatch>): Deferred<Pair<MatOfPoint2f, MatOfPoint2f>> = async(CommonPool) {
        val (pts1After, pts2After) = normalMatch.filterMatches(matches_list)
        return@async Pair(pts1After, pts2After)
    }

    private fun calculateChangeResult(calculateChanger: calculateChange, RansacMatch: Mat): Deferred<Mat> = async(CommonPool) {
        val calculateChangeImage = calculateChanger.homographyCalculateChange(RansacMatch)
        return@async calculateChangeImage
    }

    private fun setMatrix(view: ImageView, bitmap: Bitmap, orientation: Int, width: Int){
		view.setScaleType(ScaleType.MATRIX)
		view.setImageBitmap(bitmap)
		var wOrg = bitmap.getWidth()
		var hOrg = bitmap.getHeight()
		var lp :LayoutParams = view.getLayoutParams() as LayoutParams
		var factor: Float? = null
		var mat = Matrix()
		mat.reset()
		when(orientation)
		{
			1 -> {
                factor = width/wOrg.toFloat()
                mat.preScale(factor, factor)
                lp.width = wOrg*factor.toInt()
                lp.height = hOrg*factor.toInt()
            }
			2 -> {
                factor = width/wOrg.toFloat()
                mat.postScale(factor, -factor)
                mat.postTranslate(0f, hOrg*factor)
                lp.width = wOrg*factor.toInt()
                lp.height = hOrg*factor.toInt()
            }
			3 -> {
                mat.postRotate(180f, wOrg/2f, hOrg/2f)
                factor = width/wOrg.toFloat()
                mat.postScale(factor, factor)
                lp.width = wOrg*factor.toInt()
                lp.height = hOrg*factor.toInt()
            }
            4 -> {
                factor = width/wOrg.toFloat()
                mat.postScale(-factor, factor)
                mat.postTranslate(wOrg*factor, 0f)
                lp.width = wOrg*factor.toInt()
                lp.height = hOrg*factor.toInt()
            }
			5 -> {
                mat.postRotate(270f, 0f, 0f)
                factor = width/hOrg.toFloat()
                mat.postScale(factor, -factor)
                lp.width = hOrg*factor.toInt()
                lp.height = wOrg*factor.toInt()
            }
			6 -> {
                mat.postRotate(90f, 0f, 0f)
                factor = width/wOrg.toFloat()
                mat.postScale(factor, factor)
                mat.postTranslate(hOrg*factor, 0f)
                lp.width = hOrg*factor.toInt()
                lp.height = wOrg*factor.toInt()
            }
            7 -> {
                mat.postRotate(90f, 0f, 0f)
                factor = width/wOrg.toFloat()
                mat.postScale(factor, -factor)
                mat.postTranslate(hOrg*factor, wOrg*factor)
                lp.width = hOrg*factor.toInt()
                lp.height = wOrg*factor.toInt()
            }
            8 -> {
                mat.postRotate(270f, 0f, 0f)
                factor = width/wOrg.toFloat()
                mat.postScale(factor, factor)
                mat.postTranslate(0f, wOrg*factor)
                lp.width = hOrg*factor.toInt()
                lp.height = wOrg*factor.toInt()
            }
		}
		view.setLayoutParams(lp)
		view.setImageMatrix(mat)
		view.invalidate()
	}

    companion object {
        private val RESULT_PICK_IMAGEFILE1: Int = 1001
        private val RESULT_PICK_IMAGEFILE2: Int = 1002
        private val RESULT_CAMERA: Int = 1003
        private val REQUEST_PERMISSION: Int = 1004
    }
}