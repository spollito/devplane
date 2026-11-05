# service-catalog Specification

## Purpose
The service catalog is DevPlane's record of every service: its name, who owns it,
what it does, and where it is in its lifecycle. Later features, such as creating,
deploying, and monitoring services, build on this record.

## Requirements

### Requirement: Register a service
The system SHALL let a client register a service with `POST /api/v1/services`, giving a `name`, an `owner`, an optional `description` of at most 300 characters, and an optional `lifecycle` stage.

#### Scenario: Registers a new service
- **GIVEN** no service named `orders-api` exists
- **WHEN** a client sends `POST /api/v1/services` with name `orders-api`, owner `payments-team`, and description `Order management REST API`
- **THEN** the response status is 201 Created and the `Location` header points to `/api/v1/services/orders-api`
- **AND** the body contains the service's `name`, `owner`, `description`, `lifecycle`, `createdAt`, and `updatedAt`, with both timestamps set to the time of registration in ISO 8601 UTC format

#### Scenario: Rejects a duplicate name
- **GIVEN** a service named `orders-api` exists
- **WHEN** a client registers another service named `orders-api`
- **THEN** the response status is 409 Conflict
- **AND** the existing service is unchanged

#### Scenario: Rejects missing required fields
- **WHEN** a client registers a service without a `name` or an `owner`
- **THEN** the response status is 400 Bad Request and the `errors` list identifies both the `name` and `owner` fields
- **AND** no service is registered

#### Scenario: Rejects an overlong description
- **WHEN** a client registers a service with a 301-character description
- **THEN** the response status is 400 Bad Request and the `errors` list identifies the `description` field

### Requirement: Service names and owners are DNS-safe identifiers
The system SHALL accept a service `name` or `owner` only if it is 1 to 63 characters long, starts with a lowercase letter, contains only lowercase letters, digits, and hyphens, and does not end with a hyphen.

#### Scenario: Accepts a valid identifier
- **WHEN** a client registers a service named `orders-api-2` with owner `payments-team`
- **THEN** the response status is 201 Created

#### Scenario: Rejects uppercase letters and spaces
- **WHEN** a client registers a service named `Orders API`
- **THEN** the response status is 400 Bad Request and the `errors` list identifies the `name` field

#### Scenario: Rejects a name longer than 63 characters
- **WHEN** a client registers a service whose name is 64 characters long
- **THEN** the response status is 400 Bad Request and the `errors` list identifies the `name` field

#### Scenario: Rejects a leading digit or a trailing hyphen
- **WHEN** a client registers a service named `2-orders` or `orders-`
- **THEN** the response status is 400 Bad Request and the `errors` list identifies the `name` field

#### Scenario: Applies the same rules to owners
- **WHEN** a client registers a service with owner `Payments Team`
- **THEN** the response status is 400 Bad Request and the `errors` list identifies the `owner` field

### Requirement: Services have a lifecycle stage
The system SHALL record each service's `lifecycle` as one of `experimental`, `production`, or `deprecated`, using `experimental` when a registration does not give one.

#### Scenario: Defaults to experimental
- **WHEN** a client registers a service without a `lifecycle`
- **THEN** the registered service's `lifecycle` is `experimental`

#### Scenario: Accepts an explicit stage
- **WHEN** a client registers a service with lifecycle `production`
- **THEN** the registered service's `lifecycle` is `production`

#### Scenario: Rejects an unknown stage
- **WHEN** a client registers a service with lifecycle `beta`
- **THEN** the response status is 400 Bad Request and the `errors` list identifies the `lifecycle` field

### Requirement: View a service
The system SHALL return a registered service's details from `GET /api/v1/services/{name}`. The `repository` field is `null` until the service's repository is provisioned, and then holds the repository's `url`, `template`, and `createdAt`. Every response that returns a service, including lists, registration, and updates, uses this same representation.

#### Scenario: Returns a registered service
- **GIVEN** a service named `orders-api` is registered
- **WHEN** a client sends `GET /api/v1/services/orders-api`
- **THEN** the response status is 200 OK and the body contains the service's `name`, `owner`, `description`, `lifecycle`, `createdAt`, and `updatedAt`
- **AND** `repository` is `null`

#### Scenario: Shows a provisioned repository
- **GIVEN** `orders-api`'s repository was provisioned at `https://github.com/octo-dev/orders-api` from the `java-rest-api` template
- **WHEN** a client sends `GET /api/v1/services/orders-api`, or lists services
- **THEN** the service's `repository` has `url` `https://github.com/octo-dev/orders-api`, `template` `java-rest-api`, and the `createdAt` of the provisioning

#### Scenario: Returns 404 for an unknown service
- **WHEN** a client requests a service name that is not registered
- **THEN** the response status is 404 Not Found

### Requirement: List services
The system SHALL list services from `GET /api/v1/services`, sorted by name, one page at a time, as an object with `items`, `page`, `size`, `totalItems`, and `totalPages`. Pages are numbered from 0, the default page size is 20, and the largest allowed page size is 100. A page past the last SHALL have empty `items` and the same totals.

#### Scenario: Lists services alphabetically
- **GIVEN** services named `payments-api`, `auth-service`, and `orders-api` are registered
- **WHEN** a client sends `GET /api/v1/services`
- **THEN** the response status is 200 OK and `items` lists `auth-service`, `orders-api`, and `payments-api`, in that order

#### Scenario: Empty catalog
- **GIVEN** no services are registered
- **WHEN** a client lists services
- **THEN** the response status is 200 OK, `items` is empty, and `totalItems` is 0

#### Scenario: Returns the requested page
- **GIVEN** 25 services are registered
- **WHEN** a client sends `GET /api/v1/services?page=1&size=10`
- **THEN** `items` contains the 11th through 20th services by name
- **AND** `totalItems` is 25 and `totalPages` is 3

#### Scenario: Caps the page size
- **GIVEN** 150 services are registered
- **WHEN** a client requests a page size of 500
- **THEN** `items` contains 100 services and `size` is 100

#### Scenario: Page past the last
- **GIVEN** 3 services are registered
- **WHEN** a client sends `GET /api/v1/services?page=1`
- **THEN** the response status is 200 OK and `items` is empty
- **AND** `totalItems` is 3 and `totalPages` is 1

#### Scenario: Page number too large to reach
- **GIVEN** 3 services are registered, 2 of them owned by `payments-team`
- **WHEN** a client sends `GET /api/v1/services?page=2147483647`, and `GET /api/v1/services?owner=payments-team&page=2147483647`
- **THEN** each response status is 200 OK and `items` is empty, not 500 Internal Server Error
- **AND** `totalItems` is 3 for the first and 2 for the second, and `page` is `2147483647` for both

### Requirement: Filter services by owner
The system SHALL limit the list to services with the given owner when a client adds an `owner` query parameter to `GET /api/v1/services`.

#### Scenario: Lists only the owner's services
- **GIVEN** `orders-api` and `payments-api` are owned by `payments-team`, and `auth-service` is owned by `identity-team`
- **WHEN** a client sends `GET /api/v1/services?owner=payments-team`
- **THEN** `items` lists only `orders-api` and `payments-api`

#### Scenario: Owner with no services
- **WHEN** a client lists services for an owner that owns none
- **THEN** the response status is 200 OK and `items` is empty

### Requirement: Update a service
The system SHALL let a client replace a service's `owner`, `description`, and `lifecycle` with `PUT /api/v1/services/{name}`, applying the same validation as registration. `owner` and `lifecycle` are required, an omitted `description` clears it, and the name cannot change.

#### Scenario: Updates a service
- **GIVEN** `orders-api` is registered with owner `payments-team` and lifecycle `experimental`
- **WHEN** a client sends `PUT /api/v1/services/orders-api` with owner `checkout-team` and lifecycle `production`
- **THEN** the response status is 200 OK and the body shows the new owner and lifecycle
- **AND** `updatedAt` is the time of the update and `createdAt` is unchanged

#### Scenario: Rejects an invalid update
- **GIVEN** `orders-api` is registered
- **WHEN** a client sends an update without a `lifecycle`
- **THEN** the response status is 400 Bad Request and the `errors` list identifies the `lifecycle` field
- **AND** the service is unchanged

#### Scenario: Rejects an update to an unknown service
- **WHEN** a client updates a service name that is not registered
- **THEN** the response status is 404 Not Found

### Requirement: Unregister a service
The system SHALL let a client remove a service from the catalog with `DELETE /api/v1/services/{name}`.

#### Scenario: Removes a service
- **GIVEN** `orders-api` is registered
- **WHEN** a client sends `DELETE /api/v1/services/orders-api`
- **THEN** the response status is 204 No Content
- **AND** `GET /api/v1/services/orders-api` returns 404 Not Found, and the service no longer appears in lists

#### Scenario: Rejects deleting an unknown service
- **WHEN** a client deletes a service name that is not registered
- **THEN** the response status is 404 Not Found

### Requirement: Catalog survives restarts
The system SHALL store the catalog durably, so registered services remain after the application restarts.

#### Scenario: Service is still listed after a restart
- **GIVEN** `orders-api` is registered
- **WHEN** the application is stopped and started again
- **THEN** `GET /api/v1/services/orders-api` returns the service with its original details
