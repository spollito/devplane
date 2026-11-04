package io.github.spollito.devplane.api;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.spollito.devplane.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** The "Every page sends the headers" scenario from the web-ui spec. */
@IntegrationTest
class FrameDenyFilterTests {

	@Autowired
	private MockMvcTester mvc;

	@BeforeEach
	void registerOrdersApi() {
		mvc.delete().uri("/api/v1/services/orders-api").exchange();
		mvc.post().uri("/api/v1/services").contentType(MediaType.APPLICATION_JSON).content("""
				{"name": "orders-api", "owner": "payments-team"}
				""").exchange();
	}

	@ParameterizedTest
	@ValueSource(strings = { "/services", "/services/orders-api", "/services/orders-api/unregister", "/register",
			"/does-not-exist", "/swagger-ui/index.html", "/api/v1/services" })
	void everyResponseRefusesToBeFramed(String path) {
		MvcTestResult result = mvc.get().uri(path).exchange();

		assertThat(result).hasHeader("X-Frame-Options", "DENY")
			.hasHeader("Content-Security-Policy", "frame-ancestors 'none'");
	}

}
