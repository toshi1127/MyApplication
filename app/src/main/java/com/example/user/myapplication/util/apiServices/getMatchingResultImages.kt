package com.example.user.myapplication.util.apiServices

import com.example.user.myapplication.util.receiveData.resultImages
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import okhttp3.RequestBody



class getMatchingResultImages {
    companion object {
        private const val BASE_URL = "http://10.0.2.2"
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder().baseUrl(BASE_URL).addConverterFactory(GsonConverterFactory.create()).build()
    }

    private val getMatchingResultImagesApi: getResultImages by lazy {
        retrofit.create(getResultImages::class.java)
    }
    fun getResultImages(targetImage: File): Deferred<Any> = async(CommonPool) {
        try {
            val requestBody = RequestBody.create(MediaType.parse("multipart/form-data"), targetImage)
            val userCall = getMatchingResultImagesApi.getResultImages(requestBody)
            val response = userCall.execute()
            response?.let {
                if (response.isSuccessful) {
                    return@async response.body()!!
                }
            }
            return@async resultImages()
        }  catch (e: Exception) {
            print(e)
        }
    }
//    companion object {
//        fun createService(): getResultImages {
//            val apiUrl = "https://api.sample.com/"
//            val client = builderHttpClient()
//            val gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()
//            val retrofit = Retrofit.Builder()
//                    .baseUrl(apiUrl)
//                    .client(client)
//                    .addConverterFactory(GsonConverterFactory.create(gson))
//                    .build()
//            return retrofit.create(getResultImages::class.java)
//        }
//
//        private fun builderHttpClient(): OkHttpClient {
//            val client = OkHttpClient.Builder()
//            val logging = HttpLoggingInterceptor()
//            logging.level = HttpLoggingInterceptor.Level.BODY
//            client.addInterceptor(logging)
//
//            return client.build()
//        }
//    }
}