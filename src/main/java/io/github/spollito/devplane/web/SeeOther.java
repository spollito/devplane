package io.github.spollito.devplane.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.view.RedirectView;

/**
 * The redirect after a form post. Spring's {@code redirect:} prefix sends 302, and
 * 303 See Other is what tells a browser to follow with {@code GET}.
 */
final class SeeOther {

	private SeeOther() {
	}

	static RedirectView to(String path) {
		RedirectView redirect = new RedirectView(path, true);
		redirect.setStatusCode(HttpStatus.SEE_OTHER);
		redirect.setExposeModelAttributes(false);
		return redirect;
	}

}
