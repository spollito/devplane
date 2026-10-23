package io.github.spollito.devplane.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.spollito.devplane.IntegrationTest;
import io.github.spollito.devplane.catalog.ServiceCatalog;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Scenarios from the web-ui spec's "Pages work without JavaScript" and "Pages meet the
 * accessibility minimums" requirements, checked on every page.
 */
@IntegrationTest
class PageStructureTests {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private ServiceCatalog catalog;

	@BeforeEach
	void registerOrdersApi() {
		TestCatalog.clear(catalog);
		TestCatalog.register(catalog, "orders-api", "payments-team", "Order management REST API");
	}

	// Pages work without JavaScript

	@ParameterizedTest
	@ValueSource(strings = { "/services", "/services/orders-api", "/register", "/services/orders-api/unregister" })
	void everyActionIsALinkOrAForm(String path) {
		Document page = page(path);

		assertThat(page.select("button")).isNotEmpty().allSatisfy(button -> {
			Element form = button.closest("form");
			assertThat(form).as("form around %s", button).isNotNull();
			assertThat(form.attr("method")).isNotBlank();
			assertThat(form.attr("action")).isNotBlank();
		});
		assertThat(page.select("a")).allSatisfy(link -> assertThat(link.attr("href")).as("href of %s", link).isNotBlank());
	}

	// Pages meet the accessibility minimums

	@ParameterizedTest
	@ValueSource(strings = { "/services", "/services/orders-api", "/register", "/services/orders-api/unregister" })
	void structureOfEveryPage(String path) {
		Document page = page(path);

		Element firstLink = page.selectFirst("a");
		assertThat(firstLink.text()).isEqualTo("Skip to content");
		assertThat(firstLink.attr("href")).isEqualTo("#main");
		assertThat(page.select("main#main")).hasSize(1);
		assertThat(page.select("h1")).hasSize(1);
		assertThat(page.select("header")).isNotEmpty();
		assertThat(page.select("nav")).isNotEmpty();
		assertThat(page.title()).startsWith(page.selectFirst("h1").text().replaceFirst("\\?$", "").split(" ")[0])
			.endsWith(" · DevPlane");
		assertThat(page.select("input")).allSatisfy(input -> {
			boolean labelledByFor = !input.id().isEmpty() && !page.select("label[for=" + input.id() + "]").isEmpty();
			boolean insideLabel = input.closest("label") != null;
			assertThat(labelledByFor || insideLabel).as("label for %s", input).isTrue();
		});
	}

	private Document page(String path) {
		MvcTestResult result = mvc.get().uri(path).exchange();
		assertThat(result).hasStatusOk();
		return HtmlPages.parse(result);
	}

}
