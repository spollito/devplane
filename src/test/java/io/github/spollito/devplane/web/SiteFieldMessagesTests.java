package io.github.spollito.devplane.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.validation.FieldError;

/**
 * Part of the "Rules drift from the real app" scenario from the public-site spec: the
 * demo says the same thing as the real register form when a required field is empty.
 */
class SiteFieldMessagesTests {

	@ParameterizedTest
	@ValueSource(strings = { "name", "owner" })
	void theDemoUsesTheFormsMissingFieldMessages(String field) throws IOException {
		FieldError missing = new FieldError("form", field, null, false, new String[] { "NotNull" }, null,
				"must not be null");

		assertThat(Files.readString(Path.of("site/demo/catalog.js"))).contains("\"" + FieldMessages.of(missing) + "\"");
	}

}
