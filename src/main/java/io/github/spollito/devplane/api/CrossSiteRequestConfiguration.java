package io.github.spollito.devplane.api;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Checks every request, for pages and the API alike, with {@link CrossSiteRequestInterceptor}. */
@Configuration(proxyBeanMethods = false)
class CrossSiteRequestConfiguration implements WebMvcConfigurer {

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new CrossSiteRequestInterceptor());
	}

}
