package com.example.data.remote

import com.example.data.model.GitHubEvent
import com.example.data.model.GitHubIssue
import com.example.data.model.GitHubRelease
import com.example.data.model.GitHubRepo
import com.example.data.model.GitHubUser
import com.example.data.model.IssueComment
import com.example.data.model.RepoContentItem
import com.example.data.model.RepoSearchResponse
import com.example.data.model.UserSearchResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface GitHubApiService {

    @GET("search/repositories")
    suspend fun searchRepositories(
        @Query("q") query: String,
        @Query("sort") sort: String = "stars",
        @Query("order") order: String = "desc",
        @Query("per_page") perPage: Int = 25,
        @Query("page") page: Int = 1
    ): RepoSearchResponse

    @GET("search/users")
    suspend fun searchUsers(
        @Query("q") query: String,
        @Query("per_page") perPage: Int = 25,
        @Query("page") page: Int = 1
    ): UserSearchResponse

    @GET("events")
    suspend fun getPublicEvents(
        @Query("per_page") perPage: Int = 30
    ): List<GitHubEvent>

    @GET("users/{username}")
    suspend fun getUser(
        @Path("username") username: String
    ): GitHubUser

    @GET("users/{username}/repos")
    suspend fun getUserRepos(
        @Path("username") username: String,
        @Query("sort") sort: String = "updated",
        @Query("per_page") perPage: Int = 30
    ): List<GitHubRepo>

    @GET("users/{username}/events/public")
    suspend fun getUserEvents(
        @Path("username") username: String,
        @Query("per_page") perPage: Int = 20
    ): List<GitHubEvent>

    @GET("repos/{owner}/{repo}")
    suspend fun getRepository(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): GitHubRepo

    @GET("repos/{owner}/{repo}/readme")
    suspend fun getReadme(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): RepoContentItem

    @GET("repos/{owner}/{repo}/releases")
    suspend fun getReleases(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 20
    ): List<GitHubRelease>

    @GET("repos/{owner}/{repo}/issues")
    suspend fun getIssues(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("state") state: String = "open",
        @Query("per_page") perPage: Int = 25
    ): List<GitHubIssue>

    @GET("repos/{owner}/{repo}/issues/{number}/comments")
    suspend fun getIssueComments(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("number") number: Int,
        @Query("per_page") perPage: Int = 30
    ): List<IssueComment>

    @GET("repos/{owner}/{repo}/contents")
    suspend fun getRootContents(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): List<RepoContentItem>

    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getDirectoryContents(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path", encoded = true) path: String
    ): List<RepoContentItem>

    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getFileContent(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path", encoded = true) path: String
    ): RepoContentItem
}
