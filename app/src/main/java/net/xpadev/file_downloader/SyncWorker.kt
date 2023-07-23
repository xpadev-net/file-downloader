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
        storage.tryCleanup()
        val endpoint = inputData.getString("endpoint") ?: return Result.failure();
        setForeground(createForegroundInfo("loading metadata"))
        val res = this.network.fetchJson<TargetListResponse>(endpoint)
        for (item in res.data){
            var spaceLeft = storage.getFreeBytes();
            while (spaceLeft.isFailure || item.fileSize+storage.GB*5 > spaceLeft.getOrDefault(0)){
                setForeground(createForegroundInfo("waiting for upload..."))
                spaceLeft = storage.getFreeBytes();
                Thread.sleep(10000L)
            }
            setForeground(createForegroundInfo("downloading..."))
            val result = this.network.download(item.link)
            if (result.isFailure){
                continue;
            }
            this.network.fetchString("${res.markAsComplete}?id=${item.id}")
        }
        setForeground(createForegroundInfo("success"))
        return Result.success()
    }

    private fun createForegroundInfo(progress: String): ForegroundInfo {
        val id = "net.xpadev.file_downloader.SyncWorker"
        val title = "downloading"
        val cancel = "cancel"
        // This PendingIntent can be used to cancel the worker
        val intent = WorkManager.getInstance(applicationContext)
            .createCancelPendingIntent(getId())

        val notification = NotificationCompat.Builder(applicationContext, id)
            .setContentTitle(title)
            .setTicker(title)
            .setContentText(progress)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            // Add the cancel action to the notification which can
            // be used to cancel the worker
            .addAction(android.R.drawable.ic_delete, cancel, intent)
            .build()

        return ForegroundInfo(notificationId++, notification)
    }


}