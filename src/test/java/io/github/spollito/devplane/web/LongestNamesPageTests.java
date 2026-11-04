package io.github.spollito.devplane.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.spollito.devplane.IntegrationTest;
import io.github.spollito.devplane.catalog.ServiceCatalog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * The "Pages render the longest names" scenario from the web-ui spec. Whether they fit is
 * a question of layout, which the screenshot check in docs/ui-guidelines.md covers.
 */
@IntegrationTest
class LongestNamesPageTests {

	private static final String NAME = "aaaaaaaaaa-bbbbbbbbbb-cccccccccc-dddddddddd-eeeeeeeeee-ffffffff";

	private static final String OWNER = "zzzzzzzzzz-yyyyyyyyyy-xxxxxxxxxx-wwwwwwwwww-vvvvvvvvvv-uuuuuuuu";

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private ServiceCatalog catalog;

	@BeforeEach
	void registerTheLongestNames() {
		assertThat(NAME).hasSize(63);
		assertThat(OWNER).hasSize(63);
		TestCatalog.clear(catalog);
		TestCatalog.register(catalog, NAME, OWNER, "The longest name and owner the catalog accepts");
		TestCatalog.giveRepository(catalog, NAME);
	}

	@ParameterizedTest
	@ValueSource(strings = { "/services", "/services/" + NAME, "/services/" + NAME + "/unregister" })
	void pagesRenderTheLongestNames(String path) {
		MvcTestResult result = mvc.get().uri(path).exchange();

		assertThat(result).hasStatusOk();
		assertThat(HtmlPages.parse(result).select("main").text()).contains(NAME);
	}

}
