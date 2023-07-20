package net.xpadev.file_downloader

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.util.Log
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.CompletableFuture

class NetworkUtils (private val applicationContext: Context){

    inline fun <reified T> fetchJson(link: String):T{
        val json = fetchString(link).trimIndent();
        return Json.decodeFromString<T>(json)
    }

    fun fetchString(link: String): String {
        val url = URL(link)
        val con = url.openConnection() as HttpURLConnection
        con.connectTimeout = 20_000
        con.readTimeout = 20_000
        con.connect()
        return con.inputStream.bufferedReader(Charsets.UTF_8).use { br ->
            br.readLines().joinToString("")
        }
    }

    fun download(link: String, _fileName: String=""): Result<String> {
        val completableFuture = CompletableFuture<Result<String>>()
        Thread {
            try {
                val fileName = if (_fileName==""){
                    link.substring(link.lastIndexOf("/") + 1)
                }else{
                    _fileName
                }
                val manager = applicationContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val request = DownloadManager.Request(Uri.parse(link))
                request.setTitle(fileName)
                request.setDescription("Downloading")
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                val downloadId = manager.enqueue(request)

                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                        if (id == downloadId) {
                            Log.i("SyncService", "Download successful")
                            completableFuture.complete(Result.success("success"))
                        }
                    }
                }
                applicationContext.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))


            } catch (e: Exception) {
                completableFuture.complete(Result.failure(e))
            }
        }.start()
        return completableFuture.get();
    }
}