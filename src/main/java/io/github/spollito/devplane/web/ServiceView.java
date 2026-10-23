package io.github.spollito.devplane.web;

import io.github.spollito.devplane.catalog.RegisteredService;
import org.jspecify.annotations.Nullable;

/**
 * A service as its page shows it, with display-ready values.
 *
 * @param description the description, or {@code null} for "No description"
 * @param stage the lifecycle stage's API name, such as "experimental"
 * @param repository the repository, or {@code null} until one is created
 * @param repositoryCreated when the repository was created, or {@code null}
 */
record ServiceView(String name, String owner, String ownerUrl, @Nullable String description, String stage,
		PageTime registered, PageTime updated, @Nullable RepositoryLink repository,
		@Nullable PageTime repositoryCreated) {

	static ServiceView of(RegisteredService service, PageTimes times) {
		return new ServiceView(service.name(), service.owner(), CatalogPages.ownerUrl(service.owner()),
				service.description(), service.lifecycle().apiValue(), PageTime.of(service.createdAt(), times),
				PageTime.of(service.updatedAt(), times), RepositoryLink.of(service.repository()),
				(service.repository() != null) ? PageTime.of(service.repository().createdAt(), times) : null);
	}

}
