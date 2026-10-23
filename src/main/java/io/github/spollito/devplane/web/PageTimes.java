package io.github.spollito.devplane.web;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import org.springframework.stereotype.Component;

/**
 * Formats times for pages. Exact times use the JVM's time zone, which on the
 * developer's laptop is their own. Relative times count from the application's
 * {@link Clock}, so tests control them.
 */
@Component
class PageTimes {

	private static final DateTimeFormatter EXACT = DateTimeFormatter.ofPattern("MMM d, uuuu, HH:mm", Locale.ENGLISH);

	private final Clock clock;

	private final ZoneId zone = ZoneId.systemDefault();

	PageTimes(Clock clock) {
		this.clock = clock;
	}

	/** For example "Oct 21, 2026, 16:40". */
	String exact(Instant time) {
		return EXACT.format(time.atZone(zone));
	}

	/**
	 * For example "just now", "5 minutes ago", "3 hours ago" (earlier the same day),
	 * "Yesterday", "4 days ago", "2 weeks ago", "3 months ago", or "1 year ago".
	 */
	String relative(Instant time) {
		Instant now = clock.instant();
		long minutes = Duration.between(time, now).toMinutes();
		if (minutes < 1) {
			return "just now";
		}
		if (minutes < 60) {
			return ago(minutes, "minute");
		}
		long days = ChronoUnit.DAYS.between(time.atZone(zone).toLocalDate(), LocalDate.ofInstant(now, zone));
		if (days < 1) {
			return ago(minutes / 60, "hour");
		}
		if (days == 1) {
			return "Yesterday";
		}
		if (days < 7) {
			return ago(days, "day");
		}
		if (days < 30) {
			return ago(days / 7, "week");
		}
		if (days < 365) {
			return ago(days / 30, "month");
		}
		return ago(days / 365, "year");
	}

	private static String ago(long count, String unit) {
		return count + " " + unit + ((count == 1) ? "" : "s") + " ago";
	}

}
