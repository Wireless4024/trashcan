package com.wireless4024.trashcan.ext

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

// taken from java source code and make it async
@Throws(IOException::class)
suspend fun InputStream.transferToAsync(out: OutputStream): Long {
	val transferred = AtomicLong()
	val buffer = ByteArray(4096)
	val read = AtomicInteger()
	use {
		while (readAsync(buffer, 0, 4096).also(read::set) >= 0) {
			withContext(Dispatchers.IO) {
				out.write(buffer, 0, read.get().also { transferred.getAndAdd(it.toLong()) })
			}
		}
	}
	return transferred.get()
}

suspend fun InputStream.readAsync(b: ByteArray, off: Int, len: Int): Int {
	return withContext(Dispatchers.IO) { this@readAsync.read(b, off, len) }
}