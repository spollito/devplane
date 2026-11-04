import assert from "node:assert/strict";
import { test } from "node:test";

import {
	browserStorage,
	createCatalog,
	exactTime,
	relativeTime,
	validate,
} from "./catalog.js";

const NOW = new Date(2026, 9, 22, 12, 0).getTime();
const MINUTE = 60 * 1000;
const HOUR = 60 * MINUTE;
const DAY = 24 * HOUR;
const LONGEST = `a${"-b".repeat(31)}`;

function memoryStorage(initial = null) {
	let text = initial;
	return {
		read: () => text,
		write: (value) => {
			text = value;
		},
		text: () => text,
	};
}

function newCatalog(storage = memoryStorage()) {
	return createCatalog({ storage, now: () => NOW });
}

const valid = { name: "my-service", owner: "my-team", description: "", lifecycle: "experimental" };

test("lists the sample services sorted by name", () => {
	const names = newCatalog().list().map((service) => service.name);
	assert.ok(names.length > 5);
	assert.deepEqual(names, [...names].sort());
	assert.ok(names.includes("orders-api"));
});

test("sample repositories are marked as samples", () => {
	const repository = newCatalog().get("auth-service").repository;
	assert.equal(repository.kind, "sample");
	assert.equal(repository.fullName, "octo-dev/auth-service");
	assert.equal(repository.url, undefined);
});

test("filters by owner", () => {
	const catalog = newCatalog();
	const owners = new Set(catalog.list("payments-team").map((service) => service.owner));
	assert.deepEqual([...owners], ["payments-team"]);
	assert.deepEqual(catalog.list("billing-team"), []);
});

test("registers a valid service", () => {
	const catalog = newCatalog();
	const { service } = catalog.register(valid);
	assert.equal(service.name, "my-service");
	assert.equal(service.description, null);
	assert.equal(service.repository, null);
	assert.equal(catalog.get("my-service").owner, "my-team");
});

test("defaults the lifecycle to experimental", () => {
	const { service } = newCatalog().register({ ...valid, lifecycle: "" });
	assert.equal(service.lifecycle, "experimental");
});

test("accepts the longest valid names", () => {
	assert.equal(LONGEST.length, 63);
	assert.deepEqual(validate({ ...valid, name: LONGEST, owner: LONGEST }).errors, {});
});

test("reports every invalid field with the real app's wording", () => {
	const catalog = newCatalog();
	const before = catalog.list().length;
	const { errors } = catalog.register({ name: "My Service", owner: "", description: "", lifecycle: "beta" });
	assert.deepEqual(errors, {
		name: "Name must be 1 to 63 lowercase letters, digits, or hyphens, start with a letter, and not end with a hyphen",
		owner: "Enter the team that owns the service",
		lifecycle: "Lifecycle must be one of experimental, production, or deprecated",
	});
	assert.equal(catalog.list().length, before);
});

for (const name of ["Orders", "2-orders", "orders-", "a".repeat(64), "with space"]) {
	test(`rejects the name "${name}"`, () => {
		assert.ok(validate({ ...valid, name }).errors.name);
	});
}

test("rejects an overlong description, and allows the limit", () => {
	assert.equal(validate({ ...valid, description: "x".repeat(301) }).errors.description, "Description must be at most 300 characters");
	assert.deepEqual(validate({ ...valid, description: "x".repeat(300) }).errors, {});
});

test("an empty name reads as an instruction", () => {
	assert.equal(validate({ ...valid, name: "   " }).errors.name, "Enter a name for the service");
});

test("rejects a duplicate name, and leaves the existing service alone", () => {
	const catalog = newCatalog();
	const result = catalog.register({ ...valid, name: "orders-api", owner: "checkout-team" });
	assert.equal(result.duplicate, "orders-api");
	assert.equal(catalog.get("orders-api").owner, "payments-team");
});

test("keeps entered markup as data", () => {
	const description = "<img src=x onerror=alert(1)>";
	const { service } = newCatalog().register({ ...valid, description });
	assert.equal(service.description, description);
});

test("unregisters a service", () => {
	const catalog = newCatalog();
	catalog.unregister("orders-api");
	assert.equal(catalog.get("orders-api"), null);
});

test("is empty once every service is unregistered", () => {
	const catalog = newCatalog();
	for (const service of catalog.list()) {
		catalog.unregister(service.name);
	}
	assert.equal(catalog.isEmpty(), true);
});

test("simulates a repository without a name or URL", () => {
	const catalog = newCatalog();
	catalog.simulateRepository("orders-api", "public");
	assert.deepEqual(catalog.get("orders-api").repository, {
		kind: "simulated",
		visibility: "public",
		template: "java-rest-api",
		createdAt: NOW,
	});
	assert.equal(catalog.get("orders-api").updatedAt, NOW);
});

test("reset restores the samples", () => {
	const catalog = newCatalog();
	const before = catalog.list().map((service) => service.name);
	catalog.register(valid);
	catalog.unregister("orders-api");
	catalog.reset();
	assert.deepEqual(catalog.list().map((service) => service.name), before);
});

test("changes survive a new catalog on the same storage", () => {
	const storage = memoryStorage();
	newCatalog(storage).register(valid);
	assert.equal(newCatalog(storage).get("my-service").owner, "my-team");
});

test("unreadable saved data falls back to the samples", () => {
	assert.ok(newCatalog(memoryStorage("{not json")).get("orders-api"));
	assert.ok(newCatalog(memoryStorage('{"not": "a list"}')).get("orders-api"));
});

test("blocked storage keeps working for the visit", () => {
	const blocked = browserStorage(() => {
		throw new Error("storage is blocked");
	});
	assert.equal(blocked.read(), null);
	const catalog = newCatalog(blocked);
	catalog.register(valid);
	assert.equal(catalog.get("my-service").owner, "my-team");
});

test("relative times use the real app's wording", () => {
	const cases = [
		[0, "just now"],
		[MINUTE, "1 minute ago"],
		[5 * MINUTE, "5 minutes ago"],
		[HOUR, "1 hour ago"],
		[3 * HOUR, "3 hours ago"],
		[DAY, "Yesterday"],
		[4 * DAY, "4 days ago"],
		[14 * DAY, "2 weeks ago"],
		[60 * DAY, "2 months ago"],
		[400 * DAY, "1 year ago"],
	];
	for (const [age, expected] of cases) {
		assert.equal(relativeTime(NOW - age, NOW), expected);
	}
});

test("exact times use the real app's format", () => {
	assert.equal(exactTime(new Date(2026, 9, 21, 16, 40).getTime()), "Oct 21, 2026, 16:40");
	assert.equal(exactTime(new Date(2026, 0, 5, 9, 3).getTime()), "Jan 5, 2026, 09:03");
});
