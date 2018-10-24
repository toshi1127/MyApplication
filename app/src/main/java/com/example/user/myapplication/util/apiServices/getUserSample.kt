package com.example.user.myapplication.util.apiServices

import com.example.user.myapplication.util.receiveData.User
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class getUserSample {
    companion object {
        private const val BASE_URL = "http://172.20.10.8"
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
            println(userCall.request())
            val response = userCall.execute()
            response?.let {
                if (response.isSuccessful) {
                    println(response.body()!!)
                    return@async response.body()!!
                }
            }
            return@async User()
        }  catch (e: Exception) {
            print(e)
        }
    }
}