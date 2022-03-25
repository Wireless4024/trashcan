@file:Suppress("FunctionName")

package com.wireless4024.trashcan.controller

import com.wireless4024.trashcan.entity.FileRecord
import com.wireless4024.trashcan.ext.transferToAsync
import com.wireless4024.trashcan.repository.FileRepository
import com.wireless4024.trashcan.storage.StorageService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.withContext
import lombok.RequiredArgsConstructor
import org.apache.tika.mime.MimeTypes
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.yaml.snakeyaml.util.UriEncoder
import java.net.InetAddress
import java.time.LocalDateTime
import kotlin.math.absoluteValue


data class UploadedFile(val file: MultipartFile, val duration: Long? = null, val quota: Int?) {
	fun quota(): Int? {
		// first time it has to fetch in iframe
		return if ((quota ?: 0) > 0) quota?.plus(1) else null
	}
}

@Suppress("unused") // used by spring
@RestController
@RequiredArgsConstructor
class StorageController {
	@Value("\${trashcan.upload.max_expire}")
	var maxExpireTime: Long = 3 * 24 * 60

	@Value("\${trashcan.upload.default_expire}")
	var defExpireTime: Long = 24 * 60

	@Value("\${trashcan.upload.local_only}")
	var localOnly: Boolean = true

	@Value("\${trashcan.http.error_as_gif}")
	var no404: Boolean = true

	private lateinit var storageService: StorageService
	private lateinit var fileRepository: FileRepository

	@Autowired
	fun FileUploadController(storageService: StorageService) {
		this.storageService = storageService
	}

	@Autowired
	fun FileRepository(repo: FileRepository) {
		this.fileRepository = repo
	}

	data class UploadInfo(
		val defaultDuration: Long,
		val maxExpire: Long,
	)

	@RequestMapping("/info", method = [RequestMethod.GET], produces = [APPLICATION_JSON_VALUE])
	fun info(): UploadInfo {
		return UploadInfo(defExpireTime, maxExpireTime)
	}

	@GetMapping("/error")
	fun err(resp: HttpServletResponse) {
		resp.sendRedirect("/")
	}

	@PostMapping("/upload")
	suspend fun upload(req: HttpServletRequest, @ModelAttribute file: UploadedFile, resp: HttpServletResponse) {
		// only local upload is supported
		// change setting in application.properties
		if (localOnly && !withContext(Dispatchers.IO) {
				InetAddress.getByName(req.remoteHost)
			}.isAnyLocalAddress) return resp.e404()


		val mfile = file.file
		val (chash, fhash) = storageService.store(mfile).awaitSingleOrNull()
			?: return withContext(Dispatchers.IO) { resp.sendRedirect("/") }

		var filename = mfile.originalFilename
		if (filename == null || filename == "UNKNOWN") {
			val ext = MimeTypes.getDefaultMimeTypes()
				.forName(mfile.contentType)
				.extension
				.ifBlank { ".txt" }
			filename = "file$ext"
		}

		// default expire time is 12 hour
		val expire = LocalDateTime.now()
			.plusMinutes((file.duration?.absoluteValue ?: (defExpireTime)).coerceAtMost(maxExpireTime))
		val rawRecord = fileRepository.findByHash(fhash).awaitFirstOrNull()
		val record = if (rawRecord != null) {
			if (rawRecord.expire == null)
				rawRecord.expire = expire
			else
				rawRecord.expire = rawRecord
					.expire!!
					.compareTo(expire)
					.let { if (it == 1) rawRecord.expire else expire }
			rawRecord
		} else {
			FileRecord(fhash, filename, mfile.contentType, expire, file.quota())
		}
		fileRepository.save(record).awaitSingleOrNull()

		withContext(Dispatchers.IO) { resp.sendRedirect("./upload_finish.html?hash=${UriEncoder.encode(chash)}") }
	}

	suspend fun HttpServletResponse.e404() {
		if (no404) {
			contentType = "image/gif"
			outputStream.use {
				javaClass.classLoader.getResourceAsStream("static/404.gif")?.transferToAsync(it)
			}
		} else
			status = 404
	}

	@GetMapping("/!/{hash}")
	suspend fun find(@PathVariable("hash") hash: String, resp: HttpServletResponse) {
		val (filename, stream) = storageService.load(hash).awaitSingleOrNull() ?: return resp.e404()
		val file = fileRepository.findByHash(filename).awaitFirstOrNull() ?: return resp.e404()
		if (file.update()) {
			fileRepository.save(file).awaitSingleOrNull()
			resp.contentType = file.contentType ?: "application/x-download"
			resp.setHeader("Content-Disposition", "attachment; filename=" + (file.filename ?: "file.txt"))
			resp.outputStream.use { stream.transferToAsync(it) }
		} else {
			resp.e404()
		}
	}

	@GetMapping("/$/{hash}")
	suspend fun findNoDownload(@PathVariable("hash") hash: String, resp: HttpServletResponse) {
		val (filename, stream) = storageService.load(hash).awaitSingleOrNull() ?: return resp.e404()
		val file = fileRepository.findByHash(filename).awaitFirstOrNull() ?: return resp.e404()
		if (file.update()) {
			fileRepository.save(file).awaitSingleOrNull()
			resp.contentType = file.contentType ?: "application/x-download"
			resp.setHeader("Content-Disposition", "inline")
			resp.outputStream.use { stream.transferToAsync(it) }
		} else {
			resp.e404()
		}
	}
}