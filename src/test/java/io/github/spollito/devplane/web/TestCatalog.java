package io.github.spollito.devplane.web;

import java.net.URI;

import io.github.spollito.devplane.catalog.Lifecycle;
import io.github.spollito.devplane.catalog.RegisteredService;
import io.github.spollito.devplane.catalog.ServiceCatalog;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.PageRequest;

/** Sets up the catalog for page tests, through the catalog's public operations. */
final class TestCatalog {

	private TestCatalog() {
	}

	static void clear(ServiceCatalog catalog) {
		for (RegisteredService service : catalog.list(null, PageRequest.of(0, 1000))) {
			catalog.unregister(service.name());
		}
	}

	static void register(ServiceCatalog catalog, String name, String owner, @Nullable String description) {
		catalog.register(name, owner, description, Lifecycle.EXPERIMENTAL);
	}

	static void giveRepository(ServiceCatalog catalog, String name) {
		catalog.recordRepository(name, URI.create("https://github.com/octo-dev/" + name), "java-rest-api");
	}

}
