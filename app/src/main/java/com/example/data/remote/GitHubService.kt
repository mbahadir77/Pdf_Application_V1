package com.example.data.remote

import com.example.data.remote.model.CreateGitHubIssueRequest
import com.example.data.remote.model.GitHubIssue
import com.example.data.remote.model.GitHubRelease
import com.example.data.remote.model.GitHubUser
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * İlmNet - GitHub API Servis Arayüzü (Retrofit v2).
 * Repository Issues sekmesini dağıtık akademik post havuzu, Releases sekmesini ise PDF depolama olarak kullanır.
 */
interface GitHubService {
    @GET("users/{username}")
    suspend fun getUserProfile(
        @Path("username") username: String
    ): Response<GitHubUser>

    @GET("repos/{owner}/{repo}/releases")
    suspend fun getReleases(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<List<GitHubRelease>>

    @GET("repos/{owner}/{repo}/issues")
    suspend fun getRepoIssues(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("state") state: String = "open",
        @Query("per_page") perPage: Int = 30
    ): Response<List<GitHubIssue>>

    @POST("repos/{owner}/{repo}/issues")
    suspend fun createIssue(
        @Header("Authorization") token: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body request: CreateGitHubIssueRequest
    ): Response<GitHubIssue>

    @GET("repos/{owner}/{repo}/issues/{issue_number}/comments")
    suspend fun getIssueComments(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("issue_number") issueNumber: Long
    ): Response<List<com.example.data.remote.model.GitHubIssueComment>>

    @POST("repos/{owner}/{repo}/issues/{issue_number}/comments")
    suspend fun createIssueComment(
        @Header("Authorization") token: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("issue_number") issueNumber: Long,
        @Body request: com.example.data.remote.model.CreateGitHubCommentRequest
    ): Response<com.example.data.remote.model.GitHubIssueComment>

    @retrofit2.http.DELETE("repos/{owner}/{repo}/issues/comments/{comment_id}")
    suspend fun deleteIssueComment(
        @Header("Authorization") token: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("comment_id") commentId: Long
    ): Response<Unit>
}
