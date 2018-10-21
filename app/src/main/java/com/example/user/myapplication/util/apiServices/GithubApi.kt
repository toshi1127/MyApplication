package com.example.user.myapplication.util.apiServices
import com.example.user.myapplication.util.receiveData.User

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.Call

interface GithubApi {
    @GET("users/{user_name}")
    fun fetchUser(@Path("user_name") userName: String): Call<User>
}