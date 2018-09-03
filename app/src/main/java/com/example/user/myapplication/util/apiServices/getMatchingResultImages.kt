package com.example.user.myapplication.util.apiServices

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class getMatchingResultImages {
    companion object {
        fun createService(): getResultImages {
            val apiUrl = "https://api.sample.com/"
            val client = builderHttpClient()
            val gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()
            val retrofit = Retrofit.Builder()
                    .baseUrl(apiUrl)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build()
            return retrofit.create(getResultImages::class.java)
        }

        private fun builderHttpClient(): OkHttpClient {
            val client = OkHttpClient.Builder()
            val logging = HttpLoggingInterceptor()
            logging.level = HttpLoggingInterceptor.Level.BODY
            client.addInterceptor(logging)

            return client.build()
        }
    }
}