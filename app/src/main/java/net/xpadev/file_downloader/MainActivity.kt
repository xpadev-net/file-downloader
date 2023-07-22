package net.xpadev.file_downloader

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.work.Data
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import java.net.URL
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {
    private val manager = WorkManager.getInstance()
    private lateinit var sharedPref: SharedPreferences
    private val REQUEST_CODE = 1
    private val encrypt = EncryptionUtils()
    override fun onStart() {
        super.onStart()

        val account = GoogleSignIn.getLastSignedInAccount(this)
        if(account != null) {
            Log.i("Main",account.account.toString())
            Log.i("Main",account.id.toString())
            Log.i("Main",account.idToken.toString())
            Log.i("Main",account.serverAuthCode.toString())
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        this.sharedPref = getSharedPreferences(
            "net.xpadev.file_downloader", Context.MODE_PRIVATE);
        val startProcessButton = findViewById<Button>(R.id.StartProcessButton)
        val saveButton = findViewById<Button>(R.id.SaveButton)
        val authButton = findViewById<Button>(R.id.gcp_auth)
        val targetListApiEndpointInput = findViewById<EditText>(R.id.targetListApiEndpointInput)
        val gcpClientIdInput = findViewById<EditText>(R.id.gcp_client_id)
        val gcpClientSecretInput = findViewById<EditText>(R.id.gcp_client_secret)

        targetListApiEndpointInput.setText(this.sharedPref.getString("endpoint", ""))
        gcpClientIdInput.setText(encrypt.decrypt(this.sharedPref.getString("gcp_clientId", "")))
        gcpClientSecretInput.setText(encrypt.decrypt(this.sharedPref.getString("gcp_clientSecret", "")))
        Log.i("Main", "init")

        startProcessButton.setOnClickListener {
            Log.i("Main", "pressed")
            val value = targetListApiEndpointInput.text.toString()
            if (!save(targetListApiEndpointInput.text.toString(),gcpClientIdInput.text.toString(),gcpClientSecretInput.text.toString())){
                return@setOnClickListener;
            }
            val request = PeriodicWorkRequestBuilder<SyncWorker>(15,TimeUnit.MINUTES).apply {
                val data = Data.Builder().apply {
                    putString("endpoint", value)
                }.build()
                setInputData(data)
            }.build()
            manager.enqueue(request)
        }
        saveButton.setOnClickListener{
            save(targetListApiEndpointInput.text.toString(),gcpClientIdInput.text.toString(),gcpClientSecretInput.text.toString());
        }
        authButton.setOnClickListener{

            val mScope = Scope("https://www.googleapis.com/auth/photoslibrary")
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestServerAuthCode(gcpClientIdInput.text.toString())
                .requestScopes(mScope)
                .build()
            val mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
            Log.i("Main","start activity")

            startActivityForResult(mGoogleSignInClient.signInIntent, REQUEST_CODE);


        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE && data != null) {
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            val account = result?.signInAccount
            if (account != null) {
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
    private fun save(url: String,gcpClientId: String, gcpClientSecret: String): Boolean{
        if (!isValidURL(url)){
            Toast.makeText(applicationContext, "正しくないURLです。", Toast.LENGTH_SHORT).show()
            return false
        }
        Toast.makeText(applicationContext, "保存しました", Toast.LENGTH_SHORT).show()
        this.sharedPref.edit().putString("endpoint",url).apply()
        this.sharedPref.edit().putString("gcp_clientId",encrypt.encrypt(gcpClientId)).apply()
        this.sharedPref.edit().putString("gcp_clientSecret",encrypt.encrypt(gcpClientSecret)).apply()
        return true
    }

    private fun isValidURL(urlString: String?): Boolean {
        return try {
            val url = URL(urlString)
            url.toURI()
            true
        } catch (e: Exception) {
            false
        }
    }

}