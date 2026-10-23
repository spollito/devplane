package io.github.spollito.devplane.api;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.Comparator;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Renders every Platform API error as an RFC 9457 problem details document.
 * <p>
 * The base class already covers Spring MVC's own exceptions (unknown paths,
 * unreadable bodies, unsupported methods, and so on). This class adds the
 * {@code errors} array for validation failures and a fallback for unexpected
 * exceptions that never exposes their details to the client.
 * <p>
 * Errors outside {@code /api/}, such as an unknown path, are handed to the servlet
 * container's error page with {@code sendError}, so a browser gets Spring Boot's
 * HTML error page instead. Errors in the web UI's controllers never reach this
 * class: {@code web.PageExceptionHandler} handles them first.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	/**
	 * RFC 9457's default problem type. Spring omits {@code type} unless it is set,
	 * but the API always includes it so clients see the same fields on every error.
	 */
	private static final URI DEFAULT_TYPE = URI.create("about:blank");

	@Override
	protected @Nullable ResponseEntity<Object> handleExceptionInternal(Exception ex, @Nullable Object body,
			HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
		if (request instanceof ServletWebRequest servletRequest && !isApiPath(servletRequest.getRequest())) {
			sendError(servletRequest.getResponse(), statusCode.value());
			return null;
		}
		return super.handleExceptionInternal(ex, body, headers, statusCode, request);
	}

	@Override
	protected ResponseEntity<Object> createResponseEntity(@Nullable Object body, HttpHeaders headers,
			HttpStatusCode statusCode, WebRequest request) {
		if (body instanceof ProblemDetail problem) {
			withDefaultType(problem);
		}
		return super.createResponseEntity(body, headers, statusCode, request);
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<InvalidField> errors = ex.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(error -> new InvalidField(error.getField(), error.getDefaultMessage()))
			.sorted(Comparator.comparing(InvalidField::field))
			.toList();
		ProblemDetail body = ex.getBody();
		body.setProperty("errors", errors);
		return handleExceptionInternal(ex, body, headers, status, request);
	}

	@ExceptionHandler(Exception.class)
	@Nullable ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request, HttpServletResponse response) {
		log.error("Unexpected error while handling a request", ex);
		if (!isApiPath(request)) {
			sendError(response, HttpStatus.INTERNAL_SERVER_ERROR.value());
			return null;
		}
		return withDefaultType(
				ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred."));
	}

	private static boolean isApiPath(@Nullable HttpServletRequest request) {
		return request != null && request.getRequestURI().startsWith(request.getContextPath() + "/api/");
	}

	private static void sendError(@Nullable HttpServletResponse response, int status) {
		if (response == null || response.isCommitted()) {
			return;
		}
		try {
			response.sendError(status);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private static ProblemDetail withDefaultType(ProblemDetail problem) {
		if (problem.getType() == null) {
			problem.setType(DEFAULT_TYPE);
		}
		return problem;
	}

	/** One entry in the {@code errors} array of a validation failure. */
	public record InvalidField(String field, String message) {
	}

}
