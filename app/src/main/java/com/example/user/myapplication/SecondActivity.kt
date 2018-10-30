package com.example.user.myapplication

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import kotlinx.android.synthetic.main.activity_second.*
import java.io.FileDescriptor

class SecondActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)

        val button: Button = findViewById(R.id.button2)
        result_img.setImageBitmap(getImages(intent.extras.get("imageDate") as Uri))
        answer.text = "撮影画像は${0}番のACアダプターです"
        count_txt.text = "マッチング数: ${intent.extras.get("inliers")}"
        score.text = "Score: ${(intent.extras.get("inliers") as String).toFloat()/(intent.extras.get("matched") as String).toFloat()}"

        button.setOnClickListener {

            // 渡す値を設定
            val intent = Intent()

            // 情報を渡して MainActivity の onActivityResult を呼び出す
            setResult(Activity.RESULT_OK, intent)

            // アクティビティを閉じる
            finish()
        }
    }
    private fun getImages(uri: Uri): Bitmap {
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
}