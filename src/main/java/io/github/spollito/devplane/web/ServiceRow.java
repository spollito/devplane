package io.github.spollito.devplane.web;

import io.github.spollito.devplane.catalog.RegisteredService;
import org.jspecify.annotations.Nullable;

/**
 * One row of the catalog table, with display-ready values.
 *
 * @param ownerUrl the catalog filtered to this owner
 * @param description the description, or {@code null} for "No description"
 * @param stage the lifecycle stage's API name, such as "experimental"
 * @param repository the repository, or {@code null} for "None"
 * @param updated when the service was last updated
 */
record ServiceRow(String name, String owner, String ownerUrl, @Nullable String description, String stage,
		@Nullable RepositoryLink repository, PageTime updated) {

	static ServiceRow of(RegisteredService service, PageTimes times) {
		return new ServiceRow(service.name(), service.owner(), CatalogPages.ownerUrl(service.owner()),
				service.description(), service.lifecycle().apiValue(), RepositoryLink.of(service.repository()),
				PageTime.of(service.updatedAt(), times));
	}

}
