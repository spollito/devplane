package io.github.spollito.devplane.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Instant;
import java.util.stream.IntStream;

import io.github.spollito.devplane.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Scenarios from the service-catalog spec's "View a service", "List services", and
 * "Filter services by owner" requirements.
 */
@IntegrationTest
class ViewAndListServicesTests {

	private static final Instant REGISTERED_AT = Instant.parse("2026-02-10T08:15:00Z");

	private static final Instant PROVISIONED_AT = Instant.parse("2026-02-11T09:30:00.123456Z");

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private ServiceRepository repository;

	@BeforeEach
	void clearCatalog() {
		repository.deleteAll();
	}

	// View a service

	@Test
	void returnsARegisteredService() {
		repository.save(new ServiceEntity("orders-api", "payments-team", "Order management REST API",
				Lifecycle.PRODUCTION, REGISTERED_AT));

		assertThat(mvc.get().uri("/api/v1/services/orders-api")).hasStatusOk().bodyJson().isLenientlyEqualTo("""
				{
				  "name": "orders-api",
				  "owner": "payments-team",
				  "description": "Order management REST API",
				  "lifecycle": "production",
				  "createdAt": "2026-02-10T08:15:00Z",
				  "updatedAt": "2026-02-10T08:15:00Z",
				  "repository": null
				}
				""");
	}

	@Test
	void showsAProvisionedRepository() {
		ServiceEntity service = new ServiceEntity("orders-api", "payments-team", null, Lifecycle.EXPERIMENTAL,
				REGISTERED_AT);
		service.recordRepository(URI.create("https://github.com/octo-dev/orders-api"), "java-rest-api",
				PROVISIONED_AT);
		repository.save(service);
		String repositoryJson = """
				{
				  "url": "https://github.com/octo-dev/orders-api",
				  "template": "java-rest-api",
				  "createdAt": "2026-02-11T09:30:00.123456Z"
				}
				""";

		assertThat(mvc.get().uri("/api/v1/services/orders-api")).hasStatusOk().bodyJson().isLenientlyEqualTo("""
				{"repository": %s}
				""".formatted(repositoryJson));
		assertThat(mvc.get().uri("/api/v1/services")).hasStatusOk().bodyJson().isLenientlyEqualTo("""
				{"items": [{"name": "orders-api", "repository": %s}]}
				""".formatted(repositoryJson));
	}

	@Test
	void returns404ForAnUnknownService() {
		assertThat(mvc.get().uri("/api/v1/services/unknown-service")).hasStatus(HttpStatus.NOT_FOUND)
			.hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
	}

	// List services

	@Test
	void listsServicesAlphabetically() {
		register("payments-api", "payments-team");
		register("auth-service", "identity-team");
		register("orders-api", "payments-team");

		MvcTestResult result = mvc.get().uri("/api/v1/services").exchange();

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson()
			.extractingPath("$.items[*].name")
			.asArray()
			.containsExactly("auth-service", "orders-api", "payments-api");
	}

	@Test
	void emptyCatalog() {
		assertThat(mvc.get().uri("/api/v1/services")).hasStatusOk().bodyJson().isLenientlyEqualTo("""
				{"items": [], "totalItems": 0}
				""");
	}

	@Test
	void returnsTheRequestedPage() {
		registerNumbered(25);

		MvcTestResult result = mvc.get().uri("/api/v1/services?page=1&size=10").exchange();

		assertThat(result).bodyJson()
			.extractingPath("$.items[*].name")
			.asArray()
			.containsExactlyElementsOf(IntStream.rangeClosed(11, 20).mapToObj(ViewAndListServicesTests::numbered).toList());
		assertThat(result).bodyJson().isLenientlyEqualTo("""
				{"page": 1, "size": 10, "totalItems": 25, "totalPages": 3}
				""");
	}

	@Test
	void capsThePageSize() {
		registerNumbered(150);

		MvcTestResult result = mvc.get().uri("/api/v1/services?size=500").exchange();

		assertThat(result).bodyJson().extractingPath("$.items").asArray().hasSize(100);
		assertThat(result).bodyJson().extractingPath("$.size").isEqualTo(100);
	}

	@Test
	void pagePastTheLast() {
		registerNumbered(3);

		assertThat(mvc.get().uri("/api/v1/services?page=1")).hasStatusOk().bodyJson().isLenientlyEqualTo("""
				{"items": [], "totalItems": 3, "totalPages": 1}
				""");
	}

	@Test
	void pageNumberTooLargeToReach() {
		register("orders-api", "payments-team");
		register("payments-api", "payments-team");
		register("auth-service", "identity-team");

		assertThat(mvc.get().uri("/api/v1/services?page=2147483647")).hasStatusOk()
			.bodyJson()
			.isLenientlyEqualTo("""
					{"items": [], "page": 2147483647, "totalItems": 3, "totalPages": 1}
					""");
		assertThat(mvc.get().uri("/api/v1/services?owner=payments-team&page=2147483647")).hasStatusOk()
			.bodyJson()
			.isLenientlyEqualTo("""
					{"items": [], "page": 2147483647, "totalItems": 2, "totalPages": 1}
					""");
	}

	// Filter services by owner

	@Test
	void listsOnlyTheOwnersServices() {
		register("orders-api", "payments-team");
		register("payments-api", "payments-team");
		register("auth-service", "identity-team");

		assertThat(mvc.get().uri("/api/v1/services?owner=payments-team")).bodyJson()
			.extractingPath("$.items[*].name")
			.asArray()
			.containsExactly("orders-api", "payments-api");
	}

	@Test
	void ownerWithNoServices() {
		register("orders-api", "payments-team");

		assertThat(mvc.get().uri("/api/v1/services?owner=search-team")).hasStatusOk()
			.bodyJson()
			.extractingPath("$.items")
			.asArray()
			.isEmpty();
	}

	private void register(String name, String owner) {
		repository.save(new ServiceEntity(name, owner, null, Lifecycle.EXPERIMENTAL, REGISTERED_AT));
	}

	/** Registers {@code service-001} through {@code service-<count>}, which sort in numeric order. */
	private void registerNumbered(int count) {
		repository.saveAll(IntStream.rangeClosed(1, count)
			.mapToObj(i -> new ServiceEntity(numbered(i), "platform-team", null, Lifecycle.EXPERIMENTAL, REGISTERED_AT))
			.toList());
	}

	private static String numbered(int i) {
		return "service-%03d".formatted(i);
	}

}
