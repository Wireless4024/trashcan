package com.wireless4024.trashcan

import org.flywaydb.core.Flyway
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.flyway.FlywayProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FlywayConfig(@Autowired private val flywayProperties: FlywayProperties) {
	@Bean(initMethod = "migrate")
	fun flyway(): Flyway? {

		return Flyway(
			Flyway.configure()
				.baselineOnMigrate(true)
				.dataSource(flywayProperties.url, flywayProperties.user, flywayProperties.password)
		)
	}
}