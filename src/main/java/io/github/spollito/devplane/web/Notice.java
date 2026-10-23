package io.github.spollito.devplane.web;

import java.io.Serializable;

/**
 * A success notice shown after a redirect, such as "Registered {@code orders-api}."
 * The identifier is set in monospace, so it's kept apart from the text around it.
 * It travels in a flash attribute, which lives in the HTTP session.
 */
record Notice(String lead, String id, String trail) implements Serializable {

	static Notice of(String lead, String id) {
		return new Notice(lead + " ", id, ".");
	}

}
