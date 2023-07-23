package net.xpadev.file_downloader

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.work.Data
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import java.net.URL
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {
    private val manager = WorkManager.getInstance()
    private val google = GoogleUtils(applicationContext)
    private val pref = PrefUtils(applicationContext)
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
        val startProcessButton = findViewById<Button>(R.id.StartProcessButton)
        val saveButton = findViewById<Button>(R.id.SaveButton)
        val authButton = findViewById<Button>(R.id.gcp_auth)
        val targetListApiEndpointInput = findViewById<EditText>(R.id.targetListApiEndpointInput)
        val gcpClientIdInput = findViewById<EditText>(R.id.gcp_client_id)
        val gcpClientSecretInput = findViewById<EditText>(R.id.gcp_client_secret)

        targetListApiEndpointInput.setText(pref.get(Val.Pref.endpoint))
        gcpClientIdInput.setText(pref.get(Val.Pref.gcpClientId))
        gcpClientSecretInput.setText(pref.get(Val.Pref.gcpClientSecret))
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
            val clientId = gcpClientIdInput.text.toString();
            val gso = google.getGoogleSignInOptions(clientId)
            val mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
            Log.i("Main","start activity")

            val startForResult =
                registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult? ->
                    google.processLogin(result)
                }
            startForResult.launch(mGoogleSignInClient.signInIntent)


        }
    }

    private fun save(url: String,gcpClientId: String, gcpClientSecret: String): Boolean{
        if (!isValidURL(url)){
            Toast.makeText(applicationContext, "正しくないURLです。", Toast.LENGTH_SHORT).show()
            return false
        }
        Toast.makeText(applicationContext, "保存しました", Toast.LENGTH_SHORT).show()
        pref.save(Val.Pref.endpoint,url)
        pref.save(Val.Pref.gcpClientId, gcpClientId)
        pref.save(Val.Pref.gcpClientSecret, gcpClientSecret)
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