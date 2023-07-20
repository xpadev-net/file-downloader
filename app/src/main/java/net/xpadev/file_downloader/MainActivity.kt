package net.xpadev.file_downloader

import android.content.Context
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
import java.net.URL
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {
    private val manager = WorkManager.getInstance()
    private lateinit var sharedPref: SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        this.sharedPref = getSharedPreferences(
            "net.xpadev.file_downloader", Context.MODE_PRIVATE);
        val startProcessButton = findViewById<Button>(R.id.StartProcessButton)
        val saveButton = findViewById<Button>(R.id.SaveButton)
        val targetListApiEndpointInput = findViewById<EditText>(R.id.targetListApiEndpointInput)

        targetListApiEndpointInput.setText(this.sharedPref.getString("endpoint", ""))
        Log.i("Main", "init")
        startProcessButton.setOnClickListener {
            Log.i("Main", "pressed")
            val value = targetListApiEndpointInput.text.toString()
            if (!saveValue(value)){
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
            saveValue(targetListApiEndpointInput.text.toString());
        }
    }

    private fun saveValue(value: String): Boolean{
        if (!isValidURL(value)){
            Toast.makeText(applicationContext, "正しくないURLです。", Toast.LENGTH_SHORT).show()
            return false
        }
        Toast.makeText(applicationContext, "保存しました", Toast.LENGTH_SHORT).show()
        this.sharedPref.edit().putString("endpoint",value).apply()
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