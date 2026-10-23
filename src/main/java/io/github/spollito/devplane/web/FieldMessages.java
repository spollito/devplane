package io.github.spollito.devplane.web;

import java.util.Map;

import org.springframework.validation.FieldError;

/**
 * Turns a field's validation error into the sentence a page shows under the field.
 * The constraint messages are the API's own, so pages and the API say the same
 * thing: "must not be null" becomes an instruction, and any other message follows
 * the field's label.
 */
final class FieldMessages {

	private static final Map<String, String> LABELS = Map.of("name", "Name", "owner", "Owner", "description",
			"Description", "lifecycle", "Lifecycle", "visibility", "Visibility", "template", "Template");

	private static final Map<String, String> MISSING = Map.of("name", "Enter a name for the service", "owner",
			"Enter the team that owns the service");

	private FieldMessages() {
	}

	/** For example "Enter a name for the service", or "Name must be 1 to 63 lowercase letters…". */
	static String of(FieldError error) {
		String field = error.getField();
		if ("NotNull".equals(error.getCode())) {
			String missing = MISSING.get(field);
			if (missing != null) {
				return missing;
			}
			return "Enter the " + label(field).toLowerCase();
		}
		return label(field) + " " + error.getDefaultMessage();
	}

	private static String label(String field) {
		return LABELS.getOrDefault(field, field);
	}

}
