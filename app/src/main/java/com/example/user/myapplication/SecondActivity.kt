package com.example.user.myapplication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.Toast

class SecondActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)

        println("セカンドページ: ${intent.extras.get("number")}, ${intent.extras.get("string")}, ${intent.extras.get("number")}")

        val button: Button = findViewById(R.id.button2)
        button.setOnClickListener {

            // 渡す値を設定
            val intent = Intent()

            // 情報を渡して MainActivity の onActivityResult を呼び出す
            setResult(Activity.RESULT_OK, intent)

            // アクティビティを閉じる
            finish()
        }
    }
}