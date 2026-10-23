package io.github.spollito.devplane.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.spollito.devplane.IntegrationTest;
import jakarta.servlet.RequestDispatcher;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Scenarios from the web-ui spec's "Pages show errors as HTML" requirement. MockMvc
 * doesn't follow the servlet container's error dispatch, so an unknown path is
 * checked in two steps: the 404 that hands the error to the container, and the page
 * Spring Boot's error controller then renders.
 */
@IntegrationTest
class ErrorPagesTests {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void unknownPage() {
		MvcTestResult result = mvc.get().uri("/does-not-exist").accept(MediaType.TEXT_HTML).exchange();

		assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(result.getResponse().getErrorMessage()).isNull();
		assertThat(result.getResponse().getContentType()).isNull();
		assertThat(result).bodyText().isEmpty();

		MvcTestResult page = mvc.get()
			.uri("/error")
			.requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 404)
			.requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/does-not-exist")
			.accept(MediaType.TEXT_HTML)
			.exchange();

		assertThat(page).hasStatus(HttpStatus.NOT_FOUND).hasContentTypeCompatibleWith(MediaType.TEXT_HTML);
		Document document = HtmlPages.parse(page);
		assertThat(document.select("h1").text()).isEqualTo("Page not found");
		assertThat(document.select("main a[href=/services]")).isNotEmpty();
	}

	@Test
	void apiPathsKeepProblemDetails() {
		assertThat(mvc.get().uri("/api/v1/does-not-exist").accept(MediaType.TEXT_HTML))
			.hasStatus(HttpStatus.NOT_FOUND)
			.hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
			.bodyJson()
			.extractingPath("$.status")
			.isEqualTo(404);
	}

}
