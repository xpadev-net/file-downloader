package net.xpadev.file_downloader

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResult
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.xpadev.file_downloader.structure.*
import java.io.FileNotFoundException
import java.io.IOException


class GoogleUtils (private val applicationContext: Context) {
    private var network: NetworkUtils = NetworkUtils(applicationContext);
    private val pref = PrefUtils(applicationContext)
    private var archivedArray = emptyArray<String>()

    fun getGoogleSignInOptions(clientId: String):GoogleSignInOptions {
        val mScope = Scope(Val.Google.scope)
        return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestServerAuthCode(clientId, true)
            .requestEmail()
            .requestScopes(mScope)
            .build()
    }
    fun processLogin(result: ActivityResult?) {
        val data = result?.data
        if (result == null || data == null) return
        val response = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
        val account = response?.signInAccount
        if (account != null) {
            Toast.makeText(applicationContext, "${account.email}でログインしました", Toast.LENGTH_SHORT).show()
            try {
                exchangeCodeToToken(account.serverAuthCode.toString())
            } catch (e: ApiException) {
                Log.e(javaClass.simpleName,e.toString())
            }
        }
    }
    private fun exchangeCodeToToken(code:String){
        val gcpClientId = pref.get(Val.Pref.gcpClientId)
        val gcpClientSecret = pref.get(Val.Pref.gcpClientSecret)
        val gcpCallbackUrl = pref.get(Val.Pref.gcpCallbackUrl)
        val body = Json.encodeToString(GoogleTokenExchangeRequestBody(
            clientId = gcpClientId,
            clientSecret = gcpClientSecret,
            code= code,
            grantType = "authorization_code",
            redirectUri = gcpCallbackUrl,
        ))
        Thread {
            val json = network.postJson<GoogleOauthResponse>(Val.Google.tokenEndpoint,body)
            pref.save(Val.Pref.gcpAccessToken,json.accessToken)
            pref.save(Val.Pref.gcpRefreshToken,json.refreshToken)
        }.start()
    }

    private fun refreshToken() {
        val gcpClientId = pref.get(Val.Pref.gcpClientId)
        val gcpClientSecret = pref.get(Val.Pref.gcpClientSecret)
        val gcpRefreshToken = pref.get(Val.Pref.gcpRefreshToken)
        val body = Json.encodeToString(GoogleTokenRefreshRequestBody(
            clientId = gcpClientId,
            clientSecret = gcpClientSecret,
            refreshToken = gcpRefreshToken,
            grantType = "refresh_token"
        ))
        val json = network.postJson<GoogleTokenRefreshResponse>(Val.Google.tokenEndpoint,body)
        pref.save(Val.Pref.gcpAccessToken,json.accessToken)
    }

    fun getPhotosList(): Array<String>{
        var pageToken: String? = null
        for (i in 1..3){
            Log.i(javaClass.simpleName,"loading photo list (page: ${i})")
            val response = fetchPhotosList(pageToken) ?: return archivedArray
            var flag = false
            response.mediaItems.forEach {item ->
                if (item.filename in archivedArray){
                    flag = true
                }else{
                    archivedArray += item.filename
                }
            }
            if (flag){
                return archivedArray;
            }
            pageToken = response.nextPageToken
        }
        return archivedArray
    }

    private fun fetchPhotosList(pageToken: String? = null): GooglePhotosSearchResponse?{
        val body = Json.encodeToString(GooglePhotosSearchRequestBody(
            orderBy = "MediaMetadata.creation_time desc",
            pageSize = 100,
            pageToken = pageToken,
        ))
        val json = try {
            network.postJson<GooglePhotosSearchResponse>(Val.Google.photosSearchEndpoint,body,true)
        }catch (_: FileNotFoundException){
            Log.i(javaClass.simpleName,"refreshing token...")
            refreshToken()
            network.postJson<GooglePhotosSearchResponse>(Val.Google.photosSearchEndpoint,body,true)
        }catch (_: IOException){
            return null
        }
        return json
    }

    private inline fun<reified T> merge(vararg arrays: Array<T>): Array<T> {
        val list: MutableList<T> = ArrayList()
        for (array in arrays) {
            list.addAll(array.map { i -> i })
        }
        return list.toTypedArray()
    }
}