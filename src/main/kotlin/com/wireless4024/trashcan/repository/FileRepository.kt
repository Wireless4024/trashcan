package com.wireless4024.trashcan.repository

import com.wireless4024.trashcan.entity.FileRecord
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Repository
interface FileRepository : R2dbcRepository<FileRecord, Long> {
	fun findByHash(hash: String): Mono<FileRecord>
/*"
	@Modifying
	@Query("INSERT INTO file_record (hash, filename, content_type, expire, quota) VALUES (:#{#entity.hash},:#{#entity.filename},:#{#entity.contentType},:#{#entity.expire},:#{#entity.quota})")
	override fun <S : FileRecord?> save(entity: S): Mono<S>*/
}