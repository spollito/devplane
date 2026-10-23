package io.github.spollito.devplane.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * The "Unexpected error" scenario of the web-ui spec's "Pages show errors as HTML"
 * requirement, through a test-only page controller. It's in the {@code web} package,
 * so {@link PageExceptionHandler} applies to it as it does to the real pages.
 */
// Component scanning skips classes nested in tests, so the controller is imported
// explicitly. Naming it in @WebMvcTest keeps the application's controllers out.
@WebMvcTest(controllers = PageExceptionHandlerTests.TestOnlyPages.class)
@Import(PageExceptionHandlerTests.TestOnlyPages.class)
class PageExceptionHandlerTests {

	private static final String INTERNAL_DETAIL = "connection pool exhausted on db-host-42";

	@Autowired
	private MockMvcTester mvc;

	@Test
	void unexpectedError() {
		MvcTestResult result = mvc.get().uri("/test-only/fail").accept(MediaType.TEXT_HTML).exchange();

		assertThat(result).hasStatus(HttpStatus.INTERNAL_SERVER_ERROR)
			.hasContentTypeCompatibleWith(MediaType.TEXT_HTML);
		assertThat(HtmlPages.parse(result).select("h1").text()).isEqualTo("DevPlane couldn't finish this request");
		assertThat(result).bodyText()
			.doesNotContain(INTERNAL_DETAIL)
			.doesNotContain("IllegalStateException")
			.doesNotContain("at io.github.spollito");
	}

	@Controller
	static class TestOnlyPages {

		@GetMapping("/test-only/fail")
		String fail() {
			throw new IllegalStateException(INTERNAL_DETAIL);
		}

	}

}
