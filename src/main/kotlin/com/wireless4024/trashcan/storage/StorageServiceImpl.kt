package com.wireless4024.trashcan.storage

import com.wireless4024.trashcan.crypt.*
import com.wireless4024.trashcan.ext.transferToAsync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.mono
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import reactor.core.publisher.Mono
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.FileAttribute
import java.nio.file.attribute.FileStoreAttributeView
import java.nio.file.attribute.FileTime
import java.util.*
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.outputStream
import kotlin.streams.asSequence


@Service
class StorageServiceImpl @Autowired constructor(properties: StorageProperties) : StorageService {
	private val rootLocation: Path

	init {
		rootLocation = Paths.get(properties.location)
		// ensure upload folder existed
		Files.createDirectories(rootLocation)
		// remove old temp files
		Files.list(rootLocation)
			.asSequence()
			.filter { it.endsWith(".dat") }
			.forEach { it.deleteIfExists() }
	}

	private suspend fun storeAsync(file: MultipartFile): Pair<String, String>? {
		var destinationFile: Path? = null
		try {
			if (file.isEmpty) {
				return null
			}
			destinationFile = withContext(Dispatchers.IO) { Files.createTempFile(rootLocation, "upload-", ".dat") }

			val hash = file.inputStream.hash224ToAsync(
				destinationFile.outputStream(
					StandardOpenOption.CREATE,
					StandardOpenOption.WRITE
				)
			)
			val b64 = Base64.getEncoder()
			val strHash = b64.encodeToString(hash).replace('/', '_')

			val storeHash = b64.encodeToString(hash512Async(hash)).replace('/', '_')

			val outPath = rootLocation.resolve(storeHash)
			if (outPath.exists()) return strHash to storeHash
			val dst = withContext(Dispatchers.IO) {
				Files.newOutputStream(
					outPath,
					StandardOpenOption.CREATE,
					StandardOpenOption.WRITE
				)
			}

			/// cpu bound use fixed thread size
			val enc = withContext(Dispatchers.Default) { CipherStreamWriter(strHash.toCharArray(), dst) }

			val input = withContext(Dispatchers.IO) { Files.newInputStream(destinationFile) }
			enc.use { input.transferToAsync(it) }
			return strHash to storeHash
		} catch (e: IOException) {
			e.printStackTrace()
			destinationFile?.deleteIfExists()
			return null
		}
	}

	private fun <T> Mono<T>.nullToEmpty(): Mono<T> {
		return flatMap { if (it == null) Mono.empty() else Mono.just(it) }
	}

	override fun store(file: MultipartFile): Mono<Pair<String, String>> {
		return mono { storeAsync(file) }.nullToEmpty()
	}

	private fun resolveHash(hash: String): Pair<ByteArray, String> {
		val dec = Base64.getDecoder()
		val rawHash = dec.decode(hash.replace('_', '/'))
		return rawHash to Base64.getEncoder().encodeToString(hash512(rawHash)).replace('/', '_')
	}

	private fun getPath(filename: String): Path? {
		val path = rootLocation.resolve(filename)
		return if (path.exists()) path else null
	}

	private suspend fun loadAsync(filename: String): Pair<String, CipherStreamReader>? {
		try {
			val (_, target) = resolveHash(filename)
			val file = getPath(target) ?: return null
			val input = withContext(Dispatchers.IO) { Files.newInputStream(file, StandardOpenOption.READ) }
			return target to withContext(Dispatchers.Default) { CipherStreamReader(filename.toCharArray(), input) }
		} catch (e: Throwable) {
			return null
		}
	}

	override fun load(filename: String): Mono<Pair<String, CipherStreamReader>> {
		return mono { loadAsync(filename) }.nullToEmpty()
	}

	override fun tryDrop(filename: String): Mono<Boolean> {
		val path = rootLocation.resolve(filename)

		return if (path.exists()) {
			mono { withContext(Dispatchers.IO) { path.deleteIfExists() } }
		} else {
			Mono.just(false)
		}
	}

	override fun init() {
		try {
			Files.createDirectories(rootLocation)
		} catch (e: IOException) {
			e.printStackTrace()
		}
	}
}