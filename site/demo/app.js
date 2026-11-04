// The demo's screens and routing. Each screen matches a Thymeleaf page in the real app, with
// the same CSS classes and wording. Elements are only built with textContent and attributes,
// so text a visitor enters can't become markup.

import {
	browserStorage,
	createCatalog,
	exactTime,
	LIFECYCLE_STAGES,
	relativeTime,
	TEMPLATE_DESCRIPTION,
	TEMPLATE_NAME,
} from "./catalog.js";

const SVG = "http://www.w3.org/2000/svg";
const SIMULATED_WAIT_MS = 1000;
const RUN_LOCALLY = "../#run";

const catalog = createCatalog({ storage: browserStorage(() => window.localStorage) });
const main = document.getElementById("main");

// The notice to show on the next screen, as the real app's flash attributes do.
let pendingNotice = null;

function h(tag, attributes = {}, ...children) {
	const element = document.createElement(tag);
	for (const [name, value] of Object.entries(attributes)) {
		if (value === true) {
			element.setAttribute(name, "");
		}
		else if (value !== false && value !== null && value !== undefined) {
			element.setAttribute(name, value);
		}
	}
	element.append(...children.flat().filter((child) => child !== null && child !== undefined && child !== false));
	return element;
}

function drawing(viewBox, className, shapes) {
	const svg = document.createElementNS(SVG, "svg");
	if (viewBox) {
		svg.setAttribute("viewBox", viewBox);
	}
	svg.setAttribute("aria-hidden", "true");
	if (className) {
		svg.setAttribute("class", className);
	}
	for (const [tag, attributes] of shapes) {
		const shape = document.createElementNS(SVG, tag);
		for (const [name, value] of Object.entries(attributes)) {
			shape.setAttribute(name, value);
		}
		svg.append(shape);
	}
	return svg;
}

// From Lucide (ISC License), as in the real app's fragments/icons.html.
const icon = {
	download: () => drawing("0 0 24 24", "icon", [
		["path", { d: "M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" }],
		["path", { d: "m7 10 5 5 5-5" }],
		["path", { d: "M12 15V3" }],
	]),
	success: () => drawing("0 0 24 24", "icon", [
		["circle", { cx: 12, cy: 12, r: 10 }],
		["path", { d: "m9 12 2 2 4-4" }],
	]),
	error: () => drawing("0 0 24 24", "icon", [
		["circle", { cx: 12, cy: 12, r: 10 }],
		["path", { d: "M12 8v4" }],
		["path", { d: "M12 16h.01" }],
	]),
};

const LIFECYCLE_MARKERS = {
	production: [["rect", { width: 10, height: 10, fill: "currentColor" }]],
	experimental: [["rect", { x: 1, y: 1, width: 8, height: 8, fill: "none", stroke: "currentColor", "stroke-width": 2 }]],
	deprecated: [
		["rect", { x: 1, y: 1, width: 8, height: 8, fill: "none", stroke: "currentColor", "stroke-width": 2 }],
		["path", { d: "M1.5 8.5 8.5 1.5", stroke: "currentColor", "stroke-width": 1.5 }],
	],
};

const LIFECYCLE_HINTS = {
	experimental: "Still being built or tried out.",
	production: "Built and in use.",
	deprecated: "Being retired. New work should use something else.",
};

function capitalize(text) {
	return text.charAt(0).toUpperCase() + text.slice(1);
}

function id(text, className = "") {
	return h("span", { class: `id ${className}`.trim() }, text);
}

function lifecycle(stage) {
	return h("span", { class: `lifecycle lifecycle--${stage}` },
		drawing("0 0 10 10", null, LIFECYCLE_MARKERS[stage]), capitalize(stage));
}

function serviceHref(name, suffix = "") {
	return `#/services/${encodeURIComponent(name)}${suffix}`;
}

function ownerHref(owner) {
	return `#/services?owner=${encodeURIComponent(owner)}`;
}

function relative(time) {
	return h("time", { datetime: new Date(time).toISOString(), title: exactTime(time) }, relativeTime(time, Date.now()));
}

function exact(time) {
	return h("time", { datetime: new Date(time).toISOString() }, exactTime(time));
}

// No viewBox: devplane.css sizes the rectangle to the step, whatever the step's shape.
function outline() {
	return drawing(null, "step__outline", [["rect", {}]]);
}

function demoNotice() {
	const reset = h("button", { class: "button", type: "button" }, "Reset demo");
	reset.addEventListener("click", () => {
		catalog.reset();
		pendingNotice = successNotice("The demo is back to its sample data.");
		navigate("#/services");
	});
	return h("div", { class: "demo-notice" },
		h("p", {}, "This is a demo with sample data. It stays in your browser, and nothing is created on GitHub."),
		h("div", { class: "demo-notice__actions" },
			h("a", { href: "../" }, "About DevPlane"),
			h("a", { href: RUN_LOCALLY }, "Run the real app"),
			reset));
}

function successNotice(...content) {
	return h("div", { class: "notice notice--success", role: "status", tabindex: "-1", "data-focus": true },
		icon.success(), h("p", {}, ...content));
}

function pageHeader(title, ...rest) {
	return h("div", { class: "page-header" }, h("div", { class: "page-header__text" }, title), ...rest);
}

function breadcrumbs(...items) {
	return h("nav", { class: "breadcrumbs", "aria-label": "Breadcrumbs" }, h("ol", {}, items.map((item) => h("li", {}, item))));
}

function step(attributes, ...children) {
	const { kind = "", ...rest } = attributes;
	return h("li", { class: `step ${kind}`.trim(), ...rest }, ...children);
}

function stepHead(title, ...status) {
	return h("div", { class: "step__head" }, h("h3", {}, title), h("p", { class: "step__status" }, ...status));
}

// The catalog

function catalogTable(services) {
	const rows = services.map((service) => h("tr", {},
		h("td", {},
			h("a", { class: "id", href: serviceHref(service.name) }, service.name),
			h("div", { class: "table__owner-below small muted id" }, service.owner)),
		service.description
			? h("td", { class: "cell-description", title: service.description }, service.description)
			: h("td", { class: "cell-description muted" }, "No description"),
		h("td", { class: "cell-owner" }, h("a", { class: "secondary-link id", href: ownerHref(service.owner) }, service.owner)),
		h("td", {}, lifecycle(service.lifecycle)),
		service.repository
			? h("td", { class: "cell-repository" }, repositoryLabel(service.repository))
			: h("td", { class: "cell-repository muted" }, "None"),
		h("td", { class: "cell-updated muted" }, relative(service.updatedAt))));
	return [
		h("table", { class: "table" },
			h("caption", { class: "visually-hidden" }, "Registered services"),
			h("thead", {}, h("tr", {},
				h("th", { scope: "col", class: "cell-name" }, "Name"),
				h("th", { scope: "col", class: "cell-description" }, "Description"),
				h("th", { scope: "col", class: "cell-owner" }, "Owner"),
				h("th", { scope: "col", class: "cell-lifecycle" }, "Lifecycle"),
				h("th", { scope: "col", class: "cell-repository" }, "Repository"),
				h("th", { scope: "col", class: "cell-updated" }, "Updated"))),
			h("tbody", {}, rows)),
		h("div", { class: "table-footer" },
			h("p", { class: "muted" }, services.length === 1 ? "1 service" : `${services.length} services`)),
	];
}

/** Sample and simulated repositories don't exist, so they're never links. */
function repositoryLabel(repository) {
	return repository.kind === "sample" ? id(repository.fullName) : h("span", { class: "muted" }, "Simulated");
}

function catalogResults(owner) {
	const services = catalog.list(owner);
	if (services.length > 0) {
		return catalogTable(services);
	}
	return [h("div", { class: "empty" },
		h("p", {}, id(owner), " owns no services."),
		h("a", { href: "#/services" }, "Show all services"))];
}

function catalogScreen(query) {
	const owner = (query.get("owner") ?? "").trim() || null;
	document.title = "Services · DevPlane demo";
	if (catalog.isEmpty()) {
		return [
			pageHeader(h("h1", {}, "Services")),
			h("section", { class: "intro", "aria-labelledby": "intro-heading" },
				h("div", { class: "intro__text" },
					h("h2", { id: "intro-heading" }, "No services are registered yet"),
					h("p", { class: "muted" }, "DevPlane keeps track of your services and who owns them, and gives each one a starting project and a GitHub repository.")),
				h("ol", { class: "path path--even" },
					step({ kind: "step--next" }, outline(),
						stepHead("Catalog entry", "Register the service with its name, owner, and lifecycle stage."),
						h("div", { class: "step__actions" }, h("a", { class: "button button--primary", href: "#/register" }, "Register service"))),
					step({ kind: "step--later" },
						stepHead("Project", "Download a project generated from ", id(TEMPLATE_NAME), `, a ${TEMPLATE_DESCRIPTION}.`)),
					step({ kind: "step--later" },
						stepHead("Repository", "DevPlane creates a GitHub repository and pushes the project as its first commit. This needs a GitHub token.")))),
		];
	}
	const results = h("div", { id: "results" }, catalogResults(owner));
	const input = h("input", { class: "input id", id: "owner", name: "owner", type: "text", autocomplete: "off", spellcheck: "false", value: owner ?? "" });
	const filter = () => {
		const value = input.value.trim() || null;
		results.replaceChildren(...catalogResults(value));
		history.replaceState(null, "", value === null ? "#/services" : ownerHref(value));
	};
	input.addEventListener("input", filter);
	const form = h("form", { class: "filter", role: "search" },
		h("label", { class: "field__label", for: "owner" }, "Owner"),
		input,
		h("button", { class: "button", type: "submit" }, "Filter"));
	form.addEventListener("submit", (event) => {
		event.preventDefault();
		filter();
	});
	return [
		pageHeader(h("h1", {}, "Services"), h("a", { class: "button button--primary", href: "#/register" }, "Register service")),
		form,
		results,
	];
}

// A service's page

function repositoryStep(service) {
	const { repository } = service;
	if (repository?.kind === "sample") {
		return step({ id: "repository-step" },
			stepHead("Repository", "Created ", exact(repository.createdAt), " from ", id(repository.template)),
			h("p", {}, id(repository.fullName)),
			h("p", { class: "small muted" }, "A sample. This repository doesn't exist."));
	}
	if (repository?.kind === "simulated") {
		return step({ id: "repository-step" },
			stepHead("Repository", "Simulated ", exact(repository.createdAt), ". Nothing was created on GitHub."),
			h("p", {}, `The real DevPlane creates a ${repository.visibility} repository named `, id(service.name),
				" from ", id(repository.template), ", and pushes the project as its first commit."),
			h("p", {}, h("a", { href: RUN_LOCALLY }, "Run the real app")));
	}
	return repositoryForm(service);
}

function repositoryForm(service) {
	const choice = (value, label, hint, checked) => h("label", { class: "choice" },
		h("input", { type: "radio", name: "visibility", value, checked }),
		h("span", {}, label),
		h("span", { class: "choice__hint" }, hint));
	const fieldset = h("fieldset", { class: "field" },
		h("legend", { class: "field__label" }, "Visibility"),
		choice("private", "Private", "Only you and the people you invite on GitHub can see it.", true),
		choice("public", "Public", "Anyone on the internet can see it.", false));
	const button = h("button", { class: "button button--primary", type: "submit" },
		h("span", { class: "when-idle" }, "Create repository"),
		h("span", { class: "when-pending" }, "Creating repository…"));
	const form = h("form", { class: "step__options" },
		fieldset,
		h("dl", { class: "facts" },
			h("dt", {}, "Template"), h("dd", { class: "id" }, TEMPLATE_NAME),
			h("dt", {}, "Name"), h("dd", { class: "id" }, service.name)),
		h("div", { class: "step__actions" },
			button,
			h("p", { class: "when-pending muted" }, "This takes a few seconds in the real app.")));
	const stepElement = step({ kind: "step--next", id: "repository-step" }, outline(),
		stepHead("Repository", "Not created yet. The real DevPlane creates it on GitHub and pushes the project as the first commit. The demo only simulates this."),
		form);
	form.addEventListener("submit", (event) => {
		event.preventDefault();
		const visibility = new FormData(form).get("visibility");
		const route = location.hash;
		// devplane.css shows the pending label while a form has htmx's request class.
		form.classList.add("htmx-request");
		button.disabled = true;
		fieldset.disabled = true;
		stepElement.setAttribute("aria-busy", "true");
		window.setTimeout(() => {
			catalog.simulateRepository(service.name, visibility);
			if (location.hash === route) {
				pendingNotice = successNotice("Simulated creating a repository for ", id(`${service.name}.`), " Nothing was created on GitHub.");
				render();
			}
		}, SIMULATED_WAIT_MS);
	});
	return stepElement;
}

function serviceScreen(name) {
	const service = catalog.get(name);
	if (service === null) {
		return unknownServiceScreen(name);
	}
	document.title = `${service.name} · DevPlane demo`;
	return [
		breadcrumbs(h("a", { href: "#/services" }, "Services"), h("span", { class: "id", "aria-current": "page" }, service.name)),
		h("div", { class: "page-header" },
			h("div", { class: "page-header__text" },
				h("h1", {}, id(service.name)),
				h("p", { class: "muted" }, service.description ?? "No description"))),
		h("dl", { class: "summary" },
			h("div", {}, h("dt", {}, "Owner"), h("dd", {}, h("a", { class: "secondary-link id", href: ownerHref(service.owner) }, service.owner))),
			h("div", {}, h("dt", {}, "Lifecycle"), h("dd", {}, lifecycle(service.lifecycle))),
			h("div", {}, h("dt", {}, "Registered"), h("dd", {}, exact(service.createdAt))),
			h("div", {}, h("dt", {}, "Updated"), h("dd", {}, exact(service.updatedAt)))),
		h("section", { class: "section", "aria-labelledby": "code-heading" },
			h("h2", { id: "code-heading" }, "Code"),
			h("ol", { class: "path" },
				step({}, stepHead("Catalog entry", "Registered ", exact(service.createdAt))),
				step({},
					stepHead("Project", "The demo always downloads the real ", id("orders-api"), " example, generated from ", id(TEMPLATE_NAME), ". The real app generates a project for each service."),
					h("div", { class: "step__actions" },
						h("a", { class: "button", href: "../downloads/orders-api.zip", download: true }, icon.download(), "Download project"),
						h("span", { class: "small muted id" }, "orders-api.zip"))),
				repositoryStep(service))),
		h("section", { class: "section unregister", "aria-labelledby": "unregister-heading" },
			h("div", { class: "unregister__text" },
				h("h2", { id: "unregister-heading" }, "Unregister"),
				h("p", { class: "muted" }, "Removes ", id(service.name), " from the demo's catalog.")),
			h("a", { class: "button button--danger", href: serviceHref(service.name, "/unregister") }, "Unregister service")),
	];
}

function unknownServiceScreen(name) {
	document.title = `No service named ${name} · DevPlane demo`;
	return [
		pageHeader(h("h1", {}, "No service named ", id(name))),
		h("div", { class: "prose" },
			h("p", {}, "No service by that name is in the demo. It may have been unregistered."),
			h("p", {}, h("a", { href: "#/services" }, "Go to Services"))),
	];
}

// Register a service

function field(fieldName, label, hint, values, errors, extra = {}) {
	const error = errors[fieldName];
	const described = `${fieldName}-hint${error ? ` ${fieldName}-error` : ""}`;
	return h("div", { class: "field" },
		h("label", { class: "field__label", for: fieldName }, label),
		h("p", { class: "field__hint", id: `${fieldName}-hint` }, hint),
		error ? h("p", { class: "field__error", id: `${fieldName}-error` }, ...error) : null,
		h("input", {
			class: extra.mono ? "input id" : "input",
			id: fieldName,
			name: fieldName,
			type: "text",
			autocomplete: "off",
			spellcheck: extra.mono ? "false" : null,
			value: values[fieldName] ?? "",
			"aria-invalid": error ? "true" : null,
			"aria-describedby": described,
		}));
}

function errorSummary(entries) {
	const links = entries.map(([fieldName, message]) => {
		const link = h("a", { href: `#${fieldName}` }, ...message);
		// The address's hash belongs to the router, so this moves focus directly.
		link.addEventListener("click", (event) => {
			event.preventDefault();
			document.getElementById(fieldName).focus();
		});
		return h("li", {}, link);
	});
	return h("div", { class: "notice notice--error error-summary", id: "error-summary", tabindex: "-1", "data-focus": true },
		icon.error(),
		h("div", { class: "notice__body" },
			h("h2", { class: "notice__title" }, "Some fields need changes"),
			h("ul", {}, links)));
}

function registerScreen(values = { lifecycle: "experimental" }, failure = { errors: {}, duplicate: null }) {
	document.title = "Register service · DevPlane demo";
	const errors = Object.fromEntries(Object.entries(failure.errors).map(([name, message]) => [name, [message]]));
	const summary = Object.entries(errors);
	if (failure.duplicate) {
		const name = failure.duplicate;
		errors.name = ["A service named ", h("a", { href: serviceHref(name) }, id(name)), " already exists"];
		summary.unshift(["name", ["A service named ", id(name), " already exists"]]);
	}
	const stage = values.lifecycle || "experimental";
	const form = h("form", { class: "form", novalidate: true },
		summary.length > 0 ? errorSummary(summary) : null,
		field("name", "Name", "Lowercase letters, digits, and hyphens. Starts with a letter. Can't be changed later.", values, errors, { mono: true }),
		field("owner", "Owner", "The team that owns the service, written the same way as a name.", values, errors, { mono: true }),
		field("description", "Description (optional)", "What the service does, in up to 300 characters.", values, errors),
		h("fieldset", { class: "field" },
			h("legend", { class: "field__label" }, "Lifecycle"),
			LIFECYCLE_STAGES.map((value) => h("label", { class: "choice" },
				h("input", { type: "radio", name: "lifecycle", value, checked: value === stage }),
				h("span", {}, capitalize(value)),
				h("span", { class: "choice__hint" }, LIFECYCLE_HINTS[value])))),
		h("div", { class: "form__actions" },
			h("button", { class: "button button--primary", type: "submit" }, "Register service"),
			h("a", { class: "button", href: "#/services" }, "Cancel")));
	form.addEventListener("submit", (event) => {
		event.preventDefault();
		const entered = Object.fromEntries(new FormData(form));
		const result = catalog.register(entered);
		if (result.service) {
			pendingNotice = successNotice("Registered ", id(`${result.service.name}.`));
			navigate(serviceHref(result.service.name));
		}
		else {
			show(registerScreen(entered, result), true);
		}
	});
	return [
		breadcrumbs(h("a", { href: "#/services" }, "Services"), h("span", { "aria-current": "page" }, "Register service")),
		h("div", { class: "page-header" },
			h("div", { class: "page-header__text" },
				h("h1", {}, "Register service"),
				h("p", { class: "muted" }, "Add a service to the catalog. You can download its project and create its repository from its page."))),
		form,
	];
}

// Unregister a service

function unregisterScreen(name) {
	const service = catalog.get(name);
	if (service === null) {
		return unknownServiceScreen(name);
	}
	document.title = `Unregister ${service.name} · DevPlane demo`;
	const form = h("form", { class: "form__actions confirm" },
		h("button", { class: "button button--danger", type: "submit" }, "Unregister ", id(service.name)),
		h("a", { class: "button", href: serviceHref(service.name) }, "Cancel"));
	form.addEventListener("submit", (event) => {
		event.preventDefault();
		catalog.unregister(service.name);
		pendingNotice = successNotice("Unregistered ", id(`${service.name}.`));
		navigate("#/services");
	});
	return [
		breadcrumbs(
			h("a", { href: "#/services" }, "Services"),
			h("a", { class: "id", href: serviceHref(service.name) }, service.name),
			h("span", { "aria-current": "page" }, "Unregister")),
		pageHeader(h("h1", {}, "Unregister ", id(`${service.name}?`))),
		h("div", { class: "prose" },
			h("p", {}, id(service.name), " will be removed from the demo's catalog, with its owner, description, and lifecycle stage."),
			service.repository
				? h("p", {}, "In the real app, its repository stays on GitHub. You'd delete it there if you no longer needed it.")
				: h("p", {}, "It doesn't have a GitHub repository.")),
		form,
	];
}

// Routing

function screenFor(hash) {
	const [path, search] = hash.replace(/^#/, "").split("?");
	const parts = path.split("/").filter(Boolean);
	if (parts[0] === "register" && parts.length === 1) {
		return registerScreen();
	}
	if (parts[0] === "services") {
		let name = null;
		try {
			name = parts[1] === undefined ? null : decodeURIComponent(parts[1]);
		}
		catch {
			name = parts[1];
		}
		if (parts.length === 1) {
			return catalogScreen(new URLSearchParams(search));
		}
		if (parts.length === 2) {
			return serviceScreen(name);
		}
		if (parts.length === 3 && parts[2] === "unregister") {
			return unregisterScreen(name);
		}
	}
	return catalogScreen(new URLSearchParams());
}

/** Shows a screen. A notice or error summary takes focus; otherwise the page does, if asked. */
function show(screen, moveFocus) {
	const notice = pendingNotice;
	pendingNotice = null;
	main.replaceChildren(demoNotice(), ...(notice ? [notice] : []), ...screen);
	window.scrollTo(0, 0);
	const target = main.querySelector("[data-focus]") ?? (moveFocus ? main : null);
	target?.focus();
}

function render(moveFocus = true) {
	show(screenFor(location.hash), moveFocus);
}

function navigate(hash) {
	if (location.hash === hash) {
		render();
	}
	else {
		location.hash = hash;
	}
}

window.addEventListener("hashchange", () => render());

document.getElementById("skip-link").addEventListener("click", (event) => {
	// The address's hash belongs to the router, so this moves focus directly.
	event.preventDefault();
	main.focus();
});

render(false);
