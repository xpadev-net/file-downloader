package net.xpadev.file_downloader

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import java.net.URL
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {
    private val manager = WorkManager.getInstance()
    private lateinit var google: GoogleUtils
    private lateinit var pref: PrefUtils
    private val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult? ->
            google.processLogin(result)
        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        google = GoogleUtils(applicationContext)
        pref = PrefUtils(applicationContext)
        setContentView(R.layout.activity_main)
        val startProcessButton = findViewById<Button>(R.id.StartProcessButton)
        val saveButton = findViewById<Button>(R.id.SaveButton)
        val authButton = findViewById<Button>(R.id.gcp_auth)
        val targetListApiEndpointInput = findViewById<EditText>(R.id.targetListApiEndpointInput)
        val gcpClientIdInput = findViewById<EditText>(R.id.gcp_client_id)
        val gcpClientSecretInput = findViewById<EditText>(R.id.gcp_client_secret)
        val gcpCallbackUrlInput = findViewById<EditText>(R.id.gcp_callback_url)
        val grantStoragePermissionButton = findViewById<Button>(R.id.storagePermissionButton)

        targetListApiEndpointInput.setText(pref.get(Val.Pref.endpoint))
        gcpClientIdInput.setText(pref.get(Val.Pref.gcpClientId))
        gcpClientSecretInput.setText(pref.get(Val.Pref.gcpClientSecret))
        gcpCallbackUrlInput.setText(pref.get(Val.Pref.gcpCallbackUrl))
        Log.i(javaClass.simpleName, "init")

        createNotificationChannel()

        startProcessButton.setOnClickListener {
            Log.i(javaClass.simpleName, "pressed")
            val value = targetListApiEndpointInput.text.toString()
            if (!save(targetListApiEndpointInput.text.toString(),gcpClientIdInput.text.toString(),gcpClientSecretInput.text.toString(), gcpCallbackUrlInput.text.toString())){
                return@setOnClickListener;
            }
            val request = PeriodicWorkRequestBuilder<SyncWorker>(15,TimeUnit.MINUTES).apply {
                val data = Data.Builder().apply {
                    putString("endpoint", value)
                }.build()
                setInputData(data)
            }.build()
            manager.enqueueUniquePeriodicWork(Val.Worker.workName,
                ExistingPeriodicWorkPolicy.UPDATE,request)
        }
        saveButton.setOnClickListener{
            save(targetListApiEndpointInput.text.toString(),gcpClientIdInput.text.toString(),gcpClientSecretInput.text.toString(), gcpCallbackUrlInput.text.toString());
        }
        authButton.setOnClickListener{
            val clientId = gcpClientIdInput.text.toString();
            val gso = google.getGoogleSignInOptions(clientId)
            val mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
            Log.i(javaClass.simpleName,"start activity")
            startForResult.launch(mGoogleSignInClient.signInIntent)
        }
        grantStoragePermissionButton.setOnClickListener{
            val intent = Intent("android.settings.MANAGE_ALL_FILES_ACCESS_PERMISSION")
            startActivity(intent)
        }
    }
    override fun onStart() {
        super.onStart()
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if(account != null) {
            Log.i(javaClass.simpleName,account.account.toString())
            Log.i(javaClass.simpleName,account.id.toString())
            Log.i(javaClass.simpleName,account.idToken.toString())
            Log.i(javaClass.simpleName,account.serverAuthCode.toString())
        }
    }

    private fun save(url: String,gcpClientId: String, gcpClientSecret: String,gcpCallbackUrl: String): Boolean{
        if (!isValidURL(url) || !isValidURL(gcpCallbackUrl)){
            Toast.makeText(applicationContext, "正しくないURLです。", Toast.LENGTH_SHORT).show()
            return false
        }
        Toast.makeText(applicationContext, "保存しました", Toast.LENGTH_SHORT).show()
        pref.save(Val.Pref.endpoint,url)
        pref.save(Val.Pref.gcpClientId, gcpClientId)
        pref.save(Val.Pref.gcpClientSecret, gcpClientSecret)
        pref.save(Val.Pref.gcpCallbackUrl, gcpCallbackUrl)
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

    private fun createNotificationChannel(){
        val notificationManager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            Val.Notification.channelId,
            "SyncWorker",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)
    }

}