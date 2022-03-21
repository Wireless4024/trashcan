package com.wireless4024.trashcan.crypt

import java.io.BufferedOutputStream
import java.io.OutputStream
import javax.crypto.Cipher

class CipherStreamWriter(password: CharArray, out: OutputStream) : OutputStream() {
	private val out: BufferedOutputStream
	private val cip: Cipher

	init {
		val salt = genSalt()
		this.cip = AesHelper(password, salt).encCipher()
		this.out = BufferedOutputStream(out)
		this.out.write(salt)
	}

	private val buffer = ByteArray(1)
	override fun write(b: Int) {
		val buffer = this.buffer
		buffer[0] = b.toByte()
		out.write(cip.doFinal(buffer))
	}

	override fun write(b: ByteArray) {
		out.write(cip.update(b))
	}

	override fun write(b: ByteArray, off: Int, len: Int) {
		out.write(cip.update(b, off, len))
	}

	override fun close() {
		out.use {
			val bytes = cip.doFinal()
			out.write(bytes)
			out.flush()
		}
	}
}