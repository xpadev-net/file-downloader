package net.xpadev.file_downloader

import android.util.Log
import java.lang.Exception
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class EncryptionUtils {
    private val IV_LENGTH = 16
    private val CIPHER_MODE = "AES/CBC/PKCS5Padding"

    private val key = SecretKeySpec(Val.Encryption.key.toByteArray(), "AES")
    private val random = SecureRandom()


    fun encrypt(input: String): String {
        val cipher = Cipher.getInstance(CIPHER_MODE)
        val iv = ByteArray(IV_LENGTH)
        random.nextBytes(iv)
        val ivSpec = IvParameterSpec(iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec)
        val encrypted = cipher.doFinal(input.toByteArray())

        return Base64.getEncoder().encodeToString(cipher.iv + encrypted);
    }

    fun decrypt(rawInput: String?): String{
        if (rawInput==null||rawInput == ""){
            return ""
        }
        return try {
            val input = Base64.getDecoder().decode(rawInput)
            val iv =input.copyOfRange(0,IV_LENGTH)
            val ivSpec = IvParameterSpec(iv)
            val body = input.copyOfRange(IV_LENGTH,input.size)
            val cipher = Cipher.getInstance(CIPHER_MODE)
            cipher.init(Cipher.DECRYPT_MODE,key,ivSpec)
            String(cipher.doFinal(body))
        }catch (_:Exception){
            ""
        }
    }

}