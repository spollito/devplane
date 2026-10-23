package io.github.spollito.devplane.catalog;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/**
 * Rendered as a 409 problem details response by the API's exception handler. The
 * web UI's register form shows it under the name field.
 */
public class DuplicateServiceNameException extends ErrorResponseException {

	DuplicateServiceNameException(String name, Throwable cause) {
		super(HttpStatus.CONFLICT,
				ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "A service named '" + name + "' already exists."),
				cause);
	}

}
