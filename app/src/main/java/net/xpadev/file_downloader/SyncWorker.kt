package net.xpadev.file_downloader

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

class SyncWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    private var network: NetworkUtils = NetworkUtils(applicationContext);
    private var storage: StorageUtils = StorageUtils(applicationContext);
    private var notificationId: Int = 0;

    override suspend fun doWork(): Result {
        Log.i(javaClass.simpleName, "init")
        val endpoint = inputData.getString("endpoint") ?: return Result.failure();
        var manifestCount = 0;
        setForeground(createForegroundInfo("loading metadata (page:${manifestCount})"))
        var res = this.network.fetchJson<TargetListResponse>(endpoint)
        while (res.data.isNotEmpty()){
            var pos = 0
            for (item in res.data){
                storage.tryCleanup()
                var spaceLeft = storage.getFreeBytes();
                var tryCount = 0;
                while (spaceLeft.isFailure || item.fileSize+storage.GB*5 > spaceLeft.getOrDefault(0)){
                    setForeground(createForegroundInfo("waiting for upload... (page:${manifestCount},pos: ${pos}/${res.data.size},retry: ${tryCount})"))
                    spaceLeft = storage.getFreeBytes();
                    Thread.sleep(10000L)
                    tryCount++
                }
                setForeground(createForegroundInfo("downloading... (page:${manifestCount},pos: ${pos}/${res.data.size})"))
                val result = this.network.download(item.link)
                if (result.isFailure){
                    continue;
                }
                this.network.fetchString("${res.markAsComplete}?id=${item.id}")
                pos++
            }
            manifestCount++
            setForeground(createForegroundInfo("loading metadata (page:${manifestCount})"))
            res = this.network.fetchJson<TargetListResponse>(endpoint)
        }
        setForeground(createForegroundInfo("success"))
        return Result.success()
    }

    private fun createForegroundInfo(progress: String): ForegroundInfo {
        val title = "downloading"

        val notification = NotificationCompat.Builder(applicationContext, Val.Notification.channelId)
            .setContentTitle(title)
            .setTicker(title)
            .setContentText(progress)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()

        return ForegroundInfo(notificationId, notification)
    }


}