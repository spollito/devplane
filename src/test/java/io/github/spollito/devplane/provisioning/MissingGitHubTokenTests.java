package io.github.spollito.devplane.provisioning;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import io.github.spollito.devplane.NoGitHubTokenTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * The "Provisioning needs a GitHub token" requirement. DevPlane runs here as it does
 * without {@code DEVPLANE_GITHUB_TOKEN}, so this test uses that context instead of
 * the shared one with the fake GitHub.
 */
@NoGitHubTokenTest
class MissingGitHubTokenTests {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private GitHubProperties properties;

	@BeforeEach
	void removeOrdersApi() {
		mvc.delete().uri("/api/v1/services/orders-api").exchange();
	}

	@Test
	void reportsAMissingToken() {
		assertThat(mvc.post().uri("/api/v1/services").contentType(MediaType.APPLICATION_JSON).content("""
				{"name": "orders-api", "owner": "payments-team"}
				""")).hasStatus(HttpStatus.CREATED);

		assertThat(mvc.post().uri("/api/v1/services/orders-api/repository"))
			.hasStatus(HttpStatus.SERVICE_UNAVAILABLE)
			.hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
			.bodyJson()
			.extractingPath("$.detail")
			.asString()
			.contains("DEVPLANE_GITHUB_TOKEN");
		assertThat(mvc.get().uri("/api/v1/services/orders-api")).hasStatusOk()
			.bodyJson()
			.extractingPath("$.repository")
			.isNull();
	}

	@Test
	void defaultsToGitHubsApi() {
		assertThat(properties.apiUrl()).isEqualTo(URI.create("https://api.github.com"));
	}

}
