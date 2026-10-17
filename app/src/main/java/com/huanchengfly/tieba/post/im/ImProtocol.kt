package com.huanchengfly.tieba.post.im

import okio.ByteString.Companion.decodeBase64
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.security.KeyFactory
import java.security.SecureRandom
import java.security.spec.X509EncodedKeySpec
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * 贴吧私信长连接的帧格式与加密。
 *
 * 实现参考 aiotieba（Unlicense）的 core/websocket.py、core/account.py 与 api/init_websocket：
 * - 每帧 = 1 字节标志位 + 4 字节 cmd + 4 字节请求 id（均为大端）+ 数据；
 * - 标志位基础值 0x08，0x40 表示 gzip 压缩，0x80 表示 AES-ECB 加密（PKCS7 填充）；
 * - AES 密钥由 31 字节随机数经 PBKDF2-HMAC-SHA1（固定 salt、5 次迭代）派生出 32 字节；
 * - 随机数用贴吧的 RSA 公钥（PKCS1 v1.5）加密后在 cmd 1001 中上传。
 *
 * 只依赖 JDK，便于单元测试。
 */
object ImProtocol {
    const val CMD_UPDATE_CLIENT_INFO = 1001
    const val CMD_GET_GROUP_MSG = 202003
    const val CMD_COMMIT_PERSONAL_MSG = 205001

    /** 私信所在消息组的类型 */
    const val GROUP_TYPE_PRIVATE_MSG = 6

    /** 文本私信 */
    const val MSG_TYPE_TEXT = 1

    private const val FLAG_BASE = 0x08
    private const val FLAG_GZIP = 0x40
    private const val FLAG_ENCRYPT = 0x80
    private const val HEADER_SIZE = 9

    private val KDF_SALT = byteArrayOf(
        0xa4.toByte(), 0x0b, 0xc8.toByte(), 0x34, 0xd6.toByte(), 0x95.toByte(), 0xf3.toByte(), 0x13
    )
    private const val KDF_ITERATIONS = 5
    private const val AES_KEY_SIZE = 32
    const val SECRET_SIZE = 31

    private const val PUBLIC_KEY =
        "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAwQpwBZxXJV/JVRF/uNfyMSdu7YWwRNLM8+2xbniGp2iIQHOikPpTYQjlQgMi1uvq1kZpJ32rHo3hkwjy2l0lFwr3u4Hk2Wk7vnsqYQjAlYlK0TCzjpmiI+OiPOUNVtbWHQiLiVqFtzvpvi4AU7C1iKGvc/4IS45WjHxeScHhnZZ7njS4S1UgNP/GflRIbzgbBhyZ9kEW5/OO5YfG1fy6r4KSlDJw4o/mw5XhftyIpL+5ZBVBC6E1EIiP/dd9AbK62VV1PByfPMHMixpxI3GM2qwcmFsXcCcgvUXJBa9k6zP8dDQ3csCM2QNT+CQAOxthjtp/TFWaD7MzOdsIYb3THwIDAQAB"

    class Frame(val cmd: Int, val requestId: Int, val data: ByteArray)

    fun newSecret(random: SecureRandom = SecureRandom()): ByteArray =
        ByteArray(SECRET_SIZE).also(random::nextBytes)

    /** 用贴吧公钥加密随机数，结果放进 UpdateClientInfoReqIdl.data.secretKey */
    fun encryptSecret(secret: ByteArray): ByteArray {
        // okio 的 Base64：java.util.Base64 需要 API 26，minSdk 是 21
        val keySpec = X509EncodedKeySpec(requireNotNull(PUBLIC_KEY.decodeBase64()).toByteArray())
        val publicKey = KeyFactory.getInstance("RSA").generatePublic(keySpec)
        return Cipher.getInstance("RSA/ECB/PKCS1Padding").run {
            init(Cipher.ENCRYPT_MODE, publicKey)
            doFinal(secret)
        }
    }

    /** 由随机数派生 AES-256 密钥 */
    fun deriveAesKey(secret: ByteArray): ByteArray =
        pbkdf2HmacSha1(secret, KDF_SALT, KDF_ITERATIONS, AES_KEY_SIZE)

    /**
     * PBKDF2-HMAC-SHA1。不用 PBEKeySpec：它把密码当 char[] 再按 UTF-8 编码，
     * 而这里的密码是任意二进制随机数。
     */
    fun pbkdf2HmacSha1(password: ByteArray, salt: ByteArray, iterations: Int, length: Int): ByteArray {
        val mac = Mac.getInstance("HmacSHA1").apply { init(SecretKeySpec(password, "HmacSHA1")) }
        val output = ByteArrayOutputStream(length)
        var blockIndex = 1
        while (output.size() < length) {
            mac.update(salt)
            var u = mac.doFinal(ByteBuffer.allocate(4).putInt(blockIndex).array())
            val block = u.copyOf()
            repeat(iterations - 1) {
                u = mac.doFinal(u)
                for (i in block.indices) block[i] = (block[i].toInt() xor u[i].toInt()).toByte()
            }
            output.write(block)
            blockIndex++
        }
        return output.toByteArray().copyOf(length)
    }

    fun pack(
        data: ByteArray,
        cmd: Int,
        requestId: Int,
        aesKey: ByteArray?,
        compress: Boolean = false,
    ): ByteArray {
        var flag = FLAG_BASE
        var body = data
        if (compress) {
            flag = flag or FLAG_GZIP
            body = gzip(body)
        }
        if (aesKey != null) {
            flag = flag or FLAG_ENCRYPT
            body = aes(Cipher.ENCRYPT_MODE, aesKey, body)
        }
        return ByteBuffer.allocate(HEADER_SIZE + body.size)
            .put(flag.toByte())
            .putInt(cmd)
            .putInt(requestId)
            .put(body)
            .array()
    }

    fun parse(frame: ByteArray, aesKey: ByteArray?): Frame {
        require(frame.size >= HEADER_SIZE) { "frame too short: ${frame.size}" }
        val buffer = ByteBuffer.wrap(frame)
        val flag = buffer.get().toInt() and 0xff
        val cmd = buffer.getInt()
        val requestId = buffer.getInt()
        var body = frame.copyOfRange(HEADER_SIZE, frame.size)
        if (flag and FLAG_ENCRYPT != 0) {
            requireNotNull(aesKey) { "encrypted frame before key exchange" }
            body = aes(Cipher.DECRYPT_MODE, aesKey, body)
        }
        if (flag and FLAG_GZIP != 0) {
            body = GZIPInputStream(ByteArrayInputStream(body)).use { it.readBytes() }
        }
        return Frame(cmd, requestId, body)
    }

    /** cmd 1001 中 device 字段的 JSON（键顺序与 aiotieba 一致） */
    fun deviceJson(cuid: String, cuidGalaxy2: String, clientVersion: String, timestampMillis: Long): String =
        buildString {
            append('{')
            append("\"cuid\":").append(jsonString(cuid)).append(',')
            append("\"_client_version\":").append(jsonString(clientVersion)).append(',')
            append("\"_msg_status\":\"1\",")
            append("\"cuid_galaxy2\":").append(jsonString(cuidGalaxy2)).append(',')
            append("\"_client_type\":\"2\",")
            append("\"timestamp\":\"").append(timestampMillis).append('"')
            append('}')
        }

    private fun jsonString(value: String): String = buildString {
        append('"')
        value.forEach { c ->
            when (c) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (c < ' ') append("\\u%04x".format(c.code)) else append(c)
            }
        }
        append('"')
    }

    private fun aes(mode: Int, key: ByteArray, data: ByteArray): ByteArray =
        Cipher.getInstance("AES/ECB/PKCS5Padding").run {
            init(mode, SecretKeySpec(key, "AES"))
            doFinal(data)
        }

    private fun gzip(data: ByteArray): ByteArray =
        ByteArrayOutputStream().also { out -> GZIPOutputStream(out).use { it.write(data) } }.toByteArray()
}
