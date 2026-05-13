package com.xsyusign.mobile.util

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * 简化的 AES 加解密 — 用于本地存储密码。
 * 生产环境建议使用 Android Keystore。
 */
object CryptoUtil {

    // 固定密钥，仅用于本地混淆存储（非高强度安全场景）
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/ECB/PKCS5Padding"
    private val KEY = SecretKeySpec("xsyu@sign#2024!1".toByteArray(Charsets.UTF_8).copyOf(16), ALGORITHM)

    fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, KEY)
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    fun decrypt(encryptedText: String): String? {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, KEY)
            val decoded = Base64.decode(encryptedText, Base64.NO_WRAP)
            String(cipher.doFinal(decoded), Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }
}
