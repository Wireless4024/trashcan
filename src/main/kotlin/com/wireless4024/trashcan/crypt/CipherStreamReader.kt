package com.wireless4024.trashcan.crypt

import com.wireless4024.trashcan.ext.readAsync
import kotlinx.coroutines.*
import java.io.BufferedInputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.Executors
import javax.crypto.Cipher

const val BUFFER_SIZE=2048
class CipherStreamReader(password: CharArray, stream: InputStream) {
	private val stream: BufferedInputStream
	private val cip: Cipher

	init {
		this.stream = BufferedInputStream(stream)
		val salts = ByteArray(SALT_LEN)
		this.stream.read(salts)
		cip = AesHelper(password, salts).decCipher()
	}

	fun transferTo(out: OutputStream) {
		val buf = ByteArray(4096)
		val decBuf = ByteArray(4096)
		var len = 0
		val cip = this.cip
		while (stream.read(buf).also { len = it } >= 0) {
			val decLen = cip.update(buf, 0, len, decBuf)
			out.write(decBuf, 0, decLen)
		}
		out.write(cip.doFinal())
	}
	suspend fun transferToAsync(out: OutputStream) {
		val buf = ByteArray(BUFFER_SIZE)
		val buf2 = ByteArray(BUFFER_SIZE)
		val decBuf = ByteArray(BUFFER_SIZE)
		val decBuf2 = ByteArray(BUFFER_SIZE)
		var aLen = 0
		var bLen = 0
		val cip = this.cip
		var aRead: Deferred<Boolean>? = null
		var aWrite: Deferred<Unit>? = null
		var bRead: Deferred<Boolean>? = null
		var bWrite: Deferred<Unit>? = null
		coroutineScope {
			aRead = async { stream.readAsync(buf, 0, BUFFER_SIZE).also { aLen = it } >= 0 }
			while (true) {
				// has next
				if (aRead?.await() == true) {
					bRead = async { stream.readAsync(buf2, 0, BUFFER_SIZE).also { bLen = it } >= 0 }

					val decLen = withContext(Dispatchers.Default){cip.update(buf, 0, aLen, decBuf)}
					bWrite?.await()
					bWrite = null

					aWrite = async(Dispatchers.IO) { out.write(decBuf, 0, decLen) }
				} else {
					break
				}

				if (bRead?.await() == true) {
					aRead = async { stream.readAsync(buf, 0, BUFFER_SIZE).also { aLen = it } >= 0 }

					val decLen = withContext(Dispatchers.Default){cip.update(buf2, 0, bLen, decBuf2)}
					aWrite?.await()
					aWrite = null

					bWrite = async(Dispatchers.IO) { out.write(decBuf2, 0, decLen) }
				} else {
					break
				}
			}
			aWrite?.await()
			bWrite?.await()
		}
	}

	suspend fun transferToAsyncLegacy(out: OutputStream) {
		val buf = ByteArray(4096)
		val decBuf = ByteArray(4096)
		var len = 0
		val cip = this.cip
		while (stream.readAsync(buf, 0, BUFFER_SIZE).also { len = it } >= 0) {
			val decLen = cip.update(buf, 0, len, decBuf)
			io { out.write(decBuf, 0, decLen) }
		}
		val final = cip.doFinal()
		io { out.write(final) }
	}
}