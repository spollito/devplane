package io.github.spollito.devplane.catalog;

import java.util.Locale;

/** Where a service is in its lifecycle. The API uses the lowercase names. */
public enum Lifecycle {

	EXPERIMENTAL, PRODUCTION, DEPRECATED;

	/** Matches exactly the values {@link #fromApiValue} accepts. */
	static final String API_VALUE_PATTERN = "experimental|production|deprecated";

	/** The stage's name in the API and on pages: "experimental", "production", or "deprecated". */
	public String apiValue() {
		return name().toLowerCase(Locale.ROOT);
	}

	/**
	 * The stage with this API name.
	 * @throws IllegalArgumentException if no stage has this name, which validation
	 * against {@link #API_VALUE_PATTERN} rules out
	 */
	public static Lifecycle fromApiValue(String value) {
		return valueOf(value.toUpperCase(Locale.ROOT));
	}

}
