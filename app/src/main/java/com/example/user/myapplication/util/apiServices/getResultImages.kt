package com.example.user.myapplication.util.apiServices

import com.example.user.myapplication.util.receiveData.resultImages
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*


interface getResultImages {
    @POST("send")
    fun getResultImages(@Part targeImages: MultipartBody.Part): Call<resultImages>
}