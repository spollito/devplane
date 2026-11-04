package io.github.spollito.devplane.site;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

/**
 * The public-site spec's "The roadmap matches the README" requirement. A roadmap item
 * that's ticked in the README must be marked as built on the splash page, and the other
 * way around, in the same order and wording.
 */
class SiteRoadmapTests {

	private static final Pattern ROADMAP_ITEM = Pattern.compile("(?m)^- \\[( |x)] (.+)$");

	@Test
	void theSplashPagesRoadmapMatchesTheReadme() throws IOException {
		assertThat(splashPageItems()).as("the roadmap in site/index.html").containsExactlyElementsOf(readmeItems());
	}

	private static List<String> readmeItems() throws IOException {
		String readme = Files.readString(Path.of("README.md"));
		String roadmap = readme.substring(readme.indexOf("## Project Roadmap"), readme.indexOf("## Architecture"));
		return ROADMAP_ITEM.matcher(roadmap)
			.results()
			.map(item -> state(item.group(1).equals("x")) + item.group(2).replace("*Stretch:*", "Stretch:"))
			.toList();
	}

	private static List<String> splashPageItems() throws IOException {
		return Jsoup.parse(Path.of("site/index.html"))
			.select(".phases .items li")
			.stream()
			.map(SiteRoadmapTests::splashPageItem)
			.toList();
	}

	private static String splashPageItem(Element item) {
		return state(item.selectFirst("svg").attr("aria-label").equals("Built")) + item.text();
	}

	private static String state(boolean built) {
		return built ? "built: " : "not built: ";
	}

}
