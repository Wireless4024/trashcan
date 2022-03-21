package com.wireless4024.trashcan.crypt

import com.wireless4024.trashcan.ext.readAsync
import com.wireless4024.trashcan.ext.transferToAsync
import kotlinx.coroutines.*
import org.springframework.web.servlet.function.ServerResponse.async
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest

class HashingStreamWrapper(val out: OutputStream) : OutputStream(), AutoCloseable {
	private val digest: MessageDigest = MessageDigest.getInstance("SHA-512/224")

	private val buffer = ByteArray(1)

	override fun write(b: Int) {
		val buffer = this.buffer
		buffer[0] = b.toByte()
		write(buffer, 0, 1)
	}

	override fun write(b: ByteArray, off: Int, len: Int) {
		out.write(b, off, len)
		digest.update(b, off, len)
	}

	fun finish(): ByteArray {
		return use { digest.digest() }
	}

	suspend fun finishAsync(): ByteArray {
		return withContext(Dispatchers.Default) { use { digest.digest() } }
	}

	suspend fun transferFromAsync(stream: InputStream) {
		val buffer = ByteArray(4096)
		var read: Int

		while (stream.readAsync(buffer, 0, 4096).also { read = it } >= 0) {
			coroutineScope {
				listOf(
					async(Dispatchers.IO) { out.write(buffer, 0, read) },
					async(Dispatchers.Default) { digest.update(buffer, 0, read) }
				).awaitAll()
			}
		}
	}

	override fun flush() {
		out.flush()
	}

	override fun close() {
		out.close()
	}
}