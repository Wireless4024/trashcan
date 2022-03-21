package com.wireless4024.trashcan

import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration.WebMvcAutoConfigurationAdapter
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.web.reactive.config.ResourceHandlerRegistry
import org.springframework.web.reactive.config.WebFluxConfigurer

@Configuration
class StaticResourceConfiguration : WebFluxConfigurer {
	override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
		registry.setOrder(Ordered.HIGHEST_PRECEDENCE)
	}
}