package io.github.spollito.devplane.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/**
 * Rendered as a 403 problem details response under {@code /api/}, and as the HTML
 * 403 page for pages.
 */
class CrossSiteRequestException extends ErrorResponseException {

	CrossSiteRequestException() {
		super(HttpStatus.FORBIDDEN, ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,
				"DevPlane accepts changes only from its own pages and from clients that aren't web browsers."),
				null);
	}

}
