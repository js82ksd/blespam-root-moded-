package com.tutozz.blespam.security

import android.util.Base64
import java.nio.charset.StandardCharsets

object StringProtector {

    private val keyParts = arrayOf(
        byteArrayOf(75, 83, 35, 101, 68),
        byteArrayOf(57, 69, 64, 82, 115),
        byteArrayOf(118, 67, 83, 42, 76),
        byteArrayOf(46, 81, 117, 83, 43),
        byteArrayOf(49, 41, 97, 37, 70),
        byteArrayOf(44, 58, 89, 81, 83)
    )

    private val xorKey: ByteArray by lazy {
        keyParts.reduce { acc, bytes -> acc + bytes }
    }

    fun decrypt(encoded: String): String {
        return try {
            val encrypted = Base64.decode(encoded, Base64.DEFAULT)
            val decrypted = ByteArray(encrypted.size)
            for (i in encrypted.indices) {
                decrypted[i] = (encrypted[i].toInt() xor xorKey[i % xorKey.size].toInt()).toByte()
            }
            String(decrypted, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }

    object Encrypted {
        const val TOKEN_API = ""
        const val REPORT_API = ""
        const val VERSION_API = ""
        const val SOCIAL = ""
        const val ICONCLICK = ""
        const val FIREBASE = ""
        const val ANTIFRAUD = ""
    }
}