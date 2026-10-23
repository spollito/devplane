package io.github.spollito.devplane.web;

import io.github.spollito.devplane.catalog.SourceRepository;
import org.jspecify.annotations.Nullable;

/**
 * A service's GitHub repository as pages show it.
 *
 * @param fullName the repository as {@code owner/name}, such as {@code octo-dev/orders-api}
 * @param url its page on GitHub
 * @param hostAndPath its address without the scheme, such as {@code github.com/octo-dev/orders-api}
 */
record RepositoryLink(String fullName, String url, String hostAndPath, String template) {

	static @Nullable RepositoryLink of(@Nullable SourceRepository repository) {
		if (repository == null) {
			return null;
		}
		String path = repository.url().getPath();
		return new RepositoryLink(path.substring(1), repository.url().toString(), repository.url().getHost() + path,
				repository.template());
	}

}
