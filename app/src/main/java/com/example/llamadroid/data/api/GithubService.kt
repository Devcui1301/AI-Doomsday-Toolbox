package com.example.llamadroid.data.api

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface GithubService {
    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): ReleaseDto
    
    @GET("repos/{owner}/{repo}/releases")
    suspend fun getReleases(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 5
    ): List<ReleaseDto>
}

data class ReleaseDto(
    val id: Long,
    val tag_name: String,
    val name: String,
    val body: String,
    val assets: List<AssetDto>,
    val published_at: String
)

data class AssetDto(
    val id: Long,
    val name: String,
    val browser_download_url: String,
    val size: Long
)
