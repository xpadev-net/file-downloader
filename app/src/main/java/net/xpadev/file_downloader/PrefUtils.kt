package net.xpadev.file_downloader

import android.content.Context

class PrefUtils(applicationContext: Context) {
    private val encryption = EncryptionUtils()
    private var sharedPref = applicationContext.getSharedPreferences(Val.Pref.prefId, Context.MODE_PRIVATE);
    fun get(key: String): String{
        return encryption.decrypt(sharedPref.getString(key,""))
    }
    fun save(key: String,value: String){
        sharedPref.edit().putString(key,encryption.encrypt(value)).apply()
    }
}