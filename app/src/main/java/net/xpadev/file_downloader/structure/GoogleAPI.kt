package net.xpadev.file_downloader.structure

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GoogleOauthResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("token_type") val tokenType: String,
    val scope: String,
    @SerialName("refresh_token") val refreshToken: String,
)

@Serializable
data class GoogleTokenExchangeRequestBody(
    @SerialName("client_id") val clientId: String,
    @SerialName("client_secret") val clientSecret: String,
    @SerialName("code") val code: String,
    @SerialName("grant_type") val grantType: String,
    @SerialName("redirect_uri") val redirectUri: String,
)

@Serializable
data class GoogleTokenRefreshRequestBody(
    @SerialName("client_id") val clientId: String,
    @SerialName("client_secret") val clientSecret: String,
    @SerialName("grant_type") val grantType: String,
    @SerialName("refresh_token") val refreshToken: String,
)

@Serializable
data class GoogleTokenRefreshResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("token_type") val tokenType: String,
    val scope: String
)

@Serializable
data class GooglePhotosMediaItem(
    val id: String,
    val filename: String,
)
@Serializable
data class GooglePhotosSearchRequestBody(
    val orderBy: String,
    val pageSize: Int,
    val pageToken: String?,
)

@Serializable
data class GooglePhotosSearchResponse(
    val mediaItems: Array<GooglePhotosMediaItem>,
    val nextPageToken: String?
)