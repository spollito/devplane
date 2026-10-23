package io.github.spollito.devplane.web;

import io.github.spollito.devplane.catalog.ServiceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

/**
 * Renders errors in the web UI's controllers, and in the request check before them,
 * as HTML pages. It applies only to controllers in this package, and runs before
 * the API's handler, so pages never answer with problem details.
 */
@ControllerAdvice(basePackageClasses = PageExceptionHandler.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
class PageExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(PageExceptionHandler.class);

	/** DevPlane writes the details of its own errors, so only those are safe to show. */
	private static final String DEVPLANE_PACKAGE = "io.github.spollito.devplane.";

	@ExceptionHandler(Exception.class)
	ModelAndView handle(Exception ex) {
		if (ex instanceof ServiceNotFoundException notFound) {
			ModelAndView page = new ModelAndView("error/4xx", HttpStatus.NOT_FOUND);
			page.addObject("status", HttpStatus.NOT_FOUND.value());
			page.addObject("missingService", notFound.getName());
			return page;
		}
		if (ex instanceof ErrorResponse error && error.getStatusCode().is4xxClientError()) {
			HttpStatusCode status = error.getStatusCode();
			ModelAndView page = new ModelAndView("error/4xx", status);
			page.addObject("status", status.value());
			if (ex.getClass().getName().startsWith(DEVPLANE_PACKAGE)) {
				page.addObject("detail", error.getBody().getDetail());
			}
			return page;
		}
		log.error("Unexpected error while showing a page", ex);
		return new ModelAndView("error/5xx", HttpStatus.INTERNAL_SERVER_ERROR);
	}

}
