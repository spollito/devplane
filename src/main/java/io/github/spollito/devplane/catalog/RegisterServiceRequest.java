package io.github.spollito.devplane.catalog;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /api/v1/services}, and the fields of the web UI's register
 * form, so both apply the same rules and messages. {@code lifecycle} is a string
 * rather than the enum so that an unknown value is reported as a field error, not as
 * an unreadable request body.
 */
public record RegisterServiceRequest(
		@NotNull @Pattern(regexp = CatalogConstraints.IDENTIFIER_PATTERN,
				message = CatalogConstraints.IDENTIFIER_MESSAGE) String name,
		@NotNull @Pattern(regexp = CatalogConstraints.IDENTIFIER_PATTERN,
				message = CatalogConstraints.IDENTIFIER_MESSAGE) String owner,
		@Size(max = CatalogConstraints.MAX_DESCRIPTION_LENGTH,
				message = CatalogConstraints.DESCRIPTION_MESSAGE) String description,
		@Pattern(regexp = Lifecycle.API_VALUE_PATTERN,
				message = CatalogConstraints.LIFECYCLE_MESSAGE) String lifecycle) {

}
