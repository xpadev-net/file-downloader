package net.xpadev.file_downloader

import android.app.NotificationManager
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.CompletableFuture
import kotlin.math.roundToInt

class NetworkUtils (private val applicationContext: Context){
    val SafeJson = Json { ignoreUnknownKeys=true }
    val pref = PrefUtils(applicationContext)
    private val notificationId: Int = 0;
    private val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    inline fun <reified T> fetchJson(link: String):T{
        return parseJson<T>(fetchString(link))
    }

    inline fun <reified T> parseJson(input: String): T{
        return SafeJson.decodeFromString<T>(input.trimIndent())
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

    inline fun <reified T> postJson(link:String, body:String,auth:Boolean = false): T{
        val url = URL(link)
        val con = url.openConnection() as HttpURLConnection
        con.requestMethod = "POST"
        con.doOutput = true
        con.setChunkedStreamingMode(0)
        con.setRequestProperty("Content-type", "application/json; charset=utf-8")
        con.setRequestProperty("Content-Length", body.length.toString())
        if (auth){
            con.setRequestProperty("Authorization","Bearer ${pref.get(Val.Pref.gcpAccessToken)}")
        }
        con.useCaches = false
        val outputStream = con.outputStream
        outputStream.write(body.toByteArray())
        outputStream.flush()
        outputStream.close()
        val json = con.inputStream.bufferedReader(Charsets.UTF_8).use { br ->
            br.readLines().joinToString("")
        }
        return parseJson<T>(json)
    }

    fun download(link: String, _fileName: String=""): Result<String> {
        val completableFuture = CompletableFuture<Result<String>>()
        val fileName = _fileName.ifBlank {
            link.substring(link.lastIndexOf("/") + 1)
        }

        val url = URL(link)
        showProgress(fileName,0)

        Thread {
            try {
                val connection = url.openConnection()

                val totalBytes = connection.contentLength.toLong()
                val inputStream = BufferedInputStream(connection.getInputStream())

                var downloadedBytes: Long = 0

                val contentValues = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/*")
                    put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/Camera")
                }

                val contentResolver: ContentResolver = applicationContext.contentResolver
                val contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                val uri = contentResolver.insert(contentUri, contentValues)

                uri?.let {
                    val outputStream = contentResolver.openOutputStream(it)
                    outputStream?.use { output ->
                        val buffer = ByteArray(1024)
                        var bytesRead: Int
                        var lastProgress = 0
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead
                            val progress = (downloadedBytes.toDouble() / totalBytes.toDouble() * 100).roundToInt()
                            if (lastProgress < progress){
                                lastProgress = progress
                                showProgress(fileName, progress)
                            }
                        }
                    }
                    completableFuture.complete(Result.success("success"))
                } ?: run {
                    completableFuture.complete(Result.failure(Exception("Failed to insert into MediaStore")))
                }

                inputStream.close()

            } catch (e: Exception) {
                completableFuture.complete(Result.failure(e))
            }
        }.start()

        return completableFuture.get()
    }

    private fun showProgress(fileName: String,progress:Int){
        val note = NotificationCompat.Builder(applicationContext, Val.Notification.channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Downloading...")
            .setContentText(fileName)
            .setProgress(100,progress,false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT).build()
        manager.notify(notificationId,note)
    }
}