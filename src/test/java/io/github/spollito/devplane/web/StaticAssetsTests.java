package io.github.spollito.devplane.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.spollito.devplane.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/** Checks that DevPlane serves the stylesheet, fonts, htmx, and icon that its pages load. */
@IntegrationTest
class StaticAssetsTests {

	@Autowired
	private MockMvcTester mvc;

	@ParameterizedTest
	@ValueSource(strings = { "/css/devplane.css", "/fonts/atkinson-hyperlegible-next-latin-400-normal.woff2",
			"/fonts/atkinson-hyperlegible-next-latin-600-normal.woff2",
			"/fonts/atkinson-hyperlegible-mono-latin-400-normal.woff2", "/fonts/OFL.txt",
			"/webjars/htmx.org/dist/htmx.min.js" })
	void servesAsset(String path) {
		assertThat(mvc.get().uri(path)).hasStatusOk();
	}

	// Pages have an icon

	@Test
	void servesTheIcon() {
		assertThat(mvc.get().uri("/favicon.svg")).hasStatusOk().hasContentTypeCompatibleWith("image/svg+xml");
	}

}
