package io.github.spollito.devplane.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import io.github.spollito.devplane.IntegrationTest;
import io.github.spollito.devplane.MutableClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Scenarios from the service-catalog spec's "Register a service", "Service names
 * and owners are DNS-safe identifiers", and "Services have a lifecycle stage"
 * requirements.
 */
@IntegrationTest
class RegisterServiceTests {

	private static final Instant REGISTRATION_TIME = Instant.parse("2026-03-01T09:30:00Z");

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private ServiceRepository repository;

	@Autowired
	private MutableClock clock;

	@BeforeEach
	void emptyCatalog() {
		repository.deleteAll();
		clock.setInstant(REGISTRATION_TIME);
	}

	// Register a service

	@Test
	void registersANewService() {
		MvcTestResult result = register("""
				{"name": "orders-api", "owner": "payments-team", "description": "Order management REST API"}
				""");

		assertThat(result).hasStatus(HttpStatus.CREATED);
		assertThat(result.getResponse().getHeader("Location")).endsWith("/api/v1/services/orders-api");
		assertThat(result).bodyJson().isLenientlyEqualTo("""
				{
				  "name": "orders-api",
				  "owner": "payments-team",
				  "description": "Order management REST API",
				  "lifecycle": "experimental",
				  "createdAt": "2026-03-01T09:30:00Z",
				  "updatedAt": "2026-03-01T09:30:00Z"
				}
				""");
	}

	@Test
	void rejectsADuplicateName() {
		register("""
				{"name": "orders-api", "owner": "payments-team"}
				""");

		MvcTestResult result = register("""
				{"name": "orders-api", "owner": "checkout-team"}
				""");

		assertThat(result).hasStatus(HttpStatus.CONFLICT).hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
		assertThat(repository.findByName("orders-api")).get()
			.extracting(ServiceEntity::getOwner)
			.isEqualTo("payments-team");
	}

	@Test
	void rejectsMissingRequiredFields() {
		MvcTestResult result = register("""
				{"description": "No name or owner"}
				""");

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.errors[*].field").asArray().containsExactly("name", "owner");
		assertThat(repository.count()).isZero();
	}

	@Test
	void rejectsAnOverlongDescription() {
		String description = "x".repeat(301);

		MvcTestResult result = register("""
				{"name": "orders-api", "owner": "payments-team", "description": "%s"}
				""".formatted(description));

		assertInvalidField(result, "description");
		assertThat(result).bodyJson()
			.extractingPath("$.errors[0].message")
			.isEqualTo("must be at most 300 characters");
	}

	// Service names and owners are DNS-safe identifiers

	@ParameterizedTest
	@ValueSource(strings = { "orders-api-2", "a",
			"a23456789-123456789-123456789-123456789-123456789-123456789-123" })
	void acceptsAValidIdentifier(String name) {
		assertThat(register("""
				{"name": "%s", "owner": "payments-team"}
				""".formatted(name))).hasStatus(HttpStatus.CREATED);
	}

	@Test
	void rejectsUppercaseLettersAndSpaces() {
		assertInvalidField(register("""
				{"name": "Orders API", "owner": "payments-team"}
				"""), "name");
	}

	@Test
	void rejectsANameLongerThan63Characters() {
		String name = "a".repeat(64);

		assertInvalidField(register("""
				{"name": "%s", "owner": "payments-team"}
				""".formatted(name)), "name");
	}

	@ParameterizedTest
	@ValueSource(strings = { "2-orders", "orders-" })
	void rejectsALeadingDigitOrATrailingHyphen(String name) {
		assertInvalidField(register("""
				{"name": "%s", "owner": "payments-team"}
				""".formatted(name)), "name");
	}

	@Test
	void appliesTheSameRulesToOwners() {
		assertInvalidField(register("""
				{"name": "orders-api", "owner": "Payments Team"}
				"""), "owner");
	}

	// Services have a lifecycle stage

	@Test
	void defaultsToExperimental() {
		assertThat(register("""
				{"name": "orders-api", "owner": "payments-team"}
				""")).bodyJson().extractingPath("$.lifecycle").isEqualTo("experimental");
	}

	@Test
	void acceptsAnExplicitStage() {
		assertThat(register("""
				{"name": "orders-api", "owner": "payments-team", "lifecycle": "production"}
				""")).bodyJson().extractingPath("$.lifecycle").isEqualTo("production");
	}

	@Test
	void rejectsAnUnknownStage() {
		assertInvalidField(register("""
				{"name": "orders-api", "owner": "payments-team", "lifecycle": "beta"}
				"""), "lifecycle");
	}

	private MvcTestResult register(String json) {
		return mvc.post().uri("/api/v1/services").contentType(MediaType.APPLICATION_JSON).content(json).exchange();
	}

	private void assertInvalidField(MvcTestResult result, String field) {
		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.errors[*].field").asArray().containsExactly(field);
		assertThat(repository.count()).isZero();
	}

}
