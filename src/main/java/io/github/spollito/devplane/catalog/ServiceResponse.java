package io.github.spollito.devplane.catalog;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

/**
 * A service as the Platform API returns it. {@code repository} is {@code null}
 * until the service's repository is provisioned.
 */
record ServiceResponse(String name, String owner, String description, String lifecycle, Instant createdAt,
		Instant updatedAt, @Nullable SourceRepository repository) {

	static ServiceResponse from(RegisteredService service) {
		return new ServiceResponse(service.name(), service.owner(), service.description(),
				service.lifecycle().apiValue(), service.createdAt(), service.updatedAt(), service.repository());
	}

}
