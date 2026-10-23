package io.github.spollito.devplane.web;

import java.time.Instant;

/**
 * A time as a page shows it, in a {@code <time>} element.
 *
 * @param iso the {@code datetime} attribute
 * @param exact for example "Oct 21, 2026, 16:40"
 * @param relative for example "Yesterday"
 */
record PageTime(String iso, String exact, String relative) {

	static PageTime of(Instant time, PageTimes times) {
		return new PageTime(time.toString(), times.exact(time), times.relative(time));
	}

}
