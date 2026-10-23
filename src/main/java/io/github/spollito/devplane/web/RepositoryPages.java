package io.github.spollito.devplane.web;

import io.github.spollito.devplane.catalog.ServiceCatalog;
import io.github.spollito.devplane.catalog.SourceRepository;
import io.github.spollito.devplane.provisioning.ProvisionRepositoryRequest;
import io.github.spollito.devplane.provisioning.RepositoryProvisioner;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Creates a service's repository from its page. Success and failure both redirect
 * to the service's page, so reloading it never sends the request to GitHub again.
 * The outcome travels in flash attributes, which {@link ServicePages} shows.
 */
@Controller
class RepositoryPages {

	private final ServiceCatalog catalog;

	private final RepositoryProvisioner provisioner;

	RepositoryPages(ServiceCatalog catalog, RepositoryProvisioner provisioner) {
		this.catalog = catalog;
		this.provisioner = provisioner;
	}

	/**
	 * @throws io.github.spollito.devplane.catalog.ServiceNotFoundException for an
	 * unknown service, which becomes the unknown-service page
	 */
	@PostMapping("/services/{name}/repository")
	RedirectView create(@PathVariable String name, @Valid ProvisionRepositoryRequest request,
			BindingResult binding, RedirectAttributes redirect) {
		String servicePage = "/services/" + catalog.lookUp(name).name();
		if (binding.hasFieldErrors()) {
			fail(redirect, FieldMessages.of(binding.getFieldErrors().getFirst()), 400, request);
			return SeeOther.to(servicePage);
		}
		try {
			SourceRepository repository = provisioner.provision(name, request.templateOrDefault(),
					request.isPrivate());
			redirect.addFlashAttribute("notice",
					Notice.of("Created repository", RepositoryLink.of(repository).fullName()));
		}
		catch (ErrorResponseException ex) {
			if (ex.getStatusCode().value() == 404) {
				throw ex;
			}
			fail(redirect, ex.getBody().getDetail(), ex.getStatusCode().value(), request);
		}
		return SeeOther.to(servicePage);
	}

	private static void fail(RedirectAttributes redirect, String reason, int status,
			ProvisionRepositoryRequest request) {
		redirect.addFlashAttribute("repositoryError", reason);
		redirect.addFlashAttribute("repositoryErrorStatus", status);
		redirect.addFlashAttribute("visibility", request.isPrivate() ? "private" : "public");
	}

}
