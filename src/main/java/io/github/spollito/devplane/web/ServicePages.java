package io.github.spollito.devplane.web;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.github.spollito.devplane.catalog.DuplicateServiceNameException;
import io.github.spollito.devplane.catalog.Lifecycle;
import io.github.spollito.devplane.catalog.RegisterServiceRequest;
import io.github.spollito.devplane.catalog.ServiceCatalog;
import io.github.spollito.devplane.templates.ServiceTemplates;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

/** A service's page, and the pages that register and unregister services. */
@Controller
class ServicePages {

	/** The register form's fields, in the order the form shows them. */
	private static final List<String> FIELDS = List.of("name", "owner", "description", "lifecycle");

	private final ServiceCatalog catalog;

	private final ServiceTemplates templates;

	private final PageTimes times;

	ServicePages(ServiceCatalog catalog, ServiceTemplates templates, PageTimes times) {
		this.catalog = catalog;
		this.templates = templates;
		this.times = times;
	}

	/**
	 * Shows a service and its steps from catalog entry to repository. After a
	 * redirect, flash attributes add the notice, or why the repository wasn't created
	 * and the visibility that was chosen.
	 */
	@GetMapping("/services/{name}")
	String service(@PathVariable String name, Model model) {
		model.addAttribute("service", ServiceView.of(catalog.lookUp(name), times));
		model.addAttribute("template", templates.defaultTemplate());
		if (!model.containsAttribute("visibility")) {
			model.addAttribute("visibility", "private");
		}
		return "service";
	}

	@GetMapping("/register")
	String registerForm(Model model) {
		model.addAttribute("form", new RegisterServiceRequest(null, null, null, Lifecycle.EXPERIMENTAL.apiValue()));
		model.addAttribute("errors", Map.of());
		return "register";
	}

	/**
	 * Registers a service by the API's rules, then shows its page. Invalid fields show
	 * the form again with 400, and a name that's taken with 409, keeping what was
	 * typed.
	 */
	@PostMapping("/register")
	ModelAndView register(@Valid @ModelAttribute("form") RegisterServiceRequest form, BindingResult binding,
			RedirectAttributes redirect) {
		if (binding.hasErrors()) {
			return registerFormAgain(binding, HttpStatus.BAD_REQUEST);
		}
		try {
			catalog.register(form.name(), form.owner(), form.description(),
					(form.lifecycle() != null) ? Lifecycle.fromApiValue(form.lifecycle()) : null);
		}
		catch (DuplicateServiceNameException ex) {
			binding.rejectValue("name", "duplicate", "already exists");
			ModelAndView page = registerFormAgain(binding, HttpStatus.CONFLICT);
			page.addObject("duplicateName", form.name());
			return page;
		}
		redirect.addFlashAttribute("notice", Notice.of("Registered", form.name()));
		return new ModelAndView(SeeOther.to("/services/" + form.name()));
	}

	@GetMapping("/services/{name}/unregister")
	String unregisterConfirmation(@PathVariable String name, Model model) {
		model.addAttribute("service", ServiceView.of(catalog.lookUp(name), times));
		return "unregister";
	}

	@PostMapping("/services/{name}/unregister")
	RedirectView unregister(@PathVariable String name, RedirectAttributes redirect) {
		catalog.unregister(name);
		redirect.addFlashAttribute("notice", Notice.of("Unregistered", name));
		return SeeOther.to("/services");
	}

	/** The form again, with each invalid field's message, in the form's order. */
	private static ModelAndView registerFormAgain(BindingResult binding, HttpStatus status) {
		Map<String, String> errors = new LinkedHashMap<>();
		for (String field : FIELDS) {
			FieldError error = binding.getFieldError(field);
			if (error != null && !"duplicate".equals(error.getCode())) {
				errors.put(field, FieldMessages.of(error));
			}
		}
		ModelAndView page = new ModelAndView("register", status);
		page.addObject("errors", errors);
		return page;
	}

}
