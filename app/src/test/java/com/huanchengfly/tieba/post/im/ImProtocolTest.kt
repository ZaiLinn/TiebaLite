package com.huanchengfly.tieba.post.im

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ImProtocolTest {

    private fun hex(bytes: ByteArray) = bytes.joinToString("") { "%02x".format(it) }

    @Test
    fun `pbkdf2 matches RFC 6070 vectors`() {
        assertEquals(
            "0c60c80f961f0e71f3a9b524af6012062fe037a6",
            hex(ImProtocol.pbkdf2HmacSha1("password".toByteArray(), "salt".toByteArray(), 1, 20))
        )
        assertEquals(
            "ea6c014dc72d6f8ccd1ed92ace1d41f0d8de8957",
            hex(ImProtocol.pbkdf2HmacSha1("password".toByteArray(), "salt".toByteArray(), 2, 20))
        )
        // 跨越两个 SHA1 块（25 > 20 字节）
        assertEquals(
            "3d2eec4fe41c849b80c8d83662c0e44a8b291a964cf2f07038",
            hex(
                ImProtocol.pbkdf2HmacSha1(
                    "passwordPASSWORDpassword".toByteArray(),
                    "saltSALTsaltSALTsaltSALTsaltSALTsalt".toByteArray(),
                    4096,
                    25
                )
            )
        )
    }

    @Test
    fun `aes key derivation matches aiotieba parameters`() {
        // 期望值由 Python hashlib.pbkdf2_hmac('sha1', secret, salt, 5, 32) 计算
        val secret = ByteArray(31) { (it + 1).toByte() }
        assertEquals(
            "7bd188be85e842344d0b5058f35d14403edea9c457be913cce331c8508337e09",
            hex(ImProtocol.deriveAesKey(secret))
        )
    }

    @Test
    fun `plain frame has a 9 byte big endian header`() {
        val frame = ImProtocol.pack(byteArrayOf(1, 2, 3), cmd = 1001, requestId = 0x01020304, aesKey = null)
        assertEquals(12, frame.size)
        assertEquals(0x08, frame[0].toInt())
        assertArrayEquals(byteArrayOf(0, 0, 0x03, 0xe9.toByte()), frame.copyOfRange(1, 5))
        assertArrayEquals(byteArrayOf(1, 2, 3, 4), frame.copyOfRange(5, 9))
        assertArrayEquals(byteArrayOf(1, 2, 3), frame.copyOfRange(9, 12))
    }

    @Test
    fun `encrypted and compressed frames round trip`() {
        val key = ImProtocol.deriveAesKey(ImProtocol.newSecret())
        val payload = "私信内容 hello".repeat(20).toByteArray()
        for (compress in listOf(false, true)) {
            val frame = ImProtocol.pack(payload, cmd = 205001, requestId = 42, aesKey = key, compress = compress)
            val expectedFlag = 0x08 or 0x80 or (if (compress) 0x40 else 0)
            assertEquals(expectedFlag, frame[0].toInt() and 0xff)
            assertFalse(frame.copyOfRange(9, frame.size).contentEquals(payload))
            val parsed = ImProtocol.parse(frame, key)
            assertEquals(205001, parsed.cmd)
            assertEquals(42, parsed.requestId)
            assertArrayEquals(payload, parsed.data)
        }
    }

    @Test
    fun `secret is encrypted with a 2048 bit rsa key`() {
        val secret = ImProtocol.newSecret()
        assertEquals(ImProtocol.SECRET_SIZE, secret.size)
        assertEquals(256, ImProtocol.encryptSecret(secret).size)
    }

    @Test
    fun `device json escapes values and keeps aiotieba key order`() {
        assertEquals(
            "{\"cuid\":\"a\\\"b\",\"_client_version\":\"22.10.1.0\",\"_msg_status\":\"1\"," +
                "\"cuid_galaxy2\":\"X|V1\",\"_client_type\":\"2\",\"timestamp\":\"123\"}",
            ImProtocol.deviceJson("a\"b", "X|V1", "22.10.1.0", 123L)
        )
    }
}
