package com.example.ironwall.Utils

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {
    private const val ALGORITHM = "AES"

    fun encrypt(data: String, key: String): String {
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(padKey(key).toByteArray(), ALGORITHM))
        val encrypted = cipher.doFinal(data.toByteArray())
        return Base64.encodeToString(encrypted, Base64.DEFAULT)
    }

    fun decrypt(encryptedData: String, key: String): String {
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(padKey(key).toByteArray(), ALGORITHM))
        val decoded = Base64.decode(encryptedData, Base64.DEFAULT)
        return String(cipher.doFinal(decoded))
    }

    private fun padKey(key: String): String {
        return key.padEnd(16, ' ').take(16)
    }
}
