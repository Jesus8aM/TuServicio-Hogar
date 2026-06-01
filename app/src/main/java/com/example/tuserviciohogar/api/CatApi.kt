package com.example.tuserviciohogar.api

import com.example.tuserviciohogar.model.CatImage
import retrofit2.http.GET
import retrofit2.http.Query

interface CatApi {
    @GET("images/search")
    suspend fun getCatImages(
        @Query("limit") limit: Int = 20,
        @Query("page") page: Int = 0
    ): List<CatImage>
} 