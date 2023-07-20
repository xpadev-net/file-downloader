package net.xpadev.file_downloader

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import net.xpadev.file_downloader.structure.TargetListResponse

class SyncWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    private var network: NetworkUtils = NetworkUtils(applicationContext);
    private var storage: StorageUtils = StorageUtils(applicationContext);


    override fun doWork(): Result {
        Log.i("SyncWorker", "init")
        val endpoint = inputData.getString("endpoint") ?: return Result.failure();
        val res = this.network.fetchJson<TargetListResponse>(endpoint)
        for (item in res.data){
            var spaceLeft = storage.getFreeBytes();
            while (spaceLeft.isFailure || item.fileSize+storage.GB*5 > spaceLeft.getOrDefault(0)){
                spaceLeft = storage.getFreeBytes();
                Thread.sleep(10000L)
            }
            val result = this.network.download(item.link)
            if (result.isFailure){
                continue;
            }
            this.network.fetchString("${res.markAsComplete}?id=${item.id}")
        }
        Log.i("SyncService", "success")
        return Result.success()
    }

}