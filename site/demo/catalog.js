// The demo's data and rules, with no DOM access, so Node's test runner can check them.
//
// The rules are plain constants, because SiteRulesTests and SiteFieldMessagesTests compare
// them with the real app's. If one of those tests fails, change the value here to match.

export const IDENTIFIER_PATTERN = "[a-z]([-a-z0-9]{0,61}[a-z0-9])?";
export const IDENTIFIER_MESSAGE =
	"must be 1 to 63 lowercase letters, digits, or hyphens, start with a letter, and not end with a hyphen";
export const LIFECYCLE_STAGES = ["experimental", "production", "deprecated"];
export const LIFECYCLE_MESSAGE = "must be one of experimental, production, or deprecated";
export const MAX_DESCRIPTION_LENGTH = 300;
export const DESCRIPTION_MESSAGE = "must be at most 300 characters";
export const MISSING_NAME_MESSAGE = "Enter a name for the service";
export const MISSING_OWNER_MESSAGE = "Enter the team that owns the service";

export const TEMPLATE_NAME = "java-rest-api";
export const TEMPLATE_DESCRIPTION = "Spring Boot (Java 21) REST API with PostgreSQL";

const STORAGE_KEY = "devplane-demo";
const MINUTE = 60 * 1000;
const HOUR = 60 * MINUTE;
const DAY = 24 * HOUR;

// Sample services. Each holds its age rather than a date, so "2 days ago" is true on any visit.
const SAMPLES = [
	{ name: "auth-service", owner: "identity-team", description: "Sign-in, sessions, and access tokens", lifecycle: "production", age: 3 * DAY, repository: true },
	{ name: "billing-api", owner: "payments-team", description: "Invoices, credit notes, and stored payment methods", lifecycle: "production", age: 7 * DAY, repository: true },
	{ name: "catalog-search", owner: "search-team", description: "Full-text search over the product catalog", lifecycle: "experimental", age: 5 * HOUR },
	{ name: "email-sender", owner: "platform-team", description: "Queues and sends transactional email", lifecycle: "deprecated", age: 60 * DAY, repository: true },
	{ name: "inventory-api", owner: "warehouse-team", description: "Stock levels for each warehouse", lifecycle: "experimental", age: DAY, repository: true },
	{ name: "legacy-reports", owner: "payments-team", description: "Nightly CSV reports. Replaced by exports in billing-api.", lifecycle: "deprecated", age: 120 * DAY },
	{ name: "notifications", owner: "platform-team", description: "Push and in-app notifications", lifecycle: "production", age: 14 * DAY, repository: true },
	{ name: "orders-api", owner: "payments-team", description: "Order management REST API", lifecycle: "experimental", age: 2 * DAY },
	{ name: "payments-api", owner: "payments-team", description: "Card payments through the payment provider", lifecycle: "production", age: 10 * DAY, repository: true },
	{ name: "shipping-rates", owner: "warehouse-team", description: null, lifecycle: "experimental", age: HOUR },
	{ name: "user-profiles", owner: "identity-team", description: "Profile data and account preferences", lifecycle: "production", age: 21 * DAY, repository: true },
];

function sampleServices(now) {
	return SAMPLES.map((sample) => {
		const time = now - sample.age;
		return {
			name: sample.name,
			owner: sample.owner,
			description: sample.description,
			lifecycle: sample.lifecycle,
			createdAt: time - 30 * DAY,
			updatedAt: time,
			repository: sample.repository
				? { kind: "sample", fullName: `octo-dev/${sample.name}`, template: TEMPLATE_NAME, createdAt: time }
				: null,
		};
	});
}

function clean(value) {
	const trimmed = (value ?? "").trim();
	return trimmed === "" ? null : trimmed;
}

/**
 * Checks a registration by the real app's rules. Returns the cleaned values, and each
 * invalid field's message in the form's order. An empty field counts as missing, and an
 * empty description or lifecycle is allowed.
 */
export function validate(form) {
	const values = {
		name: clean(form.name),
		owner: clean(form.owner),
		description: clean(form.description),
		lifecycle: clean(form.lifecycle),
	};
	const identifier = new RegExp(`^(?:${IDENTIFIER_PATTERN})$`);
	const errors = {};
	if (values.name === null) {
		errors.name = MISSING_NAME_MESSAGE;
	}
	else if (!identifier.test(values.name)) {
		errors.name = `Name ${IDENTIFIER_MESSAGE}`;
	}
	if (values.owner === null) {
		errors.owner = MISSING_OWNER_MESSAGE;
	}
	else if (!identifier.test(values.owner)) {
		errors.owner = `Owner ${IDENTIFIER_MESSAGE}`;
	}
	if (values.description !== null && values.description.length > MAX_DESCRIPTION_LENGTH) {
		errors.description = `Description ${DESCRIPTION_MESSAGE}`;
	}
	if (values.lifecycle !== null && !LIFECYCLE_STAGES.includes(values.lifecycle)) {
		errors.lifecycle = `Lifecycle ${LIFECYCLE_MESSAGE}`;
	}
	return { values, errors };
}

/** Local storage when the browser allows it. When it doesn't, the demo lasts for the visit. */
export function browserStorage(store) {
	return {
		read() {
			try {
				return store().getItem(STORAGE_KEY);
			}
			catch {
				return null;
			}
		},
		write(text) {
			try {
				store().setItem(STORAGE_KEY, text);
			}
			catch {
				// Storage is blocked or full: the data stays in memory.
			}
		},
	};
}

/** The demo's catalog. It never contacts anything but the storage it's given. */
export function createCatalog({ storage, now = () => Date.now() }) {
	function load() {
		const saved = storage.read();
		if (saved !== null) {
			try {
				const services = JSON.parse(saved);
				if (Array.isArray(services)) {
					return services;
				}
			}
			catch {
				// Unreadable saved data: start again from the samples.
			}
		}
		return sampleServices(now());
	}

	let services = load();

	function save() {
		storage.write(JSON.stringify(services));
	}

	return {
		/** Every service sorted by name, or only those with the given owner. */
		list(owner = null) {
			return services
				.filter((service) => owner === null || service.owner === owner)
				.sort((a, b) => (a.name < b.name ? -1 : 1));
		},

		isEmpty() {
			return services.length === 0;
		},

		get(name) {
			return services.find((service) => service.name === name) ?? null;
		},

		/** Returns the new service, or its errors and any duplicate name. */
		register(form) {
			const { values, errors } = validate(form);
			if (Object.keys(errors).length > 0) {
				return { errors, duplicate: null };
			}
			if (services.some((service) => service.name === values.name)) {
				return { errors: {}, duplicate: values.name };
			}
			const time = now();
			const service = {
				name: values.name,
				owner: values.owner,
				description: values.description,
				lifecycle: values.lifecycle ?? "experimental",
				createdAt: time,
				updatedAt: time,
				repository: null,
			};
			services.push(service);
			save();
			return { service };
		},

		unregister(name) {
			services = services.filter((service) => service.name !== name);
			save();
		},

		/** Records that the demo simulated a repository. Nothing happens outside the browser. */
		simulateRepository(name, visibility) {
			const service = this.get(name);
			const time = now();
			service.repository = {
				kind: "simulated",
				visibility: visibility === "public" ? "public" : "private",
				template: TEMPLATE_NAME,
				createdAt: time,
			};
			service.updatedAt = time;
			save();
		},

		reset() {
			services = sampleServices(now());
			save();
		},
	};
}

function ago(count, unit) {
	return `${count} ${unit}${count === 1 ? "" : "s"} ago`;
}

function startOfDay(time) {
	const date = new Date(time);
	return new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime();
}

/** The same wording as the real app: "just now", "5 hours ago", "Yesterday", "2 weeks ago". */
export function relativeTime(time, now) {
	const minutes = Math.floor((now - time) / MINUTE);
	if (minutes < 1) {
		return "just now";
	}
	if (minutes < 60) {
		return ago(minutes, "minute");
	}
	const days = Math.round((startOfDay(now) - startOfDay(time)) / DAY);
	if (days < 1) {
		return ago(Math.floor(minutes / 60), "hour");
	}
	if (days === 1) {
		return "Yesterday";
	}
	if (days < 7) {
		return ago(days, "day");
	}
	if (days < 30) {
		return ago(Math.floor(days / 7), "week");
	}
	if (days < 365) {
		return ago(Math.floor(days / 30), "month");
	}
	return ago(Math.floor(days / 365), "year");
}

/** The same format as the real app, such as "Oct 21, 2026, 16:40". */
export function exactTime(time) {
	const date = new Date(time);
	const month = date.toLocaleString("en-US", { month: "short" });
	const pad = (number) => String(number).padStart(2, "0");
	return `${month} ${date.getDate()}, ${date.getFullYear()}, ${pad(date.getHours())}:${pad(date.getMinutes())}`;
}
