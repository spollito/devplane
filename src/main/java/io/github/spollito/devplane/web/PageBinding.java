package io.github.spollito.devplane.web;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

/**
 * Binds the page controllers' form fields. An empty field becomes {@code null}, so
 * an empty name fails "must not be null" rather than the name's pattern, and an
 * empty description is stored as no description, as when the API omits it.
 */
@ControllerAdvice(basePackageClasses = PageBinding.class)
class PageBinding {

	@InitBinder
	void trimFields(WebDataBinder binder) {
		binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
	}

}
