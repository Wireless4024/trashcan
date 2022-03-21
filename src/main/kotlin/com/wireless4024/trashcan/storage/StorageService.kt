package com.wireless4024.trashcan.storage

import com.wireless4024.trashcan.crypt.CipherStreamReader
import org.springframework.web.multipart.MultipartFile
import reactor.core.publisher.Mono


interface StorageService {
	fun init()
	fun store(file: MultipartFile):  Mono<Pair<String,String>>
	fun load(filename: String): Mono<Pair<String, CipherStreamReader>>
	fun tryDrop(filename: String): Mono<Boolean>
}