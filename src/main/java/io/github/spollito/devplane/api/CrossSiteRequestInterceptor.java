package io.github.spollito.devplane.api;

import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.WebUtils;

/**
 * Refuses requests that change data when a browser sends them from another site.
 * DevPlane has no login, so without this check any website open in the developer's
 * browser could register services or create repositories with their GitHub token.
 * <p>
 * A browser's {@code Sec-Fetch-Site} header decides first: only {@code same-origin}
 * and {@code none} (typed or bookmarked) pass. {@code same-site} is refused too,
 * because another app on {@code localhost} counts as the same site. Without that
 * header, the {@code Origin} header must match DevPlane's own address. Requests with
 * neither header, such as from curl and scripts, pass.
 */
class CrossSiteRequestInterceptor implements HandlerInterceptor {

	private static final Set<String> CHANGING_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

	private static final Set<String> OWN_SITE = Set.of("same-origin", "none");

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		if (!CHANGING_METHODS.contains(request.getMethod())) {
			return true;
		}
		String fetchSite = request.getHeader("Sec-Fetch-Site");
		boolean fromOwnSite = (fetchSite != null) ? OWN_SITE.contains(fetchSite)
				: WebUtils.isSameOrigin(new ServletServerHttpRequest(request));
		if (!fromOwnSite) {
			throw new CrossSiteRequestException();
		}
		return true;
	}

}
