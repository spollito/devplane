package io.github.spollito.devplane.catalog;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code PUT /api/v1/services/{name}}: the full set of editable fields.
 * An omitted description clears it.
 */
record UpdateServiceRequest(
		@NotNull @Pattern(regexp = CatalogConstraints.IDENTIFIER_PATTERN,
				message = CatalogConstraints.IDENTIFIER_MESSAGE) String owner,
		@Size(max = CatalogConstraints.MAX_DESCRIPTION_LENGTH,
				message = CatalogConstraints.DESCRIPTION_MESSAGE) String description,
		@NotNull @Pattern(regexp = Lifecycle.API_VALUE_PATTERN,
				message = CatalogConstraints.LIFECYCLE_MESSAGE) String lifecycle) {

}
