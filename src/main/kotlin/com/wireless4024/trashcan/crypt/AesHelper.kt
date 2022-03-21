package com.wireless4024.trashcan.crypt

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.spec.InvalidKeySpecException
import java.security.spec.KeySpec
import java.util.concurrent.ThreadLocalRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec


@Throws(NoSuchAlgorithmException::class, InvalidKeySpecException::class)
fun getKeyFromPassword(password: CharArray, salt: ByteArray, iterationCount:Int): SecretKey {
	val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512")
	val spec: KeySpec = PBEKeySpec(password, salt, iterationCount, 256)
	return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
}

/**
 * @param password chars password (this will obfuscate after use)
 * @param secureBytes secure bytes (should generate) from secure random
 * @param iterationCount password iteration count
 * @constructor
 */
class AesHelper(password: CharArray, secureBytes: ByteArray, iterationCount: Int = 256) {
	val iv: IvParameterSpec
	val key: SecretKey

	init {
		val pwd = StandardCharsets.UTF_8.encode(CharBuffer.wrap(password))
		pwd.flip()
		val buf = ByteBuffer.allocate(pwd.remaining() + secureBytes.size)
		buf.put(secureBytes)
		buf.put(pwd)
		destroy(pwd.array())

		val rawData = buf.array()
		val hash = hash512(rawData + secureBytes)
		destroy(rawData)

		val iv = ByteArray(16)
		hash.copyOfRange(24, 36).copyInto(iv)
		this.iv = IvParameterSpec(iv)

		this.key = getKeyFromPassword(password, hash, iterationCount)
		destroy(password)
	}

	fun encCipher(): Cipher {
		val cipher: Cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
		cipher.init(Cipher.ENCRYPT_MODE, key, iv)
		return cipher
	}

	fun decCipher(): Cipher {
		val cipher: Cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
		cipher.init(Cipher.DECRYPT_MODE, key, iv)
		return cipher
	}
}

const val SALT_LEN = 64

fun genSalt(): ByteArray {
	val salts = ByteArray(SALT_LEN)
	SecureRandom().nextBytes(salts)
	return salts
}

private fun destroy(array: CharArray) {
	val rng = ThreadLocalRandom.current()
	for (index in array.indices) {
		array[index] = rng.nextInt(32768).toChar()
	}
}

private fun destroy(array: ByteArray) {
	val rng = ThreadLocalRandom.current()
	rng.nextBytes(array)
}

suspend inline fun <T> io(noinline block: suspend CoroutineScope.() -> T): T {
	return withContext(Dispatchers.IO, block)
}