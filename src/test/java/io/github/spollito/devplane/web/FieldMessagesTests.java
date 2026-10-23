package io.github.spollito.devplane.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.validation.FieldError;

/** Checks the sentences pages show under invalid fields. */
class FieldMessagesTests {

	@Test
	void asksForAMissingName() {
		assertThat(FieldMessages.of(error("name", "NotNull", "must not be null")))
			.isEqualTo("Enter a name for the service");
	}

	@Test
	void asksForAMissingOwner() {
		assertThat(FieldMessages.of(error("owner", "NotNull", "must not be null")))
			.isEqualTo("Enter the team that owns the service");
	}

	@Test
	void putsTheLabelBeforeOtherMessages() {
		assertThat(FieldMessages.of(error("name", "Pattern", "must be 1 to 63 lowercase letters")))
			.isEqualTo("Name must be 1 to 63 lowercase letters");
		assertThat(FieldMessages.of(error("description", "Size", "must be at most 300 characters")))
			.isEqualTo("Description must be at most 300 characters");
		assertThat(FieldMessages.of(error("visibility", "Pattern", "must be private or public")))
			.isEqualTo("Visibility must be private or public");
	}

	@Test
	void asksForAnyOtherMissingField() {
		assertThat(FieldMessages.of(error("lifecycle", "NotNull", "must not be null"))).isEqualTo("Enter the lifecycle");
	}

	private static FieldError error(String field, String code, String message) {
		return new FieldError("form", field, null, false, new String[] { code }, null, message);
	}

}
