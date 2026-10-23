package io.github.spollito.devplane.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

/** Checks how pages show times. Gradle's test tasks run the JVM in UTC. */
class PageTimesTests {

	private static final Instant NOW = Instant.parse("2026-10-22T14:30:00Z");

	private final PageTimes times = new PageTimes(Clock.fixed(NOW, ZoneOffset.UTC));

	@Test
	void formatsExactTimes() {
		assertThat(times.exact(Instant.parse("2026-10-21T16:40:59Z"))).isEqualTo("Oct 21, 2026, 16:40");
		assertThat(times.exact(Instant.parse("2026-03-02T09:05:00Z"))).isEqualTo("Mar 2, 2026, 09:05");
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', textBlock = """
			2026-10-22T14:30:00Z | just now
			2026-10-22T14:29:30Z | just now
			2026-10-22T14:29:00Z | 1 minute ago
			2026-10-22T13:31:00Z | 59 minutes ago
			2026-10-22T13:30:00Z | 1 hour ago
			2026-10-22T00:00:00Z | 14 hours ago
			2026-10-21T23:59:00Z | Yesterday
			2026-10-21T00:00:00Z | Yesterday
			2026-10-20T23:59:00Z | 2 days ago
			2026-10-16T12:00:00Z | 6 days ago
			2026-10-15T12:00:00Z | 1 week ago
			2026-09-23T12:00:00Z | 4 weeks ago
			2026-09-22T12:00:00Z | 1 month ago
			2026-06-30T11:00:00Z | 3 months ago
			2025-10-23T12:00:00Z | 12 months ago
			2025-10-22T12:00:00Z | 1 year ago
			2023-01-01T00:00:00Z | 3 years ago
			""")
	void formatsRelativeTimes(Instant time, String expected) {
		assertThat(times.relative(time)).isEqualTo(expected);
	}

	@Test
	void showsMinutesAcrossMidnight() {
		PageTimes justAfterMidnight = new PageTimes(Clock.fixed(Instant.parse("2026-10-22T00:20:00Z"), ZoneOffset.UTC));

		assertThat(justAfterMidnight.relative(Instant.parse("2026-10-21T23:50:00Z"))).isEqualTo("30 minutes ago");
	}

}
