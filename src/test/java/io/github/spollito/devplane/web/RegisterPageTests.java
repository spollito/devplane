package io.github.spollito.devplane.web;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.springframework.test.web.servlet.assertj.MockMvcTester.MockMvcRequestBuilder;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Scenarios from the web-ui spec's "Register a service from a form", "Show form
 * errors next to their fields", "Reject a duplicate service name in the form", and
 * "Pages refuse form submissions from other sites" requirements.
 */
@IntegrationTest
class RegisterPageTests {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private ServiceCatalog catalog;

	@BeforeEach
	void clearCatalog() {
		TestCatalog.clear(catalog);
	}

	// Register a service from a form

	@Test
	void registersAService() {
		MvcTestResult result = register().param("name", "orders-api")
			.param("owner", "payments-team")
			.param("description", "Order management REST API")
			.param("lifecycle", "experimental")
			.exchange();

		assertThat(result).hasStatus(HttpStatus.SEE_OTHER).hasHeader(HttpHeaders.LOCATION, "/services/orders-api");
		MvcTestResult page = mvc.get()
			.uri("/services/orders-api")
			.flashAttrs(result.getMvcResult().getFlashMap())
			.exchange();
		assertThat(HtmlPages.parse(page).selectFirst("[role=status]").text()).isEqualTo("Registered orders-api.");
		assertThat(mvc.get().uri("/api/v1/services/orders-api")).hasStatusOk().bodyJson().isLenientlyEqualTo("""
				{"name": "orders-api", "owner": "payments-team", "description": "Order management REST API",
				"lifecycle": "experimental"}
				""");
	}

	@Test
	void offersExperimentalByDefault() {
		Document page = HtmlPages.parse(mvc.get().uri("/register").exchange());

		assertThat(page.select("input[name=lifecycle][checked]").eachAttr("value")).containsExactly("experimental");
	}

	// Show form errors next to their fields

	@Test
	void reportsEveryInvalidField() {
		MvcTestResult result = register().param("name", "Orders API")
			.param("owner", "")
			.param("lifecycle", "experimental")
			.exchange();

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST).hasContentTypeCompatibleWith(MediaType.TEXT_HTML);
		Document page = HtmlPages.parse(result);
		assertThat(page.selectFirst("input#name").val()).isEqualTo("Orders API");
		assertThat(page.selectFirst("#name-error").text()).isEqualTo(
				"Name must be 1 to 63 lowercase letters, digits, or hyphens, start with a letter, and not end with a hyphen");
		assertThat(page.selectFirst("#owner-error").text()).isEqualTo("Enter the team that owns the service");
		for (String field : new String[] { "name", "owner" }) {
			Element input = page.selectFirst("input#" + field);
			assertThat(input.attr("aria-invalid")).isEqualTo("true");
			assertThat(input.attr("aria-describedby").split(" ")).contains(field + "-error");
		}
		Element summary = page.selectFirst("#error-summary");
		assertThat(summary.hasAttr("autofocus")).isTrue();
		assertThat(summary.attr("tabindex")).isEqualTo("-1");
		assertThat(summary.select("a").eachAttr("href")).containsExactly("#name", "#owner");
		assertThat(catalog.list(null, PageRequest.of(0, 1))).isEmpty();
	}

	@Test
	void rejectsAnOverlongDescription() {
		MvcTestResult result = register().param("name", "orders-api")
			.param("owner", "payments-team")
			.param("description", "x".repeat(301))
			.param("lifecycle", "experimental")
			.exchange();

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(HtmlPages.parse(result).selectFirst("#description-error").text())
			.isEqualTo("Description must be at most 300 characters");
	}

	// Reject a duplicate service name in the form

	@Test
	void nameAlreadyRegistered() {
		TestCatalog.register(catalog, "orders-api", "payments-team", null);

		MvcTestResult result = register().param("name", "orders-api")
			.param("owner", "checkout-team")
			.param("lifecycle", "experimental")
			.exchange();

		assertThat(result).hasStatus(HttpStatus.CONFLICT);
		Element error = HtmlPages.parse(result).selectFirst("#name-error");
		assertThat(error.text()).isEqualTo("A service named orders-api already exists");
		assertThat(error.selectFirst("a").attr("href")).isEqualTo("/services/orders-api");
		assertThat(catalog.lookUp("orders-api").owner()).isEqualTo("payments-team");
	}

	// Pages refuse form submissions from other sites

	@Test
	void rejectsACrossSiteFetch() {
		MvcTestResult result = register().header("Sec-Fetch-Site", "cross-site")
			.param("name", "orders-api")
			.param("owner", "payments-team")
			.param("lifecycle", "experimental")
			.exchange();

		assertThat(result).hasStatus(HttpStatus.FORBIDDEN).hasContentTypeCompatibleWith(MediaType.TEXT_HTML);
		assertThat(HtmlPages.parse(result).select("h1").text()).isEqualTo("Request refused");
		assertThat(catalog.list(null, PageRequest.of(0, 1))).isEmpty();
	}

	private MockMvcRequestBuilder register() {
		return mvc.post().uri("/register").contentType(MediaType.APPLICATION_FORM_URLENCODED);
	}

}
