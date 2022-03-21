package com.wireless4024.trashcan.crypt

import com.wireless4024.trashcan.ext.transferToAsync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest

/**
 * transfer data from this to output stream and return hash of file
 *
 * @return length to hash
 */
fun InputStream.hash224To(out: OutputStream): ByteArray {
	val wrapOut = HashingStreamWrapper(out)
	transferTo(wrapOut)
	return wrapOut.finish()
}

suspend fun InputStream.hash224ToAsync(out: OutputStream): ByteArray {
	val wrapOut = HashingStreamWrapper(out)
	wrapOut.transferFromAsync(this)
	return wrapOut.finishAsync()
}

fun hash512(array: ByteArray): ByteArray {
	val digest = MessageDigest.getInstance("SHA3-512")
	return digest.digest(array)
}

suspend fun hash512Async(array: ByteArray): ByteArray {
	val digest = MessageDigest.getInstance("SHA3-512")
	return withContext(Dispatchers.Default) { digest.digest(array) }
}