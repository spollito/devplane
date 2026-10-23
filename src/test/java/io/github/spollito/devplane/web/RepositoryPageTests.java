package io.github.spollito.devplane.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.spollito.devplane.IntegrationTest;
import io.github.spollito.devplane.catalog.ServiceCatalog;
import io.github.spollito.devplane.provisioning.FakeGitHub;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Scenarios from the web-ui spec's "Create a service's repository from its page" and
 * "Explain why a repository wasn't created" requirements, run against
 * {@link FakeGitHub}. "No GitHub token" is in {@link NoGitHubTokenPageTests}.
 */
@IntegrationTest
class RepositoryPageTests {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private ServiceCatalog catalog;

	@Autowired
	private FakeGitHub gitHub;

	@BeforeEach
	void registerOrdersApi() {
		gitHub.reset();
		TestCatalog.clear(catalog);
		TestCatalog.register(catalog, "orders-api", "payments-team", "Order management REST API");
	}

	// Create a service's repository from its page

	@Test
	void createsAPrivateRepositoryByDefault() {
		Document before = HtmlPages.parse(mvc.get().uri("/services/orders-api").exchange());
		assertThat(before.selectFirst("input[name=visibility][value=private]").hasAttr("checked")).isTrue();
		assertThat(before.selectFirst("input[name=visibility][value=public]").hasAttr("checked")).isFalse();

		MvcTestResult result = create("visibility", "private");

		assertThat(result).hasStatus(HttpStatus.SEE_OTHER).hasHeader(HttpHeaders.LOCATION, "/services/orders-api");
		Document page = follow(result);
		assertThat(page.selectFirst("[role=status]").text()).isEqualTo("Created repository octo-dev/orders-api.");
		assertThat(page.select("a[href=https://github.com/octo-dev/orders-api]")).isNotEmpty();
		assertThat(gitHub.repository("orders-api")).hasValueSatisfying(repository -> assertThat(repository.isPrivate()).isTrue());
	}

	@Test
	void createsAPublicRepository() {
		assertThat(create("visibility", "public")).hasStatus(HttpStatus.SEE_OTHER);

		assertThat(gitHub.repository("orders-api")).hasValueSatisfying(repository -> assertThat(repository.isPrivate()).isFalse());
	}

	@Test
	void showsProgressWithJavaScript() {
		Element form = HtmlPages.parse(mvc.get().uri("/services/orders-api").exchange())
			.selectFirst("#repository-step form");

		assertThat(form.attr("method")).isEqualTo("post");
		assertThat(form.attr("action")).isEqualTo("/services/orders-api/repository");
		assertThat(form.attr("hx-post")).isEqualTo("/services/orders-api/repository");
		assertThat(form.attr("hx-select")).isEqualTo("main");
		assertThat(form.attr("hx-target")).isEqualTo("main");
		assertThat(form.attr("hx-disabled-elt")).isEqualTo("find button, find fieldset");
		assertThat(form.attr("hx-on::before-request")).contains("aria-busy");
		assertThat(form.select("button .when-pending").text()).isEqualTo("Creating repository…");
		assertThat(form.select("p.when-pending").text()).isEqualTo("This takes a few seconds.");
	}

	// Explain why a repository wasn't created

	@Test
	void gitHubRejectsTheToken() {
		gitHub.respondWith(401, """
				{"message": "Bad credentials", "status": "401"}
				""");

		MvcTestResult result = create("visibility", "private");

		assertThat(result).hasStatus(HttpStatus.SEE_OTHER).hasHeader(HttpHeaders.LOCATION, "/services/orders-api");
		assertThat(failure(follow(result))).contains("GitHub rejected the token");
		assertThat(catalog.lookUp("orders-api").repository()).isNull();
	}

	@Test
	void invalidVisibility() {
		MvcTestResult result = create("visibility", "internal");

		assertThat(result).hasStatus(HttpStatus.SEE_OTHER);
		assertThat(failure(follow(result))).contains("Visibility must be private or public");
		assertThat(gitHub.requests()).isEmpty();
	}

	@Test
	void theServiceAlreadyHasARepository() {
		TestCatalog.giveRepository(catalog, "orders-api");

		MvcTestResult result = create("visibility", "private");

		assertThat(result).hasStatus(HttpStatus.SEE_OTHER);
		Document page = follow(result);
		assertThat(failure(page)).contains("'orders-api' already has a repository");
		assertThat(gitHub.requests()).isEmpty();
		assertThat(catalog.lookUp("orders-api").repository().url()).hasToString("https://github.com/octo-dev/orders-api");
		assertThat(page.select("a.repo-link").text()).isEqualTo("octo-dev/orders-api");
	}

	@Test
	void unknownService() {
		MvcTestResult result = mvc.post()
			.uri("/services/payments-api/repository")
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.param("visibility", "private")
			.exchange();

		assertThat(result).hasStatus(HttpStatus.NOT_FOUND).hasContentTypeCompatibleWith(MediaType.TEXT_HTML);
		assertThat(HtmlPages.parse(result).select("h1").text()).isEqualTo("No service named payments-api");
	}

	private MvcTestResult create(String field, String value) {
		return mvc.post()
			.uri("/services/orders-api/repository")
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.param(field, value)
			.exchange();
	}

	/** Requests the page a redirect points to, with its flash attributes, as a browser would. */
	private Document follow(MvcTestResult redirect) {
		MvcTestResult page = mvc.get()
			.uri(redirect.getResponse().getRedirectedUrl())
			.flashAttrs(redirect.getMvcResult().getFlashMap())
			.exchange();
		assertThat(page).hasStatusOk();
		return HtmlPages.parse(page);
	}

	/** The error notice inside the repository step. */
	private static String failure(Document page) {
		Element notice = page.selectFirst("#repository-step [role=alert]");
		assertThat(notice).isNotNull();
		assertThat(notice.text()).startsWith("Couldn't create the repository");
		return notice.text();
	}

}
