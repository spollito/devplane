package io.github.spollito.devplane.api;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.spollito.devplane.IntegrationTest;
import io.github.spollito.devplane.provisioning.FakeGitHub;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Scenarios from the platform-api spec's "Requests from other sites can't change
 * anything" requirement.
 */
@IntegrationTest
class CrossSiteRequestTests {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private FakeGitHub gitHub;

	@BeforeEach
	void registerOrdersApi() {
		gitHub.reset();
		for (String name : new String[] { "orders-api", "payments-api", "auth-service" }) {
			mvc.delete().uri("/api/v1/services/{name}", name).exchange();
		}
		assertThat(mvc.post().uri("/api/v1/services").contentType(MediaType.APPLICATION_JSON).content("""
				{"name": "orders-api", "owner": "payments-team"}
				""")).hasStatus(HttpStatus.CREATED);
	}

	@Test
	void rejectsARequestFromAnotherSite() {
		assertThat(mvc.post().uri("/api/v1/services/orders-api/repository").header("Sec-Fetch-Site", "cross-site"))
			.hasStatus(HttpStatus.FORBIDDEN)
			.hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
			.bodyJson()
			.extractingPath("$.status")
			.isEqualTo(403);
		assertThat(gitHub.requests()).isEmpty();
		assertThat(mvc.get().uri("/api/v1/services/orders-api")).bodyJson().extractingPath("$.repository").isNull();
	}

	@Test
	void rejectsAnotherSitesOrigin() {
		assertThat(mvc.post()
			.uri("/api/v1/services")
			.header(HttpHeaders.ORIGIN, "https://example.com")
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"name": "payments-api", "owner": "payments-team"}
					""")).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(mvc.get().uri("/api/v1/services/payments-api")).hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void acceptsDevPlanesOwnPagesAndScripts() {
		assertThat(mvc.post()
			.uri("/api/v1/services")
			.header("Sec-Fetch-Site", "same-origin")
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"name": "payments-api", "owner": "payments-team"}
					""")).hasStatus(HttpStatus.CREATED);
		assertThat(mvc.post().uri("/api/v1/services").contentType(MediaType.APPLICATION_JSON).content("""
				{"name": "auth-service", "owner": "identity-team"}
				""")).hasStatus(HttpStatus.CREATED);
	}

	@Test
	void readingIsUnaffected() {
		assertThat(mvc.get().uri("/api/v1/services/orders-api").header("Sec-Fetch-Site", "cross-site"))
			.hasStatusOk();
	}

}
