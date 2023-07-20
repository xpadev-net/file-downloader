package net.xpadev.file_downloader

import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.util.Log


class StorageUtils (private val applicationContext: Context) {
    private var _storageManager: StorageManager = applicationContext.getSystemService(Context.STORAGE_SERVICE) as StorageManager
    val GB = 1000L * 1000L * 1000L
    val GiB = 1024L * 1024L * 1024L
    val MB = 1000L * 1000L
    val MiB = 1024L * 1024L * 1024L
    val KB = 1000L
    val KiB = 1024L

    fun getFreeBytes():Result<Long> {
        val extDirs = applicationContext.getExternalFilesDirs(null)
        for (file in extDirs){
            val storageVolume: StorageVolume? = _storageManager.getStorageVolume(file)
            if (storageVolume == null) {
                Log.d("StorageUtils", "Could not determinate StorageVolume for ${file.path}")
            } else {
                if (storageVolume.isPrimary) {
                    val uuid = StorageManager.UUID_DEFAULT
                    val storageStatsManager =
                        applicationContext.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
                    return Result.success(storageStatsManager.getFreeBytes(uuid))
                }
            }
        }
        return Result.failure(Error("target disk not found"))
    }

    fun tryCleanup(){
        val launchIntent = applicationContext.packageManager.getLaunchIntentForPackage("com.google.android.apps.photos")
        if (launchIntent != null) {
            applicationContext.startActivity(launchIntent)
        }
    }
}