package com.example.user.myapplication.util.apiServices

import com.example.user.myapplication.util.imageLoader
import com.example.user.myapplication.util.receiveData.ZipApiData
import com.example.user.myapplication.util.receiveData.resultImages
import retrofit2.Call
import retrofit2.http.*


interface getResultImages {
    @GET("api/search")
    fun apiDemo(@Query("zipcode") ZipCode: String): Call<ZipApiData>
//    @POST("send")
//    fun getresultImages(@Body targeImages: imageLoader): String
}