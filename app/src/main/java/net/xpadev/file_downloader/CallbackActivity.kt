package net.xpadev.file_downloader

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

class CallbackActivity : AppCompatActivity()  {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_callback)
        Log.i("Callback",intent?.action.toString())
        Log.i("Callback",intent?.data.toString())
    }
}