package io.github.spollito.devplane.catalog;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/**
 * Rendered as a 404 problem details response by the API's exception handler, and as
 * the unknown-service page by the web UI's.
 */
public class ServiceNotFoundException extends ErrorResponseException {

	private final String name;

	ServiceNotFoundException(String name) {
		super(HttpStatus.NOT_FOUND,
				ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "No service named '" + name + "' is registered."),
				null);
		this.name = name;
	}

	/** The name that no service has. */
	public String getName() {
		return name;
	}

}
