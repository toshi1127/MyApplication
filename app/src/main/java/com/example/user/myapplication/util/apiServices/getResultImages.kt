package com.example.user.myapplication.util.apiServices

import com.example.user.myapplication.util.receiveData.User
import com.example.user.myapplication.util.receiveData.resultImages
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*
import com.google.gson.JsonObject


interface getResultImages {
    @Headers(
            "Accept: application/json",
            "Content-type: application/json"
    )
//    @Multipart
    @POST("send")
    fun getResultImages(
            @Body body: JsonObject
//            @Part("item") item: RequestBody,
//            @Part("imageNumber") itemNumber: RequestBody,
//            @Part targeImages: MultipartBody.Part
    ): Call<resultImages>
}