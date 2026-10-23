package io.github.spollito.devplane.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import io.github.spollito.devplane.IntegrationTest;
import io.github.spollito.devplane.catalog.ServiceCatalog;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Scenarios from the web-ui spec's "Unregister a service after confirming" and "Pages
 * refuse form submissions from other sites" requirements.
 */
@IntegrationTest
class UnregisterPageTests {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private ServiceCatalog catalog;

	@BeforeEach
	void registerOrdersApi() {
		TestCatalog.clear(catalog);
		TestCatalog.register(catalog, "orders-api", "payments-team", null);
	}

	// Unregister a service after confirming

	@Test
	void asksBeforeUnregistering() {
		TestCatalog.giveRepository(catalog, "orders-api");

		Document page = HtmlPages.parse(mvc.get().uri("/services/orders-api/unregister").exchange());

		assertThat(page.select("main").text()).contains("orders-api will be removed from the catalog",
				"octo-dev/orders-api stays on GitHub");
		Element form = page.selectFirst("main form");
		assertThat(form.attr("method")).isEqualTo("post");
		assertThat(form.attr("action")).isEqualTo("/services/orders-api/unregister");
		assertThat(form.selectFirst("button").text()).isEqualTo("Unregister orders-api");
		assertThat(page.selectFirst("main a:contains(Cancel)").attr("href")).isEqualTo("/services/orders-api");
		assertThatNoException().isThrownBy(() -> catalog.lookUp("orders-api"));
	}

	@Test
	void unregistersTheService() {
		MvcTestResult result = unregister().exchange();

		assertThat(result).hasStatus(HttpStatus.SEE_OTHER).hasHeader(HttpHeaders.LOCATION, "/services");
		MvcTestResult catalogPage = mvc.get()
			.uri("/services")
			.flashAttrs(result.getMvcResult().getFlashMap())
			.exchange();
		assertThat(HtmlPages.parse(catalogPage).selectFirst("[role=status]").text()).isEqualTo("Unregistered orders-api.");
		assertThat(catalog.list(null, PageRequest.of(0, 1))).isEmpty();
	}

	@Test
	void unknownService() {
		for (MvcTestResult result : new MvcTestResult[] { mvc.get().uri("/services/payments-api/unregister").exchange(),
				mvc.post().uri("/services/payments-api/unregister").exchange() }) {
			assertThat(result).hasStatus(HttpStatus.NOT_FOUND).hasContentTypeCompatibleWith(MediaType.TEXT_HTML);
			assertThat(HtmlPages.parse(result).select("h1").text()).isEqualTo("No service named payments-api");
		}
	}

	// Pages refuse form submissions from other sites

	@Test
	void rejectsAFormFromAnotherSite() {
		MvcTestResult result = unregister().header(HttpHeaders.ORIGIN, "https://example.com").exchange();

		assertThat(result).hasStatus(HttpStatus.FORBIDDEN).hasContentTypeCompatibleWith(MediaType.TEXT_HTML);
		assertThatNoException().isThrownBy(() -> catalog.lookUp("orders-api"));
	}

	@Test
	void acceptsDevPlanesOwnForms() {
		assertThat(unregister().header("Sec-Fetch-Site", "same-origin")).hasStatus(HttpStatus.SEE_OTHER);

		assertThat(catalog.list(null, PageRequest.of(0, 1))).isEmpty();
	}

	private MockMvcTester.MockMvcRequestBuilder unregister() {
		return mvc.post().uri("/services/orders-api/unregister");
	}

}
