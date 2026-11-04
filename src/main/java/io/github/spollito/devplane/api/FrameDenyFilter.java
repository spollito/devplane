package io.github.spollito.devplane.api;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Stops other sites from showing DevPlane in a frame. A form inside a framed page is
 * {@code same-origin} to the browser, so {@link CrossSiteRequestInterceptor} can't tell
 * that another site tricked the developer into clicking it.
 * <p>
 * It's a filter so that it covers everything DevPlane serves, including Swagger UI and
 * error pages. {@code frame-ancestors} is the current standard, and
 * {@code X-Frame-Options} covers browsers that don't support it.
 */
@Component
class FrameDenyFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		response.setHeader("X-Frame-Options", "DENY");
		response.setHeader("Content-Security-Policy", "frame-ancestors 'none'");
		chain.doFilter(request, response);
	}

}
