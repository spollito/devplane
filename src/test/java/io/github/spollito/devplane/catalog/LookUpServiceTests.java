package io.github.spollito.devplane.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.time.Instant;

import io.github.spollito.devplane.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Checks the lookup that other features use to read a registered service. */
@IntegrationTest
class LookUpServiceTests {

	@Autowired
	private ServiceCatalog catalog;

	@Autowired
	private ServiceRepository repository;

	@BeforeEach
	void registerOrdersApi() {
		repository.deleteAll();
		repository.save(new ServiceEntity("orders-api", "payments-team", "Order management REST API",
				Lifecycle.PRODUCTION, Instant.parse("2026-02-10T08:15:00Z")));
	}

	@Test
	void looksUpARegisteredService() {
		assertThat(catalog.lookUp("orders-api"))
			.isEqualTo(new RegisteredService("orders-api", "payments-team", "Order management REST API",
					Lifecycle.PRODUCTION, Instant.parse("2026-02-10T08:15:00Z"), Instant.parse("2026-02-10T08:15:00Z"),
					null));
	}

	@Test
	void rejectsAnUnknownService() {
		assertThatExceptionOfType(ServiceNotFoundException.class).isThrownBy(() -> catalog.lookUp("unknown-service"));
	}

}
