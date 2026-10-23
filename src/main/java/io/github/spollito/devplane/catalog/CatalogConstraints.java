package io.github.spollito.devplane.catalog;

/** Validation rules for the catalog, shared with features that act on catalog services. */
public final class CatalogConstraints {

	/**
	 * Service names and owners are DNS labels that start with a letter, so they stay
	 * valid as GitHub repository names, Kubernetes resource names, and label values.
	 */
	public static final String IDENTIFIER_PATTERN = "[a-z]([-a-z0-9]{0,61}[a-z0-9])?";

	static final String IDENTIFIER_MESSAGE = "must be 1 to 63 lowercase letters, digits, or hyphens, "
			+ "start with a letter, and not end with a hyphen";

	static final String LIFECYCLE_MESSAGE = "must be one of experimental, production, or deprecated";

	public static final int MAX_DESCRIPTION_LENGTH = 300;

	/** Replaces Bean Validation's "size must be between 0 and 300". */
	static final String DESCRIPTION_MESSAGE = "must be at most " + MAX_DESCRIPTION_LENGTH + " characters";

	private CatalogConstraints() {
	}

}
