package io.github.spollito.devplane.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.spollito.devplane.NoGitHubTokenTest;
import io.github.spollito.devplane.catalog.ServiceCatalog;
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
 * The "No GitHub token" scenario of the web-ui spec's "Explain why a repository
 * wasn't created" requirement, in the context that runs without a token.
 */
@NoGitHubTokenTest
class NoGitHubTokenPageTests {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private ServiceCatalog catalog;

	@BeforeEach
	void registerOrdersApi() {
		TestCatalog.clear(catalog);
		TestCatalog.register(catalog, "orders-api", "payments-team", null);
	}

	@Test
	void noGitHubToken() {
		MvcTestResult result = mvc.post()
			.uri("/services/orders-api/repository")
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.param("visibility", "private")
			.exchange();

		assertThat(result).hasStatus(HttpStatus.SEE_OTHER).hasHeader(HttpHeaders.LOCATION, "/services/orders-api");
		MvcTestResult page = mvc.get().uri("/services/orders-api").flashAttrs(result.getMvcResult().getFlashMap()).exchange();
		Element notice = HtmlPages.parse(page).selectFirst("#repository-step [role=alert]");
		assertThat(notice.text()).startsWith("Couldn't create the repository").contains("DEVPLANE_GITHUB_TOKEN");
		assertThat(notice.selectFirst("a:contains(How to create the token)").attr("href"))
			.isEqualTo("https://github.com/spollito/devplane#push-it-to-github");
		assertThat(catalog.lookUp("orders-api").repository()).isNull();
	}

}
