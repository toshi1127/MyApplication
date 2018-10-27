package com.example.user.myapplication.util.apiServices

import com.example.user.myapplication.util.receiveData.User
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class getUserSample {
    companion object {
        private const val BASE_URL = "http://10.0.2.2"
//        private const val BASE_URL = "http://192.168.11.3"
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder().baseUrl(BASE_URL).addConverterFactory(GsonConverterFactory.create()).build()
    }

    private val githubApi: GithubApi by lazy {
        retrofit.create(GithubApi::class.java)
    }

    fun fetchUser(userName: String): Deferred<Any> = async(CommonPool) {
        try {
            val userCall = githubApi.fetchUser(userName)
            val response = userCall.execute()
            response?.let {
                if (response.isSuccessful) {
                    return@async response.body()!!
                }
            }
            return@async User()
        }  catch (e: Exception) {
            print(e)
        }
    }
}