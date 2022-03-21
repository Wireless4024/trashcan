package com.wireless4024.trashcan

import com.wireless4024.trashcan.ext.transferToAsync
import com.wireless4024.trashcan.repository.FileRepository
import com.wireless4024.trashcan.storage.StorageProperties
import com.wireless4024.trashcan.storage.StorageService
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.reactive.awaitLast
import kotlinx.coroutines.reactor.mono
import org.apache.tika.mime.MimeTypes
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.flyway.FlywayProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.await
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.util.MimeTypeUtils
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.kotlin.extra.math.sumAll
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.deleteExisting
import kotlin.io.path.deleteIfExists
import kotlin.io.path.extension
import kotlin.streams.asSequence


@SpringBootApplication

@EnableConfigurationProperties(
	StorageProperties::class,
	FlywayProperties::class
)
@EnableScheduling
class TrashcanApplication {
	@Value("\${spring.r2dbc.url}")
	var dbUrl: String = ""

	private lateinit var storageService: StorageService
	private lateinit var db: DatabaseClient

	@Autowired
	fun FileUploadController(storageService: StorageService) {
		this.storageService = storageService
	}

	@Bean
	fun init(storageService: StorageService): CommandLineRunner {
		return CommandLineRunner {
			File("application.properties").let { f ->
				if (!f.exists()) {
					f.outputStream().use {
						this@TrashcanApplication.javaClass.classLoader.getResourceAsStream("application.properties")
							?.transferTo(it)
					}
				}
			}

			storageService.init()

			Files.list(Path.of("./upload"))
				.asSequence()
				.filter { it.extension == "dat" }
				.forEach { it.deleteIfExists() }

			val connectionFactory: ConnectionFactory = ConnectionFactories.get(dbUrl)

			db = DatabaseClient.create(connectionFactory)
		}
	}

	@Scheduled(fixedDelay = 10000)
	fun purge() {
		if (::db.isInitialized) {
			val expired = db.sql(
				"""
				BEGIN ;
				CALL purge_file('ref');
				FETCH ALL IN ref;
				COMMIT ;				
				""".trimIndent()
			).fetch().all()

			val removed = expired
				.flatMap { storageService.tryDrop(it["filename"]?.toString() ?: return@flatMap Mono.just(false)) }
				.reduce(0) { acc, it -> if (it) acc + 1 else acc }
				.block()
			if (removed != null && removed != 0)
				println("removed $removed file(s)")
		}
	}
}

fun main(args: Array<String>) {
	runApplication<TrashcanApplication>(*args)
}
