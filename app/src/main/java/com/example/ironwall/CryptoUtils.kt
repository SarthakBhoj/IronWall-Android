package com.example.ironwall

import android.util.Base64
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

object CryptoUtils {

    private const val AES_KEY_SIZE = 16
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 16

    fun getAESKeyFromTotp(totpSecret: String): ByteArray {
        val sha = MessageDigest.getInstance("SHA-256")
        val hash = sha.digest(totpSecret.toByteArray(StandardCharsets.UTF_8))
        return hash.copyOf(AES_KEY_SIZE)
    }

    fun encrypt(plainText: String, totpSecret: String): String {
        val key = getAESKeyFromTotp(totpSecret)
        val iv = ByteArray(GCM_IV_LENGTH).also { Random.nextBytes(it) }

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), spec)

        val encrypted = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))

        val buffer = ByteBuffer.allocate(iv.size + encrypted.size)
        buffer.put(iv)
        buffer.put(encrypted)
        return Base64.encodeToString(buffer.array(), Base64.NO_WRAP)
    }

    fun decrypt(cipherText: String, totpSecret: String): String {
        val decoded = Base64.decode(cipherText, Base64.NO_WRAP)
        val buffer = ByteBuffer.wrap(decoded)
        val iv = ByteArray(GCM_IV_LENGTH).also { buffer.get(it) }
        val encrypted = ByteArray(buffer.remaining()).also { buffer.get(it) }

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(getAESKeyFromTotp(totpSecret), "AES"), spec)

        val decrypted = cipher.doFinal(encrypted)
        return String(decrypted, StandardCharsets.UTF_8)
    }
}
