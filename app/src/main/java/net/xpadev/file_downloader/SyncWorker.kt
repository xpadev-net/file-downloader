package net.xpadev.file_downloader

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import net.xpadev.file_downloader.structure.TargetListResponse
import java.lang.Exception

class SyncWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    private var network: NetworkUtils = NetworkUtils(applicationContext);
    private var storage: StorageUtils = StorageUtils(applicationContext);
    private val notify = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val notificationId: Int = 0;

    override suspend fun doWork(): Result {
        Log.i(javaClass.simpleName, "init")
        val endpoint = inputData.getString("endpoint") ?: return Result.failure();
        var manifestCount = 0;
        setForeground(createForegroundInfo())
        updateNotify("loading metadata","page: 0")
        var res = this.network.fetchJson<TargetListResponse>(endpoint)
        while (res.data.isNotEmpty()){
            var pos = 0
            for (item in res.data){
                try {
                    var spaceLeft = storage.getFreeBytes();
                    var tryCount = 0;
                    while (spaceLeft.isFailure || item.fileSize+storage.GB*5 > spaceLeft.getOrDefault(0)){
                        updateNotify("waiting for upload...","page: ${manifestCount}, pos: ${pos}/${res.data.size}, retry: $tryCount")
                        Thread.sleep(60_000L)
                        updateNotify("cleaning...","page: ${manifestCount}, pos: ${pos}/${res.data.size}")
                        storage.tryCleanup()
                        spaceLeft = storage.getFreeBytes();
                        tryCount++
                    }
                    updateNotify("downloading...","page: ${manifestCount}, pos: ${pos}/${res.data.size}")
                    val result = this.network.download(item.link,item.fileSize)
                    if (result.isFailure){
                        updateNotify("download failed","page: ${manifestCount}, pos: ${pos}/${res.data.size}")
                        continue;
                    }
                    updateNotify("successfully downloaded","page: ${manifestCount}, pos: ${pos}/${res.data.size}")
                    this.network.fetchString("${res.markAsComplete}?id=${item.id}")
                }catch (e: Exception){
                    e.printStackTrace()
                    Log.e(javaClass.simpleName,e.toString())
                }
                pos++
            }
            manifestCount++
            updateNotify("loading metadata","page: $manifestCount")
            res = this.network.fetchJson<TargetListResponse>(endpoint)
        }
        updateNotify("download completed","success")
        return Result.success()
    }

    private fun createForegroundInfo(): ForegroundInfo {
        Log.i(javaClass.simpleName,"init")

        val notification = NotificationCompat.Builder(applicationContext, Val.Notification.channelId)
            .setContentTitle("init")
            .setTicker("init")
            .setContentText("init")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()

        return ForegroundInfo(notificationId, notification)
    }

    private fun updateNotify(title:String,progress: String){
        Log.i(javaClass.simpleName,"${title}: $progress")

        val notification = NotificationCompat.Builder(applicationContext, Val.Notification.channelId)
            .setContentTitle(title)
            .setTicker(title)
            .setContentText(progress)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
        notify.notify(notificationId,notification)
    }

}