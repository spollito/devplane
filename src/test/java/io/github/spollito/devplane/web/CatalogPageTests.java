package io.github.spollito.devplane.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.IntStream;

import io.github.spollito.devplane.IntegrationTest;
import io.github.spollito.devplane.catalog.ServiceCatalog;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Scenarios from the web-ui spec's "Pages are served at DevPlane's address", "Browse
 * the catalog", "Page through the catalog", "Filter the catalog by owner", and
 * "Introduce DevPlane in an empty catalog" requirements.
 */
@IntegrationTest
class CatalogPageTests {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private ServiceCatalog catalog;

	@BeforeEach
	void clearCatalog() {
		TestCatalog.clear(catalog);
	}

	// Pages are served at DevPlane's address

	@Test
	void opensTheCatalogFromTheRoot() {
		assertThat(mvc.get().uri("/")).hasStatus(HttpStatus.FOUND).hasHeader(HttpHeaders.LOCATION, "/services");
	}

	// Browse the catalog

	@Test
	void listsServicesAlphabetically() {
		register("payments-api", "auth-service", "orders-api");

		MvcTestResult result = mvc.get().uri("/services").exchange();

		assertThat(result).hasStatusOk().hasContentTypeCompatibleWith(MediaType.TEXT_HTML);
		Document page = HtmlPages.parse(result);
		assertThat(names(page)).containsExactly("auth-service", "orders-api", "payments-api");
		assertThat(page.select("tbody tr td:first-child > a").eachAttr("href"))
			.containsExactly("/services/auth-service", "/services/orders-api", "/services/payments-api");
	}

	@Test
	void showsEachServicesRepository() {
		register("orders-api", "auth-service");
		TestCatalog.giveRepository(catalog, "orders-api");

		Document page = page("/services");

		Element repository = row(page, "orders-api").selectFirst("td.cell-repository a");
		assertThat(repository.text()).isEqualTo("octo-dev/orders-api");
		assertThat(repository.attr("href")).isEqualTo("https://github.com/octo-dev/orders-api");
		assertThat(row(page, "auth-service").selectFirst("td.cell-repository").text()).isEqualTo("None");
	}

	@Test
	void showsAMissingDescription() {
		TestCatalog.register(catalog, "orders-api", "payments-team", null);

		assertThat(row(page("/services"), "orders-api").selectFirst("td.cell-description").text())
			.isEqualTo("No description");
	}

	// Page through the catalog

	@Test
	void showsTheSecondPage() {
		registerNumbered(25);

		Document page = page("/services?page=2");

		assertThat(names(page)).containsExactlyElementsOf(numbered(21, 25));
		assertThat(page.select(".table-footer p").text()).isEqualTo("21–25 of 25");
		assertThat(page.select("a:contains(Previous)").attr("href")).isEqualTo("/services?page=1");
		assertThat(page.select("a:contains(Next)")).isEmpty();
	}

	@Test
	void hidesPagingWhenEverythingFits() {
		register("payments-api", "auth-service", "orders-api");

		Document page = page("/services");

		assertThat(page.select(".table-footer p").text()).isEqualTo("3 services");
		assertThat(page.select("a:contains(Previous), a:contains(Next)")).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = { "abc", "0", "9" })
	void correctsPageNumbersThatDontExist(String pageNumber) {
		registerNumbered(25);

		MvcTestResult result = mvc.get().uri("/services?page={page}", pageNumber).exchange();

		assertThat(result).hasStatusOk();
		assertThat(names(HtmlPages.parse(result)))
			.containsExactlyElementsOf(pageNumber.equals("9") ? numbered(21, 25) : numbered(1, 20));
	}

	// Filter the catalog by owner

	@Test
	void listsOneOwnersServices() {
		registerWithOwners();

		Document page = page("/services?owner=payments-team");

		assertThat(names(page)).containsExactly("orders-api", "payments-api");
		assertThat(page.selectFirst("input#owner").val()).isEqualTo("payments-team");
	}

	@Test
	void ownerNamesLinkToTheirList() {
		registerWithOwners();

		assertThat(row(page("/services"), "orders-api").selectFirst("td.cell-owner a").attr("href"))
			.isEqualTo("/services?owner=payments-team");
	}

	@Test
	void ownerWithNoServices() {
		registerWithOwners();

		MvcTestResult result = mvc.get().uri("/services?owner=billing-team").exchange();

		assertThat(result).hasStatusOk();
		Document page = HtmlPages.parse(result);
		assertThat(page.select("#results").text()).contains("billing-team owns no services");
		assertThat(page.select("#results a[href=/services]")).isNotEmpty();
		assertThat(page.select("table")).isEmpty();
	}

	@Test
	void blankOwner() {
		registerWithOwners();

		assertThat(names(page("/services?owner="))).hasSize(3);
	}

	@Test
	void filtersInPlaceWithJavaScript() {
		registerWithOwners();

		Element filter = page("/services").selectFirst("form.filter");

		assertThat(filter.attr("method")).isEqualTo("get");
		assertThat(filter.attr("action")).isEqualTo("/services");
		assertThat(filter.attr("hx-get")).isEqualTo("/services");
		assertThat(filter.attr("hx-trigger")).contains("input changed delay:300ms", "submit");
		assertThat(filter.attr("hx-select")).isEqualTo("#results");
		assertThat(filter.attr("hx-target")).isEqualTo("#results");
		assertThat(filter.attr("hx-push-url")).isEqualTo("true");
	}

	// Introduce DevPlane in an empty catalog

	@Test
	void emptyCatalog() {
		Document page = page("/services");

		assertThat(page.select("table")).isEmpty();
		assertThat(page.select("input#owner")).isEmpty();
		assertThat(page.select("main h2").text()).isEqualTo("No services are registered yet");
		assertThat(page.select(".path .step h3").eachText()).containsExactly("Catalog entry", "Project", "Repository");
		assertThat(page.select("a:contains(Register service)")).singleElement()
			.satisfies(link -> assertThat(link.attr("href")).isEqualTo("/register"));
	}

	private Document page(String uri) {
		MvcTestResult result = mvc.get().uri(uri).exchange();
		assertThat(result).hasStatusOk();
		return HtmlPages.parse(result);
	}

	private void register(String... names) {
		for (String name : names) {
			TestCatalog.register(catalog, name, "payments-team", "A service");
		}
	}

	private void registerWithOwners() {
		TestCatalog.register(catalog, "orders-api", "payments-team", null);
		TestCatalog.register(catalog, "payments-api", "payments-team", null);
		TestCatalog.register(catalog, "auth-service", "identity-team", null);
	}

	private void registerNumbered(int count) {
		numbered(1, count).forEach(name -> TestCatalog.register(catalog, name, "platform-team", null));
	}

	private static List<String> numbered(int first, int last) {
		return IntStream.rangeClosed(first, last).mapToObj(i -> "service-%03d".formatted(i)).toList();
	}

	private static List<String> names(Document page) {
		return page.select("tbody tr td:first-child > a").eachText();
	}

	private static Element row(Document page, String name) {
		return page.select("tbody tr").stream()
			.filter(row -> row.selectFirst("td:first-child > a").text().equals(name))
			.findFirst()
			.orElseThrow();
	}

}
