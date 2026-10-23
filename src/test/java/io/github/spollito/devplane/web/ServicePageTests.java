package io.github.spollito.devplane.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import io.github.spollito.devplane.IntegrationTest;
import io.github.spollito.devplane.MutableClock;
import io.github.spollito.devplane.catalog.ServiceCatalog;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Scenarios from the web-ui spec's "View a service's page", "Download a service's
 * project from its page", and "Show a service's repository" requirements.
 */
@IntegrationTest
class ServicePageTests {

	private static final Instant REGISTERED_AT = Instant.parse("2026-10-14T09:12:00Z");

	private static final Instant PROVISIONED_AT = Instant.parse("2026-10-22T15:02:00Z");

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private ServiceCatalog catalog;

	@Autowired
	private MutableClock clock;

	@BeforeEach
	void registerOrdersApi() {
		TestCatalog.clear(catalog);
		clock.setInstant(REGISTERED_AT);
		TestCatalog.register(catalog, "orders-api", "payments-team", "Order management REST API");
	}

	// View a service's page

	@Test
	void showsAService() {
		MvcTestResult result = mvc.get().uri("/services/orders-api").exchange();

		assertThat(result).hasStatusOk().hasContentTypeCompatibleWith(MediaType.TEXT_HTML);
		Document page = HtmlPages.parse(result);
		assertThat(page.select("h1").text()).isEqualTo("orders-api");
		assertThat(page.select(".page-header p").text()).isEqualTo("Order management REST API");
		Element owner = page.selectFirst(".summary a");
		assertThat(owner.text()).isEqualTo("payments-team");
		assertThat(owner.attr("href")).isEqualTo("/services?owner=payments-team");
		assertThat(page.select(".summary").text()).contains("Experimental", "Registered Oct 14, 2026, 09:12",
				"Updated Oct 14, 2026, 09:12");
		assertThat(page.select(".path .step h3").eachText()).containsExactly("Catalog entry", "Project", "Repository");
		assertThat(page.select("[role=alert]")).isEmpty();
	}

	@Test
	void unknownService() {
		MvcTestResult result = mvc.get().uri("/services/payments-api").exchange();

		assertThat(result).hasStatus(HttpStatus.NOT_FOUND).hasContentTypeCompatibleWith(MediaType.TEXT_HTML);
		Document page = HtmlPages.parse(result);
		assertThat(page.select("h1").text()).isEqualTo("No service named payments-api");
		assertThat(page.select("main").text()).contains("No service by that name is registered");
		assertThat(page.select("main a[href=/services]")).isNotEmpty();
	}

	// Download a service's project from its page

	@Test
	void linksToTheDownload() {
		Element project = step(HtmlPages.parse(mvc.get().uri("/services/orders-api").exchange()), "Project");

		assertThat(project.select(".id").text()).contains("java-rest-api");
		Element download = project.selectFirst("a:contains(Download project)");
		assertThat(download.attr("href")).isEqualTo("/api/v1/services/orders-api/project");
		assertThat(download.hasAttr("download")).isTrue();
	}

	// Show a service's repository

	@Test
	void showsAProvisionedRepository() {
		clock.setInstant(PROVISIONED_AT);
		TestCatalog.giveRepository(catalog, "orders-api");

		Document page = HtmlPages.parse(mvc.get().uri("/services/orders-api").exchange());

		Element repository = step(page, "Repository");
		Element link = repository.selectFirst("a.repo-link");
		assertThat(link.text()).isEqualTo("octo-dev/orders-api");
		assertThat(link.attr("href")).isEqualTo("https://github.com/octo-dev/orders-api");
		assertThat(repository.select(".step__status").text()).isEqualTo("Created Oct 22, 2026, 15:02 from java-rest-api");
		assertThat(page.select("button:contains(Create repository)")).isEmpty();
		assertThat(page.select("[role=alert]")).isEmpty();
	}

	private static Element step(Document page, String heading) {
		return page.select(".path .step").stream()
			.filter(step -> step.selectFirst("h3").text().equals(heading))
			.findFirst()
			.orElseThrow();
	}

}
