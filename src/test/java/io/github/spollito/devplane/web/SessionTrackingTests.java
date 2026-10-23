package io.github.spollito.devplane.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.spollito.devplane.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.servlet.Session.SessionTrackingMode;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;

/**
 * Pages keep flash messages in the session. Without a cookie yet, Tomcat would
 * put the session ID in the redirect URL, as in
 * {@code /services/orders-api;jsessionid=...}. MockMvc doesn't rewrite URLs, so
 * this checks the setting that prevents it.
 */
@IntegrationTest
class SessionTrackingTests {

	@Autowired
	private ServerProperties server;

	@Test
	void tracksSessionsWithACookieOnly() {
		assertThat(server.getServlet().getSession().getTrackingModes()).containsExactly(SessionTrackingMode.COOKIE);
	}

}
