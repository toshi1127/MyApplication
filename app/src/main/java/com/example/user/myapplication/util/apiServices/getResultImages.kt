package com.example.user.myapplication.util.apiServices

import com.example.user.myapplication.util.receiveData.User
import com.example.user.myapplication.util.receiveData.resultImages
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*


interface getResultImages {
    @Headers(
            "Accept: application/json",
            "Content-type: application/json"
    )
//    @Multipart
    @Streaming
    @POST("send")
    fun getResultImages(
            @Body body: HashMap<String, String>
//            @Part("item") item: RequestBody,
//            @Part("imageNumber") itemNumber: RequestBody,
//            @Part targeImages: MultipartBody.Part
    ): Call<Any>
}