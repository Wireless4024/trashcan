package com.wireless4024.trashcan.storage

import org.springframework.boot.context.properties.ConfigurationProperties


@ConfigurationProperties("storage")
class StorageProperties {
	/**
	 * Folder location for storing files
	 */
	var location = "upload"
}