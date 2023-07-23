package net.xpadev.file_downloader

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResult
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope

class GoogleUtils (private val applicationContext: Context) {
    private var network: NetworkUtils = NetworkUtils(applicationContext);
    private val pref = PrefUtils(applicationContext)

    fun getGoogleSignInOptions(clientId: String):GoogleSignInOptions {
        val mScope = Scope("https://www.googleapis.com/auth/photoslibrary")
        return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestServerAuthCode(clientId)
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
                Log.i("Main",account.account.toString())
                Log.i("Main",account.id.toString())
                Log.i("Main",account.idToken.toString())
                Log.i("Main",account.serverAuthCode.toString())
                Log.i("Main",account.account.toString())
                Log.i("Main",account.account.toString())
                Log.i("Main",account.account.toString())
                Log.i("Main",account.account.toString())
                Log.i("Main",account.account.toString())
                Log.i("Main",account.account.toString())
            } catch (e: ApiException) {
                Log.e("Main",e.toString())
            }
        }
    }
}