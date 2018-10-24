package com.example.user.myapplication.util.apiServices

import com.example.user.myapplication.util.receiveData.resultImages
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import okhttp3.MediaType
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import okhttp3.RequestBody
import okhttp3.MultipartBody



class getMatchingResultImages {
    companion object {
        private const val BASE_URL = "http://192.168.11.3"
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder().baseUrl(BASE_URL).addConverterFactory(GsonConverterFactory.create()).build()
    }

    private val getMatchingResultImagesApi: getResultImages by lazy {
        retrofit.create(getResultImages::class.java)
    }
    fun getResultImages(targetImage: File): Deferred<Any> = async(CommonPool) {
        try {
            val hashMap = HashMap<String, String>()
            hashMap.put("name", "toshi")
            hashMap.put("age", "21")
//            println("targetImage: ${targetImage}")
//            val requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), targetImage.absoluteFile)
//            println("requestBody: ${requestFile}")
//            val requestBodys = MultipartBody.Part.createFormData("uploaded_file", targetImage.name, requestFile)
//            println("body: ${requestBodys}")
//            val ItemId = RequestBody.create(okhttp3.MultipartBody.FORM, "22")
//            val ImageNumber = RequestBody.create(okhttp3.MultipartBody.FORM, "1")
            println("hashMap:${hashMap}")
            val getMatchingResultImages = getMatchingResultImagesApi.getResultImages(hashMap)
            val response = getMatchingResultImages.execute()
            response?.let {
                if (response.isSuccessful) {
                    println(response.body())
                    return@async response.body()!!
                }
            }
            return@async resultImages()
        }  catch (e: Exception) {
            print(e)
        }
    }
}