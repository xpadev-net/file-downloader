package net.xpadev.file_downloader

import android.os.Environment

object Val {
    object Pref{
        const val endpoint = "endpoint"
        const val gcpClientId = "gcp_clientId"
        const val gcpClientSecret = "gcp_clientSecret"
        const val gcpCallbackUrl = "gcp_callbackUrl"
        const val gcpAccessToken = "gcp_accessToken"
        const val gcpRefreshToken = "gcp_refreshToken"
        const val prefId = "net.xpadev.file_downloader"
    }
    object Encryption {
        const val key = ",!9=uXJ^;i]8Hp,nB5xzT;t.c{QUt'<P"
    }
    object Google{
        const val tokenEndpoint = "https://oauth2.googleapis.com/token"
        const val scope = "https://www.googleapis.com/auth/photoslibrary.readonly"
        const val photosSearchEndpoint = "https://photoslibrary.googleapis.com/v1/mediaItems:search"
    }
    object Storage{
        const val targetPath = "/storage/emulated/0/DCIM/Camera"
    }
}