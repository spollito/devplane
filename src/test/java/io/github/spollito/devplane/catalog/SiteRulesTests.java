package io.github.spollito.devplane.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The "Rules drift from the real app" scenario from the public-site spec. The demo on the
 * public site holds its own copy of the catalog's rules, because it runs with no server.
 */
class SiteRulesTests {

	private static String demoRules;

	@BeforeAll
	static void readDemoRules() throws IOException {
		demoRules = Files.readString(Path.of("site/demo/catalog.js"));
	}

	@Test
	void theDemoUsesTheCatalogsIdentifierPattern() {
		assertThat(demoRules).contains("\"" + CatalogConstraints.IDENTIFIER_PATTERN + "\"");
	}

	@Test
	void theDemoUsesTheCatalogsDescriptionLimit() {
		assertThat(demoRules).contains("MAX_DESCRIPTION_LENGTH = " + CatalogConstraints.MAX_DESCRIPTION_LENGTH + ";");
	}

	@Test
	void theDemoOffersTheCatalogsLifecycleStages() {
		String stages = String.join("\", \"", Arrays.asList(Lifecycle.API_VALUE_PATTERN.split("\\|")));
		assertThat(demoRules).contains("[\"" + stages + "\"]");
	}

	@Test
	void theDemoUsesTheCatalogsMessages() {
		assertThat(demoRules).contains("\"" + CatalogConstraints.IDENTIFIER_MESSAGE + "\"")
			.contains("\"" + CatalogConstraints.LIFECYCLE_MESSAGE + "\"")
			.contains("\"" + CatalogConstraints.DESCRIPTION_MESSAGE + "\"");
	}

}
