# Service Catalog

## MODIFIED Requirements

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
