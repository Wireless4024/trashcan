package com.wireless4024.trashcan.entity

import lombok.Builder
import lombok.Data
import org.springframework.data.annotation.Id
import org.springframework.format.annotation.DateTimeFormat
import java.time.LocalDateTime

@Data
@Builder
class FileRecord() {

	constructor(hash: String, filename: String, contentType: String?, expire: LocalDateTime?, quota: Int?) : this() {
		this.hash = hash
		this.filename = filename
		this.contentType = contentType
		this.expire = expire
		this.quota = quota
	}

	// only for spring to update / insert correctly
	@Id
	@Suppress("unused")
	var id: Int = 0

	var hash: String? = null

	var filename: String? = null

	var contentType: String? = null

	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME, pattern = "yyyy-MM-dd")
	var expire: LocalDateTime? = null

	var quota: Int? = null

	fun update(): Boolean {
		if ((expire?.compareTo(LocalDateTime.now()) ?: return false) <= 0) {
			return false
		}


		// concurrency issue here : if we fire multiple request at once
		// should update in database instead of here!
		return quota?.let {
			if (it >= 0) {
				quota = it - 1
				true
			} else {
				false
			}
		} ?: true
		/*if ( ?: false) {
			quota = quota?.minus(1)
		}*/
	}

	override fun toString(): String {
		return "File($filename, $hash, $contentType)"
	}
}