package io.github.spollito.devplane.web;

import io.github.spollito.devplane.catalog.RegisteredService;
import io.github.spollito.devplane.catalog.ServiceCatalog;
import io.github.spollito.devplane.templates.ServiceTemplates;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

/** The catalog's pages: the root, which opens the catalog, and the catalog itself. */
@Controller
class CatalogPages {

	/** The same as the API's default page size. */
	static final int PAGE_SIZE = 20;

	private final ServiceCatalog catalog;

	private final ServiceTemplates templates;

	private final PageTimes times;

	CatalogPages(ServiceCatalog catalog, ServiceTemplates templates, PageTimes times) {
		this.catalog = catalog;
		this.templates = templates;
		this.times = times;
	}

	@GetMapping("/")
	String root() {
		return "redirect:/services";
	}

	/**
	 * Lists services sorted by name, 20 to a page. Pages are numbered from 1: anything
	 * that isn't a positive whole number shows the first page, and a number past the
	 * end shows the last. A blank owner lists every service.
	 */
	@GetMapping("/services")
	String catalog(@RequestParam(required = false) @Nullable String owner,
			@RequestParam(required = false) @Nullable String page, Model model) {
		String ownerFilter = StringUtils.hasText(owner) ? owner.trim() : null;
		int requested = pageNumber(page);
		Page<RegisteredService> services = list(ownerFilter, requested);
		if (requested > services.getTotalPages() && services.getTotalPages() > 0) {
			services = list(ownerFilter, services.getTotalPages());
		}

		model.addAttribute("owner", (ownerFilter != null) ? ownerFilter : "");
		model.addAttribute("defaultTemplate", templates.defaultTemplate());
		if (services.getTotalElements() == 0) {
			boolean catalogIsEmpty = (ownerFilter == null) || catalog.list(null, PageRequest.of(0, 1)).isEmpty();
			model.addAttribute("state", catalogIsEmpty ? "empty" : "no-match");
			return "catalog";
		}
		model.addAttribute("state", "list");
		model.addAttribute("rows", services.map(service -> ServiceRow.of(service, times)).getContent());
		model.addAttribute("summary", summary(services));
		if (services.hasPrevious()) {
			model.addAttribute("previousUrl", catalogUrl(ownerFilter, services.getNumber()));
		}
		if (services.hasNext()) {
			model.addAttribute("nextUrl", catalogUrl(ownerFilter, services.getNumber() + 2));
		}
		return "catalog";
	}

	private Page<RegisteredService> list(@Nullable String owner, int pageNumber) {
		return catalog.list(owner, PageRequest.of(pageNumber - 1, PAGE_SIZE, Sort.by("name")));
	}

	private static int pageNumber(@Nullable String page) {
		if (page == null || !page.matches("[0-9]{1,9}")) {
			return 1;
		}
		return Math.max(Integer.parseInt(page), 1);
	}

	/** "21–40 of 57" when there's more than one page, or "3 services" when everything fits. */
	private static String summary(Page<?> services) {
		long total = services.getTotalElements();
		if (services.getTotalPages() <= 1) {
			return total + ((total == 1) ? " service" : " services");
		}
		long first = services.getPageable().getOffset() + 1;
		long last = first + services.getNumberOfElements() - 1;
		return first + "–" + last + " of " + total;
	}

	/** The catalog's address for a page, keeping the owner filter. */
	static String catalogUrl(@Nullable String owner, int pageNumber) {
		UriComponentsBuilder url = UriComponentsBuilder.fromPath("/services");
		if (owner != null) {
			url.queryParam("owner", owner);
		}
		return url.queryParam("page", pageNumber).encode().toUriString();
	}

	/** The catalog filtered to one owner, which every owner name on the pages links to. */
	static String ownerUrl(String owner) {
		return UriComponentsBuilder.fromPath("/services").queryParam("owner", owner).encode().toUriString();
	}

}
