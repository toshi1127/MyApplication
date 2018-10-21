package com.example.user.myapplication.util.apiServices

import com.example.user.myapplication.util.receiveData.User
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class getUserSample {
    companion object {
        private const val BASE_URL = "https://api.github.com"
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder().baseUrl(BASE_URL).addConverterFactory(GsonConverterFactory.create()).build()
    }

    private val githubApi: GithubApi by lazy {
        retrofit.create(GithubApi::class.java)
    }

    fun fetchUser(userName: String): Deferred<Any> = async(CommonPool) {
        try {
            print("startFetchUser!!!")
            print(userName)
            val userCall = githubApi.fetchUser(userName)
            print(userCall.request())
            val response = userCall.execute()
            print(userCall.execute().body())
            print(userCall.execute().errorBody())
            print(response)
            print("finishFetchUser!!!")
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