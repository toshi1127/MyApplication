package com.example.user.myapplication.util.apiServices

import com.example.user.myapplication.util.imageLoader
import com.example.user.myapplication.util.receiveData.resultImages
import retrofit2.Call
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.http.Path



interface getResultImages {
    @POST("send")
    fun getresultImages(@Body targeImages: imageLoader): Call<resultImages>
}